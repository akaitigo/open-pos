import Dexie, { type EntityTable } from 'dexie'

/**
 * オフラインキャッシュ用の商品データ。
 * 端末キャッシュからスナップショットとして保持する。
 */
export interface CachedProduct {
  id: string
  name: string
  barcode: string | null
  sku: string | null
  price: number
  categoryId: string | null
  taxRateName: string
  taxRate: string
  isReducedTax: boolean
  imageUrl: string | null
  displayOrder: number
  updatedAt: string
}

/**
 * オフライン取引の明細データ。
 */
export interface OfflineTransactionItem {
  productId: string
  productName: string
  unitPrice: number
  quantity: number
  taxRateName: string
  taxRate: string
  isReducedTax: boolean
}

/**
 * オフライン取引の支払データ。
 */
export interface OfflinePayment {
  method: string
  amount: number
  received?: number
  reference?: string
}

/**
 * オフライン取引データ。
 * オフライン中に端末ローカルに保存する取引の全情報。
 */
export interface OfflineTransaction {
  /** 自動インクリメント ID（IndexedDB 内部用） */
  localId?: number
  /** 冪等性キー（クライアント側で生成した UUID） */
  clientId: string
  storeId: string
  terminalId: string
  staffId: string
  items: OfflineTransactionItem[]
  payments: OfflinePayment[]
  /** 取引作成日時（ISO 8601 形式、端末ローカル時刻） */
  createdAt: string
  /** 同期ステータス */
  syncStatus: 'pending' | 'synced' | 'failed'
  /** 同期後のサーバー側取引ID */
  serverTransactionId?: string
  /** 同期エラーメッセージ */
  syncError?: string
}

/**
 * OpenPOS オフラインデータベース。
 * Dexie.js を使って IndexedDB にアクセスする。
 */
class OpenPosOfflineDB extends Dexie {
  products!: EntityTable<CachedProduct, 'id'>
  offlineTransactions!: EntityTable<OfflineTransaction, 'localId'>

  constructor() {
    super('openpos-offline')
    this.version(1).stores({
      products: 'id, barcode, sku, categoryId, updatedAt',
      offlineTransactions: '++localId, clientId, syncStatus, createdAt',
    })
  }
}

export const offlineDb = new OpenPosOfflineDB()

/**
 * 商品キャッシュを一括更新する。
 * API から取得した商品一覧をローカルDBに保存する。
 */
export async function cacheProducts(products: CachedProduct[]): Promise<void> {
  await offlineDb.products.bulkPut(products)
}

/**
 * キャッシュ済み商品を全て取得する。
 */
export async function getCachedProducts(): Promise<CachedProduct[]> {
  return offlineDb.products.toArray()
}

/**
 * バーコードでキャッシュ済み商品を検索する。
 */
export async function getCachedProductByBarcode(
  barcode: string,
): Promise<CachedProduct | undefined> {
  return offlineDb.products.where('barcode').equals(barcode).first()
}

/**
 * オフライン取引を保存する。
 */
export async function saveOfflineTransaction(
  tx: Omit<OfflineTransaction, 'localId'>,
): Promise<number> {
  const id = await offlineDb.offlineTransactions.add(tx as OfflineTransaction)
  return id ?? 0
}

/**
 * 未同期のオフライン取引を取得する。
 */
export async function getPendingTransactions(): Promise<OfflineTransaction[]> {
  return offlineDb.offlineTransactions.where('syncStatus').equals('pending').toArray()
}

/**
 * オフライン取引の同期ステータスを更新する。
 */
export async function updateTransactionSyncStatus(
  localId: number,
  status: 'synced' | 'failed',
  serverTransactionId?: string,
  syncError?: string,
): Promise<void> {
  await offlineDb.offlineTransactions.update(localId, {
    syncStatus: status,
    serverTransactionId,
    syncError,
  })
}

/**
 * 同期済みの取引をクリーンアップする（7日以上前のもの）。
 */
export async function cleanupSyncedTransactions(olderThanDays = 7): Promise<number> {
  const cutoff = new Date()
  cutoff.setDate(cutoff.getDate() - olderThanDays)
  const cutoffStr = cutoff.toISOString()
  const old = await offlineDb.offlineTransactions
    .where('syncStatus')
    .equals('synced')
    .filter((tx) => tx.createdAt < cutoffStr)
    .toArray()
  const ids = old.map((tx) => tx.localId).filter((id): id is number => id !== undefined)
  await offlineDb.offlineTransactions.bulkDelete(ids)
  return ids.length
}
