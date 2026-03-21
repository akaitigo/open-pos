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
import {
  PaginatedPurchaseOrdersSchema,
  PurchaseOrderSchema,
  PaginatedProductsSchema,
  formatMoney,
} from '@shared-types/openpos'
import type { PurchaseOrder, PaginatedResponse, Product } from '@shared-types/openpos'
import { toast } from '@/hooks/use-toast'

const STATUS_LABELS: Record<string, string> = {
  DRAFT: '下書き',
  ORDERED: '発注済み',
  RECEIVED: '入荷済み',
  CANCELLED: 'キャンセル',
}

const STATUS_VARIANTS: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  DRAFT: 'outline',
  ORDERED: 'secondary',
  RECEIVED: 'default',
  CANCELLED: 'destructive',
}

export function PurchaseOrdersPage() {
  const [orders, setOrders] = useState<PurchaseOrder[]>([])
  const [products, setProducts] = useState<Record<string, Product>>({})
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(0)
  const [storeId, setStoreId] = useState('')
  const [createDialogOpen, setCreateDialogOpen] = useState(false)

  const fetchProducts = useCallback(async () => {
    const result = await api.get<PaginatedResponse<Product>>(
      '/api/products',
      PaginatedProductsSchema,
      { params: { page: 1, pageSize: 200 } },
    )
    const map: Record<string, Product> = {}
    for (const p of result.data) {
      map[p.id] = p
    }
    setProducts(map)
  }, [])

  const fetchOrders = useCallback(async () => {
    if (!storeId) return
    const result = await api.get<PaginatedResponse<PurchaseOrder>>(
      '/api/inventory/purchase-orders',
      PaginatedPurchaseOrdersSchema,
      { params: { storeId, page, pageSize: 20 } },
    )
    setOrders(result.data)
    setTotalPages(result.pagination.totalPages)
  }, [storeId, page])

  useEffect(() => {
    fetchProducts().catch((err: unknown) => {
      const message = err instanceof Error ? err.message : '商品の取得に失敗しました'
      toast({ title: 'エラー', description: message, variant: 'destructive' })
    })
  }, [fetchProducts])

  useEffect(() => {
    fetchOrders().catch((err: unknown) => {
      const message = err instanceof Error ? err.message : '発注の取得に失敗しました'
      toast({ title: 'エラー', description: message, variant: 'destructive' })
    })
  }, [fetchOrders])

  async function handleStatusChange(orderId: string, newStatus: string) {
    try {
      await api.put(
        `/api/inventory/purchase-orders/${orderId}/status`,
        { status: newStatus },
        PurchaseOrderSchema,
      )
      fetchOrders()
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'ステータスの更新に失敗しました'
      toast({ title: 'エラー', description: message, variant: 'destructive' })
    }
  }

  async function handleCreateSubmit(data: {
    supplierName: string
    note: string
    items: Array<{ productId: string; orderedQuantity: number; unitCost: number }>
  }) {
    try {
      await api.post('/api/inventory/purchase-orders', { storeId, ...data }, PurchaseOrderSchema)
      setCreateDialogOpen(false)
      fetchOrders()
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '発注の作成に失敗しました'
      toast({ title: 'エラー', description: message, variant: 'destructive' })
    }
  }

  return (
    <>
      <Header title="発注管理" />
      <div className="flex flex-1 flex-col gap-4 p-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex flex-col gap-1">
            <Label htmlFor="poStoreId" className="text-xs">
              店舗ID
            </Label>
            <Input
              id="poStoreId"
              placeholder="店舗IDを入力..."
              value={storeId}
              onChange={(e) => {
                setStoreId(e.target.value)
                setPage(1)
              }}
              className="w-[300px]"
            />
          </div>
          <Button onClick={() => setCreateDialogOpen(true)} disabled={!storeId}>
            新規発注
          </Button>
        </div>

        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>発注ID</TableHead>
                <TableHead>サプライヤー</TableHead>
                <TableHead>品目数</TableHead>
                <TableHead>ステータス</TableHead>
                <TableHead>作成日時</TableHead>
                <TableHead className="w-[200px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {orders.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    {storeId ? '発注データが見つかりません' : '店舗IDを入力してください'}
                  </TableCell>
                </TableRow>
              ) : (
                orders.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell className="font-mono text-xs">{order.id.slice(0, 8)}...</TableCell>
                    <TableCell>{order.supplierName}</TableCell>
                    <TableCell>{order.items.length} 品目</TableCell>
                    <TableCell>
                      <Badge variant={STATUS_VARIANTS[order.status] ?? 'outline'}>
                        {STATUS_LABELS[order.status] ?? order.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-xs">
                      {new Date(order.createdAt).toLocaleString('ja-JP')}
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1">
                        {order.status === 'DRAFT' && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleStatusChange(order.id, 'ORDERED')}
                          >
                            発注確定
                          </Button>
                        )}
                        {order.status === 'ORDERED' && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleStatusChange(order.id, 'RECEIVED')}
                          >
                            入荷確認
                          </Button>
                        )}
                        {order.status === 'DRAFT' && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleStatusChange(order.id, 'CANCELLED')}
                          >
                            キャンセル
                          </Button>
                        )}
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

        <CreatePurchaseOrderDialog
          open={createDialogOpen}
          onOpenChange={setCreateDialogOpen}
          products={products}
          onSubmit={handleCreateSubmit}
        />
      </div>
    </>
  )
}

