import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { KioskMode } from './kiosk-mode'
import { useCartStore } from '@/stores/cart-store'
import type { Product } from '@shared-types/openpos'

const mockProducts: Product[] = [
  {
    id: '550e8400-e29b-41d4-a716-446655440001',
    organizationId: '550e8400-e29b-41d4-a716-446655440000',
    name: 'コーヒー',
    price: 30000,
    displayOrder: 0,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: '550e8400-e29b-41d4-a716-446655440002',
    organizationId: '550e8400-e29b-41d4-a716-446655440000',
    name: 'サンドイッチ',
    price: 50000,
    displayOrder: 1,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

describe('KioskMode', () => {
  const onCheckout = vi.fn()
  const onExit = vi.fn()

  beforeEach(() => {
    useCartStore.setState({ items: [] })
    vi.clearAllMocks()
  })

  it('SELECTステップで商品一覧を表示する', () => {
    render(<KioskMode products={mockProducts} onCheckout={onCheckout} onExit={onExit} />)
    expect(screen.getByText('セルフオーダー')).toBeInTheDocument()
    expect(screen.getByText('コーヒー')).toBeInTheDocument()
    expect(screen.getByText('サンドイッチ')).toBeInTheDocument()
  })

  it('スタッフモードボタンで onExit を呼ぶ', async () => {
    render(<KioskMode products={mockProducts} onCheckout={onCheckout} onExit={onExit} />)
    await userEvent.click(screen.getByTestId('kiosk-exit'))
    expect(onExit).toHaveBeenCalledOnce()
  })

  it('商品をクリックするとカートに追加される', async () => {
    render(<KioskMode products={mockProducts} onCheckout={onCheckout} onExit={onExit} />)
    await userEvent.click(screen.getByTestId(`kiosk-product-${mockProducts[0]!.id}`))
    expect(useCartStore.getState().items).toHaveLength(1)
    expect(useCartStore.getState().items[0]!.product.id).toBe(mockProducts[0]!.id)
  })

  it('カートに商品があるとフッターが表示される', async () => {
    render(<KioskMode products={mockProducts} onCheckout={onCheckout} onExit={onExit} />)
    await userEvent.click(screen.getByTestId(`kiosk-product-${mockProducts[0]!.id}`))
    expect(screen.getByText('1点')).toBeInTheDocument()
    expect(screen.getByTestId('kiosk-checkout')).toBeInTheDocument()
    expect(screen.getByTestId('kiosk-clear')).toBeInTheDocument()
  })

  it('クリアボタンでカートが空になりフッターが消える', async () => {
    render(<KioskMode products={mockProducts} onCheckout={onCheckout} onExit={onExit} />)
    await userEvent.click(screen.getByTestId(`kiosk-product-${mockProducts[0]!.id}`))
    expect(screen.getByTestId('kiosk-clear')).toBeInTheDocument()

    await userEvent.click(screen.getByTestId('kiosk-clear'))
    expect(useCartStore.getState().items).toHaveLength(0)
    expect(screen.queryByTestId('kiosk-checkout')).not.toBeInTheDocument()
  })

  it('注文するボタンでCONFIRMステップに遷移する', async () => {
    render(<KioskMode products={mockProducts} onCheckout={onCheckout} onExit={onExit} />)
    await userEvent.click(screen.getByTestId(`kiosk-product-${mockProducts[0]!.id}`))
    await userEvent.click(screen.getByTestId('kiosk-checkout'))

    expect(screen.getByText('ご注文の確認')).toBeInTheDocument()
    expect(screen.getByText('コーヒー')).toBeInTheDocument()
    expect(screen.getByText('合計')).toBeInTheDocument()
  })

  it('CONFIRMステップで戻るボタンを押すとSELECTステップに戻る', async () => {
    render(<KioskMode products={mockProducts} onCheckout={onCheckout} onExit={onExit} />)
    await userEvent.click(screen.getByTestId(`kiosk-product-${mockProducts[0]!.id}`))
    await userEvent.click(screen.getByTestId('kiosk-checkout'))
    expect(screen.getByText('ご注文の確認')).toBeInTheDocument()

    await userEvent.click(screen.getByTestId('kiosk-back'))
    expect(screen.getByText('セルフオーダー')).toBeInTheDocument()
  })

  it('お支払いボタンで onCheckout を呼びDONEステップに遷移する', async () => {
    render(<KioskMode products={mockProducts} onCheckout={onCheckout} onExit={onExit} />)
    await userEvent.click(screen.getByTestId(`kiosk-product-${mockProducts[0]!.id}`))
    await userEvent.click(screen.getByTestId('kiosk-checkout'))
    await userEvent.click(screen.getByTestId('kiosk-pay'))

    expect(onCheckout).toHaveBeenCalledOnce()
    expect(screen.getByText('ありがとうございました')).toBeInTheDocument()
    expect(screen.getByText('レシートをお受け取りください')).toBeInTheDocument()
  })

  it('DONEステップで次のお客様ボタンを押すとカートがクリアされSELECTに戻る', async () => {
    render(<KioskMode products={mockProducts} onCheckout={onCheckout} onExit={onExit} />)
    await userEvent.click(screen.getByTestId(`kiosk-product-${mockProducts[0]!.id}`))
    await userEvent.click(screen.getByTestId('kiosk-checkout'))
    await userEvent.click(screen.getByTestId('kiosk-pay'))

    await userEvent.click(screen.getByTestId('kiosk-next-customer'))
    expect(useCartStore.getState().items).toHaveLength(0)
    expect(screen.getByText('セルフオーダー')).toBeInTheDocument()
  })

  it('CONFIRMステップで数量と小計が表示される', async () => {
    render(<KioskMode products={mockProducts} onCheckout={onCheckout} onExit={onExit} />)
    // 同じ商品を2回追加
    await userEvent.click(screen.getByTestId(`kiosk-product-${mockProducts[0]!.id}`))
    await userEvent.click(screen.getByTestId(`kiosk-product-${mockProducts[0]!.id}`))
    await userEvent.click(screen.getByTestId('kiosk-checkout'))

    expect(screen.getByText('x2')).toBeInTheDocument()
  })

  it('カートが空のときフッターが表示されない', () => {
    render(<KioskMode products={mockProducts} onCheckout={onCheckout} onExit={onExit} />)
    expect(screen.queryByTestId('kiosk-checkout')).not.toBeInTheDocument()
    expect(screen.queryByTestId('kiosk-clear')).not.toBeInTheDocument()
  })
})
