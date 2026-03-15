import { useCallback, useEffect, useRef, useState } from 'react'
import { Header } from '@/components/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { api } from '@/lib/api'
import { formatMoney } from '@shared-types/openpos'
import type { Product, Category, TaxRate, PaginatedResponse } from '@shared-types/openpos'
import { ProductSchema, CategorySchema, TaxRateSchema } from '@shared-types/openpos'
import { PaginatedProductsSchema } from '@shared-types/openpos'
import { z } from 'zod'
import { toast } from '@/hooks/use-toast'

export function ProductsPage() {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [taxRates, setTaxRates] = useState<TaxRate[]>([])
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(0)
  const [search, setSearch] = useState('')
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState<Product | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null)

  const fetchProducts = useCallback(async () => {
    const result = await api.get<PaginatedResponse<Product>>(
      '/api/products',
      PaginatedProductsSchema,
      { params: { page, pageSize: 20, search: search || undefined } },
    )
    setProducts(result.data)
    setTotalPages(result.pagination.totalPages)
  }, [page, search])

  const fetchMasterData = useCallback(async () => {
    const [cats, rates] = await Promise.all([
      api.get('/api/categories', z.array(CategorySchema)),
      api.get('/api/tax-rates', z.array(TaxRateSchema)),
    ])
    setCategories(cats)
    setTaxRates(rates)
  }, [])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  useEffect(() => {
    fetchMasterData()
  }, [fetchMasterData])

  function handleCreate() {
    setEditingProduct(null)
    setDialogOpen(true)
  }

  function handleEdit(product: Product) {
    setEditingProduct(product)
    setDialogOpen(true)
  }

  async function handleDelete(id: string) {
    await api.delete(`/api/products/${id}`)
    fetchProducts()
  }

  async function handleSubmit(data: Record<string, unknown>) {
    if (editingProduct) {
      await api.put(`/api/products/${editingProduct.id}`, data, ProductSchema)
    } else {
      await api.post('/api/products', data, ProductSchema)
    }
    setDialogOpen(false)
    fetchProducts()
  }

  function getCategoryName(id: string | undefined): string {
    if (!id) return '—'
    return categories.find((c) => c.id === id)?.name ?? '—'
  }

  return (
    <>
      <Header title="商品管理" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <div className="flex items-center justify-between gap-4">
          <Input
            placeholder="商品名・バーコード・SKUで検索..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              setPage(1)
            }}
            className="max-w-sm"
          />
          <div className="flex gap-2">
            <CsvImportButton onImported={fetchProducts} />
            <Button onClick={handleCreate}>商品を追加</Button>
          </div>
        </div>

        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>商品名</TableHead>
                <TableHead>バーコード</TableHead>
                <TableHead className="text-right">価格</TableHead>
                <TableHead>カテゴリ</TableHead>
                <TableHead>ステータス</TableHead>
                <TableHead className="w-[120px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {products.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    商品が見つかりません
                  </TableCell>
                </TableRow>
              ) : (
                products.map((product) => (
                  <TableRow key={product.id}>
                    <TableCell className="font-medium">{product.name}</TableCell>
                    <TableCell className="font-mono text-sm">{product.barcode ?? '—'}</TableCell>
                    <TableCell className="text-right">{formatMoney(product.price)}</TableCell>
                    <TableCell>{getCategoryName(product.categoryId ?? undefined)}</TableCell>
                    <TableCell>
                      <Badge variant={product.isActive ? 'default' : 'secondary'}>
                        {product.isActive ? '有効' : '無効'}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1">
                        <Button variant="ghost" size="sm" onClick={() => handleEdit(product)}>
                          編集
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setDeleteTarget(product.id)}
                        >
                          削除
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page <= 1}
              onClick={() => setPage((p) => p - 1)}
            >
              前へ
            </Button>
            <span className="text-sm text-muted-foreground">
              {page} / {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              次へ
            </Button>
          </div>
        )}

        <ProductFormDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          product={editingProduct}
          categories={categories}
          taxRates={taxRates}
          onSubmit={handleSubmit}
        />

        <ConfirmDialog
          open={deleteTarget !== null}
          onOpenChange={(open) => {
            if (!open) setDeleteTarget(null)
          }}
          title="商品を削除"
          onConfirm={() => {
            if (deleteTarget) handleDelete(deleteTarget)
            setDeleteTarget(null)
          }}
        />
      </div>
    </>
  )
}

