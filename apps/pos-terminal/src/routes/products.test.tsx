import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ProductsPage } from './products'

vi.mock('html5-qrcode', () => ({
  Html5Qrcode: vi.fn().mockImplementation(() => ({
    start: vi.fn().mockRejectedValue(new Error('No camera')),
    stop: vi.fn().mockResolvedValue(undefined),
    isScanning: false,
  })),
}))

const mockCategories = [
  {
    id: 'a1b2c3d4-1111-4111-a111-111111111111',
    organizationId: '00000000-0000-0000-0000-000000000000',
    name: 'ドリンク',
    parentId: null,
    color: null,
    icon: null,
    displayOrder: 1,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'b2c3d4e5-2222-4222-a222-222222222222',
    organizationId: '00000000-0000-0000-0000-000000000000',
    name: 'フード',
    parentId: null,
    color: null,
    icon: null,
    displayOrder: 2,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

const mockProducts = [
  {
    id: 'c3d4e5f6-3333-4333-a333-333333333333',
    organizationId: '00000000-0000-0000-0000-000000000000',
    name: 'コーヒー',
    price: 35000,
    barcode: '4901234567890',
    categoryId: 'a1b2c3d4-1111-4111-a111-111111111111',
    displayOrder: 1,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'd4e5f6a7-4444-4444-a444-444444444444',
    organizationId: '00000000-0000-0000-0000-000000000000',
    name: '抹茶ラテ',
    price: 50000,
    categoryId: 'a1b2c3d4-1111-4111-a111-111111111111',
    imageUrl: 'https://example.com/matcha.jpg',
    displayOrder: 2,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

function mockFetchWith(categories: unknown[], products: unknown[], totalPages = 1) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
    const url = typeof input === 'string' ? input : (input as Request).url || input.toString()

    if (url.includes('/api/categories')) {
      return Promise.resolve(
        new Response(JSON.stringify(categories), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
    }

    return Promise.resolve(
      new Response(
        JSON.stringify({
          data: products,
          pagination: { page: 1, pageSize: 24, totalCount: products.length, totalPages },
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    )
  })
}

describe('ProductsPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('商品グリッドが表示される', async () => {
    mockFetchWith([], mockProducts)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
      expect(screen.getByText('抹茶ラテ')).toBeInTheDocument()
    })
  })

  it('商品がない場合は「商品が見つかりません」を表示', async () => {
    mockFetchWith([], [])

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('商品が見つかりません')).toBeInTheDocument()
    })
  })

  it('カテゴリタブが表示される', async () => {
    mockFetchWith(mockCategories, mockProducts)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('すべて')).toBeInTheDocument()
      expect(screen.getByText('ドリンク')).toBeInTheDocument()
      expect(screen.getByText('フード')).toBeInTheDocument()
    })
  })

  it('カテゴリがない場合はタブが非表示', async () => {
    mockFetchWith([], mockProducts)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })

    expect(screen.queryByText('すべて')).not.toBeInTheDocument()
  })

  it('検索入力でAPIリクエストが発生する', async () => {
    const fetchSpy = mockFetchWith([], mockProducts)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })

    const searchInput = screen.getByPlaceholderText('商品名・バーコードで検索...')
    fireEvent.change(searchInput, { target: { value: 'コーヒー' } })

    await waitFor(() => {
      const calls = fetchSpy.mock.calls
      const productCalls = calls.filter((call) => {
        const url =
          typeof call[0] === 'string' ? call[0] : (call[0] as Request).url || call[0].toString()
        return url.includes('/api/products')
      })
      expect(productCalls.length).toBeGreaterThanOrEqual(2)
    })
  })

  it('バーコードのある商品にバッジが表示される', async () => {
    mockFetchWith([], mockProducts)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('4901234567890')).toBeInTheDocument()
    })
  })

  it('画像URLのある商品はimgタグで表示される', async () => {
    mockFetchWith([], mockProducts)

    render(<ProductsPage />)

    await waitFor(() => {
      const img = screen.getByAltText('抹茶ラテ')
      expect(img).toBeInTheDocument()
      expect(img).toHaveAttribute('src', 'https://example.com/matcha.jpg')
    })
  })

  it('画像URLのない商品は先頭文字を表示する', async () => {
    mockFetchWith([], mockProducts)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('コ')).toBeInTheDocument()
    })
  })

  it('ページネーションボタンが表示される（totalPages > 1）', async () => {
    mockFetchWith([], mockProducts, 3)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('前へ')).toBeInTheDocument()
      expect(screen.getByText('次へ')).toBeInTheDocument()
      expect(screen.getByText('1 / 3')).toBeInTheDocument()
    })
  })

  it('前へボタンは1ページ目で無効', async () => {
    mockFetchWith([], mockProducts, 3)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('前へ')).toBeDisabled()
      expect(screen.getByText('次へ')).toBeEnabled()
    })
  })

  it('次へボタンでページが進む', async () => {
    const fetchSpy = mockFetchWith([], mockProducts, 3)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('次へ')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('次へ'))

    await waitFor(() => {
      const productCalls = fetchSpy.mock.calls.filter((call) => {
        const url =
          typeof call[0] === 'string' ? call[0] : (call[0] as Request).url || call[0].toString()
        return url.includes('/api/products') && url.includes('page=2')
      })
      expect(productCalls.length).toBeGreaterThanOrEqual(1)
    })
  })

  it('スキャンボタンでバーコードスキャナーダイアログが開く', async () => {
    mockFetchWith([], mockProducts)

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
    mockFetchWith([], mockProducts)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('スキャン')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('スキャン'))

    await waitFor(() => {
      expect(screen.getByPlaceholderText('バーコードを手入力...')).toBeInTheDocument()
    })

    const barcodeInput = screen.getByPlaceholderText('バーコードを手入力...')
    fireEvent.change(barcodeInput, { target: { value: '4901234567890' } })
    fireEvent.submit(barcodeInput.closest('form')!)

    await waitFor(() => {
      const searchInput = screen.getByPlaceholderText('商品名・バーコードで検索...')
      expect(searchInput).toHaveValue('4901234567890')
    })
  })

  it('カテゴリタブが正しいrole属性を持つ', async () => {
    mockFetchWith(mockCategories, mockProducts)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('ドリンク')).toBeInTheDocument()
    })

    const allTab = screen.getByRole('tab', { name: 'すべて' })
    const drinkTab = screen.getByRole('tab', { name: 'ドリンク' })
    const foodTab = screen.getByRole('tab', { name: 'フード' })

    expect(allTab).toHaveAttribute('aria-selected', 'true')
    expect(drinkTab).toHaveAttribute('aria-selected', 'false')
    expect(foodTab).toHaveAttribute('aria-selected', 'false')
  })

  it('商品クリックでconsole.logが呼ばれる（カートに追加のTODO）', async () => {
    mockFetchWith([], mockProducts)
    const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {})

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('コーヒー'))

    expect(consoleSpy).toHaveBeenCalledWith(
      'Add to cart:',
      'c3d4e5f6-3333-4333-a333-333333333333',
      'コーヒー',
    )

    consoleSpy.mockRestore()
  })

  it('ページネーションが1ページの場合はボタンが表示されない', async () => {
    mockFetchWith([], mockProducts, 1)

    render(<ProductsPage />)

    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })

    expect(screen.queryByText('前へ')).not.toBeInTheDocument()
    expect(screen.queryByText('次へ')).not.toBeInTheDocument()
  })
})
