/**
 * セルフレジ / セルフオーダーモード (#201)
 * 大きなボタン、スタッフ機能なし、顧客向けフロー: 選択 -> 支払い -> レシート
 */

import { useState } from 'react'
import { useCartStore, getCartSubtotal, getCartItemCount } from '@/stores/cart-store'
import { formatMoney } from '@shared-types/openpos'
import type { Product } from '@shared-types/openpos'

interface KioskModeProps {
  products: Product[]
  onCheckout: () => void
  onExit: () => void
}

export function KioskMode({ products, onCheckout, onExit }: KioskModeProps) {
  const { items, addItem, clearCart } = useCartStore()
  const subtotal = getCartSubtotal(items)
  const itemCount = getCartItemCount(items)
  const [step, setStep] = useState<'SELECT' | 'CONFIRM' | 'DONE'>('SELECT')

  if (step === 'DONE') {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-background p-8">
        <div className="text-6xl mb-8">&#10004;</div>
        <h1 className="text-4xl font-bold mb-4">ありがとうございました</h1>
        <p className="text-xl text-muted-foreground mb-8">レシートをお受け取りください</p>
        <button
          className="px-12 py-6 text-2xl rounded-xl bg-primary text-primary-foreground"
          onClick={() => {
            clearCart()
            setStep('SELECT')
          }}
          data-testid="kiosk-next-customer"
        >
          次のお客様
        </button>
      </div>
    )
  }

  if (step === 'CONFIRM') {
    return (
      <div className="flex flex-col min-h-screen bg-background p-8">
        <h1 className="text-3xl font-bold text-center mb-8">ご注文の確認</h1>
        <div className="flex-1 space-y-4">
          {items.map((item) => (
            <div
              key={item.product.id}
              className="flex items-center justify-between p-4 rounded-lg border text-xl"
            >
              <span className="flex-1">{item.product.name}</span>
              <span className="w-20 text-center">x{item.quantity}</span>
              <span className="w-32 text-right">
                {formatMoney(item.product.price * item.quantity)}
              </span>
            </div>
          ))}
        </div>
        <div className="border-t pt-6 mt-6">
          <div className="flex justify-between text-3xl font-bold mb-8">
            <span>合計</span>
            <span>{formatMoney(subtotal)}</span>
          </div>
          <div className="flex gap-4">
            <button
              className="flex-1 px-8 py-6 text-xl rounded-xl border"
              onClick={() => setStep('SELECT')}
              data-testid="kiosk-back"
            >
              戻る
            </button>
            <button
              className="flex-1 px-8 py-6 text-xl rounded-xl bg-primary text-primary-foreground"
              onClick={() => {
                onCheckout()
                setStep('DONE')
              }}
              data-testid="kiosk-pay"
            >
              お支払い
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col min-h-screen bg-background">
      {/* ヘッダー */}
      <div className="flex items-center justify-between p-4 border-b">
        <h1 className="text-2xl font-bold">セルフオーダー</h1>
        <button
          className="px-4 py-2 text-sm rounded-md border"
          onClick={onExit}
          data-testid="kiosk-exit"
        >
          スタッフモード
        </button>
      </div>

      {/* 商品グリッド */}
      <div className="flex-1 p-4 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4 overflow-y-auto">
        {products.map((product) => (
          <button
            key={product.id}
            className="flex flex-col items-center justify-center p-6 rounded-xl border hover:bg-accent transition-colors min-h-[120px]"
            onClick={() => addItem(product)}
            data-testid={`kiosk-product-${product.id}`}
          >
            <span className="text-lg font-medium text-center">{product.name}</span>
            <span className="text-xl font-bold mt-2">{formatMoney(product.price)}</span>
          </button>
        ))}
      </div>

      {/* フッター（カートサマリー） */}
      {itemCount > 0 && (
        <div className="border-t p-4 flex items-center justify-between">
          <div className="text-xl">
            <span className="text-muted-foreground">{itemCount}点</span>
            <span className="ml-4 font-bold text-2xl">{formatMoney(subtotal)}</span>
          </div>
          <div className="flex gap-2">
            <button
              className="px-6 py-4 text-lg rounded-xl border"
              onClick={clearCart}
              data-testid="kiosk-clear"
            >
              クリア
            </button>
            <button
              className="px-8 py-4 text-lg rounded-xl bg-primary text-primary-foreground"
              onClick={() => setStep('CONFIRM')}
              data-testid="kiosk-checkout"
            >
              注文する
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
