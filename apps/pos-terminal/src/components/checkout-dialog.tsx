import { useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Separator } from '@/components/ui/separator'
import { Badge } from '@/components/ui/badge'
import { useCartStore, getCartSubtotal } from '@/stores/cart-store'
import { useAuthStore } from '@/stores/auth-store'
import { api } from '@/lib/api'
import {
  FinalizeTransactionResponseSchema,
  formatMoney,
  TransactionSchema,
} from '@shared-types/openpos'
import { toast } from '@/hooks/use-toast'
import { ReceiptDialog } from '@/components/receipt-dialog'
import { useTaxRates } from '@/hooks/use-tax-rates'
import { getCartEstimatedTotal } from '@/lib/cart-totals'
import { CreditCard, Loader2, QrCode, Receipt, Wallet, X } from 'lucide-react'

interface CheckoutDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

type PaymentMethod = 'CASH' | 'CREDIT_CARD' | 'QR_CODE'

interface PaymentEntry {
  id: string
  method: PaymentMethod
  amount: number
  reference?: string
  received?: number
}

const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: '現金',
  CREDIT_CARD: 'カード',
  QR_CODE: 'QR',
}

const QUICK_CASH_AMOUNTS = [100, 500, 1000, 5000, 10000]
const CASH_KEYPAD_ROWS = [
  ['1', '2', '3'],
  ['4', '5', '6'],
  ['7', '8', '9'],
  ['00', '0', '⌫'],
]

function parseYenInput(value: string): number {
  const digits = value.replace(/[^\d]/g, '')
  if (!digits) return 0
  return Number.parseInt(digits, 10) * 100
}

function ceilToWholeYen(amount: number): number {
  if (amount <= 0) return 0
  return Math.ceil(amount / 100) * 100
}

function normalizeNumericInput(value: string): string {
  const digits = value.replace(/[^\d]/g, '')
  if (!digits) return ''
  return String(Number.parseInt(digits, 10))
}

