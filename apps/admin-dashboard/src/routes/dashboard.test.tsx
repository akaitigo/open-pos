import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { DashboardPage } from './dashboard'
import { beforeEach, describe, it, expect, vi } from 'vitest'
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
  beforeEach(() => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '00000000-0000-0000-0000-000000000000',
    })
  })

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
