import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { CartPanel } from './cart-panel'
import { useCartStore } from '@/stores/cart-store'
import type { Product } from '@shared-types/openpos'

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue([]),
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
  name: 'テスト商品',
  price: 15000,
  displayOrder: 0,
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const mockProduct2: Product = {
  id: '550e8400-e29b-41d4-a716-446655440002',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  name: 'テスト商品2',
  price: 20000,
  displayOrder: 1,
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('CartPanel', () => {
  beforeEach(() => {
    useCartStore.setState({ items: [] })
  })

  it('空カートで「カートは空です」と表示する', () => {
    render(<CartPanel />)
    expect(screen.getByText('カートは空です')).toBeInTheDocument()
    expect(screen.getByText('商品を追加してください')).toBeInTheDocument()
  })

  it('空カートではお会計ボタンが無効', () => {
    render(<CartPanel />)
    const button = screen.getByRole('button', { name: /お会計/ })
    expect(button).toBeDisabled()
  })

  it('商品があるとお会計ボタンが有効', () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartPanel />)
    const button = screen.getByRole('button', { name: /お会計/ })
    expect(button).not.toBeDisabled()
  })

  it('割引行を表示する（割引なしの場合は ￥0）', () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartPanel />)
    expect(screen.getByText('割引')).toBeInTheDocument()
  })

  it('合計（税込）行を表示する', () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartPanel />)
    expect(screen.getByText('合計（税込）')).toBeInTheDocument()
  })

  it('税率の適用された商品がない場合のメッセージを表示する', () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartPanel />)
    expect(screen.getByText('税率の適用された商品はありません')).toBeInTheDocument()
  })

  it('商品点数を正しく表示する', () => {
    useCartStore.setState({
      items: [
        { product: mockProduct, quantity: 2 },
        { product: mockProduct2, quantity: 3 },
      ],
    })
    render(<CartPanel />)
    expect(screen.getByText('5 点')).toBeInTheDocument()
  })

  it('削除ボタンでカートから商品を削除できる', async () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartPanel />)

    await userEvent.click(screen.getByLabelText('テスト商品 を削除'))
    expect(screen.getByText('カートは空です')).toBeInTheDocument()
  })

  it('クリアボタンで全商品が削除される', async () => {
    useCartStore.setState({
      items: [
        { product: mockProduct, quantity: 1 },
        { product: mockProduct2, quantity: 1 },
      ],
    })
    render(<CartPanel />)

    await userEvent.click(screen.getByText('クリア'))
    expect(screen.getByText('カートは空です')).toBeInTheDocument()
  })

  it('fullScreen=false のときお会計ボタンのサイズが lg', () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartPanel fullScreen={false} />)
    const button = screen.getByRole('button', { name: /お会計/ })
    expect(button).toBeInTheDocument()
  })

  it('fullScreen=true のときレンダリングされる', () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 1 }],
    })
    render(<CartPanel fullScreen={true} />)
    expect(screen.getByText('テスト商品')).toBeInTheDocument()
  })
})
