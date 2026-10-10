import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { HistoryPage } from './history'
import { useAuthStore } from '@/stores/auth-store'

const mockApiGet = vi.fn()

vi.mock('@/lib/api', () => ({
  api: {
    get: (...args: unknown[]) => mockApiGet(...args),
    post: vi.fn().mockResolvedValue({}),
    setOrganizationId: vi.fn(),
  },
}))

const mockTransaction = {
  id: 'tx-1',
  organizationId: 'org-1',
  storeId: 'store-1',
  terminalId: 'terminal-1',
  staffId: 'staff-1',
  transactionNumber: 'TX-001',
  type: 'SALE',
  status: 'COMPLETED',
  items: [],
  payments: [],
  appliedDiscounts: [],
  taxSummaries: [],
  subtotal: 30000,
  discountTotal: 0,
  taxTotal: 3000,
  total: 33000,
  clientId: '',
  createdAt: '2026-03-06T10:00:00Z',
  updatedAt: '2026-03-06T10:00:00Z',
}

const mockDraftTransaction = {
  ...mockTransaction,
  id: 'tx-2',
  transactionNumber: 'TX-002',
  status: 'DRAFT',
  total: 10000,
}

describe('HistoryPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      isAuthenticated: true,
      staff: null,
      storeId: 'store-1',
      storeName: 'テスト店舗',
      terminalId: null,
    })
    mockApiGet.mockReset()
  })

  it('取引履歴テーブルが表示される', async () => {
    mockApiGet.mockResolvedValue({
      data: [],
      pagination: { page: 1, pageSize: 20, totalCount: 0, totalPages: 0 },
    })
    render(<HistoryPage />)
    expect(screen.getByText('取引履歴')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getAllByText('取引履歴がありません').length).toBeGreaterThanOrEqual(1)
    })
  })

  it('取引データが表示される', async () => {
    mockApiGet.mockResolvedValue({
      data: [mockTransaction],
      pagination: { page: 1, pageSize: 20, totalCount: 1, totalPages: 1 },
    })
    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getAllByText('TX-001').length).toBeGreaterThanOrEqual(1)
    })
    expect(screen.getAllByText('完了').length).toBeGreaterThanOrEqual(1)
  })

  it('下書きステータスが正しく表示される', async () => {
    mockApiGet.mockResolvedValue({
      data: [mockDraftTransaction],
      pagination: { page: 1, pageSize: 20, totalCount: 1, totalPages: 1 },
    })
    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getAllByText('下書き').length).toBeGreaterThanOrEqual(1)
    })
  })

  it('完了取引にレシートボタンが表示される', async () => {
    mockApiGet.mockResolvedValue({
      data: [mockTransaction],
      pagination: { page: 1, pageSize: 20, totalCount: 1, totalPages: 1 },
    })
    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getAllByText('レシート').length).toBeGreaterThanOrEqual(1)
    })
  })

  it('レシートボタンでレシートが表示される', async () => {
    mockApiGet
      .mockResolvedValueOnce({
        data: [mockTransaction],
        pagination: { page: 1, pageSize: 20, totalCount: 1, totalPages: 1 },
      })
      .mockResolvedValueOnce({
        id: 'r-1',
        transactionId: 'tx-1',
        receiptData: '=== レシート ===',
      })

    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getAllByText('レシート').length).toBeGreaterThanOrEqual(1)
    })
    // Click the first receipt button (table view)
    await userEvent.click(screen.getAllByText('レシート')[0]!)
    await waitFor(() => {
      expect(screen.getByText('=== レシート ===')).toBeInTheDocument()
    })
  })

  it('ページネーションが表示される', async () => {
    mockApiGet.mockResolvedValue({
      data: [mockTransaction],
      pagination: { page: 1, pageSize: 20, totalCount: 40, totalPages: 2 },
    })
    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getByText('1 / 2')).toBeInTheDocument()
    })
    expect(screen.getByText('前へ')).toBeDisabled()
    expect(screen.getByText('次へ')).not.toBeDisabled()
  })

  it('次へボタンでページが切り替わる', async () => {
    mockApiGet.mockResolvedValue({
      data: [mockTransaction],
      pagination: { page: 1, pageSize: 20, totalCount: 40, totalPages: 2 },
    })
    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getByText('次へ')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByText('次へ'))
    await waitFor(() => {
      expect(mockApiGet).toHaveBeenCalledTimes(2)
    })
  })

  it('storeId が null の場合 API を呼ばない', () => {
    useAuthStore.setState({ storeId: null })
    render(<HistoryPage />)
    expect(mockApiGet).not.toHaveBeenCalled()
  })

  it('VOIDED ステータスが「取消」と表示される', async () => {
    const voidedTx = {
      ...mockTransaction,
      id: 'tx-v',
      transactionNumber: 'TX-VOID',
      status: 'VOIDED',
    }
    mockApiGet.mockResolvedValue({
      data: [voidedTx],
      pagination: { page: 1, pageSize: 20, totalCount: 1, totalPages: 1 },
    })
    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getAllByText('取消').length).toBeGreaterThanOrEqual(1)
    })
    // VOIDED 取引にはレシートボタンが表示されない
    expect(screen.queryByTestId('receipt-btn-tx-v')).not.toBeInTheDocument()
  })

  it('完了取引に領収書発行ボタンが表示される', async () => {
    mockApiGet.mockResolvedValue({
      data: [mockTransaction],
      pagination: { page: 1, pageSize: 20, totalCount: 1, totalPages: 1 },
    })
    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getAllByText('領収書発行').length).toBeGreaterThanOrEqual(1)
    })
  })

  it('レシート取得に失敗した場合はダイアログが表示されない', async () => {
    mockApiGet
      .mockResolvedValueOnce({
        data: [mockTransaction],
        pagination: { page: 1, pageSize: 20, totalCount: 1, totalPages: 1 },
      })
      .mockRejectedValueOnce(new Error('receipt error'))

    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getAllByText('レシート').length).toBeGreaterThanOrEqual(1)
    })
    await userEvent.click(screen.getAllByText('レシート')[0]!)
    // ダイアログが表示されない（エラーはキャッチされるだけ）
    await waitFor(() => {
      expect(mockApiGet).toHaveBeenCalledTimes(2)
    })
    expect(screen.queryByText('テストレシートデータ')).not.toBeInTheDocument()
  })

  it('領収書発行ボタンでレシートダイアログが開く', async () => {
    mockApiGet
      .mockResolvedValueOnce({
        data: [mockTransaction],
        pagination: { page: 1, pageSize: 20, totalCount: 1, totalPages: 1 },
      })
      .mockResolvedValueOnce({
        id: 'r-2',
        transactionId: 'tx-1',
        receiptData: '領収書データ',
      })

    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getAllByText('領収書発行').length).toBeGreaterThanOrEqual(1)
    })
    await userEvent.click(screen.getAllByText('領収書発行')[0]!)
    await waitFor(() => {
      expect(screen.getByText('領収書データ')).toBeInTheDocument()
    })
  })

  it('前へボタンで前のページに戻る', async () => {
    mockApiGet.mockResolvedValue({
      data: [mockTransaction],
      pagination: { page: 1, pageSize: 20, totalCount: 40, totalPages: 2 },
    })
    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getByText('次へ')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByText('次へ'))
    await waitFor(() => {
      expect(mockApiGet).toHaveBeenCalledTimes(2)
    })
    await userEvent.click(screen.getByText('前へ'))
    await waitFor(() => {
      expect(mockApiGet).toHaveBeenCalledTimes(3)
    })
  })

  it('レシートダイアログを閉じるとレシートデータがクリアされる', async () => {
    mockApiGet
      .mockResolvedValueOnce({
        data: [mockTransaction],
        pagination: { page: 1, pageSize: 20, totalCount: 1, totalPages: 1 },
      })
      .mockResolvedValueOnce({
        id: 'r-1',
        transactionId: 'tx-1',
        receiptData: 'テストレシートデータ',
      })
    render(<HistoryPage />)
    await waitFor(() => {
      expect(screen.getAllByText('レシート').length).toBeGreaterThanOrEqual(1)
    })
    await userEvent.click(screen.getAllByText('レシート')[0]!)
    await waitFor(() => {
      expect(screen.getByText('テストレシートデータ')).toBeInTheDocument()
    })
    // 閉じるボタンをクリック
    await userEvent.click(screen.getByText('閉じる'))
    await waitFor(() => {
      expect(screen.queryByText('テストレシートデータ')).not.toBeInTheDocument()
    })
  })
  it('読み込み中は履歴なしと表示しない', () => {
    mockApiGet.mockReturnValue(new Promise(() => {}))
    render(<HistoryPage />)
    expect(screen.getByText('取引履歴を読み込み中…')).toBeInTheDocument()
    expect(screen.queryByText('取引履歴がありません')).not.toBeInTheDocument()
  })

  it('取得失敗を通知し、再試行後に実際の空一覧を表示する', async () => {
    mockApiGet.mockRejectedValueOnce(new Error('network unavailable')).mockResolvedValueOnce({
      data: [],
      pagination: { page: 1, pageSize: 20, totalCount: 0, totalPages: 0 },
    })
    render(<HistoryPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent('取引履歴を取得できませんでした')
    expect(screen.queryByText('取引履歴がありません')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: '再試行' }))
    expect(await screen.findByText('取引履歴がありません')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(mockApiGet).toHaveBeenCalledTimes(2)
  })

  it('ページ取得失敗時に前ページの取引を表示し続けない', async () => {
    mockApiGet
      .mockResolvedValueOnce({
        data: [mockTransaction],
        pagination: { page: 1, pageSize: 20, totalCount: 40, totalPages: 2 },
      })
      .mockRejectedValueOnce(new Error('network unavailable'))
      .mockResolvedValueOnce({
        data: [{ ...mockTransaction, id: 'tx-next', transactionNumber: 'TX-NEXT' }],
        pagination: { page: 2, pageSize: 20, totalCount: 40, totalPages: 2 },
      })
    render(<HistoryPage />)
    await userEvent.click(await screen.findByRole('button', { name: '次のページへ' }))
    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(screen.queryByText('TX-001')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: '再試行' }))
    expect(await screen.findByText('TX-NEXT')).toBeInTheDocument()
    expect(mockApiGet.mock.calls[2]![2]).toEqual({
      params: { storeId: 'store-1', page: 2, pageSize: 20 },
    })
  })

  it('店舗切替後に遅れて届いた旧店舗の結果を表示しない', async () => {
    let resolveOld!: (value: unknown) => void
    mockApiGet
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveOld = resolve
        }),
      )
      .mockResolvedValueOnce({
        data: [],
        pagination: { page: 1, pageSize: 20, totalCount: 0, totalPages: 0 },
      })
    render(<HistoryPage />)
    act(() => useAuthStore.setState({ storeId: 'store-2' }))
    await screen.findByText('取引履歴がありません')
    await act(async () => resolveOld({ data: [mockTransaction], pagination: { totalPages: 1 } }))
    expect(screen.queryByText('TX-001')).not.toBeInTheDocument()
    expect(screen.getByText('取引履歴がありません')).toBeInTheDocument()
  })

  it('レシート取得失敗を通知し、再操作で取得できる', async () => {
    mockApiGet
      .mockResolvedValueOnce({
        data: [mockTransaction],
        pagination: { page: 1, pageSize: 20, totalCount: 1, totalPages: 1 },
      })
      .mockRejectedValueOnce(new Error('receipt unavailable'))
      .mockResolvedValueOnce({ receiptData: '再取得レシート' })
    render(<HistoryPage />)
    await userEvent.click(await screen.findByTestId('receipt-btn-tx-1'))
    expect(await screen.findByRole('alert')).toHaveTextContent('レシートを取得できませんでした')
    await userEvent.click(screen.getByTestId('receipt-btn-tx-1'))
    expect(await screen.findByText('再取得レシート')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
  describe('receipt request ordering', () => {
    function deferred() {
      let resolve!: (value: unknown) => void
      let reject!: (error: Error) => void
      const promise = new Promise((yes, no) => {
        resolve = yes
        reject = no
      })
      return { promise, resolve, reject }
    }

    async function startRequests(sameTransaction = false) {
      const older = deferred()
      const latest = deferred()
      mockApiGet
        .mockResolvedValueOnce({
          data: [
            mockTransaction,
            { ...mockTransaction, id: 'tx-other', transactionNumber: 'TX-OTHER' },
          ],
          pagination: { page: 1, pageSize: 20, totalCount: 2, totalPages: 1 },
        })
        .mockReturnValueOnce(older.promise)
        .mockReturnValueOnce(latest.promise)
      const view = render(<HistoryPage />)
      await userEvent.click(await screen.findByTestId('receipt-btn-tx-1'))
      await userEvent.click(
        screen.getByTestId(sameTransaction ? 'receipt-btn-tx-1' : 'receipt-btn-tx-other'),
      )
      return { older, latest, view }
    }

    it.each([false, true])('最新成功後の旧成功を無視する（同一取引=%s）', async (same) => {
      const { older, latest } = await startRequests(same)
      await act(async () => latest.resolve({ receiptData: '最新レシート' }))
      expect(screen.getByText('最新レシート')).toBeInTheDocument()
      await act(async () => older.resolve({ receiptData: '古いレシート' }))
      expect(screen.getByText('最新レシート')).toBeInTheDocument()
      expect(screen.queryByText('古いレシート')).not.toBeInTheDocument()
    })

    it('最新応答を待っている間も旧成功を表示しない', async () => {
      const { older, latest } = await startRequests()
      await act(async () => older.resolve({ receiptData: '古いレシート' }))
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      await act(async () => latest.resolve({ receiptData: '最新レシート' }))
      expect(screen.getByText('最新レシート')).toBeInTheDocument()
    })

    it('最新成功後の旧失敗を通知しない', async () => {
      const { older, latest } = await startRequests()
      await act(async () => latest.resolve({ receiptData: '最新レシート' }))
      await act(async () => older.reject(new Error('old error')))
      expect(screen.getByText('最新レシート')).toBeInTheDocument()
      expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    })

    it.each(['success', 'failure'])('最新失敗後の旧%sを無視する', async (oldResult) => {
      const { older, latest } = await startRequests()
      await act(async () => latest.reject(new Error('latest error')))
      await act(async () => {
        if (oldResult === 'success') older.resolve({ receiptData: '古いレシート' })
        else older.reject(new Error('old error'))
      })
      expect(screen.getByRole('alert')).toHaveTextContent('レシートを取得できませんでした')
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('閉じた後に旧応答でダイアログを再表示しない', async () => {
      const { older, latest } = await startRequests()
      await act(async () => latest.resolve({ receiptData: '最新レシート' }))
      await userEvent.click(screen.getByTestId('receipt-close-btn'))
      await act(async () => older.resolve({ receiptData: '古いレシート' }))
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('店舗変更後の応答を無視する', async () => {
      const { older, latest } = await startRequests()
      mockApiGet.mockResolvedValueOnce({
        data: [],
        pagination: { page: 1, pageSize: 20, totalCount: 0, totalPages: 0 },
      })
      act(() => useAuthStore.setState({ storeId: 'store-2' }))
      await screen.findByText('取引履歴がありません')
      await act(async () => {
        latest.resolve({ receiptData: '別店舗レシート' })
        older.reject(new Error('old error'))
      })
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    })

    it('画面を離れて戻った後に旧応答を表示しない', async () => {
      const { older, latest, view } = await startRequests()
      view.unmount()
      mockApiGet.mockResolvedValueOnce({
        data: [],
        pagination: { page: 1, pageSize: 20, totalCount: 0, totalPages: 0 },
      })
      render(<HistoryPage />)
      await screen.findByText('取引履歴がありません')
      await act(async () => {
        latest.resolve({ receiptData: '戻る前のレシート' })
        older.reject(new Error('old error'))
      })
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    })
  })
})