function CsvImportButton({ onImported }: { onImported: () => void }) {
  const fileRef = useRef<HTMLInputElement>(null)
  const [importing, setImporting] = useState(false)

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return

    setImporting(true)
    try {
      const text = await file.text()
      const lines = text.split('\n').filter((l) => l.trim())
      if (lines.length < 2) {
        toast({ title: 'CSVが空です', variant: 'destructive' })
        return
      }

      const headers = (lines[0] ?? '').split(',').map((h) => h.trim().toLowerCase())
      const nameIdx = headers.indexOf('name')
      const barcodeIdx = headers.indexOf('barcode')
      const priceIdx = headers.indexOf('price')

      if (nameIdx === -1 || priceIdx === -1) {
        toast({
          title: 'CSVに name, price 列が必要です',
          variant: 'destructive',
        })
        return
      }

      let successCount = 0
      let errorCount = 0

      for (let i = 1; i < lines.length; i++) {
        const cols = (lines[i] ?? '').split(',').map((c) => c.trim())
        const name = cols[nameIdx]
        const price = Number(cols[priceIdx])
        if (!name || !Number.isFinite(price)) {
          errorCount++
          continue
        }
        try {
          await api.post(
            '/api/products',
            {
              name,
              price: Math.round(price * 100),
              barcode: barcodeIdx >= 0 ? cols[barcodeIdx] || undefined : undefined,
            },
            ProductSchema,
          )
          successCount++
        } catch {
          errorCount++
        }
      }

      toast({
        title: 'CSVインポート完了',
        description: `成功: ${successCount} 件 / エラー: ${errorCount} 件`,
      })
      onImported()
    } catch {
      toast({ title: 'CSVの読み込みに失敗しました', variant: 'destructive' })
    } finally {
      setImporting(false)
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  return (
    <>
      <input
        ref={fileRef}
        type="file"
        accept=".csv"
        className="hidden"
        onChange={handleFileChange}
      />
      <Button variant="outline" disabled={importing} onClick={() => fileRef.current?.click()}>
        {importing ? 'インポート中...' : 'CSVインポート'}
      </Button>
    </>
  )
}

function ProductFormDialog({
  open,
  onOpenChange,
  product,
  categories,
  taxRates,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  product: Product | null
  categories: Category[]
  taxRates: TaxRate[]
  onSubmit: (data: Record<string, unknown>) => void
}) {
  const [name, setName] = useState('')
  const [price, setPrice] = useState('')
  const [barcode, setBarcode] = useState('')
  const [sku, setSku] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [taxRateId, setTaxRateId] = useState('')
  const [description, setDescription] = useState('')

  useEffect(() => {
    if (product) {
      setName(product.name)
      setPrice(String(product.price / 100))
      setBarcode(product.barcode ?? '')
      setSku(product.sku ?? '')
      setCategoryId(product.categoryId ?? '')
      setTaxRateId(product.taxRateId ?? '')
      setDescription(product.description ?? '')
    } else {
      setName('')
      setPrice('')
      setBarcode('')
      setSku('')
      setCategoryId('')
      setTaxRateId('')
      setDescription('')
    }
  }, [product, open])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const priceInSen = Math.round(Number(price) * 100)
    onSubmit({
      name,
      price: priceInSen,
      barcode: barcode || undefined,
      sku: sku || undefined,
      categoryId: categoryId || undefined,
      taxRateId: taxRateId || undefined,
      description: description || undefined,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{product ? '商品を編集' : '商品を追加'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="name">商品名 *</Label>
            <Input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="price">価格（円） *</Label>
            <Input
              id="price"
              type="number"
              min="0"
              step="1"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="barcode">バーコード</Label>
              <Input id="barcode" value={barcode} onChange={(e) => setBarcode(e.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="sku">SKU</Label>
              <Input id="sku" value={sku} onChange={(e) => setSku(e.target.value)} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="category">カテゴリ</Label>
              <select
                id="category"
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
              >
                <option value="">未設定</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="taxRate">税率</Label>
              <select
                id="taxRate"
                value={taxRateId}
                onChange={(e) => setTaxRateId(e.target.value)}
                className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
              >
                <option value="">未設定</option>
                {taxRates.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="description">説明</Label>
            <Input
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit">{product ? '更新' : '追加'}</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
