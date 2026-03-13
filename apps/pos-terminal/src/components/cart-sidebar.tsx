import { useEffect, useState } from 'react'
import { useCartStore, getCartSubtotal, getCartItemCount } from '@/stores/cart-store'
import { formatMoney } from '@shared-types/openpos'
import type { TaxRate } from '@shared-types/openpos'
import { TaxRateSchema } from '@shared-types/openpos'
import { Button } from '@/components/ui/button'
import { CheckoutDialog } from '@/components/checkout-dialog'
import { api } from '@/lib/api'
import { getCartEstimatedTotal } from '@/lib/cart-totals'
import { Minus, Plus, Trash2, ShoppingCart } from 'lucide-react'
import { z } from 'zod'

export function CartSidebar() {
  const { items, updateQuantity, removeItem, clearCart } = useCartStore()
  const [checkoutOpen, setCheckoutOpen] = useState(false)
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
  const itemCount = getCartItemCount(items)

  return (
    <aside className="flex w-[340px] shrink-0 flex-col border-l bg-muted/30">
      <div className="flex items-center justify-between border-b p-4">
        <div className="flex items-center gap-2">
          <ShoppingCart className="h-5 w-5" />
          <h2 className="font-semibold">カート</h2>
          {itemCount > 0 && (
            <span className="rounded-full bg-primary px-2 py-0.5 text-xs text-primary-foreground">
              {itemCount}
            </span>
          )}
        </div>
        {items.length > 0 && (
          <Button variant="ghost" size="sm" onClick={clearCart}>
            クリア
          </Button>
        )}
      </div>

      <div className="flex-1 overflow-auto p-4">
        {items.length === 0 ? (
          <p className="text-center text-sm text-muted-foreground">カートは空です</p>
        ) : (
          <div className="space-y-3">
            {items.map((item) => (
              <div
                key={item.product.id}
                className="flex items-start gap-2 rounded-lg border bg-background p-3"
              >
                <div className="flex-1 space-y-1">
                  <p className="text-sm font-medium leading-tight">{item.product.name}</p>
                  <p className="text-xs text-muted-foreground">{formatMoney(item.product.price)}</p>
                </div>
                <div className="flex items-center gap-1">
                  <Button
                    variant="outline"
                    size="icon"
                    className="h-7 w-7"
                    onClick={() => updateQuantity(item.product.id, item.quantity - 1)}
                  >
                    <Minus className="h-3 w-3" />
                  </Button>
                  <span className="w-8 text-center text-sm font-medium">{item.quantity}</span>
                  <Button
                    variant="outline"
                    size="icon"
                    className="h-7 w-7"
                    onClick={() => updateQuantity(item.product.id, item.quantity + 1)}
                  >
                    <Plus className="h-3 w-3" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7 text-destructive"
                    onClick={() => removeItem(item.product.id)}
                  >
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="border-t p-4">
        <div className="mb-3 space-y-1">
          <div className="flex justify-between text-sm">
            <span>小計</span>
            <span>{formatMoney(subtotal)}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span>税額</span>
            <span>{formatMoney(taxTotal)}</span>
          </div>
          <div className="flex justify-between text-sm font-medium">
            <span>合計</span>
            <span>{formatMoney(total)}</span>
          </div>
        </div>
        <Button
          className="w-full"
          size="lg"
          disabled={items.length === 0}
          onClick={() => setCheckoutOpen(true)}
        >
          お会計 {items.length > 0 && formatMoney(total)}
        </Button>
      </div>

      <CheckoutDialog open={checkoutOpen} onOpenChange={setCheckoutOpen} />
    </aside>
  )
}
