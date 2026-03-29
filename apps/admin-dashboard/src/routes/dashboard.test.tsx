import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { DashboardPage } from './dashboard'
import { beforeEach, describe, it, expect, vi } from 'vitest'
import { resetRuntimeConfigForTests } from '@/lib/runtime-config'

const mockApi = vi.hoisted(() => ({
  get: vi.fn().mockResolvedValue({
    data: [],
    pagination: { page: 1, pageSize: 1, totalCount: 0, totalPages: 0 },
  }),
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

  it('KPIカードを表示する', () => {
    renderWithProviders()
    expect(screen.getByText('本日の売上')).toBeInTheDocument()
    expect(screen.getByText('本日の取引数')).toBeInTheDocument()
    expect(screen.getByText('客単価')).toBeInTheDocument()
    expect(screen.getByText('昨日の売上')).toBeInTheDocument()
  })

  it('基本集計カードを表示する', () => {
    renderWithProviders()
    expect(screen.getByText('商品数')).toBeInTheDocument()
    expect(screen.getByText('店舗数')).toBeInTheDocument()
    expect(screen.getByText('スタッフ数')).toBeInTheDocument()
    expect(screen.getByText('取引数')).toBeInTheDocument()
  })

  it('日次売上チャートセクションを表示する', () => {
    renderWithProviders()
    expect(screen.getByText('過去7日間の売上推移')).toBeInTheDocument()
  })

  it('ローディング中は ... を表示する', () => {
    renderWithProviders()
    const loadingIndicators = screen.getAllByText('...')
    expect(loadingIndicators.length).toBeGreaterThanOrEqual(4)
  })

  it('API からデータを取得してカウントを表示する', async () => {
    mockApi.get.mockImplementation((path: string) => {
      if (path === '/api/products') {
        return Promise.resolve({
          data: [],
          pagination: { page: 1, pageSize: 1, totalCount: 42, totalPages: 42 },
        })
      }
      if (path === '/api/stores') {
        return Promise.resolve({
          data: [
            {
              id: 'store-1',
              organizationId: 'org-1',
              name: '渋谷店',
              address: null,
              phone: null,
              timezone: 'Asia/Tokyo',
              settings: '{}',
              isActive: true,
              createdAt: '2026-01-01T00:00:00Z',
              updatedAt: '2026-01-01T00:00:00Z',
            },
          ],
          pagination: { page: 1, pageSize: 1, totalCount: 3, totalPages: 3 },
        })
      }
      if (path === '/api/staff') {
        return Promise.resolve({
          data: [],
          pagination: { page: 1, pageSize: 1, totalCount: 10, totalPages: 10 },
        })
      }
      if (path === '/api/transactions') {
        return Promise.resolve({
          data: [],
          pagination: { page: 1, pageSize: 1, totalCount: 100, totalPages: 100 },
        })
      }
      if (path === '/api/analytics/daily-sales') {
        return Promise.resolve({
          data: [
            { date: '2026-03-17', grossAmount: 500000, taxAmount: 50000, transactionCount: 20 },
          ],
        })
      }
      if (path === '/api/analytics/summary') {
        return Promise.resolve({
          totalGross: 200000,
          totalTax: 20000,
          totalTransactions: 15,
          averageTransaction: 13333,
        })
      }
      return Promise.resolve({
        data: [],
        pagination: { page: 1, pageSize: 1, totalCount: 0, totalPages: 0 },
      })
    })

    renderWithProviders()

    await waitFor(() => {
      expect(screen.getByText('42')).toBeInTheDocument()
    })
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('100')).toBeInTheDocument()
  })

  it('売上サマリーと前日比を表示する', async () => {
    let summaryCallCount = 0
    mockApi.get.mockImplementation((path: string) => {
      if (path === '/api/stores') {
        return Promise.resolve({
          data: [
            {
              id: 'store-1',
              name: 'テスト店舗',
              organizationId: 'org-1',
              address: '',
              phone: '',
              timezone: 'Asia/Tokyo',
              isActive: true,
              createdAt: '2026-01-01',
              updatedAt: '2026-01-01',
            },
          ],
          pagination: { page: 1, pageSize: 1, totalCount: 1, totalPages: 1 },
        })
      }
      if (path === '/api/analytics/summary') {
        summaryCallCount++
        if (summaryCallCount === 1) {
          return Promise.resolve({
            totalGross: 300000,
            totalTax: 30000,
            totalTransactions: 15,
            averageTransaction: 20000,
          })
        }
        return Promise.resolve({
          totalGross: 200000,
          totalTax: 20000,
          totalTransactions: 10,
          averageTransaction: 20000,
        })
      }
      if (path === '/api/analytics/daily-sales') {
        return Promise.resolve({ data: [] })
      }
      return Promise.resolve({
        data: [],
        pagination: { page: 1, pageSize: 1, totalCount: 0, totalPages: 0 },
      })
    })

    renderWithProviders()

    await waitFor(() => {
      expect(screen.getByText('+50.0%')).toBeInTheDocument()
    })
    expect(screen.getByText('前日比')).toBeInTheDocument()
  })

  it('日次売上データがある場合にチャートバーを表示する', async () => {
    mockApi.get.mockImplementation(
      (path: string, _schema: unknown, _options?: { params?: Record<string, unknown> }) => {
        if (path === '/api/analytics/daily-sales') {
          return Promise.resolve({
            data: [
              { date: '2026-03-17', grossAmount: 100000, taxAmount: 10000, transactionCount: 5 },
              { date: '2026-03-18', grossAmount: 200000, taxAmount: 20000, transactionCount: 10 },
            ],
          })
        }
        if (path === '/api/analytics/summary') {
          return Promise.resolve({
            totalGross: 100000,
            totalTax: 10000,
            totalTransactions: 5,
            averageTransaction: 20000,
          })
        }
        if (path === '/api/stores') {
          return Promise.resolve({
            data: [
              {
                id: 'store-1',
                name: 'テスト店舗',
                organizationId: 'org-1',
                address: '',
                phone: '',
                timezone: 'Asia/Tokyo',
                isActive: true,
                createdAt: '2026-01-01',
                updatedAt: '2026-01-01',
              },
            ],
            pagination: { page: 1, pageSize: 1, totalCount: 1, totalPages: 1 },
          })
        }
        return Promise.resolve({
          data: [],
          pagination: { page: 1, pageSize: 1, totalCount: 0, totalPages: 0 },
        })
      },
    )

    renderWithProviders()

    await waitFor(
      () => {
        expect(screen.getByText('03-17')).toBeInTheDocument()
      },
      { timeout: 3000 },
    )
    expect(screen.getByText('03-18')).toBeInTheDocument()
  })
})
