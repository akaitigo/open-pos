import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { App } from './App'
import { resetRuntimeConfigForTests } from '@/lib/runtime-config'

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue({
      data: [],
      pagination: { page: 1, pageSize: 1, totalCount: 0, totalPages: 0 },
    }),
    setOrganizationId: vi.fn(),
    setBaseUrl: vi.fn(),
  },
  configureApi: vi.fn(),
  getDefaultApiConfig: () => ({
    apiUrl: 'http://localhost:8080',
    organizationId: '00000000-0000-0000-0000-000000000000',
  }),
}))

describe('App', () => {
  beforeEach(() => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '00000000-0000-0000-0000-000000000000',
    })
  })

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

  it('KPIカードが表示される', () => {
    render(<App />)

    expect(screen.getByText('本日の売上')).toBeInTheDocument()
    expect(screen.getByText('本日の取引数')).toBeInTheDocument()
  })

  it('売上推移セクションが表示される', () => {
    render(<App />)

    expect(screen.getByText('過去7日間の売上推移')).toBeInTheDocument()
  })
})
