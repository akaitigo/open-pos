import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  getCartEstimatedTax,
  getCartEstimatedTotal,
  getCartTaxBreakdown,
  getCartItemTax,
} from './cart-totals'
import type { CartItem } from '@/stores/cart-store'
import type { TaxRate } from '@shared-types/openpos'

const now = '2026-01-01T00:00:00Z'

const moneyAmount = fc.integer({ min: 0, max: 100_000_000 })
const quantity = fc.integer({ min: 1, max: 999 })
const taxRateValue = fc.integer({ min: 0, max: 10000 }).map((v) => (v / 10000).toString())

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

describe('Cart totals property-based tests', () => {
  it('税ブレイクダウンの課税対象額合計は商品小計合計と一致する', () => {
    fc.assert(
      fc.property(
        moneyAmount,
        moneyAmount,
        quantity,
        quantity,
        taxRateValue,
        (p1, p2, q1, q2, rate) => {
          const item1 = makeCartItem(p1, q1, 'tax-1')
          const item2: CartItem = {
            product: { ...makeCartItem(p2, q2, 'tax-1').product, id: 'prod-2' },
            quantity: q2,
          }
          const taxRates = [makeTaxRate('tax-1', rate)]
          const breakdown = getCartTaxBreakdown([item1, item2], taxRates)
          const totalTaxable = breakdown.reduce((sum, entry) => sum + entry.taxableAmount, 0)
          expect(totalTaxable).toBe(p1 * q1 + p2 * q2)
        },
      ),
    )
  })

  it('税ブレイクダウンの税額は全て非負である', () => {
    fc.assert(
      fc.property(moneyAmount, quantity, taxRateValue, (price, qty, rate) => {
        const item = makeCartItem(price, qty, 'tax-1')
        const taxRates = [makeTaxRate('tax-1', rate)]
        const breakdown = getCartTaxBreakdown([item], taxRates)
        for (const entry of breakdown) {
          expect(entry.taxAmount).toBeGreaterThanOrEqual(0)
        }
      }),
    )
  })

  it('税ブレイクダウンの税額は全て整数である', () => {
    fc.assert(
      fc.property(moneyAmount, quantity, taxRateValue, (price, qty, rate) => {
        const item = makeCartItem(price, qty, 'tax-1')
        const taxRates = [makeTaxRate('tax-1', rate)]
        const breakdown = getCartTaxBreakdown([item], taxRates)
        for (const entry of breakdown) {
          expect(Number.isInteger(entry.taxAmount)).toBe(true)
        }
      }),
    )
  })

  it('異なる税率は別グループに分かれる', () => {
    fc.assert(
      fc.property(moneyAmount, moneyAmount, quantity, quantity, (p1, p2, q1, q2) => {
        const item1 = makeCartItem(p1, q1, 'tax-standard')
        const item2: CartItem = {
          product: { ...makeCartItem(p2, q2, 'tax-reduced').product, id: 'prod-2' },
          quantity: q2,
        }
        const taxRates = [
          makeTaxRate('tax-standard', '0.10'),
          { ...makeTaxRate('tax-reduced', '0.08'), isReduced: true },
        ]
        const breakdown = getCartTaxBreakdown([item1, item2], taxRates)
        expect(breakdown.length).toBe(2)
      }),
    )
  })

  it('推定合計 = 小計 + 推定税額', () => {
    fc.assert(
      fc.property(moneyAmount, quantity, taxRateValue, (price, qty, rate) => {
        const item = makeCartItem(price, qty, 'tax-1')
        const taxRates = [makeTaxRate('tax-1', rate)]
        const items = [item]
        const subtotal = price * qty
        const tax = getCartEstimatedTax(items, taxRates)
        const total = getCartEstimatedTotal(items, taxRates)
        expect(total).toBe(subtotal + tax)
      }),
    )
  })

  it('個別商品の税額は常に非負である', () => {
    fc.assert(
      fc.property(moneyAmount, quantity, taxRateValue, (price, qty, rate) => {
        const item = makeCartItem(price, qty, 'tax-1')
        const taxRates = [makeTaxRate('tax-1', rate)]
        expect(getCartItemTax(item, taxRates)).toBeGreaterThanOrEqual(0)
      }),
    )
  })

  it('税率なしの商品は税額0', () => {
    fc.assert(
      fc.property(moneyAmount, quantity, (price, qty) => {
        const item = makeCartItem(price, qty) // no taxRateId
        const taxRates = [makeTaxRate('tax-1', '0.10')]
        expect(getCartItemTax(item, taxRates)).toBe(0)
      }),
    )
  })
})
