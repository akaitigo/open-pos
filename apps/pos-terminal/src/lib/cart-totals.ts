import type { TaxRate } from '@shared-types/openpos'
import type { CartItem } from '@/stores/cart-store'
import { getCartSubtotal } from '@/stores/cart-store'
import { useDiscountStore } from '@/stores/discount-store'

export interface CartTaxBreakdownEntry {
  taxRateKey: string
  taxRateName: string
  rate: string
  isReduced: boolean
  taxableAmount: number
  taxAmount: number
}

function getItemTaxRate(item: CartItem, taxRates: TaxRate[]): string | null {
  const taxRateId = item.product.taxRateId
  if (!taxRateId) return null
  return taxRates.find((rate) => rate.id === taxRateId)?.rate ?? null
}

export function getLineItemTaxRate(item: CartItem, taxRates: TaxRate[]): TaxRate | null {
  const taxRateId = item.product.taxRateId
  if (!taxRateId) return null
  return taxRates.find((rate) => rate.id === taxRateId) ?? null
}

function calculateExternalTax(subtotal: number, taxRate: string | null): number {
  if (!taxRate) return 0

  const parsedRate = Number(taxRate)
  if (!Number.isFinite(parsedRate) || parsedRate <= 0) return 0

  return Math.floor(subtotal * parsedRate)
}

export function getCartEstimatedTax(items: CartItem[], taxRates: TaxRate[]): number {
  // Use group-level tax calculation to match BE (floor on group subtotal, not per-item)
  const breakdown = getCartTaxBreakdown(items, taxRates)
  return breakdown.reduce((sum, entry) => sum + entry.taxAmount, 0)
}

export function getCartEstimatedTotal(items: CartItem[], taxRates: TaxRate[]): number {
  return getCartSubtotal(items) + getCartEstimatedTax(items, taxRates)
}

export function getCartItemSubtotal(item: CartItem): number {
  return item.product.price * item.quantity
}

export function getCartItemTax(item: CartItem, taxRates: TaxRate[]): number {
  return calculateExternalTax(getCartItemSubtotal(item), getItemTaxRate(item, taxRates))
}

export function getCartTaxBreakdown(
  items: CartItem[],
  taxRates: TaxRate[],
): CartTaxBreakdownEntry[] {
  const breakdown = new Map<string, CartTaxBreakdownEntry>()

  for (const item of items) {
    const taxRate = getLineItemTaxRate(item, taxRates)
    if (!taxRate) continue

    const key = taxRate.id
    const current = breakdown.get(key) ?? {
      taxRateKey: key,
      taxRateName: taxRate.name,
      rate: taxRate.rate,
      isReduced: taxRate.isReduced,
      taxableAmount: 0,
      taxAmount: 0,
    }

    current.taxableAmount += getCartItemSubtotal(item)
    breakdown.set(key, current)
  }

  // Recalculate tax from grouped taxableAmount to match BE (floor on group total)
  for (const entry of breakdown.values()) {
    entry.taxAmount = calculateExternalTax(entry.taxableAmount, entry.rate)
  }

  return Array.from(breakdown.values()).sort(
    (left, right) => Number(right.rate) - Number(left.rate),
  )
}

export function formatTaxRatePercentage(rate: string): string {
  const parsedRate = Number(rate)
  if (!Number.isFinite(parsedRate)) return rate
  return `${Math.round(parsedRate * 100)}%`
}

export function getCartDiscountTotal(): number {
  return useDiscountStore.getState().getTotalDiscount()
}
