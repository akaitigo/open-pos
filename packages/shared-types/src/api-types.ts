import { z } from 'zod'
import { MoneySchema } from './money'
import { PaginationResponseSchema } from './pagination'

/** API エラーレスポンススキーマ */
export const ApiErrorResponseSchema = z.object({
  code: z.string(),
  message: z.string(),
  details: z.record(z.unknown()).optional(),
})

export type ApiErrorResponse = z.infer<typeof ApiErrorResponseSchema>

/** ページネーション付きレスポンススキーマを生成する */
export function createPaginatedResponseSchema<T extends z.ZodTypeAny>(itemSchema: T) {
  return z.object({
    items: z.array(itemSchema),
    pagination: PaginationResponseSchema,
  })
}

/** ページネーション付きレスポンスの型 */
export type PaginatedResponse<T> = {
  items: T[]
  pagination: z.infer<typeof PaginationResponseSchema>
}

// --- ドメインスキーマ ---

/** 税率スキーマ */
export const TaxRateSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  rate: z.number().int(),
  isDefault: z.boolean(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
})

export type TaxRate = z.infer<typeof TaxRateSchema>

/** カテゴリスキーマ */
export const CategorySchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  description: z.string().nullable(),
  parentId: z.string().uuid().nullable(),
  sortOrder: z.number().int(),
  isActive: z.boolean(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
})

export type Category = z.infer<typeof CategorySchema>

/** 商品スキーマ */
export const ProductSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  description: z.string().nullable(),
  sku: z.string().nullable(),
  barcode: z.string().nullable(),
  price: MoneySchema,
  costPrice: MoneySchema.nullable(),
  categoryId: z.string().uuid().nullable(),
  taxRateId: z.string().uuid().nullable(),
  isActive: z.boolean(),
  imageUrl: z.string().url().nullable(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
})

export type Product = z.infer<typeof ProductSchema>

/** 店舗スキーマ */
export const StoreSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  address: z.string().nullable(),
  phone: z.string().nullable(),
  isActive: z.boolean(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
})

export type Store = z.infer<typeof StoreSchema>

/** スタッフスキーマ */
export const StaffSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  email: z.string().email(),
  role: z.enum(['ADMIN', 'MANAGER', 'CASHIER']),
  storeId: z.string().uuid().nullable(),
  isActive: z.boolean(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
})

export type Staff = z.infer<typeof StaffSchema>

// --- ページネーション付きレスポンススキーマ ---

export const PaginatedProductsSchema = createPaginatedResponseSchema(ProductSchema)
export const PaginatedCategoriesSchema = createPaginatedResponseSchema(CategorySchema)
export const PaginatedStoresSchema = createPaginatedResponseSchema(StoreSchema)
export const PaginatedStaffSchema = createPaginatedResponseSchema(StaffSchema)
export const PaginatedTaxRatesSchema = createPaginatedResponseSchema(TaxRateSchema)
