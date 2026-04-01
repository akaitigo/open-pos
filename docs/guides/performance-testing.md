# パフォーマンステスト計画

> **Last Updated**: 2026-04-01
> **関連ドキュメント**: [`docs/requirements/non-functional/performance.md`](../requirements/non-functional/performance.md)

## 目的

1. **SLO 達成確認** --- 各サービスのレイテンシ・スループット目標が本番相当の負荷下で満たされることを検証する
2. **ボトルネック特定** --- DB クエリ、キャッシュヒット率、コネクションプール、RabbitMQ キュー深度のどこに律速が存在するかを可視化する
3. **キャパシティプランニング** --- 水平スケール閾値とコスト見積もりの根拠データを収集する
4. **リグレッション防止** --- CI パイプラインに Smoke テストを組み込み、デプロイごとの性能劣化を早期検知する

---

## テスト対象エンドポイント

POS システムのクリティカルパスに焦点を当てる。

| # | サービス | RPC / エンドポイント | 重要度 | 備考 |
|---|---------|---------------------|--------|------|
| 1 | pos-service | `CreateTransaction` | **Critical** | 会計開始。レジ操作のエントリポイント |
| 2 | pos-service | `AddTransactionItem` | **Critical** | バーコードスキャンごとに呼び出し |
| 3 | pos-service | `FinalizeTransaction` | **Critical** | 支払処理・レシート発行。顧客待ち時間に直結 |
| 4 | pos-service | `GetReceipt` | High | レシート表示・印刷 |
| 5 | pos-service | `SyncOfflineTransactions` | High | オンライン復帰時の一括同期 |
| 6 | product-service | `GetProduct` / `ListProducts` | **Critical** | 商品マスタ参照。キャッシュ効果を検証 |
| 7 | product-service | `GetProductByBarcode` | **Critical** | バーコードスキャン検索。100ms 以内必須 |
| 8 | inventory-service | `GetStock` / `ListStocks` | High | 在庫確認。販売時の在庫チェック |
| 9 | inventory-service | `AdjustStock` | High | 取引確定後の在庫減算 |
| 10 | api-gateway | REST → gRPC プロキシ | **Critical** | 全リクエストの入口。50ms 以内 |

---

## SLO との対応

[`docs/requirements/non-functional/performance.md`](../requirements/non-functional/performance.md) で定義された SLO をテストで検証する。

| テスト対象 | SLO | k6 しきい値 |
|-----------|-----|------------|
| api-gateway (REST) | p95 < 50ms | `http_req_duration{service:gateway}: p(95) < 50` |
| pos-service `CreateTransaction` | p95 < 100ms | `grpc_req_duration{rpc:CreateTransaction}: p(95) < 100` |
| pos-service `FinalizeTransaction` | e2e < 1000ms | `pos_checkout_duration: p(95) < 1000` |
| product-service `GetProduct` | p95 < 50ms | `grpc_req_duration{rpc:GetProduct}: p(95) < 50` |
| product-service `GetProductByBarcode` | p95 < 100ms | `grpc_req_duration{rpc:GetProductByBarcode}: p(95) < 100` |
| inventory-service `GetStock` | p95 < 100ms | `grpc_req_duration{rpc:GetStock}: p(95) < 100` |
| 取引確定 e2e フロー | p95 < 1000ms | `iteration_duration: p(95) < 1000` |
| 10 端末同時アクセス | レスポンスタイム劣化 < 20% | ベースライン比較 |
| エラーレート | < 0.1% | `http_req_failed: rate < 0.001` |

---

## ツール選定: k6

### 選定理由

