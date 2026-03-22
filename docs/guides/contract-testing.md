# Contract Testing Guide

## 概要

OpenPOS はマイクロサービス間の通信に gRPC (Protocol Buffers) を使用しており、サービス間の契約（API Contract）の整合性を保証することが重要です。本ドキュメントでは、現在の契約テスト戦略と今後の導入計画を説明します。

## 現在の契約テスト: buf breaking

OpenPOS では `buf breaking` を契約テストのコアツールとして使用しています。これは Protocol Buffers のスキーマレベルで後方互換性を検証するもので、以下の保証を提供します。

### 検証内容

| チェック項目 | 説明 |
|------------|------|
| フィールド番号の変更 | 既存フィールド番号の再利用を検出 |
| フィールド型の変更 | 互換性のない型変更を検出 |
| フィールドの削除 | `reserved` なしのフィールド削除を検出 |
| RPC シグネチャの変更 | リクエスト/レスポンス型の変更を検出 |
| メッセージの削除 | 使用中のメッセージ定義の削除を検出 |

### 設定

`proto/buf.yaml` で breaking ルールセットを定義しています:

```yaml
breaking:
  use:
    - FILE
  except:
    - FILE_SAME_JAVA_MULTIPLE_FILES
```

`FILE` カテゴリは最も厳密なルールセットで、ファイル単位での後方互換性を検証します。

### CI での実行

GitHub Actions の CI パイプラインで、全 PR に対して自動実行されます:

```bash
cd proto && buf breaking --against '../.git#branch=main,subdir=proto'
```

これにより、main ブランチとの差分で後方互換性違反がある場合、CI が失敗して PR のマージがブロックされます。

### ローカルでの実行

PR 作成前にローカルで検証する場合:

```bash
cd proto
buf breaking --against '../.git#branch=main,subdir=proto'
```

## buf lint による設計品質の保証

`buf lint` は契約の「正しさ」ではなく「設計品質」を保証します:

- 命名規約の統一（snake_case フィールド、PascalCase メッセージ）
- コメントの必須化
- パッケージ構造の一貫性

```bash
cd proto && buf lint
```

## 現状の評価

### 強み

1. **スキーマレベルの保証**: proto ファイルの変更は全て CI で検証される
2. **自動化済み**: 手動テスト不要、CI パイプラインに統合済み
3. **ゼロコスト**: buf は OSS ツールで追加コスト不要
4. **高速**: lint + breaking チェックは数秒で完了

### 制約

1. **ランタイムの振る舞いは検証しない**: フィールドの意味的な変更（例: 金額の単位変更）は検出できない
2. **サービス間の結合テストではない**: 実際の gRPC 通信は検証しない

## 今後の拡張計画

### Phase 1: 現状維持（v1.0）

v1.0 リリースでは `buf breaking` + `buf lint` で十分な契約保証を提供します。gRPC の型安全性と buf のスキーマ検証の組み合わせにより、REST API と比較して契約違反のリスクは大幅に低減されています。

### Phase 2: E2E gRPC テスト（v1.1 以降）

サービス間の実際の gRPC 通信を検証する E2E テストを追加:

- Docker Compose で全サービスを起動
- gRPC クライアントから実際のリクエストを送信
- レスポンスの構造と値を検証

### Phase 3: Consumer-Driven Contract Testing（検討中）

Pact や類似フレームワークの導入を検討:

- コンシューマー（フロントエンド、他サービス）が期待する契約を定義
- プロバイダー（各サービス）が契約を満たすことを検証
- gRPC 対応の Pact プラグインが成熟した段階で評価

## 関連ドキュメント

- [テスト戦略](testing.md)
- [gRPC エラーハンドリング](grpc-error-handling.md)
- [proto/buf.yaml](../../proto/buf.yaml) — buf 設定ファイル
