import type { TaxRate } from '@shared-types/openpos'
import type { CartItem } from '@/stores/cart-store'
import { getCartSubtotal } from '@/stores/cart-store'

function getItemTaxRate(item: CartItem, taxRates: TaxRate[]): string | null {
  const taxRateId = item.product.taxRateId
  if (!taxRateId) return null
  return taxRates.find((rate) => rate.id === taxRateId)?.rate ?? null
}

function calculateExternalTax(subtotal: number, taxRate: string | null): number {
  if (!taxRate) return 0

  const parsedRate = Number(taxRate)
  if (!Number.isFinite(parsedRate) || parsedRate <= 0) return 0

  return Math.floor(subtotal * parsedRate)
}

export function getCartEstimatedTax(items: CartItem[], taxRates: TaxRate[]): number {
  return items.reduce((sum, item) => {
    const itemSubtotal = item.product.price * item.quantity
    return sum + calculateExternalTax(itemSubtotal, getItemTaxRate(item, taxRates))
  }, 0)
}

export function getCartEstimatedTotal(items: CartItem[], taxRates: TaxRate[]): number {
  return getCartSubtotal(items) + getCartEstimatedTax(items, taxRates)
}