| 観点 | k6 | JMeter | Gatling |
|------|-----|--------|---------|
| スクリプト言語 | **JavaScript/TypeScript** | XML (GUI) | Scala |
| E2E チームとの共有 | Playwright チームが即座に読める | 学習コスト高 | 学習コスト中 |
| CI 統合 | CLI ベースで容易 | ヘッドレス実行に設定必要 | sbt/Maven 依存 |
| gRPC サポート | `k6/x/grpc` 拡張 | プラグイン | 標準 |
| リソース消費 | Go バイナリ、軽量 | JVM、メモリ大 | JVM |
| Grafana 連携 | **ネイティブ（同一エコシステム）** | InfluxDB 経由 | InfluxDB 経由 |
| ライセンス | AGPL-3.0 (OSS) | Apache 2.0 | Apache 2.0 |

**結論**: JavaScript ベースで E2E テストチーム（Playwright）とスキルを共有でき、Grafana とのネイティブ連携が可能な k6 を採用する。

### インストール

```bash
# macOS
brew install k6

# Linux (Debian/Ubuntu)
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D68
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6

# Docker
docker run --rm -i grafana/k6 run - < script.js
```

---

## テストシナリオ

### 1. Smoke Test（動作確認）

最小負荷でエンドポイントの正常動作を確認する。CI の毎回のデプロイで実行。

| パラメータ | 値 |
|-----------|-----|
| VU（仮想ユーザー） | 1 |
| 実行時間 | 10 秒 |
| 目的 | エンドポイントが応答すること、エラーが 0 であること |

```javascript
export const options = {
  vus: 1,
  duration: '10s',
  thresholds: {
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<200'],
  },
};
```

### 2. Load Test（通常負荷）

1 店舗のピーク想定（10 端末 x 5 同時操作）をシミュレート。

| パラメータ | 値 |
|-----------|-----|
| VU | 50 |
| 実行時間 | 5 分 |
| 目的 | SLO 達成確認、スループット 30 件/分の確認 |

```javascript
export const options = {
  vus: 50,
  duration: '5m',
  thresholds: {
    http_req_duration: ['p(95)<200'],
    http_req_failed: ['rate<0.001'],
    iteration_duration: ['p(95)<1000'],
  },
};
```

### 3. Stress Test（限界負荷）

段階的に負荷を上げ、システムの限界点とグレースフルデグレデーションを検証する。

| パラメータ | 値 |
|-----------|-----|
| VU | 0 → 100（ランプアップ） |
| ランプアップ | 2 分で 100 VU まで増加 |
| ピーク維持 | 5 分 |
| ランプダウン | 2 分で 0 VU まで減少 |
| 目的 | 限界点の特定、エラー発生パターンの把握 |

```javascript
export const options = {
  stages: [
    { duration: '2m', target: 100 },  // ランプアップ
    { duration: '5m', target: 100 },  // ピーク維持
    { duration: '2m', target: 0 },    // ランプダウン
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // ストレス時は緩和
    http_req_failed: ['rate<0.01'],    // エラー率 1% 以内
  },
};
```

### 4. Soak Test（長時間負荷）

長時間の安定稼働を確認。メモリリーク、コネクションリーク、GC 停止を検出する。

| パラメータ | 値 |
|-----------|-----|
| VU | 20 |
| 実行時間 | 30 分 |
| 目的 | メモリリーク検出、コネクションプール枯渇検知、長時間安定性 |

```javascript
export const options = {
  vus: 20,
  duration: '30m',
  thresholds: {
    http_req_duration: ['p(95)<200'],
    http_req_failed: ['rate<0.001'],
  },
};
```

---

## k6 スクリプト雛形: POS 会計フロー

POS レジの典型的な会計フローをシミュレートするスクリプト。

