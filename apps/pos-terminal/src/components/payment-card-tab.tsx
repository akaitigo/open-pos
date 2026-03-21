import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { formatMoney } from '@shared-types/openpos'
import { CreditCard, QrCode } from 'lucide-react'
import { normalizeNumericInput } from '@/components/use-checkout'
import type { PaymentMethod } from '@/components/use-checkout'

interface PaymentCardTabProps {
  method: PaymentMethod
  remainingAmount: number
  paymentAmount: string
  reference: string
  nonCashShortfall: number
  nonCashOverpayment: number
  parsedPaymentAmount: number
  onPaymentAmountChange: (value: string) => void
  onReferenceChange: (value: string) => void
}

export function PaymentCardTab({
  method,
  remainingAmount,
  paymentAmount,
  reference,
  nonCashShortfall,
  nonCashOverpayment,
  parsedPaymentAmount,
  onPaymentAmountChange,
  onReferenceChange,
}: PaymentCardTabProps) {
  const isCard = method === 'CREDIT_CARD'
  const Icon = isCard ? CreditCard : QrCode

  return (
    <div className="space-y-4">
      <div className="rounded-xl border bg-muted/40 p-4">
        <div className="flex items-center gap-2">
          <Icon className="h-4 w-4 text-muted-foreground" />
          <p className="text-sm font-medium">
            {isCard ? 'カード端末プレースホルダー' : 'QR 決済プレースホルダー'}
          </p>
        </div>
        <p className="mt-1 text-sm text-muted-foreground">
          {isCard
            ? '端末で承認が完了したら承認番号を入力し、必要に応じて残額の一部だけを追加できます。'
            : '決済アプリで QR を読み取り、決済 ID を控えてから追加してください。'}
        </p>
        {!isCard && (
          <div className="mt-3 rounded-lg border border-dashed bg-background px-4 py-6 text-center">
            <p className="text-xs text-muted-foreground">決済コード</p>
            <p className="mt-2 font-mono text-lg tracking-[0.2em]">OPENPOS-QR</p>
          </div>
        )}
      </div>
      <div>
        <label className="text-sm font-medium">決済金額（円）</label>
        <Input
          type="number"
          inputMode="numeric"
          placeholder="残額を入力"
          value={paymentAmount}
          onChange={(event) => onPaymentAmountChange(normalizeNumericInput(event.target.value))}
          className="mt-1"
        />
        <div className="mt-2 flex flex-wrap gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onPaymentAmountChange(String(Math.ceil(remainingAmount / 100)))}
          >
            残額
          </Button>
        </div>
      </div>
      <div>
        <label className="text-sm font-medium">参照番号</label>
        <Input
          placeholder={isCard ? 'カード承認番号' : '決済ID'}
          value={reference}
          onChange={(event) => onReferenceChange(event.target.value)}
          className="mt-1"
        />
      </div>
      {nonCashShortfall > 0 && parsedPaymentAmount > 0 && (
        <p className="text-sm text-muted-foreground">
          この支払後も {formatMoney(nonCashShortfall)} 残ります。
        </p>
      )}
      {nonCashOverpayment > 0 && (
        <p className="text-sm text-destructive">残額を超える金額は追加できません。</p>
      )}
    </div>
  )
}
