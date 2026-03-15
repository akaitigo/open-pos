import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { DiscountsPage } from './discounts'
import { beforeEach, describe, it, expect, vi } from 'vitest'
import { resetRuntimeConfigForTests } from '@/lib/runtime-config'

const mockDiscounts = [
  {
    id: '44444444-4444-4444-4444-444444444444',
    organizationId: '00000000-0000-0000-0000-000000000000',
    name: '10%オフ',
    discountType: 'PERCENTAGE' as const,
    value: '10',
    startDate: null,
    endDate: null,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

const mockCoupons = [
  {
    id: '55555555-5555-5555-5555-555555555555',
    organizationId: '00000000-0000-0000-0000-000000000000',
    code: 'WELCOME2024',
    discountId: '44444444-4444-4444-4444-444444444444',
    maxUses: 100,
    currentUses: 5,
    startDate: null,
    endDate: null,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

const mockApi = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
  setOrganizationId: vi.fn(),
  setBaseUrl: vi.fn(),
}))

vi.mock('@/lib/api', () => ({
  api: mockApi,
  configureApi: vi.fn(),
  getDefaultApiConfig: () => ({
    apiUrl: 'http://localhost:8080',
    organizationId: '00000000-0000-0000-0000-000000000000',
  }),
}))

function renderPage() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <DiscountsPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('DiscountsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/coupons')) return Promise.resolve(mockCoupons)
      return Promise.resolve(mockDiscounts)
    })
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '00000000-0000-0000-0000-000000000000',
    })
  })

  it('割引・クーポン管理ヘッダーを表示する', () => {
    renderPage()
    expect(screen.getByText('割引・クーポン管理')).toBeInTheDocument()
  })

  it('割引タブに割引一覧を表示する', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('10%オフ')).toBeInTheDocument()
    })
    expect(screen.getByText('パーセント')).toBeInTheDocument()
    expect(screen.getByText('10%')).toBeInTheDocument()
  })

  it('割引を追加ボタンを表示する', () => {
    renderPage()
    expect(screen.getByText('割引を追加')).toBeInTheDocument()
  })

  it('タブを切り替えてクーポン一覧を表示する', async () => {
    const user = userEvent.setup()
    renderPage()
    // クーポンタブをクリック
    const couponTab = screen.getByRole('tab', { name: 'クーポン' })
    await user.click(couponTab)
    await waitFor(() => {
      expect(screen.getByText('WELCOME2024')).toBeInTheDocument()
    })
  })
})