```javascript
// perf/scripts/pos-checkout-flow.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// --- カスタムメトリクス ---
const checkoutDuration = new Trend('pos_checkout_duration');
const checkoutErrorRate = new Rate('pos_checkout_errors');

// --- 設定 ---
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ORG_ID = __ENV.ORG_ID || '00000000-0000-0000-0000-000000000001';
const STORE_ID = __ENV.STORE_ID || '00000000-0000-0000-0000-000000000001';

// --- テストオプション ---
export const options = {
  scenarios: {
    pos_checkout: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 10 },   // ウォームアップ
        { duration: '5m', target: 50 },   // 通常負荷
        { duration: '2m', target: 100 },  // ピーク
        { duration: '1m', target: 0 },    // クールダウン
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<200'],
    pos_checkout_duration: ['p(95)<1000'],
    pos_checkout_errors: ['rate<0.001'],
    http_req_failed: ['rate<0.001'],
  },
};

const headers = {
  'Content-Type': 'application/json',
  'X-Organization-Id': ORG_ID,
};

// --- ヘルパー関数 ---
function createTransaction() {
  const payload = JSON.stringify({
    store_id: STORE_ID,
    terminal_id: `terminal-${__VU}`,
    cashier_id: `cashier-${__VU}`,
  });

  const res = http.post(`${BASE_URL}/api/v1/transactions`, payload, { headers });

  check(res, {
    'CreateTransaction: status 200': (r) => r.status === 200,
    'CreateTransaction: has transaction_id': (r) => {
      const body = r.json();
      return body !== null && body.transaction_id !== undefined;
    },
  });

  return res.json('transaction_id');
}

function addItems(transactionId, itemCount) {
  const barcodes = [
    '4901234567890', // 商品A
    '4901234567891', // 商品B
    '4901234567892', // 商品C
    '4901234567893', // 商品D
    '4901234567894', // 商品E
  ];

  for (let i = 0; i < itemCount; i++) {
    const barcode = barcodes[i % barcodes.length];

    // バーコードで商品検索
    const searchRes = http.get(
      `${BASE_URL}/api/v1/products/barcode/${barcode}`,
      { headers }
    );
    check(searchRes, {
      'GetProductByBarcode: status 200': (r) => r.status === 200,
      'GetProductByBarcode: p95 < 100ms': (r) => r.timings.duration < 100,
    });

    // 明細追加
    const payload = JSON.stringify({
      product_id: searchRes.json('product_id'),
      quantity: Math.floor(Math.random() * 3) + 1,
    });

    const addRes = http.post(
      `${BASE_URL}/api/v1/transactions/${transactionId}/items`,
      payload,
      { headers }
    );
    check(addRes, {
      'AddTransactionItem: status 200': (r) => r.status === 200,
    });
  }
}

function checkInventory(productId) {
  const res = http.get(
    `${BASE_URL}/api/v1/inventory/stocks?store_id=${STORE_ID}&product_id=${productId}`,
    { headers }
  );
  check(res, {
    'GetStock: status 200': (r) => r.status === 200,
    'GetStock: p95 < 100ms': (r) => r.timings.duration < 100,
  });
}

function finalizeTransaction(transactionId) {
  const payload = JSON.stringify({
    payment_method: 'CASH',
    amount_tendered: 1000000, // 10,000 円（銭単位）
  });

  const res = http.post(
    `${BASE_URL}/api/v1/transactions/${transactionId}/finalize`,
    payload,
    { headers }
  );

  check(res, {
    'FinalizeTransaction: status 200': (r) => r.status === 200,
    'FinalizeTransaction: has receipt_id': (r) => {
      const body = r.json();
      return body !== null && body.receipt_id !== undefined;
    },
  });

  return res;
}

function getReceipt(transactionId) {
  const res = http.get(
    `${BASE_URL}/api/v1/transactions/${transactionId}/receipt`,
    { headers }
  );
  check(res, {
    'GetReceipt: status 200': (r) => r.status === 200,
  });
}

// --- メインシナリオ ---
export default function () {
  const checkoutStart = Date.now();
  let hasError = false;

  // 1. 取引を作成
  const transactionId = createTransaction();
  if (!transactionId) {
    checkoutErrorRate.add(1);
    return;
  }

  // 2. 商品をスキャン（3-5 品）
  const itemCount = Math.floor(Math.random() * 3) + 3;
  addItems(transactionId, itemCount);

  // 3. 取引を確定
  const finalizeRes = finalizeTransaction(transactionId);
  if (finalizeRes.status !== 200) {
    hasError = true;
  }

  // 4. レシートを取得
  getReceipt(transactionId);

  // 5. メトリクス記録
  const checkoutTime = Date.now() - checkoutStart;
  checkoutDuration.add(checkoutTime);
  checkoutErrorRate.add(hasError ? 1 : 0);

  // 顧客の入れ替わりをシミュレート（1-3 秒）
  sleep(Math.random() * 2 + 1);
}
```

