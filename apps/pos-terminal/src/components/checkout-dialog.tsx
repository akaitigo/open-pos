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
import { useCheckout, PAYMENT_METHOD_LABELS } from '@/hooks/use-checkout'
import { PaymentCashTab } from '@/components/payment-cash-tab'
import { PaymentCardTab } from '@/components/payment-card-tab'
import { ReceiptDialog } from '@/components/receipt-dialog'
import { formatMoney } from '@shared-types/openpos'
import { CreditCard, Loader2, Percent, QrCode, Receipt, Tag, Wallet, X } from 'lucide-react'
import { t } from '@/i18n'

interface CheckoutDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function CheckoutDialog({ open, onOpenChange }: CheckoutDialogProps) {
  const checkout = useCheckout(onOpenChange)

  return (
    <>
      <Dialog open={open} onOpenChange={checkout.handleClose}>
        <DialogContent className="max-h-[90vh] max-w-xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>お会計</DialogTitle>
          </DialogHeader>

          <div className="grid gap-4">
            {/* 合計金額サマリー */}
            <div className="rounded-2xl bg-muted/50 p-4">
              <div className="grid gap-3 md:grid-cols-[1.1fr_0.9fr]">
                <div className="space-y-1">
                  <p className="text-sm text-muted-foreground">合計金額</p>
                  <p className="text-3xl font-bold">{formatMoney(checkout.total)}</p>
                  <p className="text-sm text-muted-foreground">
                    小計 {formatMoney(checkout.subtotal)} / 税額 {formatMoney(checkout.taxTotal)}
                    {checkout.discountTotal > 0 &&
                      ` / 割引 -${formatMoney(checkout.discountTotal)}`}
                  </p>
                </div>
                <div className="grid gap-2 rounded-xl border bg-background/80 p-3 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">支払済</span>
                    <span className="font-medium">{formatMoney(checkout.paidAmount)}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">残額</span>
                    <span className="font-medium">{formatMoney(checkout.remainingAmount)}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">今回の支払後</span>
                    <span className="font-medium">
                      {formatMoney(checkout.remainingAfterCurrent)}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* 割引・クーポン */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Tag className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
                <p className="text-sm font-medium">割引・クーポン</p>
              </div>

              <div className="flex items-end gap-2">
                <div className="flex-1">
                  <label htmlFor="coupon-code-input" className="text-sm font-medium">
                    {t('accessibility.couponCodeLabel')}
                  </label>
                  <Input
                    id="coupon-code-input"
                    placeholder="クーポンコードを入力"
                    value={checkout.couponCode}
                    onChange={(event) => checkout.setCouponCode(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        event.preventDefault()
                        void checkout.handleApplyCoupon()
                      }
                    }}
                    className="mt-1"
                    disabled={checkout.couponValidating}
                  />
                </div>
                <Button
                  variant="outline"
                  onClick={() => void checkout.handleApplyCoupon()}
                  disabled={checkout.couponValidating || !checkout.couponCode.trim()}
                  aria-label={
                    checkout.couponValidating
                      ? t('accessibility.applyCouponLoading')
                      : t('accessibility.applyCoupon')
                  }
                >
                  {checkout.couponValidating ? (
                    <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
                  ) : (
                    '適用'
                  )}
                </Button>
              </div>

