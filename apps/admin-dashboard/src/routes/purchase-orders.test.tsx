import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { PurchaseOrdersPage } from './purchase-orders'
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

const mockOrder = {
  id: 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
  organizationId: 'org-1',
  storeId: 'store-1',
  supplierName: 'テストサプライヤー',
  status: 'DRAFT',
  note: '',
  items: [
    {
      productId: 'prod-1',
      orderedQuantity: 100,
      receivedQuantity: 0,
      unitCost: 30000,
    },
  ],
  createdAt: '2026-01-15T10:00:00Z',
  updatedAt: '2026-01-15T10:00:00Z',
}

function setupMocks(orders = [mockOrder], products = [mockProduct], totalPages = 1) {
  mockApi.get.mockImplementation((path: string) => {
    if (path === '/api/products') {
      return Promise.resolve({
        data: products,
        pagination: { page: 1, pageSize: 200, totalCount: products.length, totalPages: 1 },
      })
    }
    if (path === '/api/inventory/purchase-orders') {
      return Promise.resolve({
        data: orders,
        pagination: { page: 1, pageSize: 20, totalCount: orders.length, totalPages },
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
        <PurchaseOrdersPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('PurchaseOrdersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('発注管理ヘッダーを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('発注管理')).toBeInTheDocument()
  })

  it('店舗IDが未入力の場合メッセージを表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('店舗IDを入力してください')).toBeInTheDocument()
  })

  it('店舗IDが空の場合は新規発注ボタンが無効', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('新規発注')).toBeDisabled()
  })

  it('店舗ID入力後に発注データを表示する', async () => {
    setupMocks()
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('テストサプライヤー')).toBeInTheDocument()
    })
    expect(screen.getByText('1 品目')).toBeInTheDocument()
    expect(screen.getByText('下書き')).toBeInTheDocument()
  })

  it('DRAFT ステータスの発注には発注確定とキャンセルボタンを表示する', async () => {
    setupMocks()
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('発注確定')).toBeInTheDocument()
    })
    expect(screen.getByText('キャンセル')).toBeInTheDocument()
  })

  it('ORDERED ステータスの発注には入荷確認ボタンを表示する', async () => {
    setupMocks([{ ...mockOrder, status: 'ORDERED' }])
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('入荷確認')).toBeInTheDocument()
    })
  })

  it('発注確定ボタンでステータスをORDEREDに変更する', async () => {
    setupMocks()
    mockApi.put.mockResolvedValue({ ...mockOrder, status: 'ORDERED' })
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('発注確定')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('発注確定'))
    await waitFor(() => {
      expect(mockApi.put).toHaveBeenCalledWith(
        `/api/inventory/purchase-orders/${mockOrder.id}/status`,
        { status: 'ORDERED' },
        expect.anything(),
      )
    })
  })

  it('発注データなしの場合メッセージを表示する', async () => {
    setupMocks([])
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('発注データが見つかりません')).toBeInTheDocument()
    })
  })

  it('テーブルヘッダーを正しく表示する', () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('発注ID')).toBeInTheDocument()
    expect(screen.getByText('サプライヤー')).toBeInTheDocument()
    expect(screen.getByText('品目数')).toBeInTheDocument()
    expect(screen.getByText('ステータス')).toBeInTheDocument()
    expect(screen.getByText('作成日時')).toBeInTheDocument()
    expect(screen.getByText('操作')).toBeInTheDocument()
  })

  it('ページネーションが2ページ以上の場合にボタンを表示する', async () => {
    setupMocks([mockOrder], [mockProduct], 3)
    renderPage()
    fireEvent.change(screen.getByPlaceholderText('店舗IDを入力...'), {
      target: { value: 'store-1' },
    })
    await waitFor(() => {
      expect(screen.getByText('テストサプライヤー')).toBeInTheDocument()
    })
    expect(screen.getByText('前へ')).toBeInTheDocument()
    expect(screen.getByText('次へ')).toBeInTheDocument()
    expect(screen.getByText('1 / 3')).toBeInTheDocument()
  })
})
