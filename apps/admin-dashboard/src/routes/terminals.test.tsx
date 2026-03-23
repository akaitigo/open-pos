import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { TerminalsPage } from './terminals'
import { beforeEach, describe, it, expect, vi } from 'vitest'
import { resetRuntimeConfigForTests } from '@/lib/runtime-config'

const mockStores = {
  data: [
    {
      id: '22222222-2222-2222-2222-222222222222',
      organizationId: '00000000-0000-0000-0000-000000000000',
      name: '本店',
      address: null,
      phone: null,
      timezone: 'Asia/Tokyo',
      settings: '{}',
      isActive: true,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
  ],
  pagination: { page: 1, pageSize: 100, totalCount: 1, totalPages: 1 },
}

const mockTerminals = [
  {
    id: '33333333-3333-3333-3333-333333333333',
    organizationId: '00000000-0000-0000-0000-000000000000',
    storeId: '22222222-2222-2222-2222-222222222222',
    terminalCode: 'POS-001',
    name: 'レジ1',
    isActive: true,
    lastSyncAt: '2026-03-15T10:00:00Z',
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
        <TerminalsPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('TerminalsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/terminals')) return Promise.resolve(mockTerminals)
      return Promise.resolve(mockStores)
    })
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '00000000-0000-0000-0000-000000000000',
    })
  })

  it('端末管理ヘッダーを表示する', () => {
    renderPage()
    expect(screen.getByText('端末管理')).toBeInTheDocument()
  })

  it('端末一覧を表示する', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('POS-001')).toBeInTheDocument()
    })
    expect(screen.getByText('レジ1')).toBeInTheDocument()
    expect(screen.getByText('オンライン')).toBeInTheDocument()
  })

  it('端末を登録ボタンを表示する', () => {
    renderPage()
    expect(screen.getByText('端末を登録')).toBeInTheDocument()
  })

  it('端末が空の場合に空メッセージを表示する', async () => {
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/terminals')) return Promise.resolve([])
      return Promise.resolve(mockStores)
    })
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('端末が登録されていません')).toBeInTheDocument()
    })
  })

  it('端末登録ダイアログを表示してフォーム送信する', async () => {
    const user = (await import('@testing-library/user-event')).default.setup()
    mockApi.post.mockResolvedValue(mockTerminals[0])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('POS-001')).toBeInTheDocument()
    })
    await user.click(screen.getByText('端末を登録'))
    await waitFor(() => {
      expect(screen.getByText('端末を登録', { selector: 'h2' })).toBeInTheDocument()
    })
    await user.type(screen.getByLabelText('端末コード *'), 'POS-002')
    await user.type(screen.getByLabelText('端末名 *'), 'レジ2')
    await user.click(screen.getByRole('button', { name: '登録' }))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith(
        expect.stringContaining('/terminals'),
        expect.objectContaining({ terminalCode: 'POS-002', name: 'レジ2' }),
        expect.anything(),
      )
    })
  })

  it('キャンセルボタンで登録ダイアログを閉じる', async () => {
    const user = (await import('@testing-library/user-event')).default.setup()
    renderPage()
    await user.click(screen.getByText('端末を登録'))
    await waitFor(() => {
      expect(screen.getByText('端末を登録', { selector: 'h2' })).toBeInTheDocument()
    })
    await user.click(screen.getByText('キャンセル'))
    await waitFor(() => {
      expect(screen.queryByText('端末を登録', { selector: 'h2' })).not.toBeInTheDocument()
    })
  })

  it('オフラインの端末にオフラインバッジを表示する', async () => {
    const offlineTerminal = {
      ...mockTerminals[0]!,
      id: '44444444-4444-4444-4444-444444444444',
      isActive: false,
    }
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/terminals')) return Promise.resolve([offlineTerminal])
      return Promise.resolve(mockStores)
    })
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('オフライン')).toBeInTheDocument()
    })
  })

  it('lastSyncAtがnullの端末に未同期を表示する', async () => {
    const noSyncTerminal = {
      ...mockTerminals[0]!,
      id: '55555555-5555-5555-5555-555555555555',
      lastSyncAt: null,
    }
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/terminals')) return Promise.resolve([noSyncTerminal])
      return Promise.resolve(mockStores)
    })
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('未同期')).toBeInTheDocument()
    })
  })
})
