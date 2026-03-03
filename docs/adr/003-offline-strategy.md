# ADR-003: Dexie.js + Service Worker Background Sync によるオフライン対応

## Status

Accepted（2024-01-01）

## Context

小売店・飲食店ではWi-Fi断絶・モバイル回線不安定によるネットワーク障害が頻繁に発生する。
ネットワーク断絶時も POS 会計を継続できることは必須要件である。

フロントエンドは React/TypeScript の SPA（PWA）として実装されており、
ブラウザネイティブの技術でオフライン対応を実現する必要がある。

## Decision

**Dexie.js（IndexedDB ラッパー）+ Service Worker Background Sync** を採用する。

- **ローカルDB**: Dexie.js で IndexedDB を操作（商品・スタッフ等のマスタキャッシュ、取引キュー）
- **マスタ同期**: Periodic Background Sync（5分間隔）で差分同期
- **取引同期**: Background Sync API でオンライン復帰時に自動送信
- **冪等性**: `client_id`（UUID v4）をクライアントで生成、サーバーで重複排除

```
オフライン中: 取引 → IndexedDB(pending_transactions)
復帰時: Service Worker → POST /api/sync/transactions
サーバー: client_id で重複チェック → 未処理のみ保存
```

## Rationale（採用理由）

- **Dexie.js**: IndexedDB のバニラAPIは低レベルすぎる。Dexie は型安全・Promise ベース・クエリが直感的
- **Background Sync**: ページが閉じられていても同期が実行される。ユーザーが意識しなくてよい
- **IndexedDB**: Web Storage（localStorage）と異なり、大量データ・トランザクション・インデックスに対応

## 代替案

| 案 | 棄却理由 |
|----|---------|
| localStorage のみ | 容量制限5MB。構造化データ・インデックス非対応 |
| PouchDB + CouchDB | サーバー側に CouchDB が必要。既存 PostgreSQL と二重管理になる |
| オフライン時は操作不可 | 要件上 NG。ネットワーク障害時の会計継続が必須 |
| SQLite (WASM) | ブラウザサポートが限定的。バンドルサイズが大きい |

## Consequences

- **ポジティブ**: ネットワーク障害時もレジ操作が継続。自動同期でスタッフ負担なし。
- **ネガティブ**:
  - マスタデータとサーバーの最大5分の差異が発生しうる
  - Background Sync API はブラウザサポートが限定的（Chrome/Edge は対応、Firefox・Safari は部分対応）
  - コンフリクト解決ロジックのメンテナンスコスト
- **対策**:
  - Safari 向けに Periodic Background Sync の代替として Beacon API + フォアグラウンドポーリングを実装
  - コンフリクトルールは `docs/architecture/offline-strategy.md` で文書化・テストカバレッジ必須
