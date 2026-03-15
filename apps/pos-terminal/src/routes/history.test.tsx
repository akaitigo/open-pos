import { render, screen, waitFor } from '@testing-library/react'
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
})
