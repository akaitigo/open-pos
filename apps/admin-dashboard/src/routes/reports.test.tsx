import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { ReportsPage } from './reports'
import { beforeEach, describe, it, expect, vi } from 'vitest'
import { resetRuntimeConfigForTests } from '@/lib/runtime-config'

const mockApi = vi.hoisted(() => ({
  get: vi.fn().mockResolvedValue({ data: [] }),
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
        <ReportsPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('ReportsPage', () => {
  beforeEach(() => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '00000000-0000-0000-0000-000000000000',
    })
  })

  it('レポートヘッダーを表示する', () => {
    renderPage()
    expect(screen.getByText('レポート')).toBeInTheDocument()
  })

  it('日付入力フィールドを表示する', () => {
    renderPage()
    expect(screen.getByLabelText('開始日')).toBeInTheDocument()
    expect(screen.getByLabelText('終了日')).toBeInTheDocument()
  })

  it('レポート生成ボタンを表示する', () => {
    renderPage()
    expect(screen.getByText('レポート生成')).toBeInTheDocument()
  })

  it('レポート生成ボタンクリックでデータを表示する', async () => {
    const user = userEvent.setup()
    const mockDailySales = {
      data: [{ date: '2026-03-01', grossAmount: 100000, taxAmount: 10000, transactionCount: 5 }],
    }
    const mockSummary = {
      totalGross: 100000,
      totalNet: 90000,
      totalTax: 10000,
      totalDiscount: 0,
      totalTransactions: 5,
      averageTransaction: 20000,
    }

    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('daily-sales')) return Promise.resolve(mockDailySales)
      if (path.includes('summary')) return Promise.resolve(mockSummary)
      return Promise.resolve({ data: [] })
    })

    renderPage()
    await user.click(screen.getByText('レポート生成'))

    await waitFor(() => {
      expect(screen.getByText('期間サマリー')).toBeInTheDocument()
    })
    expect(screen.getByText('日次売上明細')).toBeInTheDocument()
    expect(screen.getByText('2026-03-01')).toBeInTheDocument()
  })

  it('データ表示後に印刷ボタンが表示される', async () => {
    const user = userEvent.setup()
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('daily-sales'))
        return Promise.resolve({
          data: [
            { date: '2026-03-01', grossAmount: 100000, taxAmount: 10000, transactionCount: 5 },
          ],
        })
      if (path.includes('summary'))
        return Promise.resolve({
          totalGross: 100000,
          totalNet: 90000,
          totalTax: 10000,
          totalDiscount: 0,
          totalTransactions: 5,
          averageTransaction: 20000,
        })
      return Promise.resolve({ data: [] })
    })

    renderPage()
    await user.click(screen.getByText('レポート生成'))

    await waitFor(() => {
      expect(screen.getByText('印刷')).toBeInTheDocument()
    })
  })
})
