import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import { useAuthStore } from '@/stores/auth-store'
import { formatMoney, PaginatedTransactionsSchema, ReceiptSchema } from '@shared-types/openpos'
import type { Transaction, PaginatedResponse } from '@shared-types/openpos'
import { Button } from '@/components/ui/button'
import { ReceiptDialog } from '@/components/receipt-dialog'

export function HistoryPage() {
  const storeId = useAuthStore((s) => s.storeId)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(0)
  const [receiptData, setReceiptData] = useState<string | null>(null)
  const [receiptOpen, setReceiptOpen] = useState(false)

  useEffect(() => {
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

  async function handleViewReceipt(transactionId: string) {
    try {
      const receipt = await api.get(`/api/transactions/${transactionId}/receipt`, ReceiptSchema)
      setReceiptData(receipt.receiptData)
      setReceiptOpen(true)
    } catch {
      // receipt not available
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
                  {tx.status === 'COMPLETED' && (
                    <div className="flex gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="min-h-11 min-w-11"
                        onClick={() => handleViewReceipt(tx.id)}
                      >
                        レシート
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="min-h-11 min-w-11"
                        onClick={() => handleViewReceipt(tx.id)}
                      >
                        領収書発行
                      </Button>
                    </div>
                  )}
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
            className="min-h-11 min-w-11"
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
            className="min-h-11 min-w-11"
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
    </div>
  )
}
