import { describe, expect, it } from 'vitest'
import { getCartEstimatedTax, getCartEstimatedTotal, getCartTaxBreakdown } from './cart-totals'
import type { CartItem } from '@/stores/cart-store'

const items: CartItem[] = [
  {
    product: {
      id: '550e8400-e29b-41d4-a716-446655440010',
      organizationId: '550e8400-e29b-41d4-a716-446655440000',
      name: 'ドリップコーヒー',
      price: 15000,
      taxRateId: '550e8400-e29b-41d4-a716-446655440020',
      displayOrder: 0,
      isActive: true,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
    quantity: 2,
  },
]

describe('cart totals', () => {
  it('calculates external tax with floor rounding', () => {
    const tax = getCartEstimatedTax(items, [
      {
        id: '550e8400-e29b-41d4-a716-446655440020',
        organizationId: '550e8400-e29b-41d4-a716-446655440000',
        name: '標準税率',
        rate: '0.10',
        isReduced: false,
        isDefault: true,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ])

    expect(tax).toBe(3000)
    expect(getCartEstimatedTotal(items, [
      {
        id: '550e8400-e29b-41d4-a716-446655440020',
        organizationId: '550e8400-e29b-41d4-a716-446655440000',
        name: '標準税率',
        rate: '0.10',
        isReduced: false,
        isDefault: true,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ])).toBe(33000)
  })

  it('falls back to subtotal when no tax rate is available', () => {
    expect(getCartEstimatedTax(items, [])).toBe(0)
    expect(getCartEstimatedTotal(items, [])).toBe(30000)
  })

  it('groups tax by tax rate', () => {
    const breakdown = getCartTaxBreakdown(items, [
      {
        id: '550e8400-e29b-41d4-a716-446655440020',
        organizationId: '550e8400-e29b-41d4-a716-446655440000',
        name: '標準税率',
        rate: '0.10',
        isReduced: false,
        isDefault: true,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ])

    expect(breakdown).toEqual([
      {
        taxRateKey: '550e8400-e29b-41d4-a716-446655440020',
        taxRateName: '標準税率',
        rate: '0.10',
        isReduced: false,
        taxableAmount: 30000,
        taxAmount: 3000,
      },
    ])
  })
})
