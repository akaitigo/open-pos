import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { OrganizationPage } from './organization'
import { beforeEach, describe, it, expect, vi } from 'vitest'
import { resetRuntimeConfigForTests } from '@/lib/runtime-config'

const mockOrg = {
  id: '11111111-1111-1111-1111-111111111111',
  name: 'テスト組織',
  businessType: '小売',
  invoiceNumber: 'T1234567890123',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const mockApi = vi.hoisted(() => ({
  get: vi.fn(),
  put: vi.fn(),
  post: vi.fn(),
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
        <OrganizationPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('OrganizationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.get.mockResolvedValue(mockOrg)
    mockApi.put.mockResolvedValue(mockOrg)
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '00000000-0000-0000-0000-000000000000',
    })
  })

  it('組織設定ヘッダーを表示する', () => {
    renderPage()
    expect(screen.getByText('組織設定')).toBeInTheDocument()
  })

  it('組織情報を取得してフォームに表示する', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByDisplayValue('テスト組織')).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('小売')).toBeInTheDocument()
    expect(screen.getByDisplayValue('T1234567890123')).toBeInTheDocument()
  })

  it('設定保存ボタンをクリックでAPIを呼び出す', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => {
      expect(screen.getByDisplayValue('テスト組織')).toBeInTheDocument()
    })
    await user.click(screen.getByText('設定を保存'))
    expect(mockApi.put).toHaveBeenCalledWith(
      '/api/organization',
      expect.objectContaining({ name: 'テスト組織' }),
      expect.anything(),
    )
  })

  it('インボイス番号の説明テキストを表示する', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText(/インボイス制度に対応する場合/)).toBeInTheDocument()
    })
  })
})
