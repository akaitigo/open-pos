import { z } from 'zod'

export const PaginationRequestSchema = z.object({
  page: z.number().int().min(1).default(1),
  pageSize: z.number().int().min(1).max(100).default(20),
})

export type PaginationRequest = z.infer<typeof PaginationRequestSchema>

export const PaginationResponseSchema = z.object({
  page: z.number().int(),
  pageSize: z.number().int(),
  totalCount: z.number().int(),
  totalPages: z.number().int(),
})

export type PaginationResponse = z.infer<typeof PaginationResponseSchema>

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
