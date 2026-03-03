# 商品管理 機能要件

## 商品CRUD

### 商品フィールド

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| organization_id | UUID | テナントID |
| name | VARCHAR(255) | 商品名 |
| barcode | VARCHAR(100) | バーコード（JAN/EAN） |
| sku | VARCHAR(100) | 在庫管理コード |
| price | BIGINT | 販売価格（銭単位） |
| tax_rate_id | UUID | 適用税率 |
| category_id | UUID | カテゴリ |
| image_url | TEXT | 商品画像URL |
| display_order | INT | 表示順 |
| is_active | BOOLEAN | 販売中フラグ |

### バーコードスキャン
- JAN13・EAN8・QRコード対応
- スキャン→商品検索（バーコード or SKU）
- 未登録バーコードは新規商品登録画面へ誘導

## カテゴリ管理

```
カテゴリ（階層構造）
├── parent_id NULL → ルートカテゴリ
└── parent_id 指定 → サブカテゴリ
```

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| parent_id | UUID | 親カテゴリ（NULLでルート） |
| name | VARCHAR(100) | カテゴリ名 |
| color | VARCHAR(7) | 表示色（`#RRGGBB`） |
| icon | VARCHAR(50) | アイコン識別子 |
| display_order | INT | 表示順 |

- 階層は最大3レベルまで
- カテゴリ削除時は商品の再カテゴリ化を要求（強制削除禁止）

## 税率管理

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| name | VARCHAR(50) | 税率名（例: 標準税率） |
| rate | DECIMAL(5,4) | 税率（例: 0.1000 = 10%） |
| tax_type | ENUM | `STANDARD` / `REDUCED` |
| is_active | BOOLEAN | 有効フラグ |

- 標準: 10%（`STANDARD`）、軽減: 8%（`REDUCED`）
- 税率変更は新レコード追加（既存レコード変更禁止、取引履歴保護）

## 割引定義

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| name | VARCHAR(100) | 割引名 |
| discount_type | ENUM | `PERCENTAGE` / `FIXED_AMOUNT` |
| value | BIGINT | 割引値（%の場合は1000=10%、金額の場合は銭単位） |
| applies_to | ENUM | `TRANSACTION` / `PRODUCT` |
| valid_from | TIMESTAMP | 有効開始日時 |
| valid_until | TIMESTAMP | 有効終了日時 |
| is_active | BOOLEAN | 有効フラグ |

## クーポン

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| code | VARCHAR(50) | クーポンコード（一意） |
| discount_id | UUID | 紐付く割引定義 |
| max_uses | INT | 最大利用回数（NULLで無制限） |
| used_count | INT | 利用済み回数 |
| valid_from | TIMESTAMP | 有効開始 |
| valid_until | TIMESTAMP | 有効終了 |

- コード入力またはスキャンで適用
- 利用回数上限チェック（楽観的ロック）

## 受け入れ条件

- [ ] バーコードスキャンで100ms以内に商品が検索される
- [ ] 同一バーコードの重複登録はバリデーションエラー
- [ ] 税率変更後も過去取引の税率が変わらない
- [ ] PERCENTAGE割引で端数切り捨てが正しく動作する
