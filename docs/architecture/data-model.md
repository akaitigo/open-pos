# データモデル

> 金額フィールドは全て **BIGINT（銭単位）**。100円 = `10000`。
> 全テーブルに `organization_id UUID NOT NULL`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ` を持つ。

## store_schema

```mermaid
erDiagram
    organizations ||--o{ stores : "has"
    organizations ||--o{ terminals : "has"
    organizations ||--o{ staff : "has"
    stores ||--o{ terminals : "placed in"
    stores ||--o{ staff : "works at"

    organizations {
        UUID id PK
        VARCHAR name
        VARCHAR business_type "RETAIL / RESTAURANT / OTHER"
        VARCHAR invoice_number UK
        VARCHAR plan
        TIMESTAMPTZ deleted_at
    }
    stores {
        UUID id PK
        UUID organization_id FK
        VARCHAR code UK
        VARCHAR name
        JSONB settings
        BOOLEAN is_active
    }
    terminals {
        UUID id PK
        UUID organization_id FK
        UUID store_id FK
        VARCHAR terminal_code UK
        TIMESTAMPTZ last_sync_at
        BOOLEAN is_active
    }
    staff {
        UUID id PK
        UUID organization_id FK
        UUID store_id FK
        VARCHAR hydra_subject UK
        VARCHAR name
        VARCHAR role "OWNER / MANAGER / CASHIER"
        VARCHAR pin_hash
    }
    audit_logs {
        UUID id PK
        UUID organization_id
        UUID staff_id FK
        VARCHAR action
        VARCHAR entity_type
        VARCHAR entity_id
        JSONB details
        VARCHAR ip_address
        TIMESTAMPTZ created_at
    }
```

### organizations
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| name | VARCHAR(255) | NOT NULL |
| business_type | VARCHAR(20) | `RETAIL`/`RESTAURANT`/`OTHER` |
| invoice_number | VARCHAR(20) | UNIQUE |
| plan | VARCHAR(20) | NOT NULL |
| deleted_at | TIMESTAMPTZ | NULL |

### stores
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | FK → organizations |
| code | VARCHAR(20) | UNIQUE |
| name | VARCHAR(255) | NOT NULL |
| address | TEXT | |
| phone | VARCHAR(20) | |
| timezone | VARCHAR(50) | DEFAULT 'Asia/Tokyo' |
| settings | JSONB | |
| is_active | BOOLEAN | DEFAULT true |

### terminals
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | FK → organizations |
| store_id | UUID | FK → stores |
| terminal_code | VARCHAR(20) | UNIQUE |
| name | VARCHAR(100) | |
| last_sync_at | TIMESTAMPTZ | |
| is_active | BOOLEAN | DEFAULT true |

### staff
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | FK → organizations |
| store_id | UUID | FK → stores |
| hydra_subject | VARCHAR(255) | UNIQUE |
| name | VARCHAR(100) | NOT NULL |
| role | VARCHAR(20) | `OWNER`/`MANAGER`/`CASHIER` |
| pin_hash | VARCHAR(255) | |
| pin_failed_count | INT | DEFAULT 0 |
| pin_locked_until | TIMESTAMPTZ | |

### audit_logs
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| staff_id | UUID | |
| action | VARCHAR(50) | NOT NULL |
| entity_type | VARCHAR(50) | NOT NULL |
| entity_id | VARCHAR(255) | |
| details | JSONB | NOT NULL DEFAULT '{}' |
| ip_address | VARCHAR(45) | |
| created_at | TIMESTAMPTZ | NOT NULL |

## product_schema

```mermaid
erDiagram
    categories ||--o{ categories : "parent"
    categories ||--o{ products : "contains"
    tax_rates ||--o{ products : "applied to"
    discounts ||--o{ coupons : "linked to"

    categories {
        UUID id PK
        UUID organization_id
        UUID parent_id FK "NULL = root"
        VARCHAR name
        VARCHAR color
        INT display_order
    }
    tax_rates {
        UUID id PK
        UUID organization_id
        VARCHAR name
        DECIMAL rate
        VARCHAR tax_type "STANDARD / REDUCED"
        BOOLEAN is_active
    }
    products {
        UUID id PK
        UUID organization_id
        UUID category_id FK
        UUID tax_rate_id FK
        VARCHAR name
        VARCHAR barcode UK
        VARCHAR sku UK
        BIGINT price
        BOOLEAN is_active
    }
    discounts {
        UUID id PK
        UUID organization_id
        VARCHAR name
        VARCHAR discount_type "PERCENTAGE / FIXED_AMOUNT"
        BIGINT value
        VARCHAR applies_to "TRANSACTION / PRODUCT"
        BOOLEAN is_active
    }
    coupons {
        UUID id PK
        UUID organization_id
        UUID discount_id FK
        VARCHAR code UK
        INT max_uses
        INT used_count
    }
```

### categories
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| parent_id | UUID | FK → categories, NULL でルート |
| name | VARCHAR(100) | NOT NULL |
| color | VARCHAR(7) | |
| icon | VARCHAR(50) | |
| display_order | INT | DEFAULT 0 |

### tax_rates
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| name | VARCHAR(50) | NOT NULL |
| rate | DECIMAL(5,4) | NOT NULL |
| tax_type | VARCHAR(20) | `STANDARD`/`REDUCED` |
| is_active | BOOLEAN | DEFAULT true |

### products
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| category_id | UUID | FK → categories |
| tax_rate_id | UUID | FK → tax_rates |
| name | VARCHAR(255) | NOT NULL |
| barcode | VARCHAR(100) | UNIQUE per org |
| sku | VARCHAR(100) | UNIQUE per org |
| price | BIGINT | NOT NULL |
| image_url | TEXT | |
| display_order | INT | DEFAULT 0 |
| is_active | BOOLEAN | DEFAULT true |

### discounts
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| name | VARCHAR(100) | NOT NULL |
| discount_type | VARCHAR(20) | `PERCENTAGE`/`FIXED_AMOUNT` |
| value | BIGINT | NOT NULL |
| applies_to | VARCHAR(20) | `TRANSACTION`/`PRODUCT` |
| valid_from | TIMESTAMPTZ | |
| valid_until | TIMESTAMPTZ | |
| is_active | BOOLEAN | DEFAULT true |

### coupons
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| discount_id | UUID | FK → discounts |
| code | VARCHAR(50) | UNIQUE |
| max_uses | INT | NULL で無制限 |
| used_count | INT | DEFAULT 0 |
| valid_from | TIMESTAMPTZ | |
| valid_until | TIMESTAMPTZ | |

## pos_schema

```mermaid
erDiagram
    transactions ||--o{ transaction_items : "contains"
    transactions ||--o{ payments : "paid by"
    transactions ||--o{ tax_summaries : "tax breakdown"
    transactions ||--o{ transaction_discounts : "discounted by"
    transactions ||--o| receipts : "receipt"

    transactions {
        UUID id PK
        UUID organization_id
        UUID store_id
        UUID terminal_id
        UUID staff_id
        VARCHAR client_id UK "offline idempotency"
        VARCHAR transaction_number UK
        VARCHAR type "SALE / RETURN / VOID"
        VARCHAR status "DRAFT / COMPLETED / VOIDED"
        BIGINT subtotal
        BIGINT tax_total
        BIGINT discount_total
        BIGINT total
        BIGINT change_amount
        VARCHAR table_number
        UUID customer_id
        TIMESTAMPTZ completed_at
        BOOLEAN deleted "soft delete"
        VARCHAR content_hash "SHA-256"
        VARCHAR idempotency_key
        BIGINT version "optimistic lock"
    }
    transaction_items {
        UUID id PK
        UUID organization_id
        UUID transaction_id FK
        UUID product_id
        VARCHAR product_name "snapshot"
        BIGINT unit_price "snapshot"
        VARCHAR tax_rate_name "snapshot"
        VARCHAR tax_rate "snapshot"
        BOOLEAN is_reduced_tax
        INT quantity
        BIGINT subtotal
        BIGINT tax_amount
        BIGINT total
    }
    payments {
        UUID id PK
        UUID organization_id
        UUID transaction_id FK
        VARCHAR method "CASH / CREDIT_CARD / QR_CODE"
        BIGINT amount
        BIGINT received "cash only"
        BIGINT change "cash only"
        VARCHAR reference "gateway token etc."
        BOOLEAN deleted "soft delete"
    }
    tax_summaries {
        UUID id PK
        UUID organization_id
        UUID transaction_id FK
        VARCHAR tax_rate_name
        VARCHAR tax_rate
        BOOLEAN is_reduced
        BIGINT taxable_amount
        BIGINT tax_amount
    }
    transaction_discounts {
        UUID id PK
        UUID organization_id
        UUID transaction_id FK
        UUID discount_id FK
        VARCHAR name
        VARCHAR discount_type "PERCENTAGE / FIXED_AMOUNT"
        VARCHAR value
        BIGINT amount
        UUID transaction_item_id FK "NULL = transaction-level"
    }
    receipts {
        UUID id PK
        UUID transaction_id FK
        TEXT pdf_url
        TIMESTAMPTZ generated_at
    }
```

### transactions
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| store_id | UUID | NOT NULL |
| terminal_id | UUID | NOT NULL |
| staff_id | UUID | NOT NULL |
| transaction_number | VARCHAR(30) | NOT NULL |
| type | VARCHAR(20) | `SALE`/`RETURN`/`VOID` DEFAULT 'SALE' |
| status | VARCHAR(20) | `DRAFT`/`COMPLETED`/`VOIDED` DEFAULT 'DRAFT' |
| client_id | VARCHAR(36) | UNIQUE per org（オフライン冪等性） |
| subtotal | BIGINT | NOT NULL DEFAULT 0 |
| tax_total | BIGINT | NOT NULL DEFAULT 0 |
| discount_total | BIGINT | NOT NULL DEFAULT 0 |
| total | BIGINT | NOT NULL DEFAULT 0 |
| change_amount | BIGINT | NOT NULL DEFAULT 0 |
| table_number | VARCHAR(20) | |
| customer_id | UUID | |
| completed_at | TIMESTAMPTZ | |
| deleted | BOOLEAN | NOT NULL DEFAULT false（論理削除） |
| content_hash | VARCHAR(64) | SHA-256（電子帳簿保存法 真正性） |
| idempotency_key | VARCHAR(128) | UNIQUE（finalize 重複防止） |
| version | BIGINT | NOT NULL DEFAULT 0（楽観的ロック） |

### transaction_items
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| transaction_id | UUID | FK → transactions |
| product_id | UUID | |
| product_name | VARCHAR(255) | NOT NULL スナップショット |
| unit_price | BIGINT | NOT NULL スナップショット |
| quantity | INT | NOT NULL DEFAULT 1 |
| tax_rate_name | VARCHAR(50) | NOT NULL スナップショット |
| tax_rate | VARCHAR(10) | NOT NULL スナップショット |
| is_reduced_tax | BOOLEAN | NOT NULL DEFAULT false |
| subtotal | BIGINT | NOT NULL |
| tax_amount | BIGINT | NOT NULL |
| total | BIGINT | NOT NULL |

### payments
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| transaction_id | UUID | FK → transactions |
| method | VARCHAR(20) | `CASH`/`CREDIT_CARD`/`QR_CODE` |
| amount | BIGINT | NOT NULL |
| received | BIGINT | 現金のみ |
| change | BIGINT | 現金のみ |
| reference | VARCHAR(255) | 決済ゲートウェイトークン等 |
| deleted | BOOLEAN | NOT NULL DEFAULT false（論理削除） |

### tax_summaries
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| transaction_id | UUID | FK → transactions |
| tax_rate_name | VARCHAR(50) | NOT NULL |
| tax_rate | VARCHAR(10) | NOT NULL |
| is_reduced | BOOLEAN | NOT NULL DEFAULT false |
| taxable_amount | BIGINT | NOT NULL 課税対象金額 |
| tax_amount | BIGINT | NOT NULL 税額 |

### transaction_discounts
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| transaction_id | UUID | FK → transactions |
| discount_id | UUID | FK → discounts |
| name | VARCHAR(100) | NOT NULL |
| discount_type | VARCHAR(20) | `PERCENTAGE`/`FIXED_AMOUNT` |
| value | VARCHAR(50) | NOT NULL |
| amount | BIGINT | NOT NULL |
| transaction_item_id | UUID | NULL で取引全体への割引 |

### receipts
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| transaction_id | UUID | FK → transactions, UNIQUE |
| pdf_url | TEXT | Cloud Storage URL |
| generated_at | TIMESTAMPTZ | NOT NULL |

## inventory_schema

```mermaid
erDiagram
    stocks ||--o{ stock_movements : "tracked by"
    purchase_orders ||--o{ purchase_order_items : "contains"

    purchase_orders {
        UUID id PK
        UUID organization_id
        UUID store_id
        VARCHAR status "DRAFT / ORDERED / RECEIVED / CANCELLED"
        VARCHAR supplier_name
        TEXT note
        TIMESTAMPTZ ordered_at
        TIMESTAMPTZ received_at
    }
    purchase_order_items {
        UUID id PK
        UUID organization_id
        UUID purchase_order_id FK
        UUID product_id
        INT ordered_quantity
        INT received_quantity
        BIGINT unit_cost
    }
    stocks {
        UUID id PK
        UUID organization_id
        UUID store_id
        UUID product_id
        INT quantity
        INT alert_threshold
        BIGINT version "optimistic lock"
    }
    stock_movements {
        UUID id PK
        UUID organization_id
        UUID stock_id FK
        VARCHAR movement_type "SALE / RETURN / RECEIPT / ADJUSTMENT / TRANSFER"
        INT quantity_delta
        UUID reference_id "transaction ID etc."
    }
```

### stocks
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| store_id | UUID | NOT NULL |
| product_id | UUID | NOT NULL |
| quantity | INT | NOT NULL |
| alert_threshold | INT | DEFAULT 10 |
| version | BIGINT | 楽観的ロック |
| UNIQUE | (store_id, product_id) | |

### stock_movements
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| stock_id | UUID | FK → stocks |
| movement_type | VARCHAR(20) | `SALE`/`RETURN`/`RECEIPT`/`ADJUSTMENT`/`TRANSFER` |
| quantity_delta | INT | NOT NULL（負数で減少） |
| reference_id | UUID | 取引ID等 |
| note | TEXT | |

### purchase_orders
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| store_id | UUID | NOT NULL |
| status | VARCHAR(20) | `DRAFT`/`ORDERED`/`RECEIVED`/`CANCELLED` |
| supplier_name | VARCHAR(255) | NOT NULL |
| note | TEXT | |
| ordered_at | TIMESTAMPTZ | |
| received_at | TIMESTAMPTZ | |

### purchase_order_items
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| purchase_order_id | UUID | FK → purchase_orders |
| product_id | UUID | NOT NULL |
| ordered_quantity | INT | NOT NULL |
| received_quantity | INT | NOT NULL DEFAULT 0 |
| unit_cost | BIGINT | NOT NULL DEFAULT 0 |

## analytics_schema

```mermaid
erDiagram
    daily_sales {
        UUID id PK
        UUID organization_id
        UUID store_id
        DATE date
        BIGINT gross_amount
        BIGINT net_amount
        BIGINT tax_amount
        INT transaction_count
        BIGINT cash_amount
        BIGINT card_amount
        BIGINT qr_amount
    }
    product_sales {
        UUID id PK
        UUID organization_id
        UUID store_id
        UUID product_id
        DATE date
        INT quantity_sold
        BIGINT gross_amount
        BIGINT net_amount
    }
    hourly_sales {
        UUID id PK
        UUID organization_id
        UUID store_id
        DATE date
        SMALLINT hour "0-23"
        INT transaction_count
        BIGINT net_amount
    }
```

### daily_sales
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| store_id | UUID | NOT NULL |
| date | DATE | NOT NULL |
| gross_amount | BIGINT | DEFAULT 0 |
| discount_amount | BIGINT | DEFAULT 0 |
| net_amount | BIGINT | DEFAULT 0 |
| tax_amount | BIGINT | DEFAULT 0 |
| transaction_count | INT | DEFAULT 0 |
| cash_amount | BIGINT | DEFAULT 0 |
| card_amount | BIGINT | DEFAULT 0 |
| qr_amount | BIGINT | DEFAULT 0 |
| UNIQUE | (store_id, date) | |

### product_sales
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| store_id | UUID | NOT NULL |
| product_id | UUID | NOT NULL |
| date | DATE | NOT NULL |
| quantity_sold | INT | DEFAULT 0 |
| gross_amount | BIGINT | DEFAULT 0 |
| discount_amount | BIGINT | DEFAULT 0 |
| net_amount | BIGINT | DEFAULT 0 |
| UNIQUE | (store_id, product_id, date) | |

### hourly_sales
| カラム | 型 | 制約 |
|--------|-----|------|
| id | UUID | PK |
| organization_id | UUID | NOT NULL |
| store_id | UUID | NOT NULL |
| date | DATE | NOT NULL |
| hour | SMALLINT | 0-23 |
| transaction_count | INT | DEFAULT 0 |
| net_amount | BIGINT | DEFAULT 0 |
| UNIQUE | (store_id, date, hour) | |