---

## 実行方法

### ローカル実行

```bash
# 1. インフラ起動（Docker Compose）
make up

# 2. バックエンドビルド & 起動
./gradlew build
# 各サービスを起動（別ターミナルまたは docker-demo モード）
make docker-demo

# 3. テストデータ投入（シードデータ）
# Flyway マイグレーション + シードが自動実行される

# 4. Smoke テスト実行
k6 run perf/scripts/pos-checkout-flow.js \
  --env BASE_URL=http://localhost:8080 \
  --env ORG_ID=00000000-0000-0000-0000-000000000001

# 5. Load テスト実行
k6 run perf/scripts/pos-checkout-flow.js \
  --vus 50 --duration 5m \
  --env BASE_URL=http://localhost:8080

# 6. Stress テスト実行（stages はスクリプト内で定義済み）
k6 run perf/scripts/pos-checkout-flow.js \
  --env BASE_URL=http://localhost:8080

# 7. 結果を JSON 出力
k6 run perf/scripts/pos-checkout-flow.js \
  --out json=perf/results/result.json \
  --env BASE_URL=http://localhost:8080

# 8. 結果を InfluxDB + Grafana に送信
k6 run perf/scripts/pos-checkout-flow.js \
  --out influxdb=http://localhost:8086/k6 \
  --env BASE_URL=http://localhost:8080
```

### CI 実行（GitHub Actions）

```yaml
# .github/workflows/perf-test.yml
name: Performance Test

on:
  # 手動トリガー
  workflow_dispatch:
    inputs:
      scenario:
        description: 'テストシナリオ'
        required: true
        default: 'smoke'
        type: choice
        options:
          - smoke
          - load
          - stress
          - soak
  # main マージ時に Smoke のみ自動実行
  push:
    branches: [main]

jobs:
  perf-test:
    runs-on: ubuntu-latest
    timeout-minutes: 60

    services:
      postgres:
        image: postgres:17
        env:
          POSTGRES_USER: openpos
          POSTGRES_PASSWORD: openpos
          POSTGRES_DB: openpos
        ports:
          - 5432:5432
      redis:
        image: redis:7
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v4

      - name: Setup k6
        uses: grafana/setup-k6-action@v1

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Build & Start services
        run: |
          ./gradlew build -x test
          # サービス起動（バックグラウンド）
          ./gradlew :services:api-gateway:quarkusDev &
          ./gradlew :services:pos-service:quarkusDev &
          ./gradlew :services:product-service:quarkusDev &
          ./gradlew :services:inventory-service:quarkusDev &
          # ヘルスチェック待機
          for i in $(seq 1 30); do
            curl -s http://localhost:8080/q/health/ready && break
            sleep 2
          done

      - name: Run smoke test
        if: github.event.inputs.scenario == 'smoke' || github.event_name == 'push'
        run: |
          k6 run perf/scripts/pos-checkout-flow.js \
            --vus 1 --duration 10s \
            --env BASE_URL=http://localhost:8080 \
            --out json=perf/results/smoke-result.json

      - name: Run load test
        if: github.event.inputs.scenario == 'load'
        run: |
          k6 run perf/scripts/pos-checkout-flow.js \
            --vus 50 --duration 5m \
            --env BASE_URL=http://localhost:8080 \
            --out json=perf/results/load-result.json

      - name: Run stress test
        if: github.event.inputs.scenario == 'stress'
        run: |
          k6 run perf/scripts/pos-checkout-flow.js \
            --env BASE_URL=http://localhost:8080 \
            --out json=perf/results/stress-result.json

      - name: Run soak test
        if: github.event.inputs.scenario == 'soak'
        run: |
          k6 run perf/scripts/pos-checkout-flow.js \
            --vus 20 --duration 30m \
            --env BASE_URL=http://localhost:8080 \
            --out json=perf/results/soak-result.json

      - name: Upload results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: perf-results-${{ github.event.inputs.scenario || 'smoke' }}
          path: perf/results/
```

