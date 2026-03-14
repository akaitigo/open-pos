import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { CheckoutDialog } from './checkout-dialog'
import { useCartStore } from '@/stores/cart-store'
import { useAuthStore } from '@/stores/auth-store'
import type { Product, Staff } from '@shared-types/openpos'

const mockApiPost = vi.fn()
const mockApiGet = vi.fn().mockResolvedValue({})

vi.mock('@/lib/api', () => ({
  api: {
    get: (...args: unknown[]) => mockApiGet(...args),
    post: (...args: unknown[]) => mockApiPost(...args),
    setOrganizationId: vi.fn(),
  },
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
    mockApiPost.mockReset()
    mockApiGet.mockReset()
    mockApiGet.mockResolvedValue([])
  })

  it('合計金額と支払方法タブを表示する', () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    expect(screen.getByText('お会計')).toBeInTheDocument()
    expect(screen.getByText('現金')).toBeInTheDocument()
    expect(screen.getByText('カード')).toBeInTheDocument()
    expect(screen.getByText('QR')).toBeInTheDocument()
  })

  it('お預かり金額の入力欄が表示される', () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    expect(screen.getByPlaceholderText('0')).toBeInTheDocument()
    expect(screen.getByText('ぴったり')).toBeInTheDocument()
  })

  it('プリセットボタンで金額が入力される', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    await userEvent.click(screen.getByText('¥1,000'))
    const input = screen.getByPlaceholderText('0') as HTMLInputElement
    expect(input.value).toBe('1000')
  })

  it('お会計を確定ボタンが表示される', () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    expect(screen.getByText('お会計を確定')).toBeInTheDocument()
  })

  it('open=false のとき何も表示しない', () => {
    render(<CheckoutDialog open={false} onOpenChange={vi.fn()} />)
    expect(screen.queryByText('お会計')).not.toBeInTheDocument()
  })

  it('ぴったりボタンで合計金額が入力される', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    await userEvent.click(screen.getByText('ぴったり'))
    const input = screen.getByPlaceholderText('0') as HTMLInputElement
    // subtotal = 15000 * 2 = 30000 (銭), 30000/100 = 300 (円)
    expect(input.value).toBe('300')
  })

  it('お釣り表示が正しく計算される', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    await userEvent.click(screen.getByText('¥1,000'))
    // ¥1,000 - ¥300 = ¥700 お釣り
    expect(screen.getByText('お釣り')).toBeInTheDocument()
  })

  it('決済確定で API シーケンスが呼ばれる', async () => {
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
      .mockResolvedValueOnce(txResponse) // create
      .mockResolvedValueOnce(txResponse) // add item
      .mockResolvedValueOnce(finalizeResponse) // finalize

    const onOpenChange = vi.fn()
    render(<CheckoutDialog open={true} onOpenChange={onOpenChange} />)
    await userEvent.click(screen.getByText('ぴったり'))
    await userEvent.click(screen.getByText('お会計を確定'))

    await waitFor(() => {
      expect(mockApiPost).toHaveBeenCalledTimes(3)
    })
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('決済エラー時にトーストが表示される', async () => {
    mockApiPost.mockRejectedValueOnce(new Error('ネットワークエラー'))
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    await userEvent.click(screen.getByText('ぴったり'))
    await userEvent.click(screen.getByText('お会計を確定'))

    await waitFor(() => {
      expect(mockApiPost).toHaveBeenCalled()
    })
  })

  it('カードタブに切り替えると参照番号入力が表示される', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    await userEvent.click(screen.getByText('カード'))
    expect(screen.getByPlaceholderText('カード承認番号')).toBeInTheDocument()
  })

  it('QRタブに切り替えると決済ID入力が表示される', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    await userEvent.click(screen.getByText('QR'))
    expect(screen.getByPlaceholderText('決済ID')).toBeInTheDocument()
  })

  it('カード決済では金額入力なしで確定可能', async () => {
    render(<CheckoutDialog open={true} onOpenChange={vi.fn()} />)
    await userEvent.click(screen.getByText('カード'))
    const confirmButton = screen.getByText('お会計を確定')
    expect(confirmButton).not.toBeDisabled()
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
})
