import { useCallback, useEffect, useState } from 'react'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/auth-store'
import {
  formatMoney,
  FinalizeTransactionResponseSchema,
  PaginatedTransactionsSchema,
  ReceiptSchema,
  TransactionSchema,
  StockSchema,
} from '@shared-types/openpos'
import type { Transaction, TransactionItem, PaginatedResponse } from '@shared-types/openpos'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { ReceiptDialog } from '@/components/receipt-dialog'
import { toast } from '@/hooks/use-toast'

export function HistoryPage() {
  const { storeId, terminalId, staff } = useAuthStore()
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(0)
  const [receiptData, setReceiptData] = useState<string | null>(null)
  const [receiptOpen, setReceiptOpen] = useState(false)
  const [returnDialogTx, setReturnDialogTx] = useState<Transaction | null>(null)

  const fetchTransactions = useCallback(() => {
    if (!storeId) return
    api
      .get<PaginatedResponse<Transaction>>('/api/transactions', PaginatedTransactionsSchema, {
        params: { storeId, page, pageSize: 20 },
      })
      .then((result) => {
        setTransactions(result.data)
        setTotalPages(result.pagination.totalPages)
      })
  }, [storeId, page])

  useEffect(() => {
    fetchTransactions()
  }, [fetchTransactions])

  async function handleViewReceipt(transactionId: string) {
    try {
      const receipt = await api.get(`/api/transactions/${transactionId}/receipt`, ReceiptSchema)
      setReceiptData(receipt.receiptData)
      setReceiptOpen(true)
    } catch {
      // receipt not available
    }
  }

  async function handleReissueReceipt(transactionId: string) {
    try {
      const receipt = await api.get(`/api/transactions/${transactionId}/receipt`, ReceiptSchema)
      setReceiptData(receipt.receiptData)
      setReceiptOpen(true)
      toast({ title: 'レシートを再発行しました' })
    } catch {
      toast({ title: 'レシートの取得に失敗しました', variant: 'destructive' })
    }
  }

  async function handleReturnItem(tx: Transaction, item: TransactionItem) {
    if (!storeId || !terminalId || !staff) return
    try {
      const returnTx = await api.post(
        '/api/transactions',
        {
          storeId,
          terminalId,
          staffId: staff.id,
          type: 'RETURN',
        },
        TransactionSchema,
      )

      await api.post(
        `/api/transactions/${returnTx.id}/items`,
        { productId: item.productId, quantity: item.quantity },
        TransactionSchema,
      )

      await api.post(
        `/api/transactions/${returnTx.id}/finalize`,
        {
          payments: [{ method: 'CASH', amount: item.total, received: 0 }],
        },
        TransactionSchema.or(FinalizeTransactionResponseSchema),
      )

      // Adjust inventory (add stock back)
      try {
        await api.post(
          '/api/inventory/stocks/adjust',
          {
            storeId,
            productId: item.productId,
            quantityChange: item.quantity,
            movementType: 'RETURN',
            referenceId: returnTx.id,
            note: `返品: ${item.productName}`,
          },
          StockSchema,
        )
      } catch {
        // inventory adjust failure is not critical
      }

      toast({ title: `${item.productName} の返品処理が完了しました` })
      setReturnDialogTx(null)
      fetchTransactions()
    } catch (err) {
      const message = err instanceof Error ? err.message : '返品処理に失敗しました'
      toast({ title: 'エラー', description: message, variant: 'destructive' })
    }
  }

  return (
    <div className="flex flex-1 flex-col gap-4 p-4">
      <h2 className="text-lg font-semibold">取引履歴</h2>

      <div className="overflow-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead className="border-b bg-muted/50">
            <tr>
              <th className="p-3 text-left font-medium">取引番号</th>
              <th className="p-3 text-left font-medium">日時</th>
              <th className="p-3 text-right font-medium">合計</th>
              <th className="p-3 text-left font-medium">ステータス</th>
              <th className="p-3 text-left font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((tx) => (
              <tr key={tx.id} className="border-b last:border-0">
                <td className="p-3 font-mono text-xs">{tx.transactionNumber}</td>
                <td className="p-3 text-xs">{new Date(tx.createdAt).toLocaleString('ja-JP')}</td>
                <td className="p-3 text-right font-medium">{formatMoney(tx.total)}</td>
                <td className="p-3">
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs ${
                      tx.status === 'COMPLETED'
                        ? 'bg-green-100 text-green-800'
                        : tx.status === 'VOIDED'
                          ? 'bg-red-100 text-red-800'
                          : 'bg-yellow-100 text-yellow-800'
                    }`}
                  >
                    {tx.status === 'COMPLETED'
                      ? '完了'
                      : tx.status === 'VOIDED'
                        ? '取消'
                        : '下書き'}
                  </span>
                </td>
                <td className="p-3">
                  <div className="flex gap-1">
                    {tx.status === 'COMPLETED' && (
                      <>
                        <Button variant="ghost" size="sm" onClick={() => handleViewReceipt(tx.id)}>
                          レシート
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleReissueReceipt(tx.id)}
                        >
                          再発行
                        </Button>
                        {tx.type !== 'RETURN' && (
                          <Button variant="ghost" size="sm" onClick={() => setReturnDialogTx(tx)}>
                            返品
                          </Button>
                        )}
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {transactions.length === 0 && (
              <tr>
                <td colSpan={5} className="p-8 text-center text-muted-foreground">
                  取引履歴がありません
                </td>
              </tr>
            )}
          </tbody>
        </table>
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

      <ReceiptDialog
        open={receiptOpen}
        receiptData={receiptData}
        onClose={() => {
          setReceiptOpen(false)
          setReceiptData(null)
        }}
      />

      <ReturnItemDialog
        open={returnDialogTx !== null}
        onOpenChange={(open) => {
          if (!open) setReturnDialogTx(null)
        }}
        transaction={returnDialogTx}
        onReturnItem={handleReturnItem}
      />
    </div>
  )
}

function ReturnItemDialog({
  open,
  onOpenChange,
  transaction,
  onReturnItem,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  transaction: Transaction | null
  onReturnItem: (tx: Transaction, item: TransactionItem) => void
}) {
  if (!transaction) return null

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg max-h-[70vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>部分返品 — {transaction.transactionNumber}</DialogTitle>
        </DialogHeader>
        <p className="text-sm text-muted-foreground">返品する商品を選択してください</p>
        <div className="space-y-2">
          {transaction.items.map((item) => (
            <div key={item.id} className="flex items-center justify-between rounded-lg border p-3">
              <div>
                <p className="text-sm font-medium">{item.productName}</p>
                <p className="text-xs text-muted-foreground">
                  {formatMoney(item.unitPrice)} x {item.quantity} = {formatMoney(item.total)}
                </p>
              </div>
              <Button variant="outline" size="sm" onClick={() => onReturnItem(transaction, item)}>
                返品
              </Button>
            </div>
          ))}
        </div>
        <div className="flex justify-end">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            閉じる
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
