import { describe, it, expect } from 'vitest'
import { PaginationRequestSchema, PaginationResponseSchema } from './pagination'

describe('PaginationRequestSchema', () => {
  it('有効なページネーションリクエストをパースできる', () => {
    const result = PaginationRequestSchema.parse({ page: 1, pageSize: 20 })

    expect(result.page).toBe(1)
    expect(result.pageSize).toBe(20)
  })

  it('デフォルト値が適用される', () => {
    const result = PaginationRequestSchema.parse({})

    expect(result.page).toBe(1)
    expect(result.pageSize).toBe(20)
  })

  it('page が 0 以下の場合はバリデーションエラー', () => {
    expect(() => PaginationRequestSchema.parse({ page: 0, pageSize: 20 })).toThrow()
  })

  it('pageSize が 100 を超える場合はバリデーションエラー', () => {
    expect(() => PaginationRequestSchema.parse({ page: 1, pageSize: 101 })).toThrow()
  })

  it('pageSize が 0 以下の場合はバリデーションエラー', () => {
    expect(() => PaginationRequestSchema.parse({ page: 1, pageSize: 0 })).toThrow()
  })
})

describe('PaginationResponseSchema', () => {
  it('有効なページネーションレスポンスをパースできる', () => {
    const result = PaginationResponseSchema.parse({
      page: 1,
      pageSize: 20,
      totalCount: 100,
      totalPages: 5,
    })

    expect(result.page).toBe(1)
    expect(result.pageSize).toBe(20)
    expect(result.totalCount).toBe(100)
    expect(result.totalPages).toBe(5)
  })

  it('必須フィールドが欠けている場合はバリデーションエラー', () => {
    expect(() =>
      PaginationResponseSchema.parse({
        page: 1,
        pageSize: 20,
      }),
    ).toThrow()
  })

  it('整数以外の値はバリデーションエラー', () => {
    expect(() =>
      PaginationResponseSchema.parse({
        page: 1.5,
        pageSize: 20,
        totalCount: 100,
        totalPages: 5,
      }),
    ).toThrow()
  })
})