function createPaymentEntryId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export function CheckoutDialog({ open, onOpenChange }: CheckoutDialogProps) {
  const items = useCartStore((s) => s.items)
  const clearCart = useCartStore((s) => s.clearCart)
  const { storeId, terminalId, staff } = useAuthStore()

  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH')
  const [paymentAmount, setPaymentAmount] = useState('')
  const [receivedAmount, setReceivedAmount] = useState('')
  const [reference, setReference] = useState('')
  const [addedPayments, setAddedPayments] = useState<PaymentEntry[]>([])
  const [processing, setProcessing] = useState(false)
  const [receiptData, setReceiptData] = useState<string | null>(null)
  const [receiptOpen, setReceiptOpen] = useState(false)
  const taxRates = useTaxRates()

  const subtotal = getCartSubtotal(items)
  const total = getCartEstimatedTotal(items, taxRates)
  const taxTotal = total - subtotal

  const paidAmount = addedPayments.reduce((sum, payment) => sum + payment.amount, 0)
  const remainingAmount = Math.max(total - paidAmount, 0)
  const roundedRemainingAmount = ceilToWholeYen(remainingAmount)
  const parsedPaymentAmount = parseYenInput(paymentAmount)
  const parsedReceivedAmount = parseYenInput(receivedAmount)

  const currentCoverageAmount =
    paymentMethod === 'CASH'
      ? parsedReceivedAmount >= roundedRemainingAmount
        ? remainingAmount
        : 0
      : Math.min(parsedPaymentAmount, remainingAmount)
  const paymentTotalWithCurrent = paidAmount + currentCoverageAmount
  const remainingAfterCurrent = Math.max(total - paymentTotalWithCurrent, 0)
  const currentChange =
    paymentMethod === 'CASH' && parsedReceivedAmount > roundedRemainingAmount
      ? parsedReceivedAmount - roundedRemainingAmount
      : 0
  const cashShortfall =
    paymentMethod === 'CASH'
      ? Math.max(roundedRemainingAmount - parsedReceivedAmount, 0)
      : 0
  const nonCashShortfall =
    paymentMethod !== 'CASH'
      ? Math.max(total - (paidAmount + parsedPaymentAmount), 0)
      : 0
  const nonCashOverpayment =
    paymentMethod !== 'CASH'
      ? Math.max(parsedPaymentAmount - remainingAmount, 0)
      : 0
  const canAddCurrentPayment =
    paymentMethod !== 'CASH' &&
    parsedPaymentAmount > 0 &&
    parsedPaymentAmount <= remainingAmount &&
    reference.trim().length > 0 &&
    paidAmount < total
  const canFinalize =
    items.length > 0 &&
    (paidAmount >= total ||
      (paymentMethod === 'CASH'
        ? remainingAmount > 0 && parsedReceivedAmount >= roundedRemainingAmount
        : parsedPaymentAmount > 0 &&
          parsedPaymentAmount <= remainingAmount &&
          paidAmount + parsedPaymentAmount >= total))

  async function handleFinalize() {
    if (!storeId || !terminalId || !staff) return

    const payments: Array<{
      method: PaymentMethod
      amount: number
      reference?: string
      received?: number
    }> = addedPayments.map(({ method, amount, reference: paymentReference, received }) => ({
      method,
      amount,
      ...(paymentReference ? { reference: paymentReference } : {}),
      ...(received != null ? { received } : {}),
    }))

    if (payments.reduce((sum, payment) => sum + payment.amount, 0) < total) {
      if (paymentMethod === 'CASH') {
        if (remainingAmount <= 0 || parsedReceivedAmount < roundedRemainingAmount) return
        payments.push({
          method: 'CASH',
          amount: remainingAmount,
          received: parsedReceivedAmount,
        })
      } else {
        if (parsedPaymentAmount <= 0 || parsedPaymentAmount > remainingAmount) return
        payments.push({
          method: paymentMethod,
          amount: parsedPaymentAmount,
          ...(reference.trim() ? { reference: reference.trim() } : {}),
        })
      }
    }

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

      const result = await api.post(
        `/api/transactions/${tx.id}/finalize`,
        { payments },
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

  function resetDialogState() {
    setPaymentMethod('CASH')
    setPaymentAmount('')
    setReceivedAmount('')
    setReference('')
    setAddedPayments([])
  }

  function handleReceiptClose() {
    setReceiptOpen(false)
    setReceiptData(null)
    clearCart()
    resetDialogState()
  }

  function handleClose(isOpen: boolean) {
    if (!processing) {
      onOpenChange(isOpen)
      if (!isOpen) resetDialogState()
    }
  }

  function handleMethodChange(nextMethod: string) {
    const method = nextMethod as PaymentMethod
    setPaymentMethod(method)
    if (method !== 'CASH' && !paymentAmount) {
      setPaymentAmount(String(Math.ceil(remainingAmount / 100)))
    }
  }

  function handleCashKeypadPress(key: string) {
    if (key === '⌫') {
      setReceivedAmount((current) => current.slice(0, -1))
      return
    }

    setReceivedAmount((current) => normalizeNumericInput(`${current}${key}`))
  }

  function handleAddCurrentPayment() {
    if (!canAddCurrentPayment) return

    setAddedPayments((current) => [
      ...current,
      {
        id: createPaymentEntryId(),
        method: paymentMethod,
        amount: parsedPaymentAmount,
        reference: reference.trim(),
      },
    ])
    setPaymentAmount('')
    setReference('')
    setPaymentMethod('CASH')
  }

  function handleRemovePayment(paymentId: string) {
    setAddedPayments((current) => current.filter((payment) => payment.id !== paymentId))
  }

  return (
    <>
      <Dialog open={open} onOpenChange={handleClose}>
        <DialogContent className="max-h-[90vh] max-w-xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>お会計</DialogTitle>
          </DialogHeader>

          <div className="grid gap-4">
            <div className="rounded-2xl bg-muted/50 p-4">
              <div className="grid gap-3 md:grid-cols-[1.1fr_0.9fr]">
                <div className="space-y-1">
                  <p className="text-sm text-muted-foreground">合計金額</p>
                  <p className="text-3xl font-bold">{formatMoney(total)}</p>
                  <p className="text-sm text-muted-foreground">
                    小計 {formatMoney(subtotal)} / 税額 {formatMoney(taxTotal)}
                  </p>
                </div>
                <div className="grid gap-2 rounded-xl border bg-background/80 p-3 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">支払済</span>
                    <span className="font-medium">{formatMoney(paidAmount)}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">残額</span>
                    <span className="font-medium">{formatMoney(remainingAmount)}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">今回の支払後</span>
                    <span className="font-medium">{formatMoney(remainingAfterCurrent)}</span>
                  </div>
                </div>
              </div>
            </div>

            {addedPayments.length > 0 && (
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <Receipt className="h-4 w-4 text-muted-foreground" />
                  <p className="text-sm font-medium">追加済みの支払</p>
                </div>
                <div className="space-y-2">
                  {addedPayments.map((payment) => (
                    <div
                      key={payment.id}
                      className="flex items-center justify-between rounded-xl border bg-background p-3"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <Badge variant="outline">{PAYMENT_METHOD_LABELS[payment.method]}</Badge>
                          <span className="font-medium">{formatMoney(payment.amount)}</span>
                        </div>
                        {payment.reference && (
                          <p className="text-xs text-muted-foreground">参照番号: {payment.reference}</p>
                        )}
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleRemovePayment(payment.id)}
                        aria-label={`${PAYMENT_METHOD_LABELS[payment.method]} 支払を削除`}
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <Separator />

            <Tabs value={paymentMethod} onValueChange={handleMethodChange}>
              <TabsList className="grid w-full grid-cols-3">
                <TabsTrigger value="CASH" className="gap-2">
                  <Wallet className="h-4 w-4" />
                  現金
                </TabsTrigger>
                <TabsTrigger value="CREDIT_CARD" className="gap-2">
                  <CreditCard className="h-4 w-4" />
                  カード
                </TabsTrigger>
                <TabsTrigger value="QR_CODE" className="gap-2">
                  <QrCode className="h-4 w-4" />
                  QR
                </TabsTrigger>
              </TabsList>

              <TabsContent value="CASH" className="space-y-4">
                <div className="rounded-xl border bg-muted/40 p-4">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">今回の現金充当額</span>
                    <span className="font-medium">{formatMoney(remainingAmount)}</span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    残額を現金で支払う前提です。分割払い時は先にカードまたは QR を追加してください。
                  </p>
                </div>

                <div>
                  <label className="text-sm font-medium">お預かり金額（円）</label>
                  <Input
                    type="number"
                    inputMode="numeric"
                    placeholder="0"
                    value={receivedAmount}
                    onChange={(event) => setReceivedAmount(normalizeNumericInput(event.target.value))}
                    className="mt-1 text-right text-lg"
                  />
                </div>

                <div className="flex flex-wrap gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setReceivedAmount(String(Math.ceil(remainingAmount / 100)))}
                  >
                    ぴったり
                  </Button>
                  {QUICK_CASH_AMOUNTS.map((yen) => (
                    <Button
                      key={yen}
                      variant="outline"
                      size="sm"
                      onClick={() => setReceivedAmount(String(yen))}
                    >
                      ¥{yen.toLocaleString('ja-JP')}
                    </Button>
                  ))}
                  <Button variant="ghost" size="sm" onClick={() => setReceivedAmount('')}>
                    C
                  </Button>
                </div>

                <div className="grid grid-cols-3 gap-2">
                  {CASH_KEYPAD_ROWS.flat().map((key) => (
                    <Button
                      key={key}
                      variant="outline"
                      size="lg"
                      className="h-12"
                      onClick={() => handleCashKeypadPress(key)}
                    >
                      {key}
                    </Button>
                  ))}
                </div>

                {cashShortfall > 0 && parsedReceivedAmount > 0 && (
                  <div className="rounded-lg border border-destructive/40 bg-destructive/5 p-3 text-sm text-destructive">
                    あと {formatMoney(cashShortfall)} 不足しています。
                  </div>
                )}

                {parsedReceivedAmount >= roundedRemainingAmount && remainingAmount > 0 && (
                  <div className="rounded-lg bg-muted p-3 text-center">
                    <p className="text-sm text-muted-foreground">お釣り</p>
                    <p className="text-xl font-bold">{formatMoney(currentChange)}</p>
                  </div>
                )}
              </TabsContent>

              <TabsContent value="CREDIT_CARD" className="space-y-4">
                <div className="rounded-xl border bg-muted/40 p-4">
                  <div className="flex items-center gap-2">
                    <CreditCard className="h-4 w-4 text-muted-foreground" />
                    <p className="text-sm font-medium">カード端末プレースホルダー</p>
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground">
                    端末で承認が完了したら承認番号を入力し、必要に応じて残額の一部だけを追加できます。
                  </p>
                </div>
                <div>
                  <label className="text-sm font-medium">決済金額（円）</label>
                  <Input
                    type="number"
                    inputMode="numeric"
                    placeholder="残額を入力"
                    value={paymentAmount}
                    onChange={(event) => setPaymentAmount(normalizeNumericInput(event.target.value))}
                    className="mt-1"
                  />
                  <div className="mt-2 flex flex-wrap gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPaymentAmount(String(Math.ceil(remainingAmount / 100)))}
                    >
                      残額
                    </Button>
                  </div>
                </div>
                <div>
                  <label className="text-sm font-medium">参照番号</label>
                  <Input
                    placeholder="カード承認番号"
                    value={reference}
                    onChange={(event) => setReference(event.target.value)}
                    className="mt-1"
                  />
                </div>
                {nonCashShortfall > 0 && parsedPaymentAmount > 0 && (
                  <p className="text-sm text-muted-foreground">
                    この支払後も {formatMoney(nonCashShortfall)} 残ります。
                  </p>
                )}
                {nonCashOverpayment > 0 && (
                  <p className="text-sm text-destructive">
                    残額を超える金額は追加できません。
                  </p>
                )}
              </TabsContent>

              <TabsContent value="QR_CODE" className="space-y-4">
                <div className="rounded-xl border bg-muted/40 p-4">
                  <div className="flex items-center gap-2">
                    <QrCode className="h-4 w-4 text-muted-foreground" />
                    <p className="text-sm font-medium">QR 決済プレースホルダー</p>
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground">
                    決済アプリで QR を読み取り、決済 ID を控えてから追加してください。
                  </p>
                  <div className="mt-3 rounded-lg border border-dashed bg-background px-4 py-6 text-center">
                    <p className="text-xs text-muted-foreground">決済コード</p>
                    <p className="mt-2 font-mono text-lg tracking-[0.2em]">OPENPOS-QR</p>
                  </div>
                </div>
                <div>
                  <label className="text-sm font-medium">決済金額（円）</label>
                  <Input
                    type="number"
                    inputMode="numeric"
                    placeholder="残額を入力"
                    value={paymentAmount}
                    onChange={(event) => setPaymentAmount(normalizeNumericInput(event.target.value))}
                    className="mt-1"
                  />
                  <div className="mt-2 flex flex-wrap gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPaymentAmount(String(Math.ceil(remainingAmount / 100)))}
                    >
                      残額
                    </Button>
                  </div>
                </div>
                <div>
                  <label className="text-sm font-medium">参照番号</label>
                  <Input
                    placeholder="決済ID"
                    value={reference}
                    onChange={(event) => setReference(event.target.value)}
                    className="mt-1"
                  />
                </div>
                {nonCashShortfall > 0 && parsedPaymentAmount > 0 && (
                  <p className="text-sm text-muted-foreground">
                    この支払後も {formatMoney(nonCashShortfall)} 残ります。
                  </p>
                )}
                {nonCashOverpayment > 0 && (
                  <p className="text-sm text-destructive">
                    残額を超える金額は追加できません。
                  </p>
                )}
              </TabsContent>
            </Tabs>
          </div>

          <DialogFooter className="flex-col gap-2 sm:flex-col">
            {paymentMethod !== 'CASH' && (
              <Button
                variant="outline"
                className="w-full"
                size="lg"
                disabled={processing || !canAddCurrentPayment}
                onClick={handleAddCurrentPayment}
              >
                この支払を追加
              </Button>
            )}
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
