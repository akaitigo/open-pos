# 開発環境セットアップ

## 前提条件

| ツール | バージョン | インストール方法 |
|--------|-----------|----------------|
| Java (GraalVM CE) | 21 | `mise install java@graalce-21` |
| Node.js | 22+ | `mise install node@22` |
| pnpm | 10+ | `npm install -g pnpm` |
| Docker | Docker Engine / Desktop | [公式サイト](https://docs.docker.com/engine/install/) |
| Docker Compose | v2+ | Docker に同梱 |
| buf | 1.x | `mise install buf` or [公式サイト](https://buf.build/docs/installation) |
| curl | 任意の現行版 | OS 標準 or パッケージマネージャ |
| jq | 任意の現行版 | `apt install jq` など |
| gh | 最新 (任意) | `apt install gh` |

## セットアップ手順

### 1. リポジトリクローン
```bash
gh repo clone akaitigo/open-pos
cd open-pos
```

### 2. ツール自動導入（mise を使う場合）
```bash
mise install
```

### 3. 前提確認
```bash
make doctor
```

### 4. 依存関係インストール
```bash
pnpm install
```

### 5. 推奨ローカル導線
```bash
make local-demo
pnpm dev:admin   # http://localhost:5174
pnpm dev:pos     # http://localhost:5173
```

`make local-demo` は以下をまとめて行います。
- Docker で infra を起動
- supported local backend (`product-service`, `store-service`, `pos-service`, `inventory-service`, `api-gateway`) を host で build / 起動
- demo data を投入
- frontend 用の `.env.development.local` を生成
- frontend 用の `public/demo-config.json` を生成
- API smoke test を実行

seed される demo data は冪等で、固定の組織・2店舗・各店舗2端末・staff・40商品・在庫・サンプル取引を再実行でも重複なく揃えます。

### 6. container 導線
```bash
make docker-demo
pnpm dev:admin
pnpm dev:pos
```

こちらは supported local backend も Docker で起動します。`make local-demo` から切り替える時は `make local-down` を実行してください。

### 7. 個別コマンド
```bash
# infra のみ起動
make up

# host-run backend の再起動
make local-up-fast
make local-down

# container backend の起動 / 停止
make docker-up-core
make docker-down-core

# demo data の再投入と API smoke
make local-seed
make local-smoke

# ローカル品質ゲート
make verify
```

## 動作確認

```bash
# Compose 状態
docker compose -f infra/compose.yml ps

# API health
curl -s http://localhost:8080/api/health
```

`apps/admin-dashboard/.env.development.local` と `apps/pos-terminal/.env.development.local` は seed 時に自動生成されます。`apps/*/public/demo-config.json` も同時に生成されるので、frontend は dev server 再起動なしで browser reload だけで最新 seed を拾えます。

## 開発ツール付き起動

```bash
make up-dev
```

追加で pgAdmin (http://localhost:15080) と Redis Commander (http://localhost:18081) が起動します。
