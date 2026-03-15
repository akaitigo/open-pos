import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { ReportsPage } from './reports'
import { beforeEach, describe, it, expect, vi } from 'vitest'
import { resetRuntimeConfigForTests } from '@/lib/runtime-config'

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue([]),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    setOrganizationId: vi.fn(),
    setBaseUrl: vi.fn(),
  },
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
})
