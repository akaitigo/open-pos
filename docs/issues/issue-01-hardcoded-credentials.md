# Docker Compose・サービス設定のハードコード認証情報を環境変数化する

**ラベル**: `type:security`, `P0:critical`

## 概要

ポートフォリオとしてリポジトリを public 化する前に、Docker Compose およびサービスの `application.properties` に直接記載されている認証情報を環境変数参照に変更する必要がある。

## 現状の問題

### 1. `infra/compose.yml` のハードコード認証情報

以下の箇所にパスワード・シークレットが直接記載されている：

- L11: `POSTGRES_PASSWORD: openpos_dev`
- L48: `RABBITMQ_DEFAULT_PASS: openpos_dev`
- L65, L81: `DSN: postgres://openpos:openpos_dev@postgres:5432/...`（Hydra）
- L82: `SECRETS_SYSTEM: openpos-hydra-secret-for-dev-only`（Hydra システムシークレット）
- L166-171, L203-208, L240-245, L277-282, L314-319: 各サービスの `DB_PASSWORD`, `RABBITMQ_PASS`

### 2. 全サービスの `application.properties`

以下5サービスの全てで同じパターン：

```properties
quarkus.datasource.password=${DB_PASSWORD:openpos_dev}   # デフォルト値にパスワード
rabbitmq-password=${RABBITMQ_PASS:openpos_dev}           # デフォルト値にパスワード
%dev.quarkus.datasource.password=openpos_dev              # dev プロファイルに直書き
```

該当サービス：
- `services/product-service/src/main/resources/application.properties`
- `services/store-service/src/main/resources/application.properties`
- `services/pos-service/src/main/resources/application.properties`
- `services/inventory-service/src/main/resources/application.properties`
- `services/analytics-service/src/main/resources/application.properties`

## 対応内容

### A. `.env.example` ファイルの作成

```env
# Database
DB_USER=openpos
DB_PASSWORD=
POSTGRES_PASSWORD=

# RabbitMQ
RABBITMQ_USER=openpos
RABBITMQ_PASS=

# Hydra
HYDRA_DSN=
HYDRA_SECRETS_SYSTEM=
```

### B. `infra/compose.yml` の修正

環境変数参照 `${VAR}` に置き換える。

### C. `application.properties` の修正

- デフォルト値からパスワードを除去: `${DB_PASSWORD:openpos_dev}` → `${DB_PASSWORD}`
- `%dev` プロファイルも環境変数参照に統一

### D. ドキュメント更新

- `docs/runbook/local-dev.md` に `.env` ファイルの設定手順を追加
- README に初回セットアップ手順として `.env` ファイルのコピーを記載

## 備考

- 現在のパスワード `openpos_dev` は開発用であり本番では使われないが、public リポジトリには記載しない方が望ましい
- 既に git 履歴にパスワードが含まれているため、公開前に `git filter-branch` や `BFG Repo-Cleaner` での履歴クリーンアップも検討すること
