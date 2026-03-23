import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { ProductsPage } from './products'
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

const mockCategories = [
  {
    id: 'cat-1',
    organizationId: 'org-1',
    name: '飲み物',
    parentId: null,
    color: '#ff0000',
    icon: null,
    displayOrder: 0,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

const mockTaxRates = [
  {
    id: 'tax-1',
    organizationId: 'org-1',
    name: '標準税率',
    rate: '0.10',
    isReduced: false,
    isDefault: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

const mockProduct = {
  id: 'prod-1',
  organizationId: 'org-1',
  name: 'コーヒー',
  price: 30000,
  barcode: '4901234567890' as string | null,
  sku: 'SKU-001',
  categoryId: 'cat-1',
  taxRateId: 'tax-1',
  imageUrl: null,
  displayOrder: 0,
  isActive: true,
  description: 'おいしいコーヒー',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

function setupMocks(products = [mockProduct], totalPages = 1) {
  mockApi.get.mockImplementation((path: string) => {
    if (path === '/api/products') {
      return Promise.resolve({
        data: products,
        pagination: { page: 1, pageSize: 20, totalCount: products.length, totalPages },
      })
    }
    if (path === '/api/categories') return Promise.resolve(mockCategories)
    if (path === '/api/tax-rates') return Promise.resolve(mockTaxRates)
    return Promise.resolve([])
  })
}

function renderPage() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <ProductsPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('ProductsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('商品テーブルを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    expect(screen.getByText('4901234567890')).toBeInTheDocument()
    expect(screen.getByText('飲み物')).toBeInTheDocument()
    expect(screen.getByText('有効')).toBeInTheDocument()
  })

  it('商品が空の場合は空メッセージを表示する', async () => {
    setupMocks([], 0)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('商品が見つかりません')).toBeInTheDocument()
    })
  })

  it('商品管理ヘッダーを表示する', async () => {
    setupMocks()
    renderPage()
    expect(screen.getByText('商品管理')).toBeInTheDocument()
  })

  it('商品を追加ボタンでダイアログが開く', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('商品を追加'))
    await waitFor(() => {
      expect(
        screen.getByText('商品を追加', { selector: '[class*="DialogTitle"], h2' }),
      ).toBeInTheDocument()
    })
  })

  it('編集ボタンで編集ダイアログが開く', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('編集'))
    await waitFor(() => {
      expect(screen.getByText('商品を編集')).toBeInTheDocument()
    })
    // 値がプリフィルされていることを確認
    expect(screen.getByDisplayValue('コーヒー')).toBeInTheDocument()
    expect(screen.getByDisplayValue('300')).toBeInTheDocument() // 30000 / 100
  })

  it('削除ボタンで確認ダイアログを表示し、確認後にapi.deleteを呼ぶ', async () => {
    setupMocks()
    mockApi.delete.mockResolvedValue(undefined)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('削除'))
    // 確認ダイアログが表示される
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
      expect(mockApi.delete).toHaveBeenCalledWith('/api/products/prod-1')
    })
  })

  it('検索入力でAPIを再呼び出しする', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(mockApi.get).toHaveBeenCalled()
    })
    const searchInput = screen.getByPlaceholderText('商品名・バーコード・SKUで検索...')
    fireEvent.change(searchInput, { target: { value: 'コーヒー' } })
    await waitFor(() => {
      expect(mockApi.get).toHaveBeenCalledWith(
        '/api/products',
        expect.anything(),
        expect.objectContaining({
          params: expect.objectContaining({ search: 'コーヒー', page: 1 }),
        }),
      )
    })
  })

  it('ページネーションが2ページ以上の場合にボタンを表示する', async () => {
    setupMocks([mockProduct], 3)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    expect(screen.getByText('前へ')).toBeInTheDocument()
    expect(screen.getByText('次へ')).toBeInTheDocument()
    expect(screen.getByText('1 / 3')).toBeInTheDocument()
  })

  it('無効な商品はバッジに無効と表示する', async () => {
    const inactiveProduct = { ...mockProduct, id: 'prod-2', isActive: false }
    setupMocks([inactiveProduct])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('無効')).toBeInTheDocument()
    })
  })

  it('バーコードなしの商品はダッシュを表示する', async () => {
    const noBarcode = { ...mockProduct, id: 'prod-3', barcode: null }
    setupMocks([noBarcode])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    // barcode が null の場合 '—' を表示
    const cells = screen.getAllByRole('cell')
    const barcodeCell = cells.find((c) => c.textContent === '—')
    expect(barcodeCell).toBeTruthy()
  })

  it('ダイアログでフォーム送信するとAPIを呼ぶ', async () => {
    setupMocks()
    mockApi.post.mockResolvedValue(mockProduct)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('商品を追加'))
    await waitFor(() => {
      expect(screen.getByLabelText('商品名 *')).toBeInTheDocument()
    })
    fireEvent.change(screen.getByLabelText('商品名 *'), { target: { value: '紅茶' } })
    fireEvent.change(screen.getByLabelText('価格（円） *'), { target: { value: '200' } })
    fireEvent.click(screen.getByText('追加'))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith(
        '/api/products',
        expect.objectContaining({ name: '紅茶', price: 20000 }),
        expect.anything(),
      )
    })
  })

  it('編集ダイアログでフォーム送信すると更新APIを呼ぶ', async () => {
    setupMocks()
    mockApi.put.mockResolvedValue(mockProduct)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('編集'))
    await waitFor(() => {
      expect(screen.getByText('商品を編集')).toBeInTheDocument()
    })
    fireEvent.change(screen.getByLabelText('商品名 *'), { target: { value: 'エスプレッソ' } })
    fireEvent.click(screen.getByText('更新'))
    await waitFor(() => {
      expect(mockApi.put).toHaveBeenCalledWith(
        '/api/products/prod-1',
        expect.objectContaining({ name: 'エスプレッソ' }),
        expect.anything(),
      )
    })
  })

  it('次へボタンでページ遷移する', async () => {
    setupMocks([mockProduct], 3)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    expect(screen.getByText('1 / 3')).toBeInTheDocument()
    fireEvent.click(screen.getByText('次へ'))
    await waitFor(() => {
      expect(mockApi.get).toHaveBeenCalledWith(
        '/api/products',
        expect.anything(),
        expect.objectContaining({
          params: expect.objectContaining({ page: 2 }),
        }),
      )
    })
  })

  it('キャンセルボタンでダイアログを閉じる', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('商品を追加'))
    await waitFor(() => {
      expect(
        screen.getByText('商品を追加', { selector: '[class*="DialogTitle"], h2' }),
      ).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('キャンセル'))
    await waitFor(() => {
      expect(
        screen.queryByText('商品を追加', { selector: '[class*="DialogTitle"], h2' }),
      ).not.toBeInTheDocument()
    })
  })

  it('CSVインポートボタンを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('CSVインポート')).toBeInTheDocument()
    })
  })

  it('説明フィールドを編集ダイアログに表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('編集'))
    await waitFor(() => {
      expect(screen.getByLabelText('説明')).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('おいしいコーヒー')).toBeInTheDocument()
  })

  it('CSVインポートでファイルを選択して商品を作成する', async () => {
    setupMocks()
    mockApi.post.mockResolvedValue(mockProduct)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('CSVインポート')).toBeInTheDocument()
    })
    const csvContent = 'name,price,barcode\n紅茶,200,4901234567891\n緑茶,150,4901234567892'
    const file = new File([csvContent], 'products.csv', { type: 'text/csv' })
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(input, { target: { files: [file] } })
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledTimes(2)
    })
  })

  it('CSVインポートで name/price 列がない場合はエラーを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('CSVインポート')).toBeInTheDocument()
    })
    const csvContent = 'barcode,sku\n4901234567891,SKU-001'
    const file = new File([csvContent], 'products.csv', { type: 'text/csv' })
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(input, { target: { files: [file] } })
    // name/price がないのでエラートーストが出るはず（APIは呼ばれない）
    await waitFor(() => {
      expect(mockApi.post).not.toHaveBeenCalled()
    })
  })

  it('CSVインポートで空のCSVの場合はエラーを表示する', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('CSVインポート')).toBeInTheDocument()
    })
    const csvContent = 'name,price'
    const file = new File([csvContent], 'empty.csv', { type: 'text/csv' })
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(input, { target: { files: [file] } })
    await waitFor(() => {
      expect(mockApi.post).not.toHaveBeenCalled()
    })
  })

  it('新規作成ダイアログでカテゴリと税率セレクトが表示される', async () => {
    setupMocks()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('商品を追加'))
    await waitFor(() => {
      expect(screen.getByLabelText('カテゴリ')).toBeInTheDocument()
    })
    expect(screen.getByLabelText('税率')).toBeInTheDocument()
    const categorySelect = screen.getByLabelText('カテゴリ') as HTMLSelectElement
    expect(categorySelect.options.length).toBeGreaterThanOrEqual(2) // 未設定 + 飲み物
  })

  it('削除APIエラー時もクラッシュしない', async () => {
    setupMocks()
    mockApi.delete.mockRejectedValue(new Error('削除失敗'))
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('コーヒー')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('削除'))
    await waitFor(() => {
      expect(screen.getByText('本当に削除しますか？この操作は取り消せません。')).toBeInTheDocument()
    })
    const dialogDeleteButton = screen
      .getAllByRole('button', { name: '削除' })
      .find((btn) => btn.closest('[role="dialog"]') !== null)
    fireEvent.click(dialogDeleteButton!)
    await waitFor(() => {
      expect(mockApi.delete).toHaveBeenCalled()
    })
  })
})
