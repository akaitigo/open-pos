# オフライン戦略詳細

## ローカルDB設計（Dexie.js）

```typescript
class OpenPosDatabase extends Dexie {
  products\!: Table<Product>;
  categories\!: Table<Category>;
  taxRates\!: Table<TaxRate>;
  discounts\!: Table<Discount>;
  staff\!: Table<Staff>;
  storeSettings\!: Table<StoreSetting>;
  pendingTransactions\!: Table<PendingTransaction>;
  syncMetadata\!: Table<SyncMetadata>;

  constructor() {
    super('open-pos-db');
    this.version(1).stores({
      products:            'id, barcode, sku, categoryId, isActive',
      categories:          'id, parentId',
      taxRates:            'id, isActive',
      discounts:           'id, isActive',
      staff:               'id, storeId',
      storeSettings:       'storeId',
      pendingTransactions: 'clientId, createdAt, syncStatus',
      syncMetadata:        'entityType',
    });
  }
}
```

## 同期フロー図

### マスタ同期（5分間隔）

```
Service Worker (periodic sync)
  → GET /api/sync/master?since={lastSyncAt}&store_id={storeId}
  ← { products: [...], categories: [...], ..., sync_timestamp: "..." }
  → Dexie bulkPut（差分更新）
  → syncMetadata.lastSyncAt を更新
```

### 取引同期（オンライン復帰時）

```
ネットワーク復帰検知
  → Service Worker: sync イベント（tag: 'sync-transactions'）
  → Dexie: pendingTransactions where syncStatus = 'pending' を取得
  → POST /api/sync/transactions { transactions: [...] }
  ← { results: [ { clientId, status, serverId } ] }
  → 成功分: Dexie から削除
  → 失敗分: syncStatus = 'failed', retryCount++
  → retryCount >= 5: syncStatus = 'error'（手動確認要）
```

## コンフリクト解決ルール

| シナリオ | 解決策 | 理由 |
|---------|-------|------|
| オフライン中の価格変更 | 取引時のスナップショット価格を使用 | 顧客への価格提示済みのため |
| 在庫がマイナスになる | マイナスを許容し `stock.low` イベント発行 | 会計継続を優先 |
| クーポン利用上限超過 | サーバー側でエラー返却、スタッフに通知 | 不正防止のためサーバーチェック必須 |
| スタッフ削除（オフライン中） | 取引を受理し監査ログに記録 | 会計継続を優先 |
| 同一 clientId の重複送信 | サーバーが冪等に処理（2件目は無視） | Background Sync のリトライ対応 |

## ネットワーク状態検知

```typescript
class NetworkStatusService {
  private isOnline = navigator.onLine;

  async checkConnectivity(): Promise<boolean> {
    if (\!navigator.onLine) return false;
    try {
      await fetch('/api/health', { method: 'HEAD', cache: 'no-cache' });
      return true;
    } catch {
      return false;
    }
  }

  initialize() {
    window.addEventListener('online', () => this.handleOnline());
    window.addEventListener('offline', () => this.handleOffline());
    // 5分ごとにアクティブチェック
    setInterval(() => this.checkConnectivity(), 5 * 60 * 1000);
  }
}
```

- `navigator.onLine` は不確実（接続はあるがインターネット不通の場合あり）
- `/api/health` への fetch で実際の疎通確認
- オフライン時: UI バナー表示 + 未同期件数表示
- 復帰時: トースト通知 + 自動同期開始

## Service Worker 登録

```typescript
// main.tsx
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js');
}

// sw.js
self.addEventListener('sync', (event: SyncEvent) => {
  if (event.tag === 'sync-transactions') {
    event.waitUntil(syncPendingTransactions());
  }
});

self.addEventListener('periodicsync', (event: PeriodicSyncEvent) => {
  if (event.tag === 'sync-master') {
    event.waitUntil(syncMasterData());
  }
});
```

## ストレージ容量管理

- IndexedDB の推奨上限: 500MB（端末ストレージの50%）
- 古い完了済み取引ローカルコピーは30日で自動削除
- ストレージ使用量をUI表示（設定画面）
