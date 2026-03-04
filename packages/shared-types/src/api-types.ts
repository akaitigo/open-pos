import { z } from 'zod'
import { PaginationResponseSchema } from './pagination'
import { ProductSchema, CategorySchema, TaxRateSchema } from './product'
import { StoreSchema, StaffSchema } from './store'

/** API エラーレスポンススキーマ */
export const ApiErrorResponseSchema = z.object({
  error: z.string(),
  message: z.string(),
  details: z.record(z.unknown()).optional(),
})

export type ApiErrorResponse = z.infer<typeof ApiErrorResponseSchema>

/** ページネーション付きレスポンススキーマを生成する */
export function createPaginatedResponseSchema<T extends z.ZodTypeAny>(itemSchema: T) {
  return z.object({
    data: z.array(itemSchema),
    pagination: PaginationResponseSchema,
  })
}

/** ページネーション付きレスポンスの型 */
export type PaginatedResponse<T> = {
  data: T[]
  pagination: z.infer<typeof PaginationResponseSchema>
}

// --- ページネーション付きレスポンススキーマ ---

export const PaginatedProductsSchema = createPaginatedResponseSchema(ProductSchema)
export const PaginatedCategoriesSchema = createPaginatedResponseSchema(CategorySchema)
export const PaginatedStoresSchema = createPaginatedResponseSchema(StoreSchema)
export const PaginatedStaffSchema = createPaginatedResponseSchema(StaffSchema)
export const PaginatedTaxRatesSchema = createPaginatedResponseSchema(TaxRateSchema)
