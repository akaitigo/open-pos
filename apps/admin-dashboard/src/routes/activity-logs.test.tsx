import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { ActivityLogsPage } from './activity-logs'
import { api } from '@/lib/api'

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/hooks/use-dark-mode', () => ({
  useDarkMode: () => ({ isDark: false, toggle: vi.fn() }),
}))

const mockApi = vi.mocked(api)

const mockLog = {
  id: 'log-1',
  staffId: 'staff-1',
  action: 'CREATE',
  entityType: 'Product',
  entityId: 'prod-1',
  details: '商品を作成',
  ipAddress: '192.168.1.1',
  createdAt: '2026-01-15T10:00:00Z',
}

function setupMocks(logs = [mockLog], totalPages = 1) {
  mockApi.get.mockResolvedValue({
    data: logs,
    pagination: { page: 1, pageSize: 20, totalCount: logs.length, totalPages },
  })
}

function renderPage() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <ActivityLogsPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('ActivityLogsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('操作履歴ヘッダーを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('操作履歴')).toBeInTheDocument()
  })

  it('操作ログ一覧を表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('CREATE')).toBeInTheDocument()
    })
    expect(screen.getByText('Product')).toBeInTheDocument()
    expect(screen.getByText('prod-1')).toBeInTheDocument()
  })

  it('ログが空の場合は空メッセージを表示する', async () => {
    setupMocks([])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('操作ログがありません')).toBeInTheDocument()
    })
  })

  it('操作ログカードタイトルを表示する', async () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('操作ログ')).toBeInTheDocument()
  })

  it('スタッフIDフィルタを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByPlaceholderText('スタッフIDでフィルタ')).toBeInTheDocument()
  })

  it('アクションフィルタドロップダウンを表示する', () => {
    setupMocks()
    renderPage()
    // select の中に全てのアクションオプションがある
    const selects = screen.getAllByRole('combobox')
    expect(selects.length).toBeGreaterThanOrEqual(1)
  })

  it('スタッフIDフィルタでAPIを再呼び出しする', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(mockApi.get).toHaveBeenCalled()
    })
    fireEvent.change(screen.getByPlaceholderText('スタッフIDでフィルタ'), {
      target: { value: 'staff-1' },
    })
    await waitFor(() => {
      expect(mockApi.get).toHaveBeenCalledWith(
        '/api/audit-logs',
        expect.anything(),
        expect.objectContaining({
          params: expect.objectContaining({ staffId: 'staff-1', page: 1 }),
        }),
      )
    })
  })

  it('entityId が null の場合はダッシュを表示する', async () => {
    setupMocks([{ ...mockLog, entityId: null }])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('-')).toBeInTheDocument()
    })
  })

  it('ページネーションが2ページ以上の場合にボタンを表示する', async () => {
    setupMocks([mockLog], 3)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('CREATE')).toBeInTheDocument()
    })
    expect(screen.getByText('前へ')).toBeInTheDocument()
    expect(screen.getByText('次へ')).toBeInTheDocument()
    expect(screen.getByText('1 / 3')).toBeInTheDocument()
  })

  it('API エラー時にクラッシュしない', async () => {
    mockApi.get.mockRejectedValue(new Error('network error'))
    renderPage()
    // 空のままで問題なし
    await waitFor(() => {
      expect(screen.getByText('操作ログがありません')).toBeInTheDocument()
    })
  })
})
