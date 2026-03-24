# Database Migration ガイド

## 概要

各マイクロサービスは Flyway を使用してスキーマ分離された PostgreSQL データベースを管理する。
マイグレーションファイルは各サービスの `src/main/resources/db/migration/` に配置する。

## 命名規則

```
V{version}__{description}.sql
```

- **version**: 連続した整数（V1, V2, V3, ...）。サブバージョン（V5_1 等）は**禁止**
- **description**: スネークケースで変更内容を簡潔に記述
- ダブルアンダースコア `__` がバージョンと説明の区切り

### 良い例

```
V1__create_pos_tables.sql
V2__add_optimistic_lock_version.sql
V3__create_drawer_settlement_tables.sql
```

### 悪い例

```
V5_1__add_missing_fk_indexes.sql     # サブバージョン禁止
V5_2__add_soft_delete.sql            # サブバージョン禁止
```

## スキーマ一覧

| サービス | スキーマ名 | 現在のバージョン |
|---------|-----------|---------------|
| pos-service | `pos_schema` | V18 |
| product-service | `product_schema` | V9 |
| store-service | `store_schema` | V9 |
| inventory-service | `inventory_schema` | V8 |
| analytics-service | `analytics_schema` | V3 |

## マイグレーション作成ルール

1. **バージョン番号は連続させる**: 次のバージョンは現在の最大値 + 1
2. **1ファイル1目的**: 複数の無関係な変更を1ファイルにまとめない
3. **冪等性を意識**: `IF NOT EXISTS`, `IF EXISTS` を活用
4. **ロールバック不可を前提に設計**: Flyway Community はロールバック非対応
5. **金額は BIGINT**: 銭単位（10000 = 100円）、DECIMAL/FLOAT 禁止
6. **全テーブルに organization_id**: マルチテナント必須カラム
7. **FK インデックス**: 外部キーカラムには必ずインデックスを作成

## マイグレーション追加手順

```bash
# 1. 次のバージョン番号を確認
ls services/{service-name}/src/main/resources/db/migration/

# 2. ファイル作成（例: pos-service V19）
touch services/pos-service/src/main/resources/db/migration/V19__add_new_feature.sql

# 3. SQL を記述

# 4. テスト実行
./gradlew :services:pos-service:test

# 5. Testcontainers 結合テストで Flyway が正常に適用されることを確認
./gradlew :services:pos-service:test --tests "*IntegrationTest*"
```

## V5 系整理の記録（pos-service）

### 問題

pos-service で V5, V5_1, V5_2, V5_3, V5_4 のサブバージョンが混在していた。
Flyway はサブバージョンを許容するが、可読性とメンテナンス性が低下する。

### 対応（2026-03-24）

サブバージョンを連続番号にリナンバリング:

| 旧バージョン | 新バージョン | 内容 |
|------------|------------|------|
| V5 | V5 | change_amount カラム追加 |
| V5_1 | V6 | FK インデックス追加 |
| V5_2 | V7 | ソフトデリート追加 |
| V5_3 | V8 | outbox_events テーブル作成 |
| V5_4 | V9 | Phase 9 POS 機能 |
| V6 | V10 | 取引金額検索インデックス |
| V7 | V11 | 予約テーブル・注文 |
| V8 | V12 | トランザクションパーティショニング |
| V9 | V13 | outbox_events に organization_id |
| V10 | V14 | 取引番号ユニーク制約 |
| V11 | V15 | アーカイブテーブル修正 |
| V12 | V16 | 取引明細ユニーク制約 |
| V13 | V17 | content_hash 追加 |
| V14 | V18 | 会計仕訳テーブル作成 |

### 既存環境での対応

v1.0 リリース前のため、既存のステージング/開発環境では DB を再作成するか `flyway repair` を実行:

```bash
# Flyway repair（チェックサム再計算）
flyway -url=jdbc:postgresql://localhost:15432/openpos \
       -schemas=pos_schema \
       -user=openpos -password=openpos \
       repair
```
