import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProductsPage } from './products'
import { useAuthStore } from '@/stores/auth-store'
import { useCartStore } from '@/stores/cart-store'

vi.mock('html5-qrcode', () => ({
  Html5Qrcode: class MockHtml5Qrcode {
    start = vi.fn().mockRejectedValue(new Error('No camera'))
    stop = vi.fn().mockResolvedValue(undefined)
    isScanning = false
  },
}))

const mockStoreId = '00000000-0000-4000-a000-000000000001'
const mockTerminalId = '00000000-0000-4000-a000-000000000010'

const rootDrinkCategory = {
  id: 'a1b2c3d4-1111-4111-a111-111111111111',
  organizationId: '00000000-0000-0000-0000-000000000000',
  name: 'ドリンク',
  parentId: null,
  color: '#2563eb',
  icon: null,
  displayOrder: 1,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const rootFoodCategory = {
  id: 'b2c3d4e5-2222-4222-a222-222222222222',
  organizationId: '00000000-0000-0000-0000-000000000000',
  name: 'フード',
  parentId: null,
  color: '#f97316',
  icon: null,
  displayOrder: 2,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const hotDrinkCategory = {
  id: 'c3d4e5f6-3333-4333-a333-333333333333',
  organizationId: '00000000-0000-0000-0000-000000000000',
  name: 'ホットドリンク',
  parentId: rootDrinkCategory.id,
  color: '#0f766e',
  icon: null,
  displayOrder: 1,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const mockCategories = [rootDrinkCategory, rootFoodCategory, hotDrinkCategory]

const mockProducts = [
  {
    id: 'd4e5f6a7-4444-4444-a444-444444444444',
    organizationId: '00000000-0000-0000-0000-000000000000',
    name: 'コーヒー',
    price: 35000,
    barcode: '4901234567890',
    sku: 'DRINK-001',
    categoryId: hotDrinkCategory.id,
    displayOrder: 1,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'e5f6a7b8-5555-4555-a555-555555555555',
    organizationId: '00000000-0000-0000-0000-000000000000',
    name: '抹茶ラテ',
    price: 50000,
    categoryId: hotDrinkCategory.id,
    imageUrl: 'https://example.com/matcha.jpg',
    displayOrder: 2,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'f6a7b8c9-6666-4666-a666-666666666666',
    organizationId: '00000000-0000-0000-0000-000000000000',
    name: 'サンドイッチ',
    price: 65000,
    barcode: '4901234567001',
    sku: 'FOOD-001',
    categoryId: rootFoodCategory.id,
    displayOrder: 3,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

const mockStocks = [
  {
    id: '11111111-1111-4111-a111-111111111111',
    organizationId: '00000000-0000-0000-0000-000000000000',
    storeId: mockStoreId,
    productId: mockProducts[0]!.id,
    quantity: 12,
    lowStockThreshold: 3,
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: '22222222-2222-4222-a222-222222222222',
    organizationId: '00000000-0000-0000-0000-000000000000',
    storeId: mockStoreId,
    productId: mockProducts[1]!.id,
    quantity: 2,
    lowStockThreshold: 3,
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: '33333333-3333-4333-a333-333333333333',
    organizationId: '00000000-0000-0000-0000-000000000000',
    storeId: mockStoreId,
    productId: mockProducts[2]!.id,
    quantity: 8,
    lowStockThreshold: 3,
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

function jsonResponse(body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

function mockFetchWith({
  categories = mockCategories,
  products = mockProducts,
  stocks = mockStocks,
}: {
  categories?: unknown[]
  products?: unknown[]
  stocks?: unknown[]
} = {}) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
    const rawUrl = typeof input === 'string' ? input : (input as Request).url || input.toString()
    const url = new URL(rawUrl)

    if (url.pathname === '/api/categories') {
      return jsonResponse(categories)
    }

    if (url.pathname === '/api/products') {
      return jsonResponse({
        data: products,
        pagination: {
          page: Number(url.searchParams.get('page') ?? '1'),
          pageSize: Number(url.searchParams.get('pageSize') ?? '100'),
          totalCount: products.length,
          totalPages: 1,
        },
      })
    }

    if (url.pathname === '/api/inventory/stocks') {
      return jsonResponse({
        data: stocks,
        pagination: {
          page: Number(url.searchParams.get('page') ?? '1'),
          pageSize: Number(url.searchParams.get('pageSize') ?? '100'),
          totalCount: stocks.length,
          totalPages: 1,
        },
      })
    }

    return Promise.reject(new Error(`Unhandled URL: ${url.toString()}`))
  })
}

function createProduct(index: number) {
  return {
    id: `00000000-0000-4000-a000-${String(index + 1).padStart(12, '0')}`,
    organizationId: '00000000-0000-0000-0000-000000000000',
    name: `商品 ${index + 1}`,
    price: 10000 + index,
    barcode: `49012345${String(index).padStart(5, '0')}`,
    sku: `ITEM-${String(index + 1).padStart(3, '0')}`,
    categoryId: index % 2 === 0 ? hotDrinkCategory.id : rootFoodCategory.id,
    displayOrder: index,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  }
}

describe('ProductsPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    useCartStore.setState({ items: [] })
    useAuthStore.setState({
      isAuthenticated: true,
      staff: null,
      storeId: mockStoreId,
      storeName: 'テスト店舗',
      terminalId: mockTerminalId,
    })
  })

  it('商品グリッドが表示される', async () => {
    mockFetchWith()

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
      expect(screen.getByText('抹茶ラテ')).toBeInTheDocument()
    })
  })

  it('カテゴリの親子ナビゲーションが表示される', async () => {
    mockFetchWith()
    const user = userEvent.setup()

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByRole('tab', { name: 'ドリンク' })).toBeInTheDocument()
      expect(screen.getByRole('tab', { name: 'フード' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('tab', { name: 'ドリンク' }))

    await waitFor(() => {
      expect(screen.getByRole('tab', { name: 'ホットドリンク' })).toBeInTheDocument()
    })
  })

  it('親カテゴリ選択で子カテゴリ商品に絞り込まれる', async () => {
    mockFetchWith()
    const user = userEvent.setup()

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('サンドイッチ')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('tab', { name: 'ドリンク' }))

    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
      expect(screen.queryByText('サンドイッチ')).not.toBeInTheDocument()
    })
  })

  it('検索入力で商品が絞り込まれる', async () => {
    mockFetchWith()

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByPlaceholderText('商品名・バーコードで検索...'), {
      target: { value: 'サンド' },
    })

    await waitFor(() => {
      expect(screen.getByText('サンドイッチ')).toBeInTheDocument()
      expect(screen.queryByText('コーヒー')).not.toBeInTheDocument()
    })
  })

  it('バーコードのある商品にバッジが表示される', async () => {
    mockFetchWith()

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('4901234567890')).toBeInTheDocument()
    })
  })

  it('画像URLのある商品はimgタグで表示される', async () => {
    mockFetchWith()

    render(<ProductsPage />)

    await waitFor(() => {
      const img = screen.getByAltText('抹茶ラテ')
      expect(img).toBeInTheDocument()
      expect(img).toHaveAttribute('src', 'https://example.com/matcha.jpg')
    })
  })

  it('画像URLのない商品は先頭文字を表示する', async () => {
    mockFetchWith()

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('コ')).toBeInTheDocument()
    })
  })

  it('在庫切れの商品は追加できない', async () => {
    mockFetchWith({
      stocks: mockStocks.map((stock) =>
        stock.productId === mockProducts[0]!.id ? { ...stock, quantity: 0 } : stock,
      ),
    })

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('在庫切れ')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('コーヒー'))

    expect(useCartStore.getState().items).toHaveLength(0)
  })

  it('商品クリックでカートに追加される', async () => {
    mockFetchWith()

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('コーヒー'))

    const items = useCartStore.getState().items
    expect(items).toHaveLength(1)
    expect(items[0]!.product.id).toBe(mockProducts[0]!.id)
    expect(items[0]!.quantity).toBe(1)
  })

  it('ローカルページネーションが表示される', async () => {
    mockFetchWith({
      products: Array.from({ length: 30 }, (_, index) => createProduct(index)),
      stocks: Array.from({ length: 30 }, (_, index) => ({
        id: `99999999-0000-4000-a000-${String(index + 1).padStart(12, '0')}`,
        organizationId: '00000000-0000-0000-0000-000000000000',
        storeId: mockStoreId,
        productId: createProduct(index).id,
        quantity: 10,
        lowStockThreshold: 3,
        updatedAt: '2026-01-01T00:00:00Z',
      })),
    })

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('1 / 2')).toBeInTheDocument()
      expect(screen.getByText('商品 1')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('次へ'))

    await waitFor(() => {
      expect(screen.getByText('2 / 2')).toBeInTheDocument()
      expect(screen.getByText('商品 30')).toBeInTheDocument()
    })
  })

  it('スキャンボタンでバーコードスキャナーダイアログが開く', async () => {
    mockFetchWith()

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('スキャン')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('スキャン'))

    await waitFor(() => {
      expect(screen.getByText('バーコードスキャン')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('バーコードを手入力...')).toBeInTheDocument()
    })
  })

  it('手動バーコード入力で検索が更新される', async () => {
    mockFetchWith()

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('スキャン')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('スキャン'))

    await waitFor(() => {
      expect(screen.getByPlaceholderText('バーコードを手入力...')).toBeInTheDocument()
    })

    const barcodeInput = screen.getByPlaceholderText('バーコードを手入力...')
    fireEvent.change(barcodeInput, { target: { value: '4901234567001' } })
    fireEvent.submit(barcodeInput.closest('form')!)

    await waitFor(() => {
      const searchInput = screen.getByPlaceholderText('商品名・バーコードで検索...')
      expect(searchInput).toHaveValue('4901234567001')
      expect(screen.getByText('サンドイッチ')).toBeInTheDocument()
    })
  })

  it('商品取得に失敗した場合はエラー表示になる', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network error'))

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('商品カタログを読み込めませんでした')).toBeInTheDocument()
      expect(screen.getByText('network error')).toBeInTheDocument()
    })
  })
})
