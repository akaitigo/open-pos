import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { InventoryPage } from './inventory'
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

const mockProduct = {
  id: 'prod-1',
  organizationId: 'org-1',
  name: 'コーヒー豆',
  price: 50000,
  barcode: null,
  sku: 'SKU-COFFEE',
  categoryId: 'cat-1',
  taxRateId: 'tax-1',
  imageUrl: null,
  displayOrder: 0,
  isActive: true,
  description: '',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const mockStock = {
  id: 'stock-1',
  organizationId: 'org-1',
  storeId: 'store-1',
  productId: 'prod-1',
  quantity: 50,
  lowStockThreshold: 10,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

function setupMocks(stocks = [mockStock], products = [mockProduct], totalPages = 1) {
  mockApi.get.mockImplementation((path: string) => {
    if (path === '/api/products') {
      return Promise.resolve({
        data: products,
        pagination: { page: 1, pageSize: 200, totalCount: products.length, totalPages: 1 },
      })
    }
    if (path === '/api/inventory/stocks') {
      return Promise.resolve({
        data: stocks,
        pagination: { page: 1, pageSize: 20, totalCount: stocks.length, totalPages },
      })
    }
    return Promise.resolve({
      data: [],
      pagination: { page: 1, pageSize: 20, totalCount: 0, totalPages: 0 },
    })
  })
}

function renderPage() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <InventoryPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('InventoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('在庫管理ヘッダーを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('在庫管理')).toBeInTheDocument()
  })

  it('店舗ID入力欄を表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByPlaceholderText('店舗IDを入力...')).toBeInTheDocument()
  })

  it('店舗IDが未入力の場合メッセージを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('店舗IDを入力してください')).toBeInTheDocument()
  })

  it('店舗ID入力後に在庫データを取得して表示する', async () => {
    setupMocks()
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('コーヒー豆')).toBeInTheDocument()
    })
    expect(screen.getByText('SKU-COFFEE')).toBeInTheDocument()
    expect(screen.getByText('50')).toBeInTheDocument()
    expect(screen.getByText('10')).toBeInTheDocument()
    expect(screen.getByText('正常')).toBeInTheDocument()
  })

  it('在庫切れの商品は「在庫切れ」バッジを表示する', async () => {
    const outOfStock = { ...mockStock, quantity: 0 }
    setupMocks([outOfStock])
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('在庫切れ')).toBeInTheDocument()
    })
  })

  it('在庫低下の商品は「在庫低下」バッジを表示する', async () => {
    const lowStock = { ...mockStock, quantity: 5 }
    setupMocks([lowStock])
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('在庫低下')).toBeInTheDocument()
    })
  })

  it('在庫低下のみ表示ボタンを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('在庫低下のみ表示')).toBeInTheDocument()
  })

  it('在庫データなしの場合メッセージを表示する', async () => {
    setupMocks([])
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('在庫データが見つかりません')).toBeInTheDocument()
    })
  })

  it('調整ボタンクリックでダイアログを表示する', async () => {
    setupMocks()
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('コーヒー豆')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('調整'))
    await waitFor(() => {
      expect(screen.getByText('在庫調整 — コーヒー豆')).toBeInTheDocument()
    })
    expect(screen.getByText('増加')).toBeInTheDocument()
    expect(screen.getByText('減少')).toBeInTheDocument()
    expect(screen.getByLabelText('数量')).toBeInTheDocument()
    expect(screen.getByLabelText('理由')).toBeInTheDocument()
  })

  it('ページネーションが2ページ以上の場合にボタンを表示する', async () => {
    setupMocks([mockStock], [mockProduct], 3)
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('コーヒー豆')).toBeInTheDocument()
    })
    expect(screen.getByText('前へ')).toBeInTheDocument()
    expect(screen.getByText('次へ')).toBeInTheDocument()
    expect(screen.getByText('1 / 3')).toBeInTheDocument()
  })

  it('テーブルヘッダーを正しく表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('商品名')).toBeInTheDocument()
    expect(screen.getByText('SKU')).toBeInTheDocument()
    expect(screen.getByText('現在在庫数')).toBeInTheDocument()
    expect(screen.getByText('アラート閾値')).toBeInTheDocument()
    expect(screen.getByText('ステータス')).toBeInTheDocument()
    expect(screen.getByText('操作')).toBeInTheDocument()
  })
})
