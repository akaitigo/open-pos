# ラベル分類体系

この Runbook では、`open-pos` で使用するラベルグループと、メンテナーがラベルを適用する方法を定義します。

## 目的

- Issue と Pull Request のトリアージを予測可能にする
- オーナーシップと影響範囲を一目で把握できるようにする
- リリースノートとロードマップの整合性を維持する

## ラベルグループ

### 作業種別

変更の主要な種別を示すために、`type:*` ラベルを一つ付与してください。

- `type:feature`
- `type:bug`
- `type:docs`
- `type:chore`
- `type:test`
- `type:epic`

これらのラベルはメンテナーが手動で管理します。ファイルパスだけで判断しないでください。

### 影響範囲

リポジトリのどの部分が影響を受けるかを示すために、`svc:*`、`app:*`、`pkg:*`、`infra`、または `proto` を使用してください。

| Label | 対象範囲 | PR での自動適用 |
|-------|---------|----------------|
| `svc:api-gateway` | `services/api-gateway/**` | あり |
| `svc:pos` | `services/pos-service/**` | あり |
| `svc:product` | `services/product-service/**` | あり |
| `svc:analytics` | `services/analytics-service/**` | あり |
| `svc:inventory` | `services/inventory-service/**` | あり |
| `svc:store` | `services/store-service/**` | あり |
| `app:pos-terminal` | `apps/pos-terminal/**` | あり |
| `app:admin` | `apps/admin-dashboard/**` | あり |
| `pkg:shared-types` | `packages/shared-types/**` | あり |
| `proto` | `proto/**` | あり |
| `infra` | CI, Compose, scripts, toolchain, ルートビルド設定 | あり |

自動化は [../../.github/labeler.yml](../../.github/labeler.yml) で定義され、[../../.github/workflows/labeler.yml](../../.github/workflows/labeler.yml) のワークフローで実行されます。

変更が横断的な場合は、一つに絞らず関連する影響範囲ラベルをすべて付与してください。

### プロダクト・技術関心事

変更がリポジトリ境界ではなくドメイン上の関心事に対応する場合、`area:*` ラベルを使用してください。

現在の `area:*` ラベル一覧:

- `area:auth`
- `area:payment`
- `area:receipt`
- `area:tax`
- `area:cart`
- `area:a11y`
- `area:sync`
- `area:i18n`
- `area:perf`
- `area:security`
- `area:monitoring`
- `area:dx`
- `area:offline`
- `area:settlement`
- `area:customer`
- `area:report`
- `area:loyalty`

これらはメンテナーが手動で付与し、ファイルパスだけでなくユーザー向けまたはアーキテクチャ上の関心事を反映すべきです。

### 優先度

優先度をトラッカー上で明示する必要がある場合、`P0:*` から `P3:*` を使用してください。

- `P0:critical`
- `P1:high`
- `P2:medium`
- `P3:low`

優先度ラベルは自動付与されません。

## メンテナー向けガイダンス

Pull Request の場合:

1. まず PR Labeler に明白な影響範囲ラベルを自動付与させる。
2. 最終的な差分に対して自動付与されたラベルが正しいか確認する。
3. 必要に応じて `type:*`、`area:*`、優先度ラベルを手動で追加・調整する。
4. マージ前に、横断的な変更やリシェイプされた PR では誤解を招くラベルを削除する。

Issue の場合:

- ラベルは常にメンテナーが手動で付与する
- 必要最小限のラベルセットを選択する
- ラベルを明確な Issue タイトルや説明の代替として使わない
