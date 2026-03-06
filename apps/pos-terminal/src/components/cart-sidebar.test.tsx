import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { CartSidebar } from './cart-sidebar'
import { useCartStore } from '@/stores/cart-store'
import type { Product } from '@shared-types/openpos'

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue({
      data: [],
      pagination: { page: 1, pageSize: 20, totalCount: 0, totalPages: 0 },
    }),
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
  })

  it('空カートでは「カートは空です」と表示する', () => {
    render(<CartSidebar />)
    expect(screen.getByText('カートは空です')).toBeInTheDocument()
    expect(screen.getByText('カート')).toBeInTheDocument()
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
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('+ボタンで数量が増える', async () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartSidebar />)
    // h-7 w-7 のアイコンボタン: [minus, plus, delete]
    const iconButtons = screen.getAllByRole('button').filter((btn) => btn.className.includes('h-7'))
    // plus is the second icon button
    await userEvent.click(iconButtons[1])
    const state = useCartStore.getState()
    expect(state.items[0].quantity).toBe(2)
  })

  it('-ボタンで数量が減る（0で削除）', async () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartSidebar />)
    const iconButtons = screen.getAllByRole('button').filter((btn) => btn.className.includes('h-7'))
    // minus is the first icon button
    await userEvent.click(iconButtons[0])
    expect(screen.getByText('カートは空です')).toBeInTheDocument()
  })

  it('小計が正しく表示される', () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 2 }],
    })
    render(<CartSidebar />)
    // 15000 * 2 = 30000銭 = ¥300
    expect(screen.getByText('小計')).toBeInTheDocument()
  })
})
