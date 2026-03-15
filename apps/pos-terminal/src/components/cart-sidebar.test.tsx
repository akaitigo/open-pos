import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { CartSidebar } from './cart-sidebar'
import { useCartStore } from '@/stores/cart-store'
import { api } from '@/lib/api'
import type { Product } from '@shared-types/openpos'

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn().mockResolvedValue({}),
    setOrganizationId: vi.fn(),
  },
}))

vi.mock('@/stores/auth-store', () => {
  const state = {
    isAuthenticated: true,
    staff: null,
    storeId: null,
    storeName: null,
    terminalId: null,
    login: vi.fn(),
    logout: vi.fn(),
  }
  const fn = (selector?: (s: typeof state) => unknown) => (selector ? selector(state) : state)
  fn.getState = () => state
  fn.setState = vi.fn()
  fn.subscribe = vi.fn()
  fn.destroy = vi.fn()
  return { useAuthStore: fn }
})

const mockProduct: Product = {
  id: '550e8400-e29b-41d4-a716-446655440001',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  name: 'ドリップコーヒー',
  price: 15000,
  taxRateId: '550e8400-e29b-41d4-a716-446655440099',
  displayOrder: 0,
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const mockProduct2: Product = {
  id: '550e8400-e29b-41d4-a716-446655440002',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  name: 'サンドイッチ',
  price: 35000,
  displayOrder: 1,
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('CartSidebar', () => {
  beforeEach(() => {
    useCartStore.setState({ items: [] })
    vi.mocked(api.get).mockResolvedValue([
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
  })

  it('空カートでは「カートは空です」と表示する', () => {
    render(<CartSidebar />)
    expect(screen.getByText('カートは空です')).toBeInTheDocument()
    expect(screen.getByText('商品を追加してください')).toBeInTheDocument()
  })

  it('カートに商品があると名前が表示される', () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartSidebar />)
    expect(screen.getByText('ドリップコーヒー')).toBeInTheDocument()
  })

  it('お会計ボタンが表示される', () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 2 }],
    })
    render(<CartSidebar />)
    const checkoutButton = screen.getByRole('button', { name: /お会計/ })
    expect(checkoutButton).not.toBeDisabled()
  })

  it('空カートではお会計ボタンが無効', () => {
    render(<CartSidebar />)
    const checkoutButton = screen.getByRole('button', { name: /お会計/ })
    expect(checkoutButton).toBeDisabled()
  })

  it('クリアボタンでカートが空になる', async () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartSidebar />)
    await userEvent.click(screen.getByText('クリア'))
    expect(screen.getByText('カートは空です')).toBeInTheDocument()
  })

  it('商品数バッジが表示される', () => {
    useCartStore.setState({
      items: [
        { product: mockProduct, quantity: 2 },
        { product: mockProduct2, quantity: 1 },
      ],
    })
    render(<CartSidebar />)
    expect(screen.getAllByText('3').length).toBeGreaterThanOrEqual(1)
  })

  it('+ボタンで数量が増える', async () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartSidebar />)
    await userEvent.click(screen.getByLabelText('ドリップコーヒー の数量を増やす'))
    const state = useCartStore.getState()
    expect(state.items[0]!.quantity).toBe(2)
  })

  it('-ボタンで数量が減る（0で削除）', async () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartSidebar />)
    await userEvent.click(screen.getByLabelText('ドリップコーヒー の数量を減らす'))
    expect(screen.getByText('カートは空です')).toBeInTheDocument()
  })

  it('数量を直接入力できる', async () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartSidebar />)

    const quantityInput = screen.getByLabelText('ドリップコーヒー の数量')
    await userEvent.clear(quantityInput)
    await userEvent.type(quantityInput, '3')

    expect(useCartStore.getState().items[0]!.quantity).toBe(3)
  })

  it('小計と税率別内訳が表示される', async () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 2 }],
    })
    render(<CartSidebar />)

    expect(await screen.findAllByText('軽減税率 8%')).not.toHaveLength(0)
    expect(screen.getByText('税率別内訳')).toBeInTheDocument()
    expect(screen.getByText('商品点数')).toBeInTheDocument()
    expect(screen.getByText('合計（税込）')).toBeInTheDocument()
  })
})
