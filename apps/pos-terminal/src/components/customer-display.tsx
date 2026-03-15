/**
 * 顧客表示ディスプレイ連携 (#213)
 * 別ウィンドウで現在のカート内容と合計を表示する。
 */

import { useEffect, useRef } from 'react'
import { useCartStore, getCartSubtotal } from '@/stores/cart-store'
import { formatMoney } from '@shared-types/openpos'

/**
 * 顧客ディスプレイウィンドウを開くボタン。
 */
export function CustomerDisplayButton() {
  const displayWindow = useRef<Window | null>(null)
  const items = useCartStore((s) => s.items)
  const subtotal = getCartSubtotal(items)

  useEffect(() => {
    if (!displayWindow.current || displayWindow.current.closed) return

    const doc = displayWindow.current.document
    updateDisplay(doc, items, subtotal)
  }, [items, subtotal])

  function openDisplay() {
    const win = window.open(
      '',
      'customer-display',
      'width=600,height=800,menubar=no,toolbar=no,location=no,status=no',
    )
    if (!win) return

    displayWindow.current = win
    const doc = win.document
    doc.title = 'お客様用ディスプレイ'
    initDisplayStyles(doc)
    updateDisplay(doc, items, subtotal)
  }

  return (
    <button
      onClick={openDisplay}
      className="px-3 py-2 text-sm rounded-md border border-input bg-background hover:bg-accent"
      data-testid="customer-display-button"
    >
      顧客ディスプレイ
    </button>
  )
}

function initDisplayStyles(doc: Document) {
  // Clear existing content safely
  while (doc.head.firstChild) doc.head.removeChild(doc.head.firstChild)
  while (doc.body.firstChild) doc.body.removeChild(doc.body.firstChild)

  const meta = doc.createElement('meta')
  meta.setAttribute('charset', 'UTF-8')
  doc.head.appendChild(meta)

  const style = doc.createElement('style')
  style.textContent = `
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      margin: 0; padding: 24px; background: #0f172a; color: #f8fafc;
    }
    h1 { font-size: 24px; text-align: center; margin-bottom: 24px; }
    .items { border-top: 1px solid #334155; }
    .item {
      display: flex; justify-content: space-between; padding: 12px 0;
      border-bottom: 1px solid #1e293b; font-size: 18px;
    }
    .item-name { flex: 1; }
    .item-qty { width: 60px; text-align: center; color: #94a3b8; }
    .item-price { width: 120px; text-align: right; }
    .total {
      display: flex; justify-content: space-between; padding: 24px 0;
      font-size: 32px; font-weight: bold; border-top: 2px solid #3b82f6; margin-top: 16px;
    }
    .empty { text-align: center; color: #64748b; font-size: 20px; padding: 48px 0; }
  `
  doc.head.appendChild(style)
}

function updateDisplay(
  doc: Document,
  items: { product: { name: string; price: number }; quantity: number }[],
  subtotal: number,
) {
  // Clear body safely using DOM methods
  while (doc.body.firstChild) doc.body.removeChild(doc.body.firstChild)

  const h1 = doc.createElement('h1')
  h1.textContent = 'お会計'
  doc.body.appendChild(h1)

  if (items.length === 0) {
    const empty = doc.createElement('div')
    empty.className = 'empty'
    empty.textContent = '商品をスキャンしてください'
    doc.body.appendChild(empty)
    return
  }

  const itemsDiv = doc.createElement('div')
  itemsDiv.className = 'items'

  for (const item of items) {
    const row = doc.createElement('div')
    row.className = 'item'

    const nameSpan = doc.createElement('span')
    nameSpan.className = 'item-name'
    nameSpan.textContent = item.product.name

    const qtySpan = doc.createElement('span')
    qtySpan.className = 'item-qty'
    qtySpan.textContent = `x${item.quantity}`

    const priceSpan = doc.createElement('span')
    priceSpan.className = 'item-price'
    priceSpan.textContent = formatMoney(item.product.price * item.quantity)

    row.appendChild(nameSpan)
    row.appendChild(qtySpan)
    row.appendChild(priceSpan)
    itemsDiv.appendChild(row)
  }
  doc.body.appendChild(itemsDiv)

  const totalDiv = doc.createElement('div')
  totalDiv.className = 'total'

  const totalLabel = doc.createElement('span')
  totalLabel.textContent = '合計'

  const totalValue = doc.createElement('span')
  totalValue.textContent = formatMoney(subtotal)

  totalDiv.appendChild(totalLabel)
  totalDiv.appendChild(totalValue)
  doc.body.appendChild(totalDiv)
}
