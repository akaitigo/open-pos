import { render, screen } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { CartPage } from './cart'
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

const mockProduct: Product = {
  id: '550e8400-e29b-41d4-a716-446655440010',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  name: '北海道おにぎり鮭',
  price: 22000,
  taxRateId: '550e8400-e29b-41d4-a716-446655440099',
  displayOrder: 0,
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('CartPage', () => {
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

  it('空カートでは追加案内が表示される', () => {
    render(<CartPage />)

    expect(screen.getByText('カートは空です')).toBeInTheDocument()
    expect(screen.getByText('商品を追加してください')).toBeInTheDocument()
  })

  it('カート明細と税率別内訳が表示される', async () => {
    useCartStore.setState({
      items: [{ product: mockProduct, quantity: 2 }],
    })

    render(<CartPage />)

    expect(screen.getByText('北海道おにぎり鮭')).toBeInTheDocument()
    expect(screen.getByDisplayValue('2')).toBeInTheDocument()
    expect(await screen.findAllByText('軽減税率 8%')).not.toHaveLength(0)
    expect(screen.getByText('合計（税込）')).toBeInTheDocument()
  })
})
