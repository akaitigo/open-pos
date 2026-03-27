import { useState } from 'react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { CheckoutDialog } from '@/components/checkout-dialog'
import { useTaxRates } from '@/hooks/use-tax-rates'
import {
  formatTaxRatePercentage,
  getCartDiscountTotal,
  getCartEstimatedTax,
  getCartEstimatedTotal,
  getCartItemSubtotal,
  getCartTaxBreakdown,
  getLineItemTaxRate,
} from '@/lib/cart-totals'
import { formatMoney } from '@shared-types/openpos'
import { getCartItemCount, getCartSubtotal, useCartStore } from '@/stores/cart-store'
import { useParkingStore } from '@/stores/parking-store'
import { toast } from '@/hooks/use-toast'
import { Minus, Pause, Play, Plus, ShoppingCart, Trash2, X } from 'lucide-react'

interface CartPanelProps {
  className?: string
  fullScreen?: boolean
}

export function CartPanel({ className, fullScreen = false }: CartPanelProps) {
  const { items, updateQuantity, removeItem, clearCart } = useCartStore()
  const addItem = useCartStore((s) => s.addItem)
  const { parkedTransactions, parkTransaction, resumeTransaction, removeParkedTransaction } =
    useParkingStore()
  const [checkoutOpen, setCheckoutOpen] = useState(false)
  const [showParked, setShowParked] = useState(false)
  const [quantityDrafts, setQuantityDrafts] = useState<Record<string, string>>({})
  const taxRates = useTaxRates()

  function handleParkTransaction() {
    if (items.length === 0) return
    const success = parkTransaction(items)
    if (success) {
      clearCart()
      toast({ title: '取引を保留しました' })
    } else {
      toast({
        title: '保留数の上限（5件）に達しています',
        variant: 'destructive',
      })
    }
  }

  function handleResumeTransaction(id: string) {
    const resumedItems = resumeTransaction(id)
    if (!resumedItems) return

    if (items.length > 0) {
      parkTransaction(items)
    }

    clearCart()
    for (const cartItem of resumedItems) {
      for (let i = 0; i < cartItem.quantity; i++) {
        addItem(cartItem.product)
      }
    }
    setShowParked(false)
    toast({ title: '保留取引を再開しました' })
  }

  const subtotal = getCartSubtotal(items)
  const taxTotal = getCartEstimatedTax(items, taxRates)
  const discountTotal = getCartDiscountTotal()
  const total = getCartEstimatedTotal(items, taxRates) - discountTotal
  const itemCount = getCartItemCount(items)
  const taxBreakdown = getCartTaxBreakdown(items, taxRates)

  function clearQuantityDraft(productId: string) {
    setQuantityDrafts((current) => {
      if (!(productId in current)) return current
      const next = { ...current }
      delete next[productId]
      return next
    })
  }

  function handleQuantityInput(productId: string, nextValue: string) {
    setQuantityDrafts((current) => ({ ...current, [productId]: nextValue }))

    const parsedValue = Number.parseInt(nextValue, 10)
    if (Number.isFinite(parsedValue) && parsedValue > 0) {
      updateQuantity(productId, parsedValue)
    }
  }

  function handleQuantityBlur(productId: string) {
    const draftValue = quantityDrafts[productId]
    if (draftValue == null) return

    const parsedValue = Number.parseInt(draftValue, 10)
    if (!Number.isFinite(parsedValue) || parsedValue <= 0) {
      removeItem(productId)
    } else {
      updateQuantity(productId, parsedValue)
    }

    clearQuantityDraft(productId)
  }

  return (
    <section data-testid="cart-panel" className={className}>
      <div className="flex items-center justify-between border-b p-4">
        <div className="flex items-center gap-2">
          <ShoppingCart className="h-5 w-5" />
          <div>
            <h2 className="font-semibold">カート</h2>
            <p className="text-xs text-muted-foreground">{itemCount} 点</p>
          </div>
          {itemCount > 0 && (
            <span className="rounded-full bg-primary px-2 py-0.5 text-xs text-primary-foreground">
              {itemCount}
            </span>
          )}
        </div>
        <div className="flex items-center gap-1">
          {parkedTransactions.length > 0 && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowParked(!showParked)}
              className="gap-1"
            >
              <Play className="h-3 w-3" />
              保留中 ({parkedTransactions.length})
            </Button>
          )}
          {items.length > 0 && (
            <>
              <Button variant="outline" size="sm" onClick={handleParkTransaction} className="gap-1">
                <Pause className="h-3 w-3" />
                保留
              </Button>
              <Button variant="ghost" size="sm" onClick={clearCart}>
                クリア
              </Button>
            </>
          )}
        </div>
      </div>

      {showParked && parkedTransactions.length > 0 && (
        <div className="border-b bg-muted/30 p-3">
          <p className="mb-2 text-xs font-medium text-muted-foreground">保留中の取引</p>
          <div className="space-y-2">
            {parkedTransactions.map((parked) => (
              <div
                key={parked.id}
                className="flex items-center justify-between rounded-lg border bg-background p-2"
              >
                <div>
                  <p className="text-sm font-medium">{parked.label}</p>
                  <p className="text-xs text-muted-foreground">
                    {parked.items.reduce((s, i) => s + i.quantity, 0)} 点 /{' '}
                    {new Date(parked.parkedAt).toLocaleTimeString('ja-JP')}
                  </p>
                </div>
                <div className="flex gap-1">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleResumeTransaction(parked.id)}
                  >
                    再開
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    aria-label="削除"
                    onClick={() => removeParkedTransaction(parked.id)}
                  >
                    <X className="h-3 w-3" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="flex-1 overflow-auto p-4">
        {items.length === 0 ? (
          <div className="flex h-full min-h-56 flex-col items-center justify-center gap-3 rounded-2xl border border-dashed bg-background/70 p-6 text-center">
            <ShoppingCart className="h-8 w-8 text-muted-foreground" />
            <div className="space-y-1">
              <p className="font-medium">カートは空です</p>
              <p className="text-sm text-muted-foreground">商品を追加してください</p>
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            {items.map((item) => {
              const lineSubtotal = getCartItemSubtotal(item)
              const taxRate = getLineItemTaxRate(item, taxRates)

              return (
                <div
                  key={item.product.id}
                  className="space-y-3 rounded-xl border bg-background p-4 shadow-xs"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="space-y-2">
                      <div className="space-y-1">
                        <p className="font-medium leading-tight">{item.product.name}</p>
                        <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                          <span>単価 {formatMoney(item.product.price)}</span>
                          {taxRate ? (
                            <Badge variant={taxRate.isReduced ? 'secondary' : 'outline'}>
                              {taxRate.isReduced ? '軽減税率' : taxRate.name}{' '}
                              {formatTaxRatePercentage(taxRate.rate)}
                            </Badge>
                          ) : (
                            <Badge variant="outline">税率未設定</Badge>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-xs text-muted-foreground">小計</p>
                      <p className="font-semibold">{formatMoney(lineSubtotal)}</p>
                    </div>
                  </div>

                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8"
                        onClick={() => {
                          clearQuantityDraft(item.product.id)
                          updateQuantity(item.product.id, item.quantity - 1)
                        }}
                        aria-label={`${item.product.name} の数量を減らす`}
                      >
                        <Minus className="h-3 w-3" />
                      </Button>
                      <Input
                        type="number"
                        min={0}
                        value={quantityDrafts[item.product.id] ?? String(item.quantity)}
                        onChange={(event) =>
                          handleQuantityInput(item.product.id, event.target.value)
                        }
                        onBlur={() => handleQuantityBlur(item.product.id)}
                        className="h-8 w-16 text-center"
                        aria-label={`${item.product.name} の数量`}
                      />
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8"
                        onClick={() => {
                          clearQuantityDraft(item.product.id)
                          updateQuantity(item.product.id, item.quantity + 1)
                        }}
                        aria-label={`${item.product.name} の数量を増やす`}
                      >
                        <Plus className="h-3 w-3" />
                      </Button>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-destructive"
                      onClick={() => {
                        clearQuantityDraft(item.product.id)
                        removeItem(item.product.id)
                      }}
                      aria-label={`${item.product.name} を削除`}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      <div className="border-t bg-background/80 p-4">
        <div className="space-y-3">
          <div className="flex justify-between text-sm">
            <span>商品点数</span>
            <span>{itemCount} 点</span>
          </div>
          <div className="flex justify-between text-sm">
            <span>小計</span>
            <span>{formatMoney(subtotal)}</span>
          </div>

          <div className="space-y-2 rounded-xl bg-muted/60 p-3">
            <div className="flex items-center justify-between text-sm font-medium">
              <span>税率別内訳</span>
              <span>{formatMoney(taxTotal)}</span>
            </div>
            {taxBreakdown.length === 0 ? (
              <p className="text-sm text-muted-foreground">税率の適用された商品はありません</p>
            ) : (
              taxBreakdown.map((entry) => (
                <div
                  key={entry.taxRateKey}
                  className="rounded-lg border bg-background/80 p-3 text-sm"
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <span className="font-medium">
                        {entry.taxRateName} {formatTaxRatePercentage(entry.rate)}
                      </span>
                      {entry.isReduced && <Badge variant="secondary">軽減税率対象</Badge>}
                    </div>
                    <span>{formatMoney(entry.taxAmount)}</span>
                  </div>
                  <div className="mt-1 flex justify-between text-xs text-muted-foreground">
                    <span>課税対象</span>
                    <span>{formatMoney(entry.taxableAmount)}</span>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="flex justify-between text-sm">
            <span>割引</span>
            <span>{formatMoney(discountTotal)}</span>
          </div>
          <div className="flex justify-between text-base font-semibold">
            <span>合計（税込）</span>
            <span>{formatMoney(total)}</span>
          </div>
        </div>

        <Button
          data-testid="checkout-button"
          className="mt-4 w-full"
          size={fullScreen ? 'default' : 'lg'}
          disabled={items.length === 0}
          onClick={() => setCheckoutOpen(true)}
        >
          お会計 {items.length > 0 && formatMoney(total)}
        </Button>
      </div>

      <CheckoutDialog open={checkoutOpen} onOpenChange={setCheckoutOpen} />
    </section>
  )
}