---

## 結果の分析方法

### k6 出力の読み方

k6 実行後に表示されるサマリから、以下の指標を確認する。

```
█ POS Checkout Flow

  ✓ CreateTransaction: status 200
  ✓ FinalizeTransaction: status 200

  checks.........................: 100.00% ✓ 1200  ✗ 0
  http_req_duration..............: avg=45ms  min=12ms  med=38ms  max=890ms  p(90)=78ms  p(95)=95ms
  http_req_failed................: 0.00%   ✓ 0      ✗ 4800
  iteration_duration.............: avg=2.1s  min=1.2s  med=1.8s  max=5.2s   p(90)=3.1s  p(95)=3.8s
  pos_checkout_duration..........: avg=820ms min=450ms med=780ms max=2100ms p(90)=950ms p(95)=980ms
  pos_checkout_errors............: 0.00%   ✓ 0      ✗ 400
  vus............................: 50      min=1     max=50
  iterations.....................: 400     80/s
```

### 重点確認指標

| 指標 | 確認ポイント | アクション |
|------|-------------|-----------|
| `http_req_duration` p95 | SLO しきい値以内か | 超過 → ボトルネック調査 |
| `http_req_duration` p99 | テールレイテンシの異常 | p95 の 2 倍以上 → GC やコネクション枯渇を疑う |
| `http_req_failed` | エラー率 0.1% 以内か | 超過 → エラーログ・ステータスコード分析 |
| `pos_checkout_duration` p95 | 会計フロー全体が 1 秒以内か | 超過 → 個別 RPC のレイテンシ内訳を確認 |
| `iteration_duration` | VU あたりの 1 サイクル時間 | 急増 → スロットリング・キュー詰まり |
| `vus` | 想定 VU 数に達しているか | 未達 → リソース不足（CPU/メモリ） |

### JSON 結果の詳細分析

```bash
# p95, p99 をパーセンタイル別に集計
k6 run --out json=result.json perf/scripts/pos-checkout-flow.js

# jq でパーセンタイル抽出
cat perf/results/result.json | jq -r '
  select(.type == "Point" and .metric == "http_req_duration")
  | [.data.time, .data.value] | @csv
' > perf/results/latency.csv

# 時系列グラフ用にCSV出力してスプレッドシートやGrafanaで可視化
```

### 合否判定基準

| テスト種別 | 合格条件 |
|-----------|---------|
| Smoke | エラー率 0%、全エンドポイント応答 |
| Load | p95 < SLO 値、エラー率 < 0.1%、スループット >= 30 件/分 |
| Stress | p95 < SLO 値 x 2.5（緩和）、エラー率 < 1%、グレースフルデグレデーション確認 |
| Soak | p95 が時間経過で悪化しない（+10% 以内）、メモリ使用量が単調増加しない |

---

## Grafana ダッシュボードとの連携

### アーキテクチャ

```
k6 ──→ InfluxDB ──→ Grafana
         ↑
  アプリメトリクス
  (Micrometer/Prometheus)
```

### InfluxDB セットアップ

```yaml
# docker-compose.perf.yml（既存の docker-compose.yml に追加 or 別ファイル）
services:
  influxdb:
    image: influxdb:2.7
    ports:
      - "8086:8086"
    environment:
      DOCKER_INFLUXDB_INIT_MODE: setup
      DOCKER_INFLUXDB_INIT_USERNAME: admin
      DOCKER_INFLUXDB_INIT_PASSWORD: adminpassword
      DOCKER_INFLUXDB_INIT_ORG: openpos
      DOCKER_INFLUXDB_INIT_BUCKET: k6
    volumes:
      - influxdb-data:/var/lib/influxdb2

  grafana:
    image: grafana/grafana:11.0.0
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana-data:/var/lib/grafana
      - ./perf/grafana/provisioning:/etc/grafana/provisioning
      - ./perf/grafana/dashboards:/var/lib/grafana/dashboards

volumes:
  influxdb-data:
  grafana-data:
```

