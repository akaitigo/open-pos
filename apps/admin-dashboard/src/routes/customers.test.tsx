import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { CustomersPage } from './customers'
import { api } from '@/lib/api'

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

const mockApi = vi.mocked(api)

const mockCustomer: {
  id: string
  organizationId: string
  name: string
  email: string | null
  phone: string | null
  points: number
  createdAt: string
  updatedAt: string
} = {
  id: '11111111-1111-1111-1111-111111111111',
  organizationId: 'org-1',
  name: '田中太郎',
  email: 'tanaka@example.com',
  phone: '090-1234-5678',
  points: 1500,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

function setupMocks(customers = [mockCustomer]) {
  mockApi.get.mockResolvedValue({
    data: customers,
    pagination: { page: 1, pageSize: 50, totalCount: customers.length, totalPages: 1 },
  })
}

function renderPage() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <CustomersPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('CustomersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('顧客管理ヘッダーを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('顧客管理')).toBeInTheDocument()
  })

  it('顧客テーブルを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    expect(screen.getByText('tanaka@example.com')).toBeInTheDocument()
    expect(screen.getByText('090-1234-5678')).toBeInTheDocument()
    expect(screen.getByText('1,500 pt')).toBeInTheDocument()
  })

  it('顧客が空の場合は空メッセージを表示する', async () => {
    setupMocks([])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('顧客が登録されていません')).toBeInTheDocument()
    })
  })

  it('読み込み中のメッセージを表示する', () => {
    mockApi.get.mockReturnValue(new Promise(() => {})) // never resolve
    renderPage()
    expect(screen.getByText('読み込み中...')).toBeInTheDocument()
  })

  it('メールなしの顧客はダッシュを表示する', async () => {
    setupMocks([{ ...mockCustomer, email: null }])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    const cells = screen.getAllByRole('cell')
    const dashCell = cells.find((c) => c.textContent === '-')
    expect(dashCell).toBeTruthy()
  })

  it('電話番号なしの顧客はダッシュを表示する', async () => {
    setupMocks([{ ...mockCustomer, phone: null }])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    const cells = screen.getAllByRole('cell')
    const dashCell = cells.find((c) => c.textContent === '-')
    expect(dashCell).toBeTruthy()
  })

  it('検索入力でAPIを再呼び出しする', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(mockApi.get).toHaveBeenCalled()
    })
    const searchInput = screen.getByPlaceholderText('顧客名で検索...')
    fireEvent.change(searchInput, { target: { value: '田中' } })
    await waitFor(() => {
      expect(mockApi.get).toHaveBeenCalledWith(
        '/api/customers',
        expect.anything(),
        expect.objectContaining({
          params: expect.objectContaining({ search: '田中', page: 1 }),
        }),
      )
    })
  })

  it('顧客を追加ボタンを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('顧客を追加')).toBeInTheDocument()
  })

  it('顧客一覧カードタイトルを表示する', async () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('顧客一覧')).toBeInTheDocument()
  })

  it('API エラー時にクラッシュせず空の一覧を表示する', async () => {
    mockApi.get.mockRejectedValue(new Error('network error'))
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('顧客が登録されていません')).toBeInTheDocument()
    })
  })
})
