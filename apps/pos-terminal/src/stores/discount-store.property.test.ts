import { describe, it, expect, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import { useDiscountStore } from './discount-store'
import type { Discount } from '@shared-types/openpos'

const now = '2026-01-01T00:00:00Z'

function makePercentageDiscount(rate: number): Discount {
  return {
    id: `discount-pct-${rate}`,
    organizationId: 'org-1',
    name: `${rate * 100}%割引`,
    discountType: 'PERCENTAGE',
    value: rate.toString(),
    startDate: null,
    endDate: null,
    isActive: true,
    createdAt: now,
    updatedAt: now,
  }
}

function makeFixedDiscount(amount: number): Discount {
  return {
    id: `discount-fix-${amount}`,
    organizationId: 'org-1',
    name: `${amount}銭引き`,
    discountType: 'FIXED_AMOUNT',
    value: amount.toString(),
    startDate: null,
    endDate: null,
    isActive: true,
    createdAt: now,
    updatedAt: now,
  }
}

// 割引率: 0.01〜1.00
const percentageRate = fc.integer({ min: 1, max: 100 }).map((v) => v / 100)
const moneyAmount = fc.integer({ min: 0, max: 100_000_000 })
const positiveAmount = fc.integer({ min: 1, max: 100_000_000 })

describe('Discount property-based tests', () => {
  beforeEach(() => {
    useDiscountStore.getState().clearDiscounts()
  })

  it('パーセンテージ割引額は常に非負である', () => {
    fc.assert(
      fc.property(moneyAmount, percentageRate, (subtotal, rate) => {
        useDiscountStore.getState().clearDiscounts()
        useDiscountStore
          .getState()
          .addDiscount(`coupon-${subtotal}-${rate}`, makePercentageDiscount(rate), subtotal)
        const discounts = useDiscountStore.getState().appliedDiscounts
        if (discounts.length > 0) {
          expect(discounts[0]!.amount).toBeGreaterThanOrEqual(0)
        }
      }),
    )
  })

  it('パーセンテージ割引額は小計を超えない', () => {
    fc.assert(
      fc.property(moneyAmount, percentageRate, (subtotal, rate) => {
        useDiscountStore.getState().clearDiscounts()
        useDiscountStore
          .getState()
          .addDiscount(`coupon-${subtotal}-${rate}`, makePercentageDiscount(rate), subtotal)
        const discounts = useDiscountStore.getState().appliedDiscounts
        if (discounts.length > 0) {
          expect(discounts[0]!.amount).toBeLessThanOrEqual(subtotal)
        }
      }),
    )
  })

  it('パーセンテージ割引額は整数である（端数切り捨て）', () => {
    fc.assert(
      fc.property(moneyAmount, percentageRate, (subtotal, rate) => {
        useDiscountStore.getState().clearDiscounts()
        useDiscountStore
          .getState()
          .addDiscount(`coupon-${subtotal}-${rate}`, makePercentageDiscount(rate), subtotal)
        const discounts = useDiscountStore.getState().appliedDiscounts
        if (discounts.length > 0) {
          expect(Number.isInteger(discounts[0]!.amount)).toBe(true)
        }
      }),
    )
  })

  it('固定額割引は小計で上限が設けられる', () => {
    fc.assert(
      fc.property(positiveAmount, positiveAmount, (fixedValue, subtotal) => {
        useDiscountStore.getState().clearDiscounts()
        useDiscountStore
          .getState()
          .addDiscount(`coupon-${fixedValue}-${subtotal}`, makeFixedDiscount(fixedValue), subtotal)
        const discounts = useDiscountStore.getState().appliedDiscounts
        if (discounts.length > 0) {
          expect(discounts[0]!.amount).toBeLessThanOrEqual(subtotal)
          expect(discounts[0]!.amount).toBeLessThanOrEqual(fixedValue)
        }
      }),
    )
  })

  it('固定額割引は min(value, subtotal) と一致する', () => {
    fc.assert(
      fc.property(positiveAmount, positiveAmount, (fixedValue, subtotal) => {
        useDiscountStore.getState().clearDiscounts()
        useDiscountStore
          .getState()
          .addDiscount(`coupon-${fixedValue}-${subtotal}`, makeFixedDiscount(fixedValue), subtotal)
        const discounts = useDiscountStore.getState().appliedDiscounts
        if (discounts.length > 0) {
          expect(discounts[0]!.amount).toBe(Math.min(fixedValue, subtotal))
        }
      }),
    )
  })

  it('合計割引額は個別割引額の和である', () => {
    fc.assert(
      fc.property(positiveAmount, percentageRate, positiveAmount, (subtotal, rate, fixedValue) => {
        useDiscountStore.getState().clearDiscounts()
        useDiscountStore
          .getState()
          .addDiscount('coupon-pct', makePercentageDiscount(rate), subtotal)
        useDiscountStore
          .getState()
          .addDiscount('coupon-fix', makeFixedDiscount(fixedValue), subtotal)
        const discounts = useDiscountStore.getState().appliedDiscounts
        const expectedTotal = discounts.reduce((sum, d) => sum + d.amount, 0)
        expect(useDiscountStore.getState().getTotalDiscount()).toBe(expectedTotal)
      }),
    )
  })
})
