import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { App } from './App'

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue({
      data: [],
      pagination: { page: 1, pageSize: 1, totalCount: 0, totalPages: 0 },
    }),
    setOrganizationId: vi.fn(),
  },
}))

describe('App', () => {
  it('クラッシュせずにレンダリングできる', () => {
    render(<App />)

    expect(screen.getByText('OpenPOS')).toBeInTheDocument()
  })

  it('サイドバーにダッシュボードリンクが表示される', () => {
    render(<App />)

    expect(screen.getAllByText('ダッシュボード').length).toBeGreaterThanOrEqual(1)
  })

  it('ダッシュボードページにサマリーカードが表示される', () => {
    render(<App />)

    expect(screen.getByText('商品数')).toBeInTheDocument()
  })
})
