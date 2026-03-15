import { z } from 'zod'
import { createPaginatedResponseSchema } from './pagination'
import { ProductSchema, CategorySchema, TaxRateSchema } from './product'
import { StoreSchema, StaffSchema } from './store'

// Re-export for backward compatibility
export { createPaginatedResponseSchema } from './pagination'
export type { PaginatedResponse } from './pagination'

/** API エラーレスポンススキーマ */
export const ApiErrorResponseSchema = z.object({
  error: z.string(),
  message: z.string(),
  details: z.record(z.string(), z.unknown()).optional(),
})

export type ApiErrorResponse = z.infer<typeof ApiErrorResponseSchema>

// --- ページネーション付きレスポンススキーマ ---
// NOTE: PaginatedStocksSchema is exported from ./inventory to avoid circular dependency

export const PaginatedProductsSchema = createPaginatedResponseSchema(ProductSchema)
export const PaginatedCategoriesSchema = createPaginatedResponseSchema(CategorySchema)
export const PaginatedStoresSchema = createPaginatedResponseSchema(StoreSchema)
export const PaginatedStaffSchema = createPaginatedResponseSchema(StaffSchema)
export const PaginatedTaxRatesSchema = createPaginatedResponseSchema(TaxRateSchema)
