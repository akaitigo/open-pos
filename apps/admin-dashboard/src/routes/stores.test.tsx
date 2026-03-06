import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { StoresPage } from './stores'
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

const mockStore = {
  id: 'store-1',
  organizationId: 'org-1',
  name: '渋谷店',
  address: '東京都渋谷区' as string | null,
  phone: '03-1234-5678' as string | null,
  timezone: 'Asia/Tokyo',
  settings: '{}',
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const mockTerminals = [
  {
    id: 'term-1',
    organizationId: 'org-1',
    storeId: 'store-1',
    terminalCode: 'POS-001',
    name: 'レジ1',
    isActive: true,
    lastSyncAt: '2026-01-01T10:00:00Z',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

function setupMocks(stores = [mockStore], totalPages = 1) {
  mockApi.get.mockImplementation((path: string) => {
    if (path === '/api/stores') {
      return Promise.resolve({
        data: stores,
        pagination: { page: 1, pageSize: 20, totalCount: stores.length, totalPages },
      })
    }
    if (path.includes('/terminals')) return Promise.resolve(mockTerminals)
    return Promise.resolve([])
  })
}

function renderPage() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <StoresPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('StoresPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('店舗テーブルを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('渋谷店')).toBeInTheDocument()
    })
    expect(screen.getByText('東京都渋谷区')).toBeInTheDocument()
    expect(screen.getByText('03-1234-5678')).toBeInTheDocument()
    expect(screen.getByText('Asia/Tokyo')).toBeInTheDocument()
    expect(screen.getByText('有効')).toBeInTheDocument()
  })

  it('店舗が空の場合は空メッセージを表示する', async () => {
    setupMocks([], 0)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('店舗が登録されていません')).toBeInTheDocument()
    })
  })

  it('店舗管理ヘッダーを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('店舗管理')).toBeInTheDocument()
  })

  it('店舗を追加ボタンでダイアログが開く', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('渋谷店')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: '店舗を追加' }))
    await waitFor(() => {
      expect(
        screen.getByText('店舗を追加', { selector: '[class*="DialogTitle"], h2' }),
      ).toBeInTheDocument()
    })
  })

  it('編集ボタンで編集ダイアログが開く', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('渋谷店')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('編集'))
    await waitFor(() => {
      expect(screen.getByText('店舗を編集')).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('渋谷店')).toBeInTheDocument()
    expect(screen.getByDisplayValue('東京都渋谷区')).toBeInTheDocument()
  })

  it('端末ボタンで端末ダイアログが開く', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('渋谷店')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('端末'))
    await waitFor(() => {
      expect(screen.getByText('渋谷店 — 端末管理')).toBeInTheDocument()
    })
    await waitFor(() => {
      expect(screen.getByText('POS-001')).toBeInTheDocument()
    })
    expect(screen.getByText('レジ1')).toBeInTheDocument()
  })

  it('フォーム送信で新規作成APIを呼ぶ', async () => {
    setupMocks()
    mockApi.post.mockResolvedValue(mockStore)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('渋谷店')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: '店舗を追加' }))
    await waitFor(() => {
      expect(screen.getByLabelText('店舗名 *')).toBeInTheDocument()
    })
    fireEvent.change(screen.getByLabelText('店舗名 *'), { target: { value: '新宿店' } })
    fireEvent.click(screen.getByRole('button', { name: '追加' }))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith(
        '/api/stores',
        expect.objectContaining({ name: '新宿店', timezone: 'Asia/Tokyo' }),
        expect.anything(),
      )
    })
  })

  it('無効な店舗はバッジに無効と表示する', async () => {
    const inactiveStore = { ...mockStore, id: 'store-2', isActive: false }
    setupMocks([inactiveStore])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('無効')).toBeInTheDocument()
    })
  })

  it('住所・電話なしの店舗はダッシュを表示する', async () => {
    const noDetail = { ...mockStore, id: 'store-3', address: null, phone: null }
    setupMocks([noDetail])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('渋谷店')).toBeInTheDocument()
    })
    const cells = screen.getAllByRole('cell')
    const dashCells = cells.filter((c) => c.textContent === '—')
    expect(dashCells.length).toBeGreaterThanOrEqual(2)
  })

  it('端末ダイアログで端末が空の場合はメッセージを表示する', async () => {
    mockApi.get.mockImplementation((path: string) => {
      if (path === '/api/stores') {
        return Promise.resolve({
          data: [mockStore],
          pagination: { page: 1, pageSize: 20, totalCount: 1, totalPages: 1 },
        })
      }
      if (path.includes('/terminals')) return Promise.resolve([])
      return Promise.resolve([])
    })
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('渋谷店')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('端末'))
    await waitFor(() => {
      expect(screen.getByText('端末が登録されていません')).toBeInTheDocument()
    })
  })

  it('端末登録フォームで送信するとAPIを呼ぶ', async () => {
    setupMocks()
    mockApi.post.mockResolvedValue(mockTerminals[0])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('渋谷店')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('端末'))
    await waitFor(() => {
      expect(screen.getByText('渋谷店 — 端末管理')).toBeInTheDocument()
    })
    fireEvent.change(screen.getByLabelText('端末コード'), { target: { value: 'POS-002' } })
    fireEvent.change(screen.getByLabelText('端末名'), { target: { value: 'レジ2' } })
    fireEvent.click(screen.getByRole('button', { name: '登録' }))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith(
        '/api/stores/store-1/terminals',
        { terminalCode: 'POS-002', name: 'レジ2' },
        expect.anything(),
      )
    })
  })
})
