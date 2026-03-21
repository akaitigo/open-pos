import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { QUICK_CASH_AMOUNTS, CASH_KEYPAD_ROWS, normalizeNumericInput } from '@/hooks/use-checkout'
import { formatMoney } from '@shared-types/openpos'

interface PaymentCashTabProps {
  remainingAmount: number
  receivedAmount: string
  setReceivedAmount: (value: string) => void
  handleCashKeypadPress: (key: string) => void
  cashShortfall: number
  parsedReceivedAmount: number
  roundedRemainingAmount: number
  currentChange: number
}

export function PaymentCashTab({
  remainingAmount,
  receivedAmount,
  setReceivedAmount,
  handleCashKeypadPress,
  cashShortfall,
  parsedReceivedAmount,
  roundedRemainingAmount,
  currentChange,
}: PaymentCashTabProps) {
  return (
    <div className="space-y-4">
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
          data-testid="checkout-exact-btn"
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
    </div>
  )
}
