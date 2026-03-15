import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useTaxRates } from './use-tax-rates'

const mockApiGet = vi.fn()

vi.mock('@/lib/api', () => ({
  api: {
    get: (...args: unknown[]) => mockApiGet(...args),
    post: vi.fn().mockResolvedValue({}),
    setOrganizationId: vi.fn(),
  },
}))

const mockTaxRates = [
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
  {
    id: '550e8400-e29b-41d4-a716-446655440021',
    organizationId: '550e8400-e29b-41d4-a716-446655440000',
    name: '軽減税率',
    rate: '0.08',
    isReduced: true,
    isDefault: false,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

describe('useTaxRates', () => {
  beforeEach(() => {
    mockApiGet.mockReset()
  })

  it('API から税率を取得する', async () => {
    mockApiGet.mockResolvedValue(mockTaxRates)

    const { result } = renderHook(() => useTaxRates())

    expect(result.current).toEqual([])

    await waitFor(() => {
      expect(result.current).toHaveLength(2)
    })

    expect(result.current[0]!.name).toBe('標準税率')
    expect(result.current[1]!.name).toBe('軽減税率')
  })

  it('API エラー時は空配列を返す', async () => {
    mockApiGet.mockRejectedValue(new Error('network error'))

    const { result } = renderHook(() => useTaxRates())

    await waitFor(() => {
      expect(mockApiGet).toHaveBeenCalled()
    })

    expect(result.current).toEqual([])
  })

  it('初期状態は空配列', () => {
    mockApiGet.mockResolvedValue(mockTaxRates)

    const { result } = renderHook(() => useTaxRates())
    expect(result.current).toEqual([])
  })
})
