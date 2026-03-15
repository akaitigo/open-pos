import { useCallback, useEffect, useState } from 'react'
import { Header } from '@/components/header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { api } from '@/lib/api'
import { PaginatedStocksSchema, StockSchema } from '@shared-types/openpos'
import type { Stock, PaginatedResponse, Product } from '@shared-types/openpos'
import { PaginatedProductsSchema } from '@shared-types/openpos'

type StockStatus = 'normal' | 'low' | 'out'

function getStockStatus(stock: Stock): StockStatus {
  if (stock.quantity <= 0) return 'out'
  if (stock.quantity <= stock.lowStockThreshold) return 'low'
  return 'normal'
}

function getStatusBadge(status: StockStatus) {
  switch (status) {
    case 'out':
      return <Badge variant="destructive">在庫切れ</Badge>
    case 'low':
      return (
        <Badge variant="secondary" className="bg-red-100 text-red-800">
          在庫低下
        </Badge>
      )
    case 'normal':
      return <Badge variant="outline">正常</Badge>
  }
}

export function InventoryPage() {
  const [stocks, setStocks] = useState<Stock[]>([])
  const [products, setProducts] = useState<Record<string, Product>>({})
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(0)
  const [storeId, setStoreId] = useState('')
  const [lowStockOnly, setLowStockOnly] = useState(false)
  const [adjustDialogOpen, setAdjustDialogOpen] = useState(false)
  const [selectedStock, setSelectedStock] = useState<Stock | null>(null)

  const fetchProducts = useCallback(async () => {
    const result = await api.get<PaginatedResponse<Product>>(
      '/api/products',
      PaginatedProductsSchema,
      { params: { page: 1, pageSize: 200 } },
    )
    const productMap: Record<string, Product> = {}
    for (const p of result.data) {
      productMap[p.id] = p
    }
    setProducts(productMap)
  }, [])

  const fetchStocks = useCallback(async () => {
    if (!storeId) return
    const result = await api.get<PaginatedResponse<Stock>>(
      '/api/inventory/stocks',
      PaginatedStocksSchema,
      { params: { storeId, page, pageSize: 20, lowStockOnly } },
    )
    setStocks(result.data)
    setTotalPages(result.pagination.totalPages)
  }, [storeId, page, lowStockOnly])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  useEffect(() => {
    fetchStocks()
  }, [fetchStocks])

  function handleAdjust(stock: Stock) {
    setSelectedStock(stock)
    setAdjustDialogOpen(true)
  }

  async function handleAdjustSubmit(data: { quantityChange: number; note: string }) {
    if (!selectedStock) return
    await api.post(
      '/api/inventory/stocks/adjust',
      {
        storeId: selectedStock.storeId,
        productId: selectedStock.productId,
        quantityChange: data.quantityChange,
        movementType: 'ADJUSTMENT',
        note: data.note,
      },
      StockSchema,
    )
    setAdjustDialogOpen(false)
    fetchStocks()
  }

  return (
    <>
      <Header title="在庫管理" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex flex-col gap-1">
            <Label htmlFor="storeId" className="text-xs">
              店舗ID
            </Label>
            <Input
              id="storeId"
              placeholder="店舗IDを入力..."
              value={storeId}
              onChange={(e) => {
                setStoreId(e.target.value)
                setPage(1)
              }}
              className="w-[300px]"
            />
          </div>
          <div className="flex items-end gap-2">
            <Button
              variant={lowStockOnly ? 'default' : 'outline'}
              size="sm"
              onClick={() => {
                setLowStockOnly(!lowStockOnly)
                setPage(1)
              }}
            >
              在庫低下のみ表示
            </Button>
          </div>
        </div>

        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>商品名</TableHead>
                <TableHead>SKU</TableHead>
                <TableHead className="text-right">現在在庫数</TableHead>
                <TableHead className="text-right">アラート閾値</TableHead>
                <TableHead>ステータス</TableHead>
                <TableHead className="w-[100px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {stocks.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    {storeId ? '在庫データが見つかりません' : '店舗IDを入力してください'}
                  </TableCell>
                </TableRow>
              ) : (
                stocks.map((stock) => {
                  const product = products[stock.productId]
                  const status = getStockStatus(stock)
                  return (
                    <TableRow
                      key={stock.id}
                      className={status === 'out' || status === 'low' ? 'bg-red-50/50' : ''}
                    >
                      <TableCell className="font-medium">
                        {product?.name ?? stock.productId}
                      </TableCell>
                      <TableCell className="font-mono text-sm">{product?.sku ?? '—'}</TableCell>
                      <TableCell className="text-right">{stock.quantity}</TableCell>
                      <TableCell className="text-right">{stock.lowStockThreshold}</TableCell>
                      <TableCell>{getStatusBadge(status)}</TableCell>
                      <TableCell>
                        <Button variant="ghost" size="sm" onClick={() => handleAdjust(stock)}>
                          調整
                        </Button>
                      </TableCell>
                    </TableRow>
                  )
                })
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

        <StockAdjustDialog
          open={adjustDialogOpen}
          onOpenChange={setAdjustDialogOpen}
          stock={selectedStock}
          productName={selectedStock ? (products[selectedStock.productId]?.name ?? '') : ''}
          onSubmit={handleAdjustSubmit}
        />
      </div>
    </>
  )
}

function StockAdjustDialog({
  open,
  onOpenChange,
  stock,
  productName,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  stock: Stock | null
  productName: string
  onSubmit: (data: { quantityChange: number; note: string }) => void
}) {
  const [adjustType, setAdjustType] = useState<'increase' | 'decrease'>('increase')
  const [quantity, setQuantity] = useState('')
  const [note, setNote] = useState('')

  useEffect(() => {
    if (open) {
      setAdjustType('increase')
      setQuantity('')
      setNote('')
    }
  }, [open])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const qty = Number.parseInt(quantity, 10)
    if (!Number.isFinite(qty) || qty <= 0) return
    onSubmit({
      quantityChange: adjustType === 'increase' ? qty : -qty,
      note,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>在庫調整 — {productName}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="text-sm text-muted-foreground">
            現在の在庫数: <span className="font-semibold">{stock?.quantity ?? 0}</span>
          </div>
          <div className="flex gap-2">
            <Button
              type="button"
              variant={adjustType === 'increase' ? 'default' : 'outline'}
              size="sm"
              onClick={() => setAdjustType('increase')}
            >
              増加
            </Button>
            <Button
              type="button"
              variant={adjustType === 'decrease' ? 'default' : 'outline'}
              size="sm"
              onClick={() => setAdjustType('decrease')}
            >
              減少
            </Button>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="qty">数量</Label>
            <Input
              id="qty"
              type="number"
              min="1"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              required
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="reason">理由</Label>
            <Input
              id="reason"
              placeholder="棚卸・廃棄・入荷など"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              required
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit">調整を実行</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
