import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { StaffPage } from './staff'
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

const mockStores = {
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
    {
      id: 'store-2',
      organizationId: 'org-1',
      name: '新宿店',
      address: null,
      phone: null,
      timezone: 'Asia/Tokyo',
      settings: '{}',
      isActive: true,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
  ],
  pagination: { page: 1, pageSize: 100, totalCount: 2, totalPages: 1 },
}

const mockStaff = [
  {
    id: 'staff-1',
    organizationId: 'org-1',
    storeId: 'store-1',
    name: '田中太郎',
    email: 'tanaka@example.com',
    role: 'OWNER' as const,
    isActive: true,
    failedPinAttempts: 0,
    isLocked: false,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'staff-2',
    organizationId: 'org-1',
    storeId: 'store-1',
    name: '佐藤花子',
    email: null,
    role: 'CASHIER' as const,
    isActive: false,
    failedPinAttempts: 5,
    isLocked: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

function setupMocks(staffList = mockStaff) {
  mockApi.get.mockImplementation((path: string) => {
    if (path === '/api/stores') {
      return Promise.resolve(mockStores)
    }
    if (path === '/api/staff') {
      return Promise.resolve({
        data: staffList,
        pagination: { page: 1, pageSize: 20, totalCount: staffList.length, totalPages: 1 },
      })
    }
    return Promise.resolve([])
  })
}

function renderPage() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <StaffPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('StaffPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('スタッフ管理ヘッダーを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('スタッフ管理')).toBeInTheDocument()
  })

  it('店舗セレクタとスタッフテーブルを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    expect(screen.getByText('tanaka@example.com')).toBeInTheDocument()
    expect(screen.getByText('オーナー')).toBeInTheDocument()
  })

  it('ロールバッジを正しく表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('オーナー')).toBeInTheDocument()
    })
    expect(screen.getByText('キャッシャー')).toBeInTheDocument()
  })

  it('ロック中のスタッフにロックバッジを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('ロック中')).toBeInTheDocument()
    })
  })

  it('無効なスタッフには無効バッジを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('無効')).toBeInTheDocument()
    })
  })

  it('スタッフが空の場合は空メッセージを表示する', async () => {
    setupMocks([])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('スタッフが登録されていません')).toBeInTheDocument()
    })
  })

  it('店舗が未登録の場合はメッセージを表示する', async () => {
    mockApi.get.mockImplementation((path: string) => {
      if (path === '/api/stores') {
        return Promise.resolve({
          data: [],
          pagination: { page: 1, pageSize: 100, totalCount: 0, totalPages: 0 },
        })
      }
      return Promise.resolve({
        data: [],
        pagination: { page: 1, pageSize: 20, totalCount: 0, totalPages: 0 },
      })
    })
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('店舗が未登録です')).toBeInTheDocument()
    })
  })

  it('編集ボタンで編集ダイアログが開く', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    const editButtons = screen.getAllByText('編集')
    fireEvent.click(editButtons[0]!)
    await waitFor(() => {
      expect(screen.getByText('スタッフを編集')).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('田中太郎')).toBeInTheDocument()
  })

  it('スタッフを追加ボタンでダイアログが開く', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: 'スタッフを追加' }))
    await waitFor(() => {
      expect(
        screen.getByText('スタッフを追加', { selector: '[class*="DialogTitle"], h2' }),
      ).toBeInTheDocument()
    })
  })

  it('メールなしのスタッフはダッシュを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('佐藤花子')).toBeInTheDocument()
    })
    const cells = screen.getAllByRole('cell')
    const dashCells = cells.filter((c) => c.textContent === '—')
    expect(dashCells.length).toBeGreaterThanOrEqual(1)
  })
})
