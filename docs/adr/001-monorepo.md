# ADR-001: モノリポ採用

## Status

Accepted（2024-01-01）

## Context

open-pos は複数の Kotlin/Quarkus マイクロサービスと React フロントエンドで構成される。
これらを別々のリポジトリで管理するか、単一リポジトリで管理するかを決定する必要があった。

開発チームは小規模（5名以下）であり、サービス間の型定義（proto）・共通ライブラリの共有が頻繁に発生する。

## Decision

**Gradle マルチプロジェクト + pnpm workspace によるモノリポ**を採用する。

```
open-pos/
├── build.gradle.kts        # ルートビルド
├── settings.gradle.kts
├── pnpm-workspace.yaml
├── proto/                  # 共有protobuf定義
├── services/
│   ├── api-gateway/
│   ├── pos-service/
│   ├── product-service/
│   ├── inventory-service/
│   ├── analytics-service/
│   └── store-service/
└── frontend/
    └── web/
```

## Rationale（採用理由）

- **proto定義の一元管理**: サービス間のgRPC定義変更が1コミットで完結
- **共通ライブラリ共有**: セキュリティユーティリティ・テストフィクスチャを重複なく共有
- **横断的なリファクタリング**: インターフェース変更の影響が全サービスで即座に確認できる
- **CI/CDの簡潔さ**: 変更のあったサービスのみビルド（Gradle incremental build）

## 代替案

| 案 | 棄却理由 |
|----|---------|
| ポリリポ（サービス別リポ） | proto変更のたびに複数リポで同期が必要。小規模チームで運用コストが高い |
| git submodule | 更新が煩雑。CI設定が複雑化する |

## Consequences

- **ポジティブ**: サービス間の変更が原子的にコミット可能。コードレビューが一か所で完結。
- **ネガティブ**: リポジトリサイズが増大。Gradle ビルドキャッシュ管理が必要。
- **対策**: `./gradlew :services:pos-service:test` のようにサービス単位でビルド可能にする。
