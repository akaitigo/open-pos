import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { ExportPage } from './export'
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
        <ExportPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('ExportPage', () => {
  beforeEach(() => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '00000000-0000-0000-0000-000000000000',
    })
  })

  it('データエクスポートヘッダーを表示する', () => {
    renderPage()
    expect(screen.getByText('データエクスポート')).toBeInTheDocument()
  })

  it('日付入力フィールドを表示する', () => {
    renderPage()
    expect(screen.getByLabelText('開始日')).toBeInTheDocument()
    expect(screen.getByLabelText('終了日')).toBeInTheDocument()
  })

  it('プレビューボタンを表示する', () => {
    renderPage()
    expect(screen.getByText('プレビュー')).toBeInTheDocument()
  })

  it('プレビューボタンクリックで API を呼び出してデータを表示する', async () => {
    const user = userEvent.setup()
    const mockData = {
      data: [
        { date: '2026-03-01', grossAmount: 100000, taxAmount: 10000, transactionCount: 5 },
        { date: '2026-03-02', grossAmount: 200000, taxAmount: 20000, transactionCount: 10 },
      ],
    }
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
      return Promise.resolve(mockData)
    })

    renderPage()
    await user.click(screen.getByText('プレビュー'))

    await waitFor(() => {
      expect(screen.getByText('2026-03-01')).toBeInTheDocument()
    })
    expect(screen.getByText('2026-03-02')).toBeInTheDocument()
  })

  it('データがある場合 CSV ダウンロードボタンが表示される', async () => {
    const user = userEvent.setup()
    const mockData = {
      data: [{ date: '2026-03-01', grossAmount: 100000, taxAmount: 10000, transactionCount: 5 }],
    }
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
      return Promise.resolve(mockData)
    })

    renderPage()
    await user.click(screen.getByText('プレビュー'))

    await waitFor(() => {
      expect(screen.getByText('CSV ダウンロード')).toBeInTheDocument()
    })
  })

  it('空のプレビューでは「データがありません」を表示する', async () => {
    const user = userEvent.setup()
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
      return Promise.resolve({ data: [] })
    })

    renderPage()
    await user.click(screen.getByText('プレビュー'))

    await waitFor(() => {
      expect(screen.getByText('データがありません')).toBeInTheDocument()
    })
  })

  it('CSVダウンロードボタンクリックでファイルをダウンロードする', async () => {
    const user = userEvent.setup()
    const mockData = {
      data: [{ date: '2026-03-01', grossAmount: 100000, taxAmount: 10000, transactionCount: 5 }],
    }
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
      return Promise.resolve(mockData)
    })

    const createObjectURL = vi.fn().mockReturnValue('blob:test')
    const revokeObjectURL = vi.fn()
    vi.stubGlobal('URL', { ...URL, createObjectURL, revokeObjectURL })

    renderPage()
    await user.click(screen.getByText('プレビュー'))

    await waitFor(() => {
      expect(screen.getByText('CSV ダウンロード')).toBeInTheDocument()
    })

    await user.click(screen.getByText('CSV ダウンロード'))
    expect(createObjectURL).toHaveBeenCalled()
    expect(revokeObjectURL).toHaveBeenCalled()
  })

  it('プレビューデータに取引件数0の行がある場合客単価を0で表示する', async () => {
    const user = userEvent.setup()
    const mockData = {
      data: [{ date: '2026-03-01', grossAmount: 0, taxAmount: 0, transactionCount: 0 }],
    }
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
      return Promise.resolve(mockData)
    })

    renderPage()
    await user.click(screen.getByText('プレビュー'))

    await waitFor(() => {
      expect(screen.getByText('2026-03-01')).toBeInTheDocument()
    })
    expect(screen.getByText('プレビュー（1件）')).toBeInTheDocument()
  })

  it('読み込み中はボタンテキストが変わる', async () => {
    const user = userEvent.setup()
    let resolveGet: ((value: unknown) => void) | undefined
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
      return new Promise((resolve) => {
        resolveGet = resolve
      })
    })

    renderPage()
    await user.click(screen.getByText('プレビュー'))

    expect(screen.getByText('読み込み中...')).toBeInTheDocument()

    resolveGet?.({ data: [] })
    await waitFor(() => {
      expect(screen.getByText('プレビュー')).toBeInTheDocument()
    })
  })

  it('日付を変更できる', async () => {
    renderPage()

    const startInput = screen.getByLabelText('開始日')
    const endInput = screen.getByLabelText('終了日')
    expect(startInput).toBeInTheDocument()
    expect(endInput).toBeInTheDocument()
  })
})
