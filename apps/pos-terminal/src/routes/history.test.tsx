import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { HistoryPage } from './history'
import { useAuthStore } from '@/stores/auth-store'

beforeEach(() => {
  useAuthStore.setState({
    isAuthenticated: true,
    staff: null,
    storeId: '00000000-0000-0000-0000-000000000001',
    storeName: 'テスト店舗',
    terminalId: null,
  })

  vi.spyOn(globalThis, 'fetch').mockResolvedValue(
    new Response(
      JSON.stringify({
        data: [],
        pagination: { page: 1, pageSize: 20, totalCount: 0, totalPages: 0 },
      }),
      {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      },
    ),
  )
})

describe('HistoryPage', () => {
  it('取引履歴テーブルが表示される', async () => {
    render(<HistoryPage />)

    expect(screen.getByText('取引履歴')).toBeInTheDocument()
    expect(screen.getByText('取引番号')).toBeInTheDocument()
    expect(screen.getByText('取引履歴がありません')).toBeInTheDocument()
  })
})
