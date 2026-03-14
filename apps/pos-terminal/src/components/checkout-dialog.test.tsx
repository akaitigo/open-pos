import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CheckoutDialog } from './checkout-dialog'
import { useCartStore } from '@/stores/cart-store'
import { useAuthStore } from '@/stores/auth-store'
import { FinalizeTransactionResponseSchema } from '@shared-types/openpos'
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
