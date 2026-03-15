import { describe, it, expect, beforeEach } from 'vitest'
import { useDiscountStore } from './discount-store'
import type { Discount } from '@shared-types/openpos'

const mockPercentageDiscount: Discount = {
  id: '550e8400-e29b-41d4-a716-446655440001',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  name: '10%割引',
  discountType: 'PERCENTAGE',
  value: '0.10',
  startDate: null,
  endDate: null,
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const mockFixedDiscount: Discount = {
  id: '550e8400-e29b-41d4-a716-446655440002',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  name: '100円引き',
  discountType: 'FIXED_AMOUNT',
  value: '10000',
  startDate: null,
  endDate: null,
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('discount-store', () => {
  beforeEach(() => {
    useDiscountStore.getState().clearDiscounts()
  })

  it('初期状態では割引が空である', () => {
    expect(useDiscountStore.getState().appliedDiscounts).toEqual([])
    expect(useDiscountStore.getState().getTotalDiscount()).toBe(0)
  })

  it('パーセンテージ割引を追加できる', () => {
    const subtotal = 100000 // 1000円
    useDiscountStore.getState().addDiscount('COUPON10', mockPercentageDiscount, subtotal)

    const discounts = useDiscountStore.getState().appliedDiscounts
    expect(discounts).toHaveLength(1)
    expect(discounts[0]!.couponCode).toBe('COUPON10')
    expect(discounts[0]!.amount).toBe(10000) // 100円
  })

  it('固定金額割引を追加できる', () => {
    const subtotal = 100000 // 1000円
    useDiscountStore.getState().addDiscount('FIXED100', mockFixedDiscount, subtotal)

    const discounts = useDiscountStore.getState().appliedDiscounts
    expect(discounts).toHaveLength(1)
    expect(discounts[0]!.couponCode).toBe('FIXED100')
    expect(discounts[0]!.amount).toBe(10000) // 100円
  })

  it('固定金額が小計を超える場合は小計で制限される', () => {
    const subtotal = 5000 // 50円（10000銭 = 100円より小さい）
    useDiscountStore.getState().addDiscount('FIXED100', mockFixedDiscount, subtotal)

    const discounts = useDiscountStore.getState().appliedDiscounts
    expect(discounts[0]!.amount).toBe(5000) // 小計が上限
  })

  it('同じクーポンコードは重複追加されない', () => {
    const subtotal = 100000
    useDiscountStore.getState().addDiscount('COUPON10', mockPercentageDiscount, subtotal)
    useDiscountStore.getState().addDiscount('COUPON10', mockPercentageDiscount, subtotal)

    expect(useDiscountStore.getState().appliedDiscounts).toHaveLength(1)
  })

  it('割引を削除できる', () => {
    const subtotal = 100000
    useDiscountStore.getState().addDiscount('COUPON10', mockPercentageDiscount, subtotal)
    useDiscountStore.getState().addDiscount('FIXED100', mockFixedDiscount, subtotal)

    useDiscountStore.getState().removeDiscount('COUPON10')

    const discounts = useDiscountStore.getState().appliedDiscounts
    expect(discounts).toHaveLength(1)
    expect(discounts[0]!.couponCode).toBe('FIXED100')
  })

  it('全割引をクリアできる', () => {
    const subtotal = 100000
    useDiscountStore.getState().addDiscount('COUPON10', mockPercentageDiscount, subtotal)
    useDiscountStore.getState().addDiscount('FIXED100', mockFixedDiscount, subtotal)

    useDiscountStore.getState().clearDiscounts()

    expect(useDiscountStore.getState().appliedDiscounts).toEqual([])
    expect(useDiscountStore.getState().getTotalDiscount()).toBe(0)
  })

  it('合計割引額を正しく計算する', () => {
    const subtotal = 100000 // 1000円
    useDiscountStore.getState().addDiscount('COUPON10', mockPercentageDiscount, subtotal)
    useDiscountStore.getState().addDiscount('FIXED100', mockFixedDiscount, subtotal)

    // 10% = 10000 + 固定100円 = 10000 = 合計 20000
    expect(useDiscountStore.getState().getTotalDiscount()).toBe(20000)
  })

  it('パーセンテージ割引は端数切り捨てで計算される', () => {
    const subtotal = 33333 // 端数
    useDiscountStore.getState().addDiscount('COUPON10', mockPercentageDiscount, subtotal)

    // floor(33333 * 0.10) = floor(3333.3) = 3333
    expect(useDiscountStore.getState().appliedDiscounts[0]!.amount).toBe(3333)
  })
})
