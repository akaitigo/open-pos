import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { formatMoney } from '@shared-types/openpos'
import { Loader2, Percent, Tag, X } from 'lucide-react'
import type { AppliedCouponDiscount } from '@/stores/discount-store'

interface CouponSectionProps {
  couponCode: string
  couponValidating: boolean
  appliedDiscounts: AppliedCouponDiscount[]
  onCouponCodeChange: (value: string) => void
  onApplyCoupon: () => void
  onRemoveDiscount: (couponCode: string) => void
}

export function CouponSection({
  couponCode,
  couponValidating,
  appliedDiscounts,
  onCouponCodeChange,
  onApplyCoupon,
  onRemoveDiscount,
}: CouponSectionProps) {
  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <Tag className="h-4 w-4 text-muted-foreground" />
        <p className="text-sm font-medium">割引・クーポン</p>
      </div>

      <div className="flex items-end gap-2">
        <div className="flex-1">
          <label className="text-sm font-medium">クーポンコード</label>
          <Input
            placeholder="クーポンコードを入力"
            value={couponCode}
            onChange={(event) => onCouponCodeChange(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault()
                onApplyCoupon()
              }
            }}
            className="mt-1"
            disabled={couponValidating}
          />
        </div>
        <Button
          variant="outline"
          onClick={onApplyCoupon}
          disabled={couponValidating || !couponCode.trim()}
        >
          {couponValidating ? <Loader2 className="h-4 w-4 animate-spin" /> : '適用'}
        </Button>
      </div>

      {appliedDiscounts.length > 0 && (
        <div className="space-y-2">
          {appliedDiscounts.map((entry) => (
            <div
              key={entry.couponCode}
              className="flex items-center justify-between rounded-xl border border-green-200 bg-green-50 p-3 dark:border-green-900 dark:bg-green-950"
            >
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <Percent className="h-3 w-3 text-green-600 dark:text-green-400" />
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
                onClick={() => onRemoveDiscount(entry.couponCode)}
                aria-label={`クーポン ${entry.couponCode} を削除`}
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
