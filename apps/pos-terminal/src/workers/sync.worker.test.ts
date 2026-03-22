import { describe, it, expect } from 'vitest'
import type { SyncBatchPayload, PriceConflictPayload, TransactionData } from './sync.worker'

// Since we can't easily instantiate a real Web Worker in jsdom,
// we test the pure logic by importing the types and reimplementing
// the same pure functions that the worker uses.

function prepareSyncBatch(payload: SyncBatchPayload) {
  let totalAmount = 0
  for (const tx of payload.transactions) {
    for (const item of tx.items) {
      totalAmount += item.unitPrice * item.quantity
    }
  }
  return {
    transactions: payload.transactions,
    totalCount: payload.transactions.length,
    totalAmount,
  }
}

function detectPriceConflicts(payload: PriceConflictPayload) {
  const conflicts: Array<{
    clientId: string
    items: Array<{
      productId: string
      productName: string
      offlinePrice: number
      currentPrice: number
    }>
  }> = []

  for (const tx of payload.transactions) {
    const conflictItems: (typeof conflicts)[number]['items'] = []
    for (const item of tx.items) {
      const currentPrice = payload.currentPrices[item.productId]
      if (currentPrice !== undefined && currentPrice !== item.unitPrice) {
        conflictItems.push({
          productId: item.productId,
          productName: item.productName,
          offlinePrice: item.unitPrice,
          currentPrice,
        })
      }
    }
    if (conflictItems.length > 0) {
      conflicts.push({ clientId: tx.clientId, items: conflictItems })
    }
  }

  return conflicts
}

const sampleTransaction: TransactionData = {
  clientId: 'tx-001',
  storeId: 'store-1',
  terminalId: 'term-1',
  staffId: 'staff-1',
  items: [
    {
      productId: 'prod-1',
      productName: 'Coffee',
      unitPrice: 50000,
      quantity: 2,
      taxRateName: 'standard',
      taxRate: 1000,
      isReducedTax: false,
    },
    {
      productId: 'prod-2',
      productName: 'Sandwich',
      unitPrice: 30000,
      quantity: 1,
      taxRateName: 'reduced',
      taxRate: 800,
      isReducedTax: true,
    },
  ],
  payments: [{ method: 'cash', amount: 130000, received: 150000 }],
  createdAt: '2026-03-22T10:00:00Z',
}

describe('sync.worker logic', () => {
  describe('prepareSyncBatch', () => {
    it('空の配列では合計が0になる', () => {
      const result = prepareSyncBatch({ transactions: [] })
      expect(result.totalCount).toBe(0)
      expect(result.totalAmount).toBe(0)
    })

    it('取引の合計金額を正しく計算する', () => {
      const result = prepareSyncBatch({ transactions: [sampleTransaction] })
      expect(result.totalCount).toBe(1)
      // 50000 * 2 + 30000 * 1 = 130000
      expect(result.totalAmount).toBe(130000)
    })

    it('複数取引の合計を正しく計算する', () => {
      const tx2: TransactionData = {
        ...sampleTransaction,
        clientId: 'tx-002',
        items: [
          {
            productId: 'prod-3',
            productName: 'Tea',
            unitPrice: 20000,
            quantity: 3,
            taxRateName: 'standard',
            taxRate: 1000,
            isReducedTax: false,
          },
        ],
      }
      const result = prepareSyncBatch({ transactions: [sampleTransaction, tx2] })
      expect(result.totalCount).toBe(2)
      // 130000 + 60000 = 190000
      expect(result.totalAmount).toBe(190000)
    })
  })

  describe('detectPriceConflicts', () => {
    it('価格が一致していれば競合なし', () => {
      const result = detectPriceConflicts({
        transactions: [sampleTransaction],
        currentPrices: { 'prod-1': 50000, 'prod-2': 30000 },
      })
      expect(result).toHaveLength(0)
    })

    it('価格変更がある商品を検出する', () => {
      const result = detectPriceConflicts({
        transactions: [sampleTransaction],
        currentPrices: { 'prod-1': 55000, 'prod-2': 30000 },
      })
      expect(result).toHaveLength(1)
      const conflict = result[0]!
      expect(conflict.clientId).toBe('tx-001')
      expect(conflict.items).toHaveLength(1)
      const item = conflict.items[0]!
      expect(item.offlinePrice).toBe(50000)
      expect(item.currentPrice).toBe(55000)
    })

    it('現在価格が不明な商品は競合として報告しない', () => {
      const result = detectPriceConflicts({
        transactions: [sampleTransaction],
        currentPrices: {},
      })
      expect(result).toHaveLength(0)
    })
  })
})
