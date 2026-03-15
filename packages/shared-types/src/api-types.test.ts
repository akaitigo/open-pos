import { describe, it, expect } from 'vitest'
import { ProductSchema, CategorySchema, TaxRateSchema } from './product'
import {
  AuthenticateByPinRequestSchema,
  AuthenticateByPinResponseSchema,
  StoreSchema,
  StaffSchema,
} from './store'
import { createPaginatedResponseSchema } from './api-types'
import { ReceiptSchema } from './transaction'
import { TransactionSchema } from './transaction'
import { z } from 'zod'

const now = new Date().toISOString()

describe('ProductSchema', () => {
  it('有効な商品データをパースできる', () => {
    const product = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      organizationId: '550e8400-e29b-41d4-a716-446655440001',
      name: 'テスト商品',
      barcode: '4901234567890',
      sku: 'SKU-001',
      price: 10000,
      displayOrder: 0,
      isActive: true,
      createdAt: now,
      updatedAt: now,
    }

    const result = ProductSchema.parse(product)
    expect(result.name).toBe('テスト商品')
    expect(result.price).toBe(10000)
  })

  it('必須フィールドが欠けている場合はバリデーションエラー', () => {
    expect(() => ProductSchema.parse({ name: 'test' })).toThrow()
  })

  it('nullable な商品フィールドをパースできる', () => {
    const product = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      organizationId: '550e8400-e29b-41d4-a716-446655440001',
      name: 'テスト商品',
      description: null,
      barcode: null,
      sku: null,
      price: 10000,
      categoryId: null,
      taxRateId: null,
      imageUrl: null,
      displayOrder: 0,
      isActive: true,
      createdAt: now,
      updatedAt: now,
    }

    const result = ProductSchema.parse(product)
    expect(result.description).toBeNull()
    expect(result.categoryId).toBeNull()
  })
})

describe('CategorySchema', () => {
  it('有効なカテゴリデータをパースできる', () => {
    const category = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      organizationId: '550e8400-e29b-41d4-a716-446655440001',
      name: '食品',
      parentId: null,
      color: null,
      icon: null,
      displayOrder: 1,
      createdAt: now,
      updatedAt: now,
    }

    const result = CategorySchema.parse(category)
    expect(result.name).toBe('食品')
  })
})

describe('StoreSchema', () => {
  it('有効な店舗データをパースできる', () => {
    const store = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      organizationId: '550e8400-e29b-41d4-a716-446655440001',
      name: '本店',
      address: '東京都渋谷区',
      phone: '03-1234-5678',
      timezone: 'Asia/Tokyo',
      settings: '{}',
      isActive: true,
      createdAt: now,
      updatedAt: now,
    }

    const result = StoreSchema.parse(store)
    expect(result.name).toBe('本店')
  })
})

describe('StaffSchema', () => {
  it('有効なスタッフデータをパースできる', () => {
    const staff = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      organizationId: '550e8400-e29b-41d4-a716-446655440001',
      storeId: '550e8400-e29b-41d4-a716-446655440002',
      name: '田中太郎',
      email: 'tanaka@example.com',
      role: 'CASHIER',
      isActive: true,
      failedPinAttempts: 0,
      isLocked: false,
      createdAt: now,
      updatedAt: now,
    }

    const result = StaffSchema.parse(staff)
    expect(result.name).toBe('田中太郎')
    expect(result.role).toBe('CASHIER')
  })

  it('無効なロールはバリデーションエラー', () => {
    const staff = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      organizationId: '550e8400-e29b-41d4-a716-446655440001',
      storeId: '550e8400-e29b-41d4-a716-446655440002',
      name: '田中太郎',
      email: 'tanaka@example.com',
      role: 'INVALID_ROLE',
      isActive: true,
      failedPinAttempts: 0,
      isLocked: false,
      createdAt: now,
      updatedAt: now,
    }

    expect(() => StaffSchema.parse(staff)).toThrow()
  })
})

describe('TaxRateSchema', () => {
  it('有効な税率データをパースできる', () => {
    const taxRate = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      organizationId: '550e8400-e29b-41d4-a716-446655440001',
      name: '標準税率',
      rate: '0.10',
      isReduced: false,
      isDefault: true,
      createdAt: now,
      updatedAt: now,
    }

    const result = TaxRateSchema.parse(taxRate)
    expect(result.name).toBe('標準税率')
    expect(result.rate).toBe('0.10')
  })
})

