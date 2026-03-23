import { describe, expect, it } from 'vitest'
import {
  getCartEstimatedTax,
  getCartEstimatedTotal,
  getCartTaxBreakdown,
  getCartItemSubtotal,
  getCartItemTax,
  formatTaxRatePercentage,
  getLineItemTaxRate,
} from './cart-totals'
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
    expect(
      getCartEstimatedTotal(items, [
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
      ]),
    ).toBe(33000)
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

  it('handles item with no taxRateId', () => {
    const noTaxItems: CartItem[] = [
      {
        product: {
          id: 'prod-no-tax',
          organizationId: 'org-1',
          name: '非課税品',
          price: 5000,
          taxRateId: null,
          displayOrder: 0,
          isActive: true,
          createdAt: '',
          updatedAt: '',
        },
        quantity: 3,
      },
    ]

    const taxRates = [
      {
        id: 'tax-std',
        organizationId: 'org-1',
        name: '標準税率',
        rate: '0.10',
        isReduced: false,
        isDefault: true,
        createdAt: '',
        updatedAt: '',
      },
    ]

    expect(getCartEstimatedTax(noTaxItems, taxRates)).toBe(0)
    expect(getCartEstimatedTotal(noTaxItems, taxRates)).toBe(15000)
    expect(getCartTaxBreakdown(noTaxItems, taxRates)).toEqual([])
  })

  it('handles invalid tax rate string', () => {
    const badRateItems: CartItem[] = [
      {
        product: {
          id: 'prod-bad',
          organizationId: 'org-1',
          name: 'Bad',
          price: 10000,
          taxRateId: 'tax-bad',
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
        id: 'tax-bad',
        organizationId: 'org-1',
        name: '不正な税率',
        rate: 'abc',
        isReduced: false,
        isDefault: false,
        createdAt: '',
        updatedAt: '',
      },
    ]

    expect(getCartEstimatedTax(badRateItems, taxRates)).toBe(0)
  })

  it('handles negative tax rate', () => {
    const negItems: CartItem[] = [
      {
        product: {
          id: 'prod-neg',
          organizationId: 'org-1',
          name: 'Neg',
          price: 10000,
          taxRateId: 'tax-neg',
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
        id: 'tax-neg',
        organizationId: 'org-1',
        name: '負の税率',
        rate: '-0.10',
        isReduced: false,
        isDefault: false,
        createdAt: '',
        updatedAt: '',
      },
    ]

    expect(getCartEstimatedTax(negItems, taxRates)).toBe(0)
  })

  it('sorts breakdown by rate descending', () => {
    const multiItems: CartItem[] = [
      {
        product: {
          id: 'prod-std',
          organizationId: 'org-1',
          name: 'Std',
          price: 10000,
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
          id: 'prod-reduced',
          organizationId: 'org-1',
          name: 'Reduced',
          price: 10000,
          taxRateId: 'tax-reduced',
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
        name: '標準税率',
        rate: '0.10',
        isReduced: false,
        isDefault: true,
        createdAt: '',
        updatedAt: '',
      },
      {
        id: 'tax-reduced',
        organizationId: 'org-1',
        name: '軽減税率',
        rate: '0.08',
        isReduced: true,
        isDefault: false,
        createdAt: '',
        updatedAt: '',
      },
    ]

    const breakdown = getCartTaxBreakdown(multiItems, taxRates)
    expect(breakdown).toHaveLength(2)
    expect(breakdown[0]!.rate).toBe('0.10')
    expect(breakdown[1]!.rate).toBe('0.08')
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

  describe('getCartItemSubtotal', () => {
    it('calculates item subtotal', () => {
      expect(getCartItemSubtotal(items[0]!)).toBe(30000)
    })
  })

  describe('getCartItemTax', () => {
    it('calculates item tax', () => {
      const taxRates = [
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
      ]
      expect(getCartItemTax(items[0]!, taxRates)).toBe(3000)
    })

    it('returns 0 for item with no matching tax rate', () => {
      expect(getCartItemTax(items[0]!, [])).toBe(0)
    })
  })

  describe('getLineItemTaxRate', () => {
    it('returns matching tax rate', () => {
      const taxRates = [
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
      ]
      const rate = getLineItemTaxRate(items[0]!, taxRates)
      expect(rate?.name).toBe('標準税率')
    })

    it('returns null for item without taxRateId', () => {
      const noTaxItem: CartItem = {
        product: {
          id: 'p1',
          organizationId: 'org-1',
          name: 'NoTax',
          price: 100,
          taxRateId: null,
          displayOrder: 0,
          isActive: true,
          createdAt: '',
          updatedAt: '',
        },
        quantity: 1,
      }
      expect(getLineItemTaxRate(noTaxItem, [])).toBeNull()
    })
  })

  describe('formatTaxRatePercentage', () => {
    it('formats valid rate to percentage', () => {
      expect(formatTaxRatePercentage('0.10')).toBe('10%')
      expect(formatTaxRatePercentage('0.08')).toBe('8%')
    })

    it('returns input for invalid rate', () => {
      expect(formatTaxRatePercentage('abc')).toBe('abc')
    })
  })
})