              {checkout.appliedDiscounts.length > 0 && (
                <div className="space-y-2">
                  {checkout.appliedDiscounts.map((entry) => (
                    <div
                      key={entry.couponCode}
                      className="flex items-center justify-between rounded-xl border border-green-200 bg-green-50 p-3 dark:border-green-900 dark:bg-green-950"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <Percent
                            className="h-3 w-3 text-green-600 dark:text-green-400"
                            aria-hidden="true"
                          />
                          <span className="text-sm font-medium">{entry.discount.name}</span>
                          <Badge variant="secondary" className="text-xs">
                            {entry.couponCode}
                          </Badge>
                        </div>
                        <p className="text-xs text-muted-foreground">
                          {entry.discount.discountType === 'PERCENTAGE'
                            ? `${Math.round(Number(entry.discount.value) * 100)}% 割引`
                            : `${formatMoney(Number.parseInt(entry.discount.value, 10))} 割引`}{' '}
                          &rarr; -{formatMoney(entry.amount)}
                        </p>
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => {
                          if (window.confirm(`クーポン「${entry.couponCode}」を削除しますか？`)) {
                            checkout.removeDiscount(entry.couponCode)
                          }
                        }}
                        aria-label={t('accessibility.removeCoupon', { code: entry.couponCode })}
                      >
                        <X className="h-4 w-4" aria-hidden="true" />
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* 追加済みの支払 */}
            {checkout.addedPayments.length > 0 && (
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <Receipt className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
                  <p className="text-sm font-medium">追加済みの支払</p>
                </div>
                <div className="space-y-2">
                  {checkout.addedPayments.map((payment) => (
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
                          <p className="text-xs text-muted-foreground">
                            参照番号: {payment.reference}
                          </p>
                        )}
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => checkout.handleRemovePayment(payment.id)}
                        aria-label={t('accessibility.removePayment', {
                          method: PAYMENT_METHOD_LABELS[payment.method],
                        })}
                      >
                        <X className="h-4 w-4" aria-hidden="true" />
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <Separator />

            {/* 支払方法タブ */}
            <Tabs
              data-testid="payment-tabs"
              value={checkout.paymentMethod}
              onValueChange={checkout.handleMethodChange}
            >
              <TabsList className="grid w-full grid-cols-3">
                <TabsTrigger data-testid="payment-tab-cash" value="CASH" className="gap-2">
                  <Wallet className="h-4 w-4" aria-hidden="true" />
                  現金
                </TabsTrigger>
                <TabsTrigger data-testid="payment-tab-card" value="CREDIT_CARD" className="gap-2">
                  <CreditCard className="h-4 w-4" aria-hidden="true" />
                  カード
                </TabsTrigger>
                <TabsTrigger data-testid="payment-tab-qr" value="QR_CODE" className="gap-2">
                  <QrCode className="h-4 w-4" aria-hidden="true" />
                  QR
                </TabsTrigger>
              </TabsList>

              <TabsContent value="CASH">
                <PaymentCashTab
                  remainingAmount={checkout.remainingAmount}
                  receivedAmount={checkout.receivedAmount}
                  setReceivedAmount={checkout.setReceivedAmount}
                  handleCashKeypadPress={checkout.handleCashKeypadPress}
                  cashShortfall={checkout.cashShortfall}
                  parsedReceivedAmount={checkout.parsedReceivedAmount}
                  roundedRemainingAmount={checkout.roundedRemainingAmount}
                  currentChange={checkout.currentChange}
                />
              </TabsContent>

              <TabsContent value="CREDIT_CARD">
                <PaymentCardTab
                  mode="CREDIT_CARD"
                  paymentAmount={checkout.paymentAmount}
                  setPaymentAmount={checkout.setPaymentAmount}
                  reference={checkout.reference}
                  setReference={checkout.setReference}
                  remainingAmount={checkout.remainingAmount}
                  nonCashShortfall={checkout.nonCashShortfall}
                  nonCashOverpayment={checkout.nonCashOverpayment}
                  parsedPaymentAmount={checkout.parsedPaymentAmount}
                />
              </TabsContent>

              <TabsContent value="QR_CODE">
                <PaymentCardTab
                  mode="QR_CODE"
                  paymentAmount={checkout.paymentAmount}
                  setPaymentAmount={checkout.setPaymentAmount}
                  reference={checkout.reference}
                  setReference={checkout.setReference}
                  remainingAmount={checkout.remainingAmount}
                  nonCashShortfall={checkout.nonCashShortfall}
                  nonCashOverpayment={checkout.nonCashOverpayment}
                  parsedPaymentAmount={checkout.parsedPaymentAmount}
                />
              </TabsContent>
            </Tabs>
          </div>

          <DialogFooter className="flex-col gap-2 sm:flex-col">
            <Button
              variant="ghost"
              className="w-full text-muted-foreground"
              size="sm"
              disabled={checkout.processing}
              onClick={() => onOpenChange(false)}
              aria-label={t('accessibility.cancelTransaction')}
            >
              取引をキャンセル
            </Button>
            {checkout.paymentMethod !== 'CASH' && (
              <Button
                variant="outline"
                className="w-full"
                size="lg"
                disabled={checkout.processing || !checkout.canAddCurrentPayment}
                onClick={checkout.handleAddCurrentPayment}
                aria-label={t('accessibility.addPayment')}
              >
                この支払を追加
              </Button>
            )}
            <Button
              data-testid="checkout-confirm-btn"
              className="w-full"
              size="lg"
              disabled={checkout.processing || !checkout.canFinalize}
              onClick={checkout.handleFinalize}
              aria-label={
                checkout.processing
                  ? t('accessibility.processingCheckout')
                  : t('accessibility.finalizeCheckout')
              }
            >
              {checkout.processing ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
                  処理中...
                </>
              ) : (
                'お会計を確定'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ReceiptDialog
        open={checkout.receiptOpen}
        receiptData={checkout.receiptData}
        onClose={checkout.handleReceiptClose}
      />
    </>
  )
}