describe('createPaginatedResponseSchema', () => {
  it('ページネーション付きレスポンスをパースできる', () => {
    const TestItemSchema = z.object({
      id: z.string(),
      name: z.string(),
    })

    const PaginatedSchema = createPaginatedResponseSchema(TestItemSchema)

    const data = {
      data: [
        { id: '1', name: 'Item 1' },
        { id: '2', name: 'Item 2' },
      ],
      pagination: {
        page: 1,
        pageSize: 20,
        totalCount: 2,
        totalPages: 1,
      },
    }

    const result = PaginatedSchema.parse(data)
    expect(result.data).toHaveLength(2)
    expect(result.pagination.totalCount).toBe(2)
  })
})

describe('AuthenticateByPin schemas', () => {
  it('storeId を含む認証リクエストをパースできる', () => {
    const result = AuthenticateByPinRequestSchema.parse({
      storeId: '550e8400-e29b-41d4-a716-446655440002',
      pin: '1234',
    })
    expect(result.storeId).toBe('550e8400-e29b-41d4-a716-446655440002')
    expect(result.pin).toBe('1234')
  })

  it('success 時に reason がなくてもパースできる', () => {
    const result = AuthenticateByPinResponseSchema.parse({
      success: true,
      staff: {
        id: '550e8400-e29b-41d4-a716-446655440000',
        organizationId: '550e8400-e29b-41d4-a716-446655440001',
        storeId: '550e8400-e29b-41d4-a716-446655440002',
        name: '田中太郎',
        email: 'tanaka@example.com',
        role: 'CASHIER',
        isActive: true,
        failedPinAttempts: 0,
        isLocked: false,
        createdAt: now,
        updatedAt: now,
      },
    })

    expect(result.success).toBe(true)
    expect(result.reason).toBeUndefined()
  })

  it('failure 時に staff がなくてもパースできる', () => {
    const result = AuthenticateByPinResponseSchema.parse({
      success: false,
      reason: 'INVALID_PIN',
    })

    expect(result.staff).toBeUndefined()
    expect(result.reason).toBe('INVALID_PIN')
  })

  it('success 時にセッショントークンを含むレスポンスをパースできる', () => {
    const result = AuthenticateByPinResponseSchema.parse({
      success: true,
      staff: {
        id: '550e8400-e29b-41d4-a716-446655440000',
        organizationId: '550e8400-e29b-41d4-a716-446655440001',
        storeId: '550e8400-e29b-41d4-a716-446655440002',
        name: '田中太郎',
        email: 'tanaka@example.com',
        role: 'MANAGER',
        isActive: true,
        failedPinAttempts: 0,
        isLocked: false,
        createdAt: now,
        updatedAt: now,
      },
      token: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.signature',
    })

    expect(result.success).toBe(true)
    expect(result.token).toBe('eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.signature')
  })
})

describe('ReceiptSchema', () => {
  it('現在の gateway レスポンス形式をパースできる', () => {
    const result = ReceiptSchema.parse({
      id: '550e8400-e29b-41d4-a716-446655440000',
      transactionId: '550e8400-e29b-41d4-a716-446655440001',
      receiptData: 'receipt body',
      pdfUrl: null,
      createdAt: now,
    })

    expect(result.receiptData).toBe('receipt body')
    expect(result.pdfUrl).toBeNull()
  })
})

describe('TransactionSchema', () => {
  it('nullable な clientId と payment.reference をパースできる', () => {
    const result = TransactionSchema.parse({
      id: '550e8400-e29b-41d4-a716-446655440000',
      organizationId: '550e8400-e29b-41d4-a716-446655440001',
      storeId: '550e8400-e29b-41d4-a716-446655440002',
      terminalId: '550e8400-e29b-41d4-a716-446655440003',
      staffId: '550e8400-e29b-41d4-a716-446655440004',
      transactionNumber: 'TX-001',
      type: 'SALE',
      status: 'COMPLETED',
      items: [],
      payments: [
        {
          id: '550e8400-e29b-41d4-a716-446655440005',
          method: 'CASH',
          amount: 10000,
          received: 10000,
          change: 0,
          reference: null,
        },
      ],
      discounts: [],
      taxSummaries: [],
      subtotal: 10000,
      discountTotal: 0,
      taxTotal: 1000,
      total: 11000,
      clientId: null,
      createdAt: now,
      updatedAt: now,
      completedAt: now,
    })

    expect(result.clientId).toBeNull()
    expect(result.payments[0]?.reference).toBeNull()
    expect(result.appliedDiscounts).toEqual([])
  })
})
