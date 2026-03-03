# 店舗管理 機能要件

## 組織（organizations）

テナントの最上位エンティティ。

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| name | VARCHAR(255) | 組織名 |
| business_type | ENUM | `RETAIL` / `RESTAURANT` / `OTHER` |
| invoice_number | VARCHAR(20) | 適格請求書発行事業者登録番号（T+13桁） |
| plan | ENUM | `FREE` / `STANDARD` / `ENTERPRISE` |
| created_at | TIMESTAMP | 登録日時 |
| updated_at | TIMESTAMP | 更新日時 |

- `invoice_number` はレシートに印字（インボイス対応）
- 組織削除は論理削除（`deleted_at`）

## 店舗（stores）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| organization_id | UUID | 所属組織 |
| code | VARCHAR(20) | 店舗コード（取引番号採番に使用） |
| name | VARCHAR(255) | 店舗名 |
| address | TEXT | 住所 |
| phone | VARCHAR(20) | 電話番号 |
| timezone | VARCHAR(50) | タイムゾーン（例: Asia/Tokyo） |
| settings | JSONB | 店舗設定（税計算方式・レシート設定等） |
| is_active | BOOLEAN | 営業中フラグ |

### settings JSONB スキーマ例
```json
{
  "tax_calculation": "inclusive",
  "receipt_footer": "ありがとうございました",
  "low_stock_notification": true
}
```

## 端末（terminals）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| organization_id | UUID | テナントID |
| store_id | UUID | 所属店舗 |
| terminal_code | VARCHAR(20) | 端末コード |
| name | VARCHAR(100) | 端末名 |
| last_sync_at | TIMESTAMP | 最終同期日時 |
| is_active | BOOLEAN | 有効フラグ |

- 端末ごとに認証トークンを発行（店舗スタッフが端末をアクティベート）
- `last_sync_at` でオフライン検知（5分以上未同期でアラート）

## スタッフ（staff）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| organization_id | UUID | テナントID |
| store_id | UUID | 所属店舗 |
| hydra_subject | VARCHAR(255) | ORY Hydra のサブジェクト（外部ID） |
| name | VARCHAR(100) | 表示名 |
| role | ENUM | `OWNER` / `MANAGER` / `CASHIER` |
| pin_hash | VARCHAR(255) | PINハッシュ（bcrypt, cost=12） |
| pin_failed_count | INT | 連続失敗回数 |
| pin_locked_until | TIMESTAMP | ロック解除日時 |

### PINロックポリシー
- 5回連続失敗でロック
- ロック時間: 30分
- MANAGER以上がロック解除可能

### ロール権限マトリクス

| 操作 | CASHIER | MANAGER | OWNER |
|------|---------|---------|-------|
| POS会計 | ○ | ○ | ○ |
| 商品管理 | ✗ | ○ | ○ |
| 在庫管理 | ✗ | ○ | ○ |
| スタッフ管理 | ✗ | ✗ | ○ |
| 売上分析 | ✗ | ○ | ○ |
| 組織設定 | ✗ | ✗ | ○ |

## 受け入れ条件

- [ ] 異なる組織のスタッフは別組織のデータにアクセスできない
- [ ] PINを5回失敗するとアカウントがロックされる
- [ ] 端末コードはURL/QRでアクティベート可能
- [ ] `timezone` に基づき日次集計の区切り時刻が変わる
