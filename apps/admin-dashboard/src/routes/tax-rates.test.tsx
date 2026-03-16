import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { TaxRatesPage } from './tax-rates'
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

const mockTaxRate = {
  id: 'tax-1',
  organizationId: 'org-1',
  name: '標準税率',
  rate: '0.10',
  isReduced: false,
  isDefault: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const mockReducedTaxRate = {
  id: 'tax-2',
  organizationId: 'org-1',
  name: '軽減税率',
  rate: '0.08',
  isReduced: true,
  isDefault: false,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

function setupMocks(taxRates = [mockTaxRate, mockReducedTaxRate]) {
  mockApi.get.mockResolvedValue(taxRates)
}

function renderPage() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <TaxRatesPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('TaxRatesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('税率管理ヘッダーを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('税率管理')).toBeInTheDocument()
  })

  it('税率テーブルを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('標準税率')).toBeInTheDocument()
    })
    expect(screen.getByText('10%')).toBeInTheDocument()
    expect(screen.getAllByText('軽減税率').length).toBeGreaterThan(0)
    expect(screen.getByText('8%')).toBeInTheDocument()
  })

  it('デフォルト税率にはバッジを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('デフォルト')).toBeInTheDocument()
    })
  })

  it('軽減税率にはバッジを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('軽減')).toBeInTheDocument()
    })
  })

  it('税率が空の場合は空メッセージを表示する', async () => {
    setupMocks([])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('税率が登録されていません')).toBeInTheDocument()
    })
  })

  it('税率を追加ボタンでダイアログが開く', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('標準税率')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('税率を追加'))
    await waitFor(() => {
      expect(
        screen.getByText('税率を追加', { selector: '[class*="DialogTitle"], h2' }),
      ).toBeInTheDocument()
    })
    expect(screen.getByLabelText('税率名 *')).toBeInTheDocument()
    expect(screen.getByLabelText('税率（%） *')).toBeInTheDocument()
  })

  it('編集ボタンで編集ダイアログが開く', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('標準税率')).toBeInTheDocument()
    })
    const editButtons = screen.getAllByText('編集')
    fireEvent.click(editButtons[0]!)
    await waitFor(() => {
      expect(screen.getByText('税率を編集')).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('標準税率')).toBeInTheDocument()
    expect(screen.getByDisplayValue('10')).toBeInTheDocument()
  })

  it('削除ボタンで確認ダイアログを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('標準税率')).toBeInTheDocument()
    })
    const deleteButtons = screen.getAllByText('削除')
    fireEvent.click(deleteButtons[0]!)
    await waitFor(() => {
      expect(screen.getByText('本当に削除しますか？この操作は取り消せません。')).toBeInTheDocument()
    })
  })

  it('確認ダイアログで削除を実行する', async () => {
    setupMocks()
    mockApi.delete.mockResolvedValue(undefined)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('標準税率')).toBeInTheDocument()
    })
    // テーブルの削除ボタンをクリック
    const deleteButtons = screen.getAllByText('削除')
    fireEvent.click(deleteButtons[0]!)
    await waitFor(() => {
      expect(screen.getByText('本当に削除しますか？この操作は取り消せません。')).toBeInTheDocument()
    })
    // ダイアログ内の削除ボタンをクリック
    const dialogDeleteButton = screen
      .getAllByRole('button', { name: '削除' })
      .find((btn) => btn.closest('[role="dialog"]') !== null)
    expect(dialogDeleteButton).toBeTruthy()
    fireEvent.click(dialogDeleteButton!)
    await waitFor(() => {
      expect(mockApi.delete).toHaveBeenCalledWith('/api/tax-rates/tax-1')
    })
  })

  it('テーブルヘッダーを正しく表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('税率名')).toBeInTheDocument()
    expect(screen.getByText('税率')).toBeInTheDocument()
    expect(screen.getByText('軽減税率')).toBeInTheDocument()
  })

  it('追加ダイアログでフォーム送信するとAPIを呼ぶ', async () => {
    setupMocks()
    mockApi.post.mockResolvedValue(mockTaxRate)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('標準税率')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('税率を追加'))
    await waitFor(() => {
      expect(screen.getByLabelText('税率名 *')).toBeInTheDocument()
    })
    fireEvent.change(screen.getByLabelText('税率名 *'), { target: { value: '新税率' } })
    fireEvent.change(screen.getByLabelText('税率（%） *'), { target: { value: '15' } })
    fireEvent.click(screen.getByText('追加'))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith(
        '/api/tax-rates',
        expect.objectContaining({ name: '新税率', rate: '0.15' }),
        expect.anything(),
      )
    })
  })
})
