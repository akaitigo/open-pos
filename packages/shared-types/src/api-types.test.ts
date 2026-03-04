import { describe, it, expect } from 'vitest'
import {
  ProductSchema,
  CategorySchema,
  StoreSchema,
  StaffSchema,
  TaxRateSchema,
  createPaginatedResponseSchema,
} from './api-types'
import { z } from 'zod'

const now = new Date().toISOString()

describe('ProductSchema', () => {
  it('有効な商品データをパースできる', () => {
    const product = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      organizationId: '550e8400-e29b-41d4-a716-446655440001',
      name: 'テスト商品',
      description: null,
      sku: 'SKU-001',
      barcode: '4901234567890',
      price: { amount: 10000, currency: 'JPY' },
      costPrice: null,
      categoryId: null,
      taxRateId: null,
      isActive: true,
      imageUrl: null,
      createdAt: now,
      updatedAt: now,
    }

    const result = ProductSchema.parse(product)
    expect(result.name).toBe('テスト商品')
    expect(result.price.amount).toBe(10000)
  })

  it('必須フィールドが欠けている場合はバリデーションエラー', () => {
    expect(() => ProductSchema.parse({ name: 'test' })).toThrow()
  })
})

describe('CategorySchema', () => {
  it('有効なカテゴリデータをパースできる', () => {
    const category = {
      id: '550e8400-e29b-41d4-a716-446655440000',
      organizationId: '550e8400-e29b-41d4-a716-446655440001',
      name: '食品',
      description: null,
      parentId: null,
      sortOrder: 1,
      isActive: true,
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
      name: '田中太郎',
      email: 'tanaka@example.com',
      role: 'CASHIER',
      storeId: null,
      isActive: true,
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
      name: '田中太郎',
      email: 'tanaka@example.com',
      role: 'INVALID_ROLE',
      storeId: null,
      isActive: true,
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
      rate: 10,
      isDefault: true,
      createdAt: now,
      updatedAt: now,
    }

    const result = TaxRateSchema.parse(taxRate)
    expect(result.name).toBe('標準税率')
    expect(result.rate).toBe(10)
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
      items: [
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
    expect(result.items).toHaveLength(2)
    expect(result.pagination.totalCount).toBe(2)
  })
})
