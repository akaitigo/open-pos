import { useEffect, useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Separator } from '@/components/ui/separator'
import { useCartStore, getCartSubtotal } from '@/stores/cart-store'
import { useAuthStore } from '@/stores/auth-store'
import { api } from '@/lib/api'
import {
  formatMoney,
  TaxRateSchema,
  TransactionSchema,
  FinalizeTransactionResponseSchema,
} from '@shared-types/openpos'
import type { TaxRate } from '@shared-types/openpos'
import { toast } from '@/hooks/use-toast'
import { ReceiptDialog } from '@/components/receipt-dialog'
import { getCartEstimatedTotal } from '@/lib/cart-totals'
import { Loader2 } from 'lucide-react'
import { z } from 'zod'

interface CheckoutDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function CheckoutDialog({ open, onOpenChange }: CheckoutDialogProps) {
  const items = useCartStore((s) => s.items)
  const clearCart = useCartStore((s) => s.clearCart)
  const { storeId, terminalId, staff } = useAuthStore()

  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [receivedAmount, setReceivedAmount] = useState('')
  const [reference, setReference] = useState('')
  const [processing, setProcessing] = useState(false)
  const [receiptData, setReceiptData] = useState<string | null>(null)
  const [receiptOpen, setReceiptOpen] = useState(false)
  const [taxRates, setTaxRates] = useState<TaxRate[]>([])

  useEffect(() => {
    let cancelled = false

    api
      .get('/api/tax-rates', z.array(TaxRateSchema))
      .then((result) => {
        if (!cancelled) setTaxRates(result)
      })
      .catch(() => {
        if (!cancelled) setTaxRates([])
      })

    return () => {
      cancelled = true
    }
  }, [])

  const subtotal = getCartSubtotal(items)
  const total = getCartEstimatedTotal(items, taxRates)
  const taxTotal = total - subtotal

  const minimumCashReceived = Math.ceil(total / 100) * 100
  const parsedReceivedAmount = receivedAmount ? Number(receivedAmount) : 0
  const received =
    Number.isFinite(parsedReceivedAmount) && parsedReceivedAmount > 0
      ? Math.round(parsedReceivedAmount * 100)
      : 0
  const change =
    paymentMethod === 'CASH' && received > minimumCashReceived ? received - minimumCashReceived : 0
  const canFinalize = paymentMethod === 'CASH' ? received >= minimumCashReceived : true

  const presets = [
    { label: 'ぴったり', value: minimumCashReceived },
    { label: '\u00a51,000', value: 100000 },
    { label: '\u00a52,000', value: 200000 },
    { label: '\u00a55,000', value: 500000 },
    { label: '\u00a510,000', value: 1000000 },
  ]

  async function handleFinalize() {
    if (!storeId || !terminalId || !staff) return
    setProcessing(true)
    try {
      const tx = await api.post(
        '/api/transactions',
        { storeId, terminalId, staffId: staff.id },
        TransactionSchema,
      )

      for (const item of items) {
        await api.post(
          `/api/transactions/${tx.id}/items`,
          { productId: item.product.id, quantity: item.quantity },
          TransactionSchema,
        )
      }

      const payment = {
        method: paymentMethod,
        amount: total,
        ...(paymentMethod === 'CASH' && received > 0 ? { received } : {}),
        ...(reference ? { reference } : {}),
      }

      const result = await api.post(
        `/api/transactions/${tx.id}/finalize`,
        { payments: [payment] },
        FinalizeTransactionResponseSchema,
      )

      setReceiptData(result.receipt.receiptData)
      setReceiptOpen(true)
      onOpenChange(false)
    } catch (err) {
      const message = err instanceof Error ? err.message : '決済処理に失敗しました'
      toast({ title: 'エラー', description: message, variant: 'destructive' })
    } finally {
      setProcessing(false)
    }
  }

  function handleReceiptClose() {
    setReceiptOpen(false)
    setReceiptData(null)
    clearCart()
    setReceivedAmount('')
    setReference('')
    setPaymentMethod('CASH')
  }

  function handleClose(isOpen: boolean) {
    if (!processing) {
      onOpenChange(isOpen)
      if (!isOpen) {
        setReceivedAmount('')
        setReference('')
        setPaymentMethod('CASH')
      }
    }
  }

  return (
    <>
      <Dialog open={open} onOpenChange={handleClose}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>お会計</DialogTitle>
          </DialogHeader>

          <div className="text-center">
            <p className="text-sm text-muted-foreground">合計金額</p>
            <p className="text-3xl font-bold">{formatMoney(total)}</p>
            <p className="mt-1 text-sm text-muted-foreground">
              小計 {formatMoney(subtotal)} / 税額 {formatMoney(taxTotal)}
            </p>
          </div>

          <Separator />

          <Tabs value={paymentMethod} onValueChange={setPaymentMethod}>
            <TabsList className="w-full">
              <TabsTrigger value="CASH" className="flex-1">
                現金
              </TabsTrigger>
              <TabsTrigger value="CREDIT_CARD" className="flex-1">
                カード
              </TabsTrigger>
              <TabsTrigger value="QR_CODE" className="flex-1">
                QR
              </TabsTrigger>
            </TabsList>

            <TabsContent value="CASH" className="space-y-3">
              <div>
                <label className="text-sm font-medium">お預かり金額（円）</label>
                <Input
                  type="number"
                  placeholder="0"
                  value={receivedAmount}
                  onChange={(e) => setReceivedAmount(e.target.value)}
                  className="mt-1"
                />
              </div>
              <div className="flex flex-wrap gap-2">
                {presets.map((preset) => (
                  <Button
                    key={preset.label}
                    variant="outline"
                    size="sm"
                    onClick={() => setReceivedAmount(String(preset.value / 100))}
                  >
                    {preset.label}
                  </Button>
                ))}
              </div>
              {received > 0 && received >= minimumCashReceived && (
                <div className="rounded-lg bg-muted p-3 text-center">
                  <p className="text-sm text-muted-foreground">お釣り</p>
                  <p className="text-xl font-bold">{formatMoney(change)}</p>
                </div>
              )}
            </TabsContent>

            <TabsContent value="CREDIT_CARD" className="space-y-3">
              <div>
                <label className="text-sm font-medium">参照番号（任意）</label>
                <Input
                  placeholder="カード承認番号"
                  value={reference}
                  onChange={(e) => setReference(e.target.value)}
                  className="mt-1"
                />
              </div>
            </TabsContent>

            <TabsContent value="QR_CODE" className="space-y-3">
              <div>
                <label className="text-sm font-medium">参照番号（任意）</label>
                <Input
                  placeholder="決済ID"
                  value={reference}
                  onChange={(e) => setReference(e.target.value)}
                  className="mt-1"
                />
              </div>
            </TabsContent>
          </Tabs>

          <DialogFooter>
            <Button
              className="w-full"
              size="lg"
              disabled={processing || !canFinalize}
              onClick={handleFinalize}
            >
              {processing ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  処理中...
                </>
              ) : (
                'お会計を確定'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ReceiptDialog open={receiptOpen} receiptData={receiptData} onClose={handleReceiptClose} />
    </>
  )
}
