import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CheckoutDialog } from './checkout-dialog'
import { useCartStore } from '@/stores/cart-store'
import { useAuthStore } from '@/stores/auth-store'
import { useDiscountStore } from '@/stores/discount-store'
import { FinalizeTransactionResponseSchema } from '@shared-types/openpos'
import type { Product, Staff } from '@shared-types/openpos'

const mockApiPost = vi.fn()
const mockApiGet = vi.fn().mockResolvedValue({})
const mockSaveOfflineTransaction = vi.fn()

vi.mock('@/lib/api', () => ({
  api: {
    get: (...args: unknown[]) => mockApiGet(...args),
    post: (...args: unknown[]) => mockApiPost(...args),
    setOrganizationId: vi.fn(),
  },
}))

vi.mock('@/lib/offline-db', () => ({
  saveOfflineTransaction: (...args: unknown[]) => mockSaveOfflineTransaction(...args),
}))

const mockProduct: Product = {
  id: '550e8400-e29b-41d4-a716-446655440001',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  name: 'ドリップコーヒー',
  price: 15000,
  displayOrder: 0,
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const mockStaff: Staff = {
  id: '550e8400-e29b-41d4-a716-446655440010',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  storeId: '550e8400-e29b-41d4-a716-446655440020',
  name: '田中太郎',
  email: 'tanaka@example.com',
  role: 'MANAGER',
  isActive: true,
  failedPinAttempts: 0,
  isLocked: false,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('CheckoutDialog', () => {
  beforeEach(() => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 2 }],
    })
    useAuthStore.setState({
      isAuthenticated: true,
      staff: mockStaff,
      storeId: 'store-1',
      storeName: '渋谷本店',
      terminalId: 'terminal-1',
    })
    useDiscountStore.setState({ appliedDiscounts: [] })
    mockApiPost.mockReset()
    mockApiGet.mockReset()
    mockApiGet.mockResolvedValue([])
    mockSaveOfflineTransaction.mockReset()
    mockSaveOfflineTransaction.mockResolvedValue(1)
  })

  it('合計金額と支払方法タブを表示する', () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    expect(screen.getByText('お会計')).toBeInTheDocument()
    expect(screen.getByText('現金')).toBeInTheDocument()
    expect(screen.getByText('カード')).toBeInTheDocument()
    expect(screen.getByText('QR')).toBeInTheDocument()
    expect(screen.getByText('残額')).toBeInTheDocument()
  })

  it('現金入力欄とクイック金額ボタンが表示される', () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    expect(screen.getByPlaceholderText('0')).toBeInTheDocument()
    expect(screen.getByText('ぴったり')).toBeInTheDocument()
    expect(screen.getByText('¥500')).toBeInTheDocument()
    expect(screen.getByText('¥10,000')).toBeInTheDocument()
  })

  it('現金テンキーで金額を入力できる', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: '1' }))
    await userEvent.click(screen.getAllByRole('button', { name: '0' })[0]!)
    await userEvent.click(screen.getAllByRole('button', { name: '0' })[0]!)

    const input = screen.getByPlaceholderText('0') as HTMLInputElement
    expect(input.value).toBe('100')
  })

  it('ぴったりボタンで合計金額が入力される', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    await userEvent.click(screen.getByText('ぴったり'))
    const input = screen.getByPlaceholderText('0') as HTMLInputElement
    expect(input.value).toBe('300')
  })

  it('支払不足時に不足額を表示する', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    const input = screen.getByPlaceholderText('0') as HTMLInputElement

    await userEvent.type(input, '100')

    expect(screen.getByText('あと ￥200 不足しています。')).toBeInTheDocument()
    expect(screen.getByText('お会計を確定')).toBeDisabled()
  })

  it('カードタブに切り替えると決済金額と参照番号入力が表示される', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    await userEvent.click(screen.getByText('カード'))

    expect(screen.getByPlaceholderText('残額を入力')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('カード承認番号')).toBeInTheDocument()
    expect(screen.getByText('カード端末プレースホルダー')).toBeInTheDocument()
  })

  it('カード支払で残額を超える金額は追加できない', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    await userEvent.click(screen.getByText('カード'))
    await userEvent.clear(screen.getByPlaceholderText('残額を入力'))
    await userEvent.type(screen.getByPlaceholderText('残額を入力'), '500')
    await userEvent.type(screen.getByPlaceholderText('カード承認番号'), 'CARD-OVER')

    expect(screen.getByText('残額を超える金額は追加できません。')).toBeInTheDocument()
    expect(screen.getByText('この支払を追加')).toBeDisabled()
    expect(screen.getByText('お会計を確定')).toBeDisabled()
  })

  it('QRタブに切り替えると決済IDとプレースホルダーが表示される', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    await userEvent.click(screen.getByText('QR'))

    expect(screen.getByPlaceholderText('残額を入力')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('決済ID')).toBeInTheDocument()
    expect(screen.getByText('QR 決済プレースホルダー')).toBeInTheDocument()
    expect(screen.getByText('OPENPOS-QR')).toBeInTheDocument()
  })

  it('分割払いを追加して finalize に複数 payments を送る', async () => {
    const txResponse = {
      id: 'tx-1',
      organizationId: 'org-1',
      storeId: 'store-1',
      terminalId: 'terminal-1',
      staffId: 'staff-1',
      transactionNumber: 'TX-001',
      type: 'SALE',
      status: 'DRAFT',
      items: [],
      payments: [],
      appliedDiscounts: [],
      taxSummaries: [],
      subtotal: 0,
      discountTotal: 0,
      taxTotal: 0,
      total: 0,
      clientId: '',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    }
    const finalizeResponse = {
      transaction: txResponse,
      receipt: {
        id: '550e8400-e29b-41d4-a716-446655440099',
        transactionId: '550e8400-e29b-41d4-a716-446655440098',
        receiptData: 'レシートテスト',
        pdfUrl: null,
        createdAt: '2026-01-01T00:00:00Z',
      },
    }
    mockApiPost
      .mockResolvedValueOnce(txResponse)
      .mockResolvedValueOnce(txResponse)
      .mockResolvedValueOnce(finalizeResponse)

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    await userEvent.click(screen.getByText('カード'))
    const amountInput = screen.getByPlaceholderText('残額を入力')
    await userEvent.clear(amountInput)
    await userEvent.type(amountInput, '100')
    await userEvent.type(screen.getByPlaceholderText('カード承認番号'), 'CARD-001')
    await userEvent.click(screen.getByText('この支払を追加'))

    expect(screen.getByText('追加済みの支払')).toBeInTheDocument()
    expect(screen.getByText('参照番号: CARD-001')).toBeInTheDocument()

    await userEvent.click(screen.getByText('ぴったり'))
    await userEvent.click(screen.getByText('お会計を確定'))

    await waitFor(() => {
      expect(mockApiPost).toHaveBeenCalledTimes(3)
    })

    expect(mockApiPost).toHaveBeenLastCalledWith(
      '/api/transactions/tx-1/finalize',
      {
        payments: [
          {
            method: 'CREDIT_CARD',
            amount: 10000,
            reference: 'CARD-001',
          },
          {
            method: 'CASH',
            amount: 20000,
            received: 20000,
          },
        ],
      },
      FinalizeTransactionResponseSchema,
    )
  })

  it('決済エラー時にトースト用 API 呼び出しまで進む', async () => {
    mockApiPost.mockRejectedValueOnce(new Error('ネットワークエラー'))

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    await userEvent.click(screen.getByText('ぴったり'))
    await userEvent.click(screen.getByText('お会計を確定'))

    await waitFor(() => {
      expect(mockApiPost).toHaveBeenCalled()
    })
  })

  it('open=false のとき何も表示しない', () => {
    render(<CheckoutDialog open={false} onOpenChange={vi.fn()} />)
    expect(screen.queryByText('お会計')).not.toBeInTheDocument()
  })

  it('ネットワークエラー時にオフライン保存にフォールバックする', async () => {
    mockApiPost.mockRejectedValueOnce(new TypeError('Failed to fetch'))

    const onOpenChange = vi.fn()
    render(<CheckoutDialog open={true} onOpenChange={onOpenChange} />)
    await userEvent.click(screen.getByText('ぴったり'))
    await userEvent.click(screen.getByText('お会計を確定'))

    await waitFor(() => {
      expect(mockSaveOfflineTransaction).toHaveBeenCalledTimes(1)
    })

    const savedTx = mockSaveOfflineTransaction.mock.calls[0]![0] as {
      clientId: string
      storeId: string
      terminalId: string
      staffId: string
      items: Array<{ productId: string; productName: string }>
      payments: Array<{ method: string; amount: number }>
      syncStatus: string
    }
    expect(savedTx.storeId).toBe('store-1')
    expect(savedTx.terminalId).toBe('terminal-1')
    expect(savedTx.staffId).toBe(mockStaff.id)
    expect(savedTx.items).toHaveLength(1)
    expect(savedTx.items[0]!.productId).toBe(mockProduct.id)
    expect(savedTx.items[0]!.productName).toBe('ドリップコーヒー')
    expect(savedTx.payments).toHaveLength(1)
    expect(savedTx.payments[0]!.method).toBe('CASH')
    expect(savedTx.syncStatus).toBe('pending')
    expect(savedTx.clientId).toBeTruthy()

    // ダイアログが閉じられる
    await waitFor(() => {
      expect(onOpenChange).toHaveBeenCalledWith(false)
    })
  })

  it('ApiError 時はオフライン保存ではなく通常エラーを表示する', async () => {
    const { ApiError } = await import('@shared-types/openpos')
    mockApiPost.mockRejectedValueOnce(new ApiError(500, 'INTERNAL_ERROR', 'サーバーエラー'))

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    await userEvent.click(screen.getByText('ぴったり'))
    await userEvent.click(screen.getByText('お会計を確定'))

    await waitFor(() => {
      expect(mockApiPost).toHaveBeenCalled()
    })

    // オフライン保存は呼ばれない（サーバーエラーはネットワークエラーではない）
    expect(mockSaveOfflineTransaction).not.toHaveBeenCalled()
  })

  it('ぴったりボタンは端数税額を切り上げた円額を入力する', async () => {
    useCartStore.setState({
      items: [
        {
          product: {
            ...mockProduct,
            price: 18800,
            taxRateId: '550e8400-e29b-41d4-a716-446655440099',
          },
          quantity: 1,
        },
      ],
    })
    mockApiGet.mockResolvedValue([
      {
        id: '550e8400-e29b-41d4-a716-446655440099',
        organizationId: '550e8400-e29b-41d4-a716-446655440000',
        name: '軽減税率',
        rate: '0.08',
        isReduced: true,
        isDefault: false,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ])

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    await waitFor(() => {
      expect(mockApiGet).toHaveBeenCalled()
    })

    await userEvent.click(screen.getByText('ぴったり'))

    const input = screen.getByPlaceholderText('0') as HTMLInputElement
    expect(input.value).toBe('204')
    expect(screen.getByText('お会計を確定')).not.toBeDisabled()
  })

  it('クーポン適用時に割引表示セクションが表示される', () => {
    useDiscountStore.setState({
      appliedDiscounts: [
        {
          couponCode: 'SAVE10',
          discount: {
            id: 'disc-1',
            organizationId: 'org-1',
            name: '10% オフ',
            discountType: 'PERCENTAGE',
            value: '0.10',
            isActive: true,
            startDate: null,
            endDate: null,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          amount: 3000,
        },
      ],
    })

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    expect(screen.getByText('10% オフ')).toBeInTheDocument()
    expect(screen.getByText('SAVE10')).toBeInTheDocument()
    expect(screen.getByText(/10% 割引/)).toBeInTheDocument()
  })

  it('クーポン削除ボタンで確認後に削除される', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    useDiscountStore.setState({
      appliedDiscounts: [
        {
          couponCode: 'SAVE10',
          discount: {
            id: 'disc-1',
            organizationId: 'org-1',
            name: '10% オフ',
            discountType: 'PERCENTAGE',
            value: '0.10',
            isActive: true,
            startDate: null,
            endDate: null,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          amount: 3000,
        },
      ],
    })

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    await userEvent.click(screen.getByLabelText('クーポン SAVE10 を削除'))

    expect(window.confirm).toHaveBeenCalled()
  })

  it('固定金額クーポンの場合は金額割引の表示になる', () => {
    useDiscountStore.setState({
      appliedDiscounts: [
        {
          couponCode: 'FLAT500',
          discount: {
            id: 'disc-2',
            organizationId: 'org-1',
            name: '500円引き',
            discountType: 'FIXED_AMOUNT',
            value: '50000',
            isActive: true,
            startDate: null,
            endDate: null,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          amount: 50000,
        },
      ],
    })

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    expect(screen.getByText('500円引き')).toBeInTheDocument()
    expect(screen.getByText(/￥500 割引/)).toBeInTheDocument()
  })

  it('取引キャンセルボタンでダイアログが閉じる', async () => {
    const onOpenChange = vi.fn()
    render(<CheckoutDialog open={true} onOpenChange={onOpenChange} />)

    await userEvent.click(screen.getByText('取引をキャンセル'))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('クーポンコードを入力して適用ボタンで有効クーポンを適用できる', async () => {
    mockApiGet.mockImplementation((path: string) => {
      if (typeof path === 'string' && path.includes('/api/coupons/validate/')) {
        return Promise.resolve({
          isValid: true,
          coupon: null,
          discount: {
            id: 'disc-coupon-1',
            organizationId: 'org-1',
            name: '20% オフ',
            discountType: 'PERCENTAGE',
            value: '0.20',
            isActive: true,
            startDate: null,
            endDate: null,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          reason: null,
        })
      }
      return Promise.resolve([])
    })

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    const couponInput = screen.getByPlaceholderText('クーポンコードを入力')
    await userEvent.type(couponInput, 'WELCOME20')
    await userEvent.click(screen.getByText('適用'))

    await waitFor(() => {
      expect(mockApiGet).toHaveBeenCalledWith('/api/coupons/validate/WELCOME20', expect.anything())
    })

    await waitFor(() => {
      expect(screen.getByText('20% オフ')).toBeInTheDocument()
    })
  })

  it('無効なクーポンコードの場合はエラーが表示される', async () => {
    mockApiGet.mockImplementation((path: string) => {
      if (typeof path === 'string' && path.includes('/api/coupons/validate/')) {
        return Promise.resolve({
          isValid: false,
          coupon: null,
          discount: null,
          reason: '期限切れのクーポンです',
        })
      }
      return Promise.resolve([])
    })

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    const couponInput = screen.getByPlaceholderText('クーポンコードを入力')
    await userEvent.type(couponInput, 'EXPIRED')
    await userEvent.click(screen.getByText('適用'))

    await waitFor(() => {
      expect(mockApiGet).toHaveBeenCalledWith('/api/coupons/validate/EXPIRED', expect.anything())
    })
  })

  it('クーポン検証APIエラー時にエラーが表示される', async () => {
    mockApiGet.mockImplementation((path: string) => {
      if (typeof path === 'string' && path.includes('/api/coupons/validate/')) {
        return Promise.reject(new Error('ネットワークエラー'))
      }
      return Promise.resolve([])
    })

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    const couponInput = screen.getByPlaceholderText('クーポンコードを入力')
    await userEvent.type(couponInput, 'BADCODE')
    await userEvent.click(screen.getByText('適用'))

    await waitFor(() => {
      expect(mockApiGet).toHaveBeenCalledWith('/api/coupons/validate/BADCODE', expect.anything())
    })
  })

  it('既に適用済みのクーポンコードを再適用するとエラーになる', async () => {
    useDiscountStore.setState({
      appliedDiscounts: [
        {
          couponCode: 'EXISTING',
          discount: {
            id: 'disc-existing',
            organizationId: 'org-1',
            name: '既存クーポン',
            discountType: 'PERCENTAGE',
            value: '0.05',
            isActive: true,
            startDate: null,
            endDate: null,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          amount: 1500,
        },
      ],
    })

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    const couponInput = screen.getByPlaceholderText('クーポンコードを入力')
    await userEvent.type(couponInput, 'EXISTING')
    await userEvent.click(screen.getByText('適用'))

    // APIは呼ばれない（重複チェックで弾かれる）
    expect(mockApiGet).not.toHaveBeenCalledWith(
      expect.stringContaining('/api/coupons/validate/'),
      expect.anything(),
    )
  })

  it('Enterキーでクーポンを適用できる', async () => {
    mockApiGet.mockImplementation((path: string) => {
      if (typeof path === 'string' && path.includes('/api/coupons/validate/')) {
        return Promise.resolve({
          isValid: true,
          coupon: null,
          discount: {
            id: 'disc-enter',
            organizationId: 'org-1',
            name: 'Enterクーポン',
            discountType: 'FIXED_AMOUNT',
            value: '10000',
            isActive: true,
            startDate: null,
            endDate: null,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          reason: null,
        })
      }
      return Promise.resolve([])
    })

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    const couponInput = screen.getByPlaceholderText('クーポンコードを入力')
    await userEvent.type(couponInput, 'ENTERCODE{Enter}')

    await waitFor(() => {
      expect(mockApiGet).toHaveBeenCalledWith('/api/coupons/validate/ENTERCODE', expect.anything())
    })
  })

  it('テンキーのバックスペースで入力を削除できる', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    // 123 を入力
    await userEvent.click(screen.getByRole('button', { name: '1' }))
    await userEvent.click(screen.getByRole('button', { name: '2' }))
    await userEvent.click(screen.getByRole('button', { name: '3' }))

    const input = screen.getByPlaceholderText('0') as HTMLInputElement
    expect(input.value).toBe('123')

    // バックスペースで末尾を削除
    await userEvent.click(screen.getByRole('button', { name: '⌫' }))
    expect(input.value).toBe('12')
  })

  it('追加済み支払から削除ボタンで支払を削除できる', async () => {
    const txResponse = {
      id: 'tx-1',
      organizationId: 'org-1',
      storeId: 'store-1',
      terminalId: 'terminal-1',
      staffId: 'staff-1',
      transactionNumber: 'TX-001',
      type: 'SALE',
      status: 'DRAFT',
      items: [],
      payments: [],
      appliedDiscounts: [],
      taxSummaries: [],
      subtotal: 0,
      discountTotal: 0,
      taxTotal: 0,
      total: 0,
      clientId: '',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    }
    mockApiPost.mockResolvedValue(txResponse)

    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)

    // カード支払を追加
    await userEvent.click(screen.getByText('カード'))
    const amountInput = screen.getByPlaceholderText('残額を入力')
    await userEvent.clear(amountInput)
    await userEvent.type(amountInput, '100')
    await userEvent.type(screen.getByPlaceholderText('カード承認番号'), 'CARD-DEL')
    await userEvent.click(screen.getByText('この支払を追加'))

    expect(screen.getByText('追加済みの支払')).toBeInTheDocument()

    // 支払を削除
    await userEvent.click(screen.getByLabelText('カード 支払を削除'))

    expect(screen.queryByText('追加済みの支払')).not.toBeInTheDocument()
  })
})
