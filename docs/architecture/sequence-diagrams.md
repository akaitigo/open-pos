# シーケンス図

主要フローの Mermaid シーケンス図。

## POS 会計フロー

POS端末からの取引確定の一連の流れ。pos-service が取引を記録し、RabbitMQ 経由で inventory-service（在庫減算）と analytics-service（集計更新）に非同期通知する。

```mermaid
sequenceDiagram
    actor Cashier as レジ担当
    participant POS as POS Terminal<br/>(React PWA)
    participant GW as api-gateway
    participant PS as pos-service
    participant PD as product-service
    participant INV as inventory-service
    participant MQ as RabbitMQ
    participant AN as analytics-service

    Cashier->>POS: 商品スキャン / 数量入力
    POS->>GW: GET /api/products/{barcode}
    GW->>PD: gRPC GetProduct
    PD-->>GW: Product (price, tax_rate)
    GW-->>POS: 商品情報

    Cashier->>POS: 会計確定
    POS->>POS: client_id (UUID) 生成（冪等性キー）
    POS->>GW: POST /api/transactions
    GW->>PS: gRPC CreateTransaction

    PS->>PS: 税額計算 (銭単位 BIGINT)
    PS->>PS: Transaction + Items + Payment 保存
    PS->>PS: Receipt PDF 生成
    PS-->>GW: TransactionResponse
    GW-->>POS: 201 Created

    PS--)MQ: sale.completed

    par 在庫更新
        MQ--)INV: sale.completed
        INV->>INV: 冪等性チェック (event_id)
        INV->>INV: stock.quantity 減算 (楽観的ロック)
        opt quantity <= alert_threshold
            INV--)MQ: stock.low
        end
    and 売上集計
        MQ--)AN: sale.completed
        AN->>AN: 冪等性チェック (event_id)
        AN->>AN: daily_sales / product_sales / hourly_sales UPSERT
    end

    POS->>Cashier: レシート表示
```

## オフライン同期フロー

ネットワーク切断中は IndexedDB に取引を保存し、復帰時に Service Worker の Background Sync で自動送信する。`client_id` による冪等性でリトライが安全に行われる。

```mermaid
sequenceDiagram
    actor Cashier as レジ担当
    participant POS as POS Terminal<br/>(React PWA)
    participant IDB as IndexedDB<br/>(Dexie.js)
    participant SW as Service Worker
    participant GW as api-gateway
    participant PS as pos-service

    Note over POS: ネットワーク切断

    Cashier->>POS: 会計確定（オフライン）
    POS->>POS: client_id (UUID) 生成
    POS->>IDB: pendingTransactions に保存<br/>syncStatus = "pending"
    POS->>Cashier: レシート表示（ローカル生成）

    Note over POS: さらに取引が続く...
    Cashier->>POS: 会計確定（オフライン）
    POS->>IDB: pendingTransactions に保存

    Note over POS,GW: ネットワーク復帰

    POS->>POS: online イベント検知
    POS->>SW: sync 登録 (tag: "sync-transactions")

    SW->>IDB: syncStatus = "pending" を全件取得
    loop 未同期トランザクションごと
        SW->>GW: POST /api/sync/transactions
        GW->>PS: gRPC CreateTransaction
        PS->>PS: client_id で冪等性チェック
        alt 新規取引
            PS->>PS: Transaction 保存 + イベント発行
            PS-->>GW: 201 Created (serverId)
        else 重複 (client_id 既存)
            PS-->>GW: 200 OK (既存の serverId)
        end
        GW-->>SW: レスポンス
        alt 同期成功
            SW->>IDB: pendingTransaction 削除
        else 同期失敗
            SW->>IDB: syncStatus = "failed"<br/>retryCount++
            opt retryCount >= 5
                SW->>IDB: syncStatus = "error"
                SW->>POS: スタッフに手動確認を通知
            end
        end
    end

    par マスタデータ同期
        SW->>GW: GET /api/sync/master?since={lastSyncAt}
        GW-->>SW: 差分データ (products, categories, ...)
        SW->>IDB: bulkPut で差分更新
        SW->>IDB: syncMetadata.lastSyncAt 更新
    end
```

## PIN 認証フロー

POS端末のスタッフ認証。ORY Hydra の OAuth2/OIDC とは別に、端末上での簡易切り替えとして PIN コードを使用する。PIN ハッシュは store-service 側で検証し、成功時に JWT を発行する。

```mermaid
sequenceDiagram
    actor Staff as スタッフ
    participant POS as POS Terminal<br/>(React PWA)
    participant GW as api-gateway
    participant SS as store-service
    participant DB as PostgreSQL<br/>(store_schema)

    Staff->>POS: PIN 入力 (4-6桁)
    POS->>GW: POST /api/auth/pin<br/>{terminal_id, pin}
    GW->>SS: gRPC VerifyPin

    SS->>DB: SELECT staff WHERE store_id = terminal.store_id
    DB-->>SS: staff レコード

    alt pin_locked_until > NOW()
        SS-->>GW: FAILED_PRECONDITION<br/>"アカウントロック中"
        GW-->>POS: 423 Locked
        POS->>Staff: ロック中エラー表示
    else PIN 照合
        SS->>SS: bcrypt.verify(pin, pin_hash)
        alt PIN 一致
            SS->>DB: pin_failed_count = 0 に更新
            SS->>SS: JWT 生成<br/>(sub, role, store_id, terminal_id)
            SS-->>GW: VerifyPinResponse (token, staff)
            GW-->>POS: 200 OK {token, staff}
            POS->>POS: JWT をメモリ保持
            POS->>Staff: ログイン成功 / レジ画面表示
        else PIN 不一致
            SS->>DB: pin_failed_count++ 更新
            opt pin_failed_count >= 5
                SS->>DB: pin_locked_until = NOW() + 15min
            end
            SS-->>GW: UNAUTHENTICATED<br/>"PIN が正しくありません"
            GW-->>POS: 401 Unauthorized
            POS->>Staff: 認証失敗<br/>残り試行回数表示
        end
    end
```
