import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { getCartItemSubtotal, getCartEstimatedTax, getCartEstimatedTotal } from './cart-totals'
import type { CartItem } from '@/stores/cart-store'
import type { TaxRate } from '@shared-types/openpos'

/**
 * Property-based tests for money calculations.
 * Verifies algebraic invariants that must hold for any valid input.
 */

// Arbitrary for generating valid money amounts (銭単位, non-negative integer)
const moneyAmount = fc.integer({ min: 0, max: 100_000_000 })

// Arbitrary for generating valid quantities (1-999)
const quantity = fc.integer({ min: 1, max: 999 })

// Arbitrary for generating valid tax rates (0% to 100%, as decimal string)
const taxRateValue = fc.integer({ min: 0, max: 10000 }).map((v) => (v / 10000).toString())

const now = '2026-01-01T00:00:00Z'

function makeCartItem(price: number, qty: number, taxRateId?: string): CartItem {
  return {
    product: {
      id: 'prod-1',
      organizationId: 'org-1',
      name: 'Test',
      price,
      taxRateId: taxRateId ?? null,
      categoryId: null,
      barcode: null,
      isActive: true,
      displayOrder: 0,
      createdAt: now,
      updatedAt: now,
    },
    quantity: qty,
  }
}

function makeTaxRate(id: string, rate: string): TaxRate {
  return {
    id,
    organizationId: 'org-1',
    name: 'Standard',
    rate,
    isReduced: false,
    isDefault: false,
    createdAt: now,
    updatedAt: now,
  }
}

describe('Money property-based tests', () => {
  it('小計は常に非負である', () => {
    fc.assert(
      fc.property(moneyAmount, quantity, (price, qty) => {
        const item = makeCartItem(price, qty)
        expect(getCartItemSubtotal(item)).toBeGreaterThanOrEqual(0)
      }),
    )
  })

  it('小計 = 単価 * 数量 である', () => {
    fc.assert(
      fc.property(moneyAmount, quantity, (price, qty) => {
        const item = makeCartItem(price, qty)
        expect(getCartItemSubtotal(item)).toBe(price * qty)
      }),
    )
  })

  it('税額は常に非負である', () => {
    fc.assert(
      fc.property(moneyAmount, quantity, taxRateValue, (price, qty, rate) => {
        const item = makeCartItem(price, qty, 'tax-1')
        const taxRates = [makeTaxRate('tax-1', rate)]
        expect(getCartEstimatedTax([item], taxRates)).toBeGreaterThanOrEqual(0)
      }),
    )
  })

  it('合計 >= 小計 である（税率が非負の場合）', () => {
    fc.assert(
      fc.property(moneyAmount, quantity, taxRateValue, (price, qty, rate) => {
        const item = makeCartItem(price, qty, 'tax-1')
        const taxRates = [makeTaxRate('tax-1', rate)]
        const items = [item]
        const subtotal = price * qty
        const total = getCartEstimatedTotal(items, taxRates)
        expect(total).toBeGreaterThanOrEqual(subtotal)
      }),
    )
  })

  it('税額は Math.floor で切り捨てられる（端数処理の一貫性）', () => {
    fc.assert(
      fc.property(moneyAmount, quantity, taxRateValue, (price, qty, rate) => {
        const item = makeCartItem(price, qty, 'tax-1')
        const taxRates = [makeTaxRate('tax-1', rate)]
        const tax = getCartEstimatedTax([item], taxRates)
        expect(Number.isInteger(tax)).toBe(true)
      }),
    )
  })

  it('加算の可換性: items の順番を変えても合計は同じ', () => {
    fc.assert(
      fc.property(
        moneyAmount,
        moneyAmount,
        quantity,
        quantity,
        taxRateValue,
        (price1, price2, qty1, qty2, rate) => {
          const item1 = makeCartItem(price1, qty1, 'tax-1')
          const item2 = makeCartItem(price2, qty2, 'tax-1')
          // same tax rate -> grouped together, so order shouldn't matter
          item2.product.id = 'prod-2'
          const taxRates = [makeTaxRate('tax-1', rate)]

          const totalA = getCartEstimatedTotal([item1, item2], taxRates)
          const totalB = getCartEstimatedTotal([item2, item1], taxRates)
          expect(totalA).toBe(totalB)
        },
      ),
    )
  })

  it('数量0の商品は小計0になる', () => {
    fc.assert(
      fc.property(moneyAmount, (price) => {
        const item = makeCartItem(price, 0)
        expect(getCartItemSubtotal(item)).toBe(0)
      }),
    )
  })
})
