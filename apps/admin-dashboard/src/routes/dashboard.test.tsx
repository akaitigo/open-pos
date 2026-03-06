import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { DashboardPage } from './dashboard'
import { vi } from 'vitest'

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue({
      data: [],
      pagination: { page: 1, pageSize: 1, totalCount: 0, totalPages: 0 },
    }),
    setOrganizationId: vi.fn(),
  },
}))

function renderWithProviders() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <DashboardPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('DashboardPage', () => {
  it('ダッシュボードヘッダーを表示する', () => {
    renderWithProviders()
    expect(screen.getByText('ダッシュボード')).toBeInTheDocument()
  })

  it('サマリーカードを表示する', () => {
    renderWithProviders()
    expect(screen.getByText('商品数')).toBeInTheDocument()
    expect(screen.getByText('店舗数')).toBeInTheDocument()
    expect(screen.getByText('スタッフ数')).toBeInTheDocument()
    expect(screen.getByText('取引数')).toBeInTheDocument()
  })

  it('ローディング中は ... を表示する', () => {
    renderWithProviders()
    const loadingIndicators = screen.getAllByText('...')
    expect(loadingIndicators.length).toBeGreaterThanOrEqual(4)
  })
})
