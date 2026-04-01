# open-pos

> Universal Point of Sale System -- 汎用POSシステム

[![CI](https://github.com/akaitigo/open-pos/actions/workflows/ci.yml/badge.svg)](https://github.com/akaitigo/open-pos/actions/workflows/ci.yml)
[![Security](https://github.com/akaitigo/open-pos/actions/workflows/security.yml/badge.svg)](https://github.com/akaitigo/open-pos/actions/workflows/security.yml)
[![Release Drafter](https://github.com/akaitigo/open-pos/actions/workflows/release-drafter.yml/badge.svg)](https://github.com/akaitigo/open-pos/actions/workflows/release-drafter.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

マルチテナント・オフライン対応の汎用 POS（Point of Sale）システム。マイクロサービスアーキテクチャで構築。

> [!IMPORTANT]
> **このリポジトリは AI 駆動開発の実験プロジェクトです。**
> コミュニティサポート、外部コントリビューションの受付、メンテナンスの保証は行いません。

## プロジェクト状態

- **デモフロー**: `make local-demo` / `make docker-demo`
- **品質ゲート**: CI、依存関係監査、シークレットスキャン、CodeQL、Playwright E2E
- **認証**: ORY Hydra v2.2（OIDC/PKCE）+ RBAC（Owner / Manager / Cashier）

## 主な機能

- **マルチテナント**: Hibernate Filter による組織レベルのデータ分離
- **オフライン対応**: IndexedDB（Dexie.js）によるオフライン動作、オンライン復帰時に自動同期
- **マイクロサービス**: gRPC + RabbitMQ で連携する 6 つのバックエンドサービス
- **モダンフロントエンド**: React 19 + TypeScript + Tailwind CSS + shadcn/ui

## デモ

### POS 会計フロー

![POS checkout flow](docs/assets/demo/pos-checkout.gif)

### 管理ダッシュボード

![Admin dashboard](docs/assets/demo/admin-dashboard.png)

### 在庫管理

![Inventory management](docs/assets/demo/admin-inventory.png)

### POS 商品グリッド

![POS product grid](docs/assets/demo/pos-products.png)

デモアセットの再生成方法:

```bash
pnpm e2e:install
make local-demo
pnpm dev:admin
pnpm dev:pos
pnpm demo:assets
```

詳細は [docs/guides/demo-assets.md](docs/guides/demo-assets.md) を参照。

## アーキテクチャ

```
┌──────────────────────────────────────────────────────┐
│                    Clients                            │
│  ┌─────────────┐              ┌──────────────────┐   │
│  │ POS Terminal│              │ Admin Dashboard   │   │
│  │ (React PWA) │              │ (React SPA)       │   │
│  └──────┬──────┘              └────────┬─────────┘   │
└─────────┼──────────────────────────────┼─────────────┘
          │           REST               │
     ┌────▼─────────────────────────────▼────┐
     │           api-gateway (BFF)            │
     │        Quarkus REST + Auth             │
     └──┬────┬────┬────┬────┬────┬───────────┘
        │gRPC│    │    │    │    │
   ┌────▼┐ ┌─▼──┐│┌───▼┐┌──▼─┐┌▼────────┐
   │ pos ││prod-│││inv- ││stor││analytics │
   │ svc ││uct  │││ent- ││e   ││service   │
   └──┬──┘└──┬──┘│└──┬──┘└──┬─┘└────┬────┘
      │      │   │   │      │       │
      └──────┴───┴───┴──────┴───────┘
              │           │
     ┌────────▼──┐  ┌─────▼──────┐
     │ PostgreSQL│  │  RabbitMQ  │
     │  + Redis  │  │  (Events)  │
     └───────────┘  └────────────┘
```

| サービス | 技術 | 役割 |
| --- | --- | --- |
| api-gateway | Quarkus (REST) | BFF、認証、テナント注入 |
| pos-service | Quarkus (gRPC) | 取引、決済、レシート |
| product-service | Quarkus (gRPC) | 商品、カテゴリ、税率 |
| inventory-service | Quarkus (gRPC) | 在庫、入出庫管理 |
| analytics-service | Quarkus (gRPC) | 売上分析 |
| store-service | Quarkus (gRPC) | 店舗、スタッフ管理 |
| pos-terminal | React PWA | POS 端末（タブレット最適化） |
| admin-dashboard | React SPA | 管理画面（デスクトップ） |

## 技術スタック

| カテゴリ | 技術 |
| --- | --- |
| バックエンド | Kotlin 2.3 / Quarkus 3.34 / GraalVM CE 21 / Gradle 9.4 |
| フロントエンド | React 19 / TypeScript / Vite 7 / Tailwind CSS + shadcn/ui |
| データベース | PostgreSQL 17（スキーマ分離、Flyway マイグレーション） |
| キャッシュ | Redis 7（Lettuce、cache-aside パターン） |
| メッセージング | RabbitMQ 4（SmallRye Reactive Messaging） |
| 認証 | ORY Hydra v2.2（OIDC/PKCE） |
| API | gRPC（proto3 + buf ツールチェーン） |

## はじめに

### 前提条件

| ツール | バージョン | 用途 |
| --- | --- | --- |
| Java (GraalVM CE) | 21 | バックエンドのビルド・実行 |
| Node.js | 22+ | フロントエンドのビルド・実行 |
| pnpm | 10+ | フロントエンドパッケージ管理 |
| Docker & Docker Compose | v2+ | インフラ（PostgreSQL, Redis, RabbitMQ, Hydra）の起動 |
| buf CLI | 1.x | Protocol Buffers のリント・コード生成 |
| curl | 任意 | seed/smoke スクリプト |
| jq | 任意 | seed/smoke スクリプト |
| grpcurl | 任意（オプション） | `make grpc-test` で使用 |
| mise | 最新（推奨） | ツールバージョン管理 |

> [!TIP]
> [mise](https://mise.jdx.dev/) を使えば Java, Node.js, pnpm, buf を `.mise.toml` の定義通りに一括インストールできます。

### クイックスタート

```bash
# 1. リポジトリをクローン
git clone https://github.com/akaitigo/open-pos.git
cd open-pos

# 2. ツールのインストール（mise 利用時）
mise install

# 3. 前提条件の確認
make doctor

# 4. フロントエンド依存関係のインストール
pnpm install

# 5a. ローカルデモ起動（推奨: インフラは Docker、バックエンドはホスト実行）
make local-demo
pnpm dev:admin   # http://localhost:5174
pnpm dev:pos     # http://localhost:5173

# 5b. コンテナデモ起動（バックエンドも Docker で実行）
make docker-demo
pnpm dev:admin
pnpm dev:pos
```

`make local-demo` / `make docker-demo` は `apps/*/public/demo-config.json` を生成するため、ブラウザをリロードするだけで最新のシードデータ（組織・店舗・端末ID）を読み込めます。

シードされるデモデータは冪等です。固定の組織（`テスト株式会社`）、2店舗、各店舗2端末、owner/manager/cashier スタッフ、40商品、在庫、10件のサンプル取引が作成されます。

### 開発コマンド

```bash
# インフラ + 開発ツール起動
make up-dev
make logs         # Docker Compose ログ
make logs-pos     # pos-service ログ（動作中のモード）

# バックエンドを quarkusDev モードで起動（個別サービス開発向け）
make dev-product   # product-service
make dev-gateway   # api-gateway

# フロントエンド開発サーバー
pnpm dev:pos       # POS 端末 → http://localhost:5173
pnpm dev:admin     # 管理画面 → http://localhost:5174

# テスト
make test          # バックエンドテスト
make test-apps     # フロントエンド単体/機能テスト
make grpc-test     # gRPC ヘルスチェック（起動中のサービスに対して）
make verify        # typecheck + lint + バックエンド/フロントエンドテスト
pnpm e2e:install   # Playwright ブラウザの初回インストール
make verify-full   # verify + docker-demo + Playwright E2E

# リント
make lint          # Proto + Frontend

# データベースユーティリティ
make db-backup
make db-restore FILE=.local/backups/openpos-20260314-120000.sql
make reset         # PostgreSQL ボリューム再作成 + 再シード
```

`pnpm test` は `packages/` と `apps/` の単体/機能テストを実行します。E2E は `pnpm test:e2e` でオプトイン（Playwright ブラウザに依存しない）。

### ローカル開発モードの詳細

2つのローカル開発モードがあります。詳細は [docs/guides/local-development.md](docs/guides/local-development.md) を参照してください。

| モード | コマンド | 用途 |
| --- | --- | --- |
| `local-demo` | `make local-demo` | 日常開発（ホスト実行） |
| `docker-demo` | `make docker-demo` | リリース検証、CI 再現 |

モード切替時は現在のバックエンドを停止してから新しいモードを起動してください:

```bash
# local-demo → docker-demo に切り替え
make local-down
make docker-demo

# docker-demo → local-demo に切り替え
make docker-down-core
make local-demo
```

セットアップの詳細は [docs/guides/setup.md](docs/guides/setup.md) を参照。

## プロジェクト構成

```
open-pos/
├── proto/              # Protobuf 定義（buf workspace）
├── services/           # Quarkus マイクロサービス
│   ├── api-gateway/
│   ├── pos-service/
│   ├── product-service/
│   ├── inventory-service/
│   ├── analytics-service/
│   └── store-service/
├── apps/               # React フロントエンド
│   ├── pos-terminal/
│   └── admin-dashboard/
├── packages/           # 共有 TypeScript パッケージ
│   └── shared-types/
├── e2e/                # Playwright E2E テスト
├── infra/              # Docker Compose + 初期化スクリプト
└── docs/               # アーキテクチャ・設計・ガイド
```

## ドキュメント一覧

| カテゴリ | リンク |
| --- | --- |
| ドキュメント索引 | [docs/README.md](docs/README.md) |
| セットアップ | [docs/guides/setup.md](docs/guides/setup.md) |
| ローカル開発モード | [docs/guides/local-development.md](docs/guides/local-development.md) |
| ローカル開発 Runbook | [docs/runbook/local-dev.md](docs/runbook/local-dev.md) |
| アーキテクチャ概要 | [docs/architecture/system-overview.md](docs/architecture/system-overview.md) |
| API 設計 | [docs/architecture/api-design.md](docs/architecture/api-design.md) |
| データモデル | [docs/architecture/data-model.md](docs/architecture/data-model.md) |
| 要件概要 | [docs/requirements/overview.md](docs/requirements/overview.md) |
| ロードマップ | [docs/plans/roadmap.md](docs/plans/roadmap.md) |
| ADR | [docs/adr/001-monorepo.md](docs/adr/001-monorepo.md) |

## トラブルシューティング

### `make doctor` が失敗する

`make doctor` は前提ツールのバージョンを検証します。失敗した場合は出力を確認し、不足ツールをインストールしてください。

```bash
# mise を使う場合（推奨）
mise install

# 手動の場合
# Java: https://github.com/graalvm/graalvm-ce-builds/releases
# Node.js: https://nodejs.org/
# buf: https://buf.build/docs/installation
```

### Docker デーモンに接続できない

Docker Desktop（または Docker Engine）が起動していることを確認してください。

```bash
docker info
```

WSL2 環境では Docker Desktop の設定で「Use the WSL 2 based engine」が有効であることを確認してください。

### ポート競合

open-pos は非標準ポートを使用しますが、他のプロジェクトと競合する場合があります:

| サービス | ポート |
| --- | --- |
| PostgreSQL | 15432 |
| Redis | 16379 |
| RabbitMQ AMQP / UI | 15672 / 15673 |
| Hydra Public / Admin | 14444 / 14445 |
| api-gateway | 8080 |
| POS Terminal dev | 5173 |
| Admin Dashboard dev | 5174 |

競合するプロセスを停止してから `make local-demo` または `make docker-demo` を実行してください。

### バックエンドが起動しない

```bash
# ログ確認（ホスト実行モードの場合）
ls .local/logs/
cat .local/logs/pos-service.log

# 再ビルドして再起動
make local-down
make local-up
```

### シードデータが反映されない

```bash
# 再シード
make local-seed
make local-smoke

# ブラウザをリロード（dev server の再起動は不要）
```

### モード切替後に smoke テストが失敗する

同時に1つのバックエンドモードだけが動作するようにしてください:

```bash
make local-down
make docker-down-core
# その後、目的のモードを起動
```

### 完全リセット

何をしても解決しない場合:

```bash
make down
docker volume rm $(docker volume ls -q | grep open-pos) 2>/dev/null || true
make local-demo
```

## ライセンス

MIT License -- 詳細は [LICENSE](LICENSE) を参照。
