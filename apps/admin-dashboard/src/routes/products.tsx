import { useCallback, useEffect, useState } from 'react'
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
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { ConfirmDialog } from '@/components/confirm-dialog'
import { api } from '@/lib/api'
import { formatMoney } from '@shared-types/openpos'
import type { Product, Category, TaxRate, PaginatedResponse } from '@shared-types/openpos'
import { ProductSchema, CategorySchema, TaxRateSchema } from '@shared-types/openpos'
import { PaginatedProductsSchema } from '@shared-types/openpos'
import { z } from 'zod'

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
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [bulkDeleteOpen, setBulkDeleteOpen] = useState(false)

  const fetchProducts = useCallback(async () => {
    const result = await api.get<PaginatedResponse<Product>>(
      '/api/products',
      PaginatedProductsSchema,
      { params: { page, pageSize: 20, search: search || undefined } },
    )
    setProducts(result.data)
    setTotalPages(result.pagination.totalPages)
    setSelectedIds(new Set())
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

  async function handleBulkDelete() {
    await Promise.all(Array.from(selectedIds).map((id) => api.delete(`/api/products/${id}`)))
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
    if (!id) return '\u2014'
    return categories.find((c) => c.id === id)?.name ?? '\u2014'
  }

  function toggleSelect(id: string) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  function toggleSelectAll() {
    if (selectedIds.size === products.length) {
      setSelectedIds(new Set())
    } else {
      setSelectedIds(new Set(products.map((p) => p.id)))
    }
  }

  const allSelected = products.length > 0 && selectedIds.size === products.length
  const someSelected = selectedIds.size > 0

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
          <div className="flex items-center gap-2">
            {someSelected && (
              <Button variant="destructive" size="sm" onClick={() => setBulkDeleteOpen(true)}>
                一括削除（{selectedIds.size}件）
              </Button>
            )}
            <Button onClick={handleCreate}>商品を追加</Button>
          </div>
        </div>

        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-[40px]">
                  <input
                    type="checkbox"
                    checked={allSelected}
                    onChange={toggleSelectAll}
                    className="h-4 w-4 rounded border-gray-300"
                    aria-label="すべて選択"
                  />
                </TableHead>
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
                  <TableCell colSpan={7} className="text-center text-muted-foreground">
                    商品が見つかりません
                  </TableCell>
                </TableRow>
              ) : (
                products.map((product) => (
                  <TableRow key={product.id}>
                    <TableCell>
                      <input
                        type="checkbox"
                        checked={selectedIds.has(product.id)}
                        onChange={() => toggleSelect(product.id)}
                        className="h-4 w-4 rounded border-gray-300"
                        aria-label={`${product.name}を選択`}
                      />
                    </TableCell>
                    <TableCell className="font-medium">{product.name}</TableCell>
                    <TableCell className="font-mono text-sm">
                      {product.barcode ?? '\u2014'}
                    </TableCell>
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
          description="本当に削除しますか？この操作は取り消せません。"
          onConfirm={() => {
            if (deleteTarget) handleDelete(deleteTarget)
            setDeleteTarget(null)
          }}
        />

        <ConfirmDialog
          open={bulkDeleteOpen}
          onOpenChange={setBulkDeleteOpen}
          title="商品を一括削除"
          description={`選択した${selectedIds.size}件の商品を削除しますか？この操作は取り消せません。`}
          onConfirm={handleBulkDelete}
        />
      </div>
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