function CreatePurchaseOrderDialog({
  open,
  onOpenChange,
  products,
  onSubmit,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  products: Record<string, Product>
  onSubmit: (data: {
    supplierName: string
    note: string
    items: Array<{ productId: string; orderedQuantity: number; unitCost: number }>
  }) => void
}) {
  const [supplierName, setSupplierName] = useState('')
  const [note, setNote] = useState('')
  const [items, setItems] = useState<
    Array<{ productId: string; orderedQuantity: string; unitCost: string }>
  >([{ productId: '', orderedQuantity: '', unitCost: '' }])

  useEffect(() => {
    if (open) {
      setSupplierName('')
      setNote('')
      setItems([{ productId: '', orderedQuantity: '', unitCost: '' }])
    }
  }, [open])

  function addItem() {
    setItems((prev) => [...prev, { productId: '', orderedQuantity: '', unitCost: '' }])
  }

  function removeItem(index: number) {
    setItems((prev) => prev.filter((_, i) => i !== index))
  }

  function updateItem(index: number, field: string, value: string) {
    setItems((prev) => prev.map((item, i) => (i === index ? { ...item, [field]: value } : item)))
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const parsedItems = items
      .filter((item) => item.productId && item.orderedQuantity)
      .map((item) => ({
        productId: item.productId,
        orderedQuantity: Number.parseInt(item.orderedQuantity, 10),
        unitCost: Math.round(Number(item.unitCost || '0') * 100),
      }))
    if (parsedItems.length === 0) return
    onSubmit({ supplierName, note, items: parsedItems })
  }

  const productList = Object.values(products)

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>新規発注</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="supplier">サプライヤー名 *</Label>
            <Input
              id="supplier"
              value={supplierName}
              onChange={(e) => setSupplierName(e.target.value)}
              required
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="poNote">備考</Label>
            <Input id="poNote" value={note} onChange={(e) => setNote(e.target.value)} />
          </div>

          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <Label>発注品目</Label>
              <Button type="button" variant="outline" size="sm" onClick={addItem}>
                品目を追加
              </Button>
            </div>
            {items.map((item, index) => (
              <div key={index} className="flex items-end gap-2 rounded-lg border p-3">
                <div className="flex flex-1 flex-col gap-1">
                  <Label className="text-xs">商品</Label>
                  <select
                    value={item.productId}
                    onChange={(e) => updateItem(index, 'productId', e.target.value)}
                    className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
                    required
                  >
                    <option value="">選択...</option>
                    {productList.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} ({formatMoney(p.price)})
                      </option>
                    ))}
                  </select>
                </div>
                <div className="flex w-20 flex-col gap-1">
                  <Label className="text-xs">数量</Label>
                  <Input
                    type="number"
                    min="1"
                    value={item.orderedQuantity}
                    onChange={(e) => updateItem(index, 'orderedQuantity', e.target.value)}
                    required
                  />
                </div>
                <div className="flex w-24 flex-col gap-1">
                  <Label className="text-xs">仕入単価（円）</Label>
                  <Input
                    type="number"
                    min="0"
                    step="1"
                    value={item.unitCost}
                    onChange={(e) => updateItem(index, 'unitCost', e.target.value)}
                  />
                </div>
                {items.length > 1 && (
                  <Button type="button" variant="ghost" size="sm" onClick={() => removeItem(index)}>
                    削除
                  </Button>
                )}
              </div>
            ))}
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit">発注を作成</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