### k6 → InfluxDB 送信

```bash
# InfluxDB v2 への出力
k6 run perf/scripts/pos-checkout-flow.js \
  --out influxdb=http://localhost:8086/k6 \
  --env BASE_URL=http://localhost:8080

# 環境変数で認証情報を設定
export K6_INFLUXDB_ORGANIZATION=openpos
export K6_INFLUXDB_TOKEN=<your-influxdb-token>
```

### Grafana ダッシュボード構成

推奨パネル構成:

| パネル | 可視化 | データソース |
|--------|--------|-------------|
| リクエストレート | Time series | InfluxDB (k6) |
| レスポンスタイム (p50/p95/p99) | Time series | InfluxDB (k6) |
| エラーレート | Stat + Time series | InfluxDB (k6) |
| VU 数推移 | Time series | InfluxDB (k6) |
| POS 会計フロー所要時間 | Histogram | InfluxDB (k6) |
| RPC 別レイテンシ内訳 | Bar gauge | InfluxDB (k6) |
| CPU / メモリ使用率 | Time series | Prometheus (Micrometer) |
| DB コネクションプール | Time series | Prometheus (Micrometer) |
| Redis ヒット率 | Gauge | Prometheus (Micrometer) |
| RabbitMQ キュー深度 | Time series | Prometheus (Micrometer) |

### Grafana ダッシュボード JSON の配置

```
perf/
├── scripts/
│   └── pos-checkout-flow.js
├── results/                    # .gitignore 対象
│   └── .gitkeep
└── grafana/
    ├── provisioning/
    │   └── datasources/
    │       └── influxdb.yml
    └── dashboards/
        └── k6-openpos.json
```

### アプリケーションメトリクスとの相関分析

Quarkus サービスは Micrometer + Prometheus でメトリクスを公開している。k6 の負荷テスト中にこれらのメトリクスを同時に Grafana で監視することで、ボトルネックを特定する。

```bash
# 負荷テスト中に別ターミナルで確認
# Prometheus メトリクスエンドポイント
curl http://localhost:8080/q/metrics

# 確認すべき指標:
# - http_server_requests_seconds_bucket  (リクエストレイテンシ)
# - db_pool_active_connections           (DB コネクション)
# - jvm_memory_used_bytes                (JVM メモリ)
# - rabbitmq_consumed_total              (MQ 処理数)
```

---

## ディレクトリ構成

```
perf/
├── scripts/
│   ├── pos-checkout-flow.js     # POS 会計フロー（メイン）
│   ├── product-search.js        # 商品検索特化
│   ├── inventory-check.js       # 在庫確認特化
│   └── offline-sync.js          # オフライン同期
├── results/                     # テスト結果（.gitignore 対象）
│   └── .gitkeep
├── grafana/
│   ├── provisioning/
│   │   └── datasources/
│   │       └── influxdb.yml
│   └── dashboards/
│       └── k6-openpos.json
└── README.md                    # perf ディレクトリの説明
```

---

## 次のステップ

1. [ ] `perf/scripts/pos-checkout-flow.js` を実装し、ローカルで Smoke テスト実行
2. [ ] テストデータ（商品・店舗・在庫）のシードスクリプト整備
3. [ ] InfluxDB + Grafana の Docker Compose 追加
4. [ ] Grafana ダッシュボード JSON の作成
5. [ ] CI ワークフロー（`.github/workflows/perf-test.yml`）の追加
6. [ ] ステージング環境での Load / Stress テスト実行
7. [ ] SLO 未達項目のボトルネック調査と改善
