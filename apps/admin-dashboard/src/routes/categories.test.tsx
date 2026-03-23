import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { CategoriesPage } from './categories'
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
    displayOrder: 1,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'cat-2',
    organizationId: 'org-1',
    name: '食べ物',
    parentId: 'cat-1',
    color: null,
    icon: '🍕',
    displayOrder: 2,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

function renderPage() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <CategoriesPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('CategoriesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('カテゴリテーブルを表示する', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    expect(screen.getByText('食べ物')).toBeInTheDocument()
    expect(screen.getByText('#ff0000')).toBeInTheDocument()
  })

  it('カテゴリが空の場合は空メッセージを表示する', async () => {
    mockApi.get.mockResolvedValue([])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('カテゴリが登録されていません')).toBeInTheDocument()
    })
  })

  it('カテゴリ管理ヘッダーを表示する', () => {
    mockApi.get.mockResolvedValue([])
    renderPage()
    expect(screen.getByText('カテゴリ管理')).toBeInTheDocument()
  })

  it('カテゴリを追加ボタンでダイアログが開く', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    fireEvent.click(screen.getByRole('button', { name: 'カテゴリを追加' }))
    await waitFor(() => {
      expect(
        screen.getByText('カテゴリを追加', { selector: '[class*="DialogTitle"], h2' }),
      ).toBeInTheDocument()
    })
  })

  it('編集ボタンで編集ダイアログが開く', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    const editButtons = screen.getAllByText('編集')
    fireEvent.click(editButtons[0]!)
    await waitFor(() => {
      expect(screen.getByText('カテゴリを編集')).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('飲み物')).toBeInTheDocument()
  })

  it('編集時に親カテゴリセレクトが自分自身を除外する', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    const editButtons = screen.getAllByText('編集')
    fireEvent.click(editButtons[0]!) // 飲み物を編集
    await waitFor(() => {
      expect(screen.getByText('カテゴリを編集')).toBeInTheDocument()
    })
    const parentSelect = screen.getByLabelText('親カテゴリ') as HTMLSelectElement
    const options = Array.from(parentSelect.options)
    const optionValues = options.map((o) => o.value)
    // 飲み物(cat-1)自身は選択肢に含まれない
    expect(optionValues).not.toContain('cat-1')
    // 食べ物(cat-2)は含まれる
    expect(optionValues).toContain('cat-2')
  })

  it('削除ボタンで確認ダイアログを表示し、確認後にapi.deleteを呼ぶ', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    mockApi.delete.mockResolvedValue(undefined)
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    const deleteButtons = screen.getAllByText('削除')
    fireEvent.click(deleteButtons[0]!)
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
      expect(mockApi.delete).toHaveBeenCalledWith('/api/categories/cat-1')
    })
  })

  it('フォーム送信で新規作成APIを呼ぶ', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    mockApi.post.mockResolvedValue(mockCategories[0])
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    fireEvent.click(screen.getByRole('button', { name: 'カテゴリを追加' }))
    await waitFor(() => {
      expect(screen.getByLabelText('カテゴリ名 *')).toBeInTheDocument()
    })
    fireEvent.change(screen.getByLabelText('カテゴリ名 *'), { target: { value: 'デザート' } })
    fireEvent.click(screen.getByRole('button', { name: '追加' }))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith(
        '/api/categories',
        expect.objectContaining({ name: 'デザート' }),
        expect.anything(),
      )
    })
  })

  it('親カテゴリ名を表示する', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('食べ物')).toBeInTheDocument()
    })
    // 食べ物の親は飲み物（cat-1）。テーブル内に飲み物の表示があるか
    // 飲み物自体の行 + 食べ物の親カテゴリ列、両方に「飲み物」が表示される
    const drinkTexts = screen.getAllByText('飲み物')
    expect(drinkTexts.length).toBeGreaterThanOrEqual(2)
  })

  it('アイコンが設定されたカテゴリはアイコンを表示する', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('🍕')).toBeInTheDocument()
    })
  })

  it('カラーなしのカテゴリはダッシュを表示する', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('食べ物')).toBeInTheDocument()
    })
    // 食べ物のカラーは null なので '—' 表示
    const cells = screen.getAllByRole('cell')
    const dashCells = cells.filter((c) => c.textContent === '—')
    expect(dashCells.length).toBeGreaterThanOrEqual(1)
  })

  it('ツリー表示に切り替える', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    fireEvent.click(screen.getByRole('button', { name: 'ツリー表示' }))
    // ツリー表示では順序が表示される
    await waitFor(() => {
      expect(screen.getByText(/順序: 1/)).toBeInTheDocument()
    })
  })

  it('ツリー表示で空の場合メッセージを表示する', async () => {
    mockApi.get.mockResolvedValue([])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('カテゴリが登録されていません')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: 'ツリー表示' }))
    await waitFor(() => {
      expect(screen.getByText('カテゴリが登録されていません')).toBeInTheDocument()
    })
  })

  it('ツリー表示で子ノードの折りたたみができる', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    fireEvent.click(screen.getByRole('button', { name: 'ツリー表示' }))
    await waitFor(() => {
      expect(screen.getByText('食べ物')).toBeInTheDocument()
    })
    // 折りたたみボタンをクリック
    const collapseButton = screen.getByRole('button', { name: '折りたたむ' })
    fireEvent.click(collapseButton)
    // 展開ボタンに変わる
    expect(screen.getByRole('button', { name: '展開する' })).toBeInTheDocument()
  })

  it('カテゴリ取得エラー時にクラッシュしない', async () => {
    mockApi.get.mockRejectedValue(new Error('取得エラー'))
    renderPage()
    expect(screen.getByText('カテゴリ管理')).toBeInTheDocument()
  })

  it('カテゴリ保存エラー時にクラッシュしない', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    mockApi.post.mockRejectedValue(new Error('保存エラー'))
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    fireEvent.click(screen.getByRole('button', { name: 'カテゴリを追加' }))
    await waitFor(() => {
      expect(screen.getByLabelText('カテゴリ名 *')).toBeInTheDocument()
    })
    fireEvent.change(screen.getByLabelText('カテゴリ名 *'), { target: { value: 'テスト' } })
    fireEvent.click(screen.getByRole('button', { name: '追加' }))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalled()
    })
  })

  it('カテゴリ削除エラー時にクラッシュしない', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    mockApi.delete.mockRejectedValue(new Error('削除エラー'))
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    const deleteButtons = screen.getAllByText('削除')
    fireEvent.click(deleteButtons[0]!)
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

  it('フォームのカラー・アイコン・表示順フィールドを入力できる', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    mockApi.post.mockResolvedValue(mockCategories[0])
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    fireEvent.click(screen.getByRole('button', { name: 'カテゴリを追加' }))
    await waitFor(() => {
      expect(screen.getByLabelText('カテゴリ名 *')).toBeInTheDocument()
    })
    fireEvent.change(screen.getByLabelText('カテゴリ名 *'), { target: { value: 'スイーツ' } })
    fireEvent.change(screen.getByLabelText('カラー'), { target: { value: '#00ff00' } })
    fireEvent.change(screen.getByLabelText('アイコン'), { target: { value: '🍰' } })
    fireEvent.change(screen.getByLabelText('表示順'), { target: { value: '5' } })
    fireEvent.click(screen.getByRole('button', { name: '追加' }))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith(
        '/api/categories',
        expect.objectContaining({
          name: 'スイーツ',
          color: '#00ff00',
          icon: '🍰',
          displayOrder: 5,
        }),
        expect.anything(),
      )
    })
  })

  it('キャンセルボタンでダイアログを閉じる', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    fireEvent.click(screen.getByRole('button', { name: 'カテゴリを追加' }))
    await waitFor(() => {
      expect(
        screen.getByText('カテゴリを追加', { selector: '[class*="DialogTitle"], h2' }),
      ).toBeInTheDocument()
    })
    fireEvent.click(screen.getByText('キャンセル'))
    await waitFor(() => {
      expect(
        screen.queryByText('カテゴリを追加', { selector: '[class*="DialogTitle"], h2' }),
      ).not.toBeInTheDocument()
    })
  })

  it('ツリー表示で編集ボタンが動作する', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    fireEvent.click(screen.getByRole('button', { name: 'ツリー表示' }))
    await waitFor(() => {
      expect(screen.getByText(/順序: 1/)).toBeInTheDocument()
    })
    // ツリー表示内の編集ボタンをクリック
    const editButtons = screen.getAllByText('編集')
    fireEvent.click(editButtons[0]!)
    await waitFor(() => {
      expect(screen.getByText('カテゴリを編集')).toBeInTheDocument()
    })
  })

  it('ツリー表示で削除ボタンが動作する', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    mockApi.delete.mockResolvedValue(undefined)
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    fireEvent.click(screen.getByRole('button', { name: 'ツリー表示' }))
    await waitFor(() => {
      expect(screen.getByText(/順序: 1/)).toBeInTheDocument()
    })
    const deleteButtons = screen.getAllByText('削除')
    fireEvent.click(deleteButtons[0]!)
    await waitFor(() => {
      expect(screen.getByText('本当に削除しますか？この操作は取り消せません。')).toBeInTheDocument()
    })
  })

  it('編集ダイアログでフォーム更新送信する', async () => {
    mockApi.get.mockResolvedValue(mockCategories)
    mockApi.put.mockResolvedValue(mockCategories[0])
    renderPage()
    await waitFor(() => {
      expect(screen.getAllByText('飲み物').length).toBeGreaterThanOrEqual(1)
    })
    const editButtons = screen.getAllByText('編集')
    fireEvent.click(editButtons[0]!)
    await waitFor(() => {
      expect(screen.getByText('カテゴリを編集')).toBeInTheDocument()
    })
    fireEvent.change(screen.getByLabelText('カテゴリ名 *'), { target: { value: 'ドリンク' } })
    fireEvent.click(screen.getByRole('button', { name: '更新' }))
    await waitFor(() => {
      expect(mockApi.put).toHaveBeenCalledWith(
        '/api/categories/cat-1',
        expect.objectContaining({ name: 'ドリンク' }),
        expect.anything(),
      )
    })
  })
})
