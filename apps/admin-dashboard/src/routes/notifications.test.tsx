import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { NotificationsPage } from './notifications'
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

const mockNotification = {
  id: '22222222-2222-2222-2222-222222222222',
  organizationId: 'org-1',
  type: 'LOW_STOCK',
  title: '在庫アラート',
  message: 'コーヒー豆の在庫が残り5個です',
  isRead: false,
  createdAt: '2026-01-15T10:00:00Z',
  updatedAt: '2026-01-15T10:00:00Z',
}

function setupMocks(notifications = [mockNotification]) {
  mockApi.get.mockResolvedValue({
    data: notifications,
    pagination: { page: 1, pageSize: 50, totalCount: notifications.length, totalPages: 1 },
  })
}

function renderPage() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <NotificationsPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('通知ヘッダーを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('通知')).toBeInTheDocument()
  })

  it('通知一覧を表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('在庫アラート')).toBeInTheDocument()
    })
    expect(screen.getByText('コーヒー豆の在庫が残り5個です')).toBeInTheDocument()
    expect(screen.getByText('LOW_STOCK')).toBeInTheDocument()
  })

  it('未読の通知には未読バッジを表示する', async () => {
    setupMocks([mockNotification])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('未読')).toBeInTheDocument()
    })
  })

  it('既読の通知には未読バッジを表示しない', async () => {
    setupMocks([{ ...mockNotification, isRead: true }])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('在庫アラート')).toBeInTheDocument()
    })
    expect(screen.queryByText('未読')).not.toBeInTheDocument()
  })

  it('通知が空の場合は空メッセージを表示する', async () => {
    setupMocks([])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('通知はありません')).toBeInTheDocument()
    })
  })

  it('読み込み中のメッセージを表示する', () => {
    mockApi.get.mockReturnValue(new Promise(() => {}))
    renderPage()
    expect(screen.getByText('読み込み中...')).toBeInTheDocument()
  })

  it('すべて既読ボタンを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('すべて既読')).toBeInTheDocument()
  })

  it('通知一覧カードタイトルを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('通知一覧')).toBeInTheDocument()
  })

  it('API エラー時にクラッシュしない', async () => {
    mockApi.get.mockRejectedValue(new Error('network error'))
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('通知はありません')).toBeInTheDocument()
    })
  })

  it('通知の作成日時を表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('2026-01-15T10:00:00Z')).toBeInTheDocument()
    })
  })
})
