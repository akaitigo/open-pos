# open-pos へのコントリビューション

open-pos への貢献に興味を持っていただきありがとうございます！このガイドでは開発の始め方を説明します。

## 開発環境セットアップ

### 前提条件

- Java 21（GraalVM CE 推奨）
- Node.js 22+ と pnpm
- Docker と `docker compose`
- buf CLI（Protocol Buffers 用）
- curl
- jq

### はじめに

```bash
# リポジトリをクローン
git clone https://github.com/akaitigo/open-pos.git
cd open-pos

mise install
make doctor

# フロントエンドの依存関係をインストール
pnpm install

# 推奨ローカル検証パス
make local-demo
pnpm dev:admin
pnpm dev:pos
```

### ローカル実行

```bash
# ホスト上でバックエンドを実行（日常開発に推奨）
make local-demo

# コンテナ化されたバックエンド（リリース検証に有用）
make docker-demo

# 使用中のバックエンドモードのみ再起動
make local-up-fast
make docker-up-core
```

## コントリビューション方法

### Issue の報告

- 重複を避けるため、[既存の Issue](https://github.com/akaitigo/open-pos/issues) を確認してください。
- 適切な Issue テンプレートを使用してください。
- 再現手順と環境の詳細を含めてください。

### 変更の提出

1. リポジトリを **フォーク** し、機能ブランチを作成:
   ```bash
   git checkout -b feature/<issue-number>-short-description
   ```

2. 以下のコーディング規約に従って **変更を実装**。

3. 提出前に **ローカルチェックを実行**:
   ```bash
   make doctor        # ツールの健全性チェック
   make verify        # 型チェック + lint + バックエンド/フロントエンド単体・機能テスト
   make local-smoke   # 任意: デモスタック実行中の場合、シード済み API スモークテスト
   pnpm e2e:install   # 一度きりの Playwright ブラウザインストール
   make verify-full   # 任意: Playwright を含む完全なリリーススタイル検証
   ```

4. 以下を含む **プルリクエストを作成**:
   - 明確なタイトルと説明
   - 説明に `Closes #<issue-number>` を記載
   - CI チェックが通過していること

### ブランチ命名

```
feature/123-add-product-search
fix/456-fix-price-calculation
```

## コーディング規約

### バックエンド（Kotlin / Quarkus）

- [Kotlin コーディング規約](https://kotlinlang.org/docs/coding-conventions.html) に従う
- `!!`（非null断言）禁止 — `?.let {}`、`?: throw`、または `requireNotNull()` を使用
- CDI Bean のデフォルトスコープに `@ApplicationScoped` を使用
- 全金額は **銭単位**（1/100 円）の `Long`/`BIGINT`
- 全テーブルにマルチテナント分離のための `organization_id` が必要

### フロントエンド（TypeScript / React）

- `any` 型禁止 — `unknown` + 型ガードまたはジェネリクスを使用
- 関数コンポーネント + hooks のみ（クラスコンポーネント禁止）
- `@shared-types/openpos` の共有型を使用
- `formatMoney()` で金額をフォーマット（shared-types）

### Protocol Buffers

- `.proto` ファイル編集後に `buf lint` と `buf format -w` を実行
- フィールド番号の再利用禁止 — 削除したフィールドは `reserved` でマーク
- 全 message、field、RPC にコメントを記述

### テスト

全機能にはテストが必要です:

| レベル | バックエンド | フロントエンド |
|-------|---------|----------|
| 単体テスト | JUnit 5 + `@InjectMock` | Vitest + RTL |
| 機能テスト | `@QuarkusTest` | Vitest + RTL |
| 結合テスト | `@QuarkusTest` + Testcontainers | — |
| E2E | Playwright | Playwright |

- AAA パターン（Arrange-Act-Assert）に従う
- 新規コードの行カバレッジ 80% 以上
- E2E テストは `data-testid` と Page Object Model を使用

## アーキテクチャ

### レイヤー依存関係（上から下のみ）

```
Proto -> Config/Filter -> Entity -> Repository -> Service -> gRPC Handler
```

- Entity は Service を参照してはならない
- gRPC Handler は Repository を直接呼び出してはならない
- サービス間の通信は RabbitMQ イベント経由のみ（直接のサービス間呼び出し禁止）

## 行動規範

参加前に [行動規範](CODE_OF_CONDUCT.md) を読んでください。

## サポート

セットアップや日常利用で困った場合は、[SUPPORT.md](SUPPORT.md) から始めて `make doctor` の出力を添えてください。バグを起票する前に、オープンエンドな質問には GitHub Discussions をご利用ください。

## ライセンス

コントリビューションにより、あなたの貢献が [MIT License](LICENSE) の下でライセンスされることに同意します。
