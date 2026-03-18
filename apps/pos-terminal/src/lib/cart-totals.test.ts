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


  it('calculates tax on group subtotal matching BE (not per-item sum)', () => {
    const oddItems: CartItem[] = [
      {
        product: {
          id: 'prod-a',
          organizationId: 'org-1',
          name: 'A',
          price: 37,
          taxRateId: 'tax-std',
          displayOrder: 0,
          isActive: true,
          createdAt: '',
          updatedAt: '',
        },
        quantity: 1,
      },
      {
        product: {
          id: 'prod-b',
          organizationId: 'org-1',
          name: 'B',
          price: 37,
          taxRateId: 'tax-std',
          displayOrder: 0,
          isActive: true,
          createdAt: '',
          updatedAt: '',
        },
        quantity: 1,
      },
      {
        product: {
          id: 'prod-c',
          organizationId: 'org-1',
          name: 'C',
          price: 37,
          taxRateId: 'tax-std',
          displayOrder: 0,
          isActive: true,
          createdAt: '',
          updatedAt: '',
        },
        quantity: 1,
      },
    ]

    const taxRates = [
      {
        id: 'tax-std',
        organizationId: 'org-1',
        name: '標準税率10%',
        rate: '0.10',
        isReduced: false,
        isDefault: true,
        createdAt: '',
        updatedAt: '',
      },
    ]

    const breakdown = getCartTaxBreakdown(oddItems, taxRates)
    expect(breakdown[0]!.taxableAmount).toBe(111)
    expect(breakdown[0]!.taxAmount).toBe(11)

    expect(getCartEstimatedTax(oddItems, taxRates)).toBe(11)
  })
})
