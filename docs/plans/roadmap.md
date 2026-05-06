# open-pos Product Roadmap

> **Last Updated**: 2026-05-07
> **Status**: Active
> **Owner**: Repository maintainer

## Overview

open-pos は、Kotlin/Quarkus のマイクロサービスと React フロントエンドで構成された、マルチテナント・オフライン対応の POS モノリポです。

2026-03 時点の phase-based 計画は、現在の実装速度と実際の到達点に対して stale になりました。現時点では、固定日付のフェーズよりも「いま何を出荷品質まで持っていくか」を軸に運用した方が正確です。

このファイルは、現在の delivery stream と exit criteria を示します。個別の実装タスクは GitHub issue / PR を source of truth とします。

## Current Position

### Delivered Baseline

- 6 backend services + 2 frontend apps が同一モノリポで動作
- `make local-demo` / `make docker-demo` によるローカル再現可能なデモフロー
- ORY Hydra を使った OIDC/PKCE 認証、RBAC、マルチテナント境界
- POS / inventory / analytics / admin dashboard の主要フロー
- CI, CodeQL, dependency audit, secret scanning, Playwright E2E を含む品質ゲート

### Current Posture

- リポジトリは「production-ready SaaS」ではなく、「self-hostable beta / release-candidate baseline」として扱う
- 実装面の中核機能は揃っている
- 残課題は、運用整備、公開ドキュメント整合、最終的な release verification に集中している

## Active Delivery Streams

| Stream | Status | Why it matters | Exit criteria |
| --- | --- | --- | --- |
| Release & Repository Hygiene | In progress | 公開リポジトリとしての説明責任を満たす | README / roadmap / runbook が現行実装と一致する |
| Security & Operational Readiness | In progress | 実装済みでも運用体制が曖昧だと本番運用できない | 秘密情報運用、インシデント責任分界、環境変数/デプロイ文書が揃う |
| Business Correctness & Resilience | In progress | POS としての信頼性を担保する | 重要フローの再検証、障害時のグレースフルデグレーション確認 |
| Maintainability & Hotspot Reduction | Planned | 実装速度と安全な変更容易性を維持する | 巨大ファイル/サービス境界の分割方針を実装に落とす |

## Stream Details

### 1. Release & Repository Hygiene

**Goal**: 公開ドキュメントと実際の挙動のズレをなくし、初見の開発者や利用者が誤解しない状態にする。

**Current work**:

- README のプロジェクト表現を「managed production SaaS」相当から現実的な表現へ補正
- 古い phase roadmap を current-state ベースへ更新
- stale な release planning document を release readiness ベースへ更新
- incident runbook から placeholder を除去し、現行サポートモデルに合わせる

**Exit criteria**:

- README の status / stack / supported workflows が実装と一致する
- roadmap / v1 release doc が stale issue list ではなく current delivery stream を説明する
- runbook に空欄や TODO placeholder が残らない

### 2. Security & Operational Readiness

**Goal**: 実装済み機能を「安全に運用できるか」で評価し、残る本番ブロッカーを明確にする。

**Focus**:

- 秘密情報ローテーションと履歴スキャン
- デプロイ先ごとの責任分界の明文化
- インシデント時の連絡経路と復旧コマンドの整備
- dependency maintenance を継続可能な運用に維持

**Exit criteria**:

- secret handling policy が文書化され、履歴スキャン手順がある
- incident ownership が maintainer-owned か deployer-owned か明記されている
- setup / release / incident docs が同じ前提を共有している

### 3. Business Correctness & Resilience

**Goal**: POS としての重要フローが、平常時だけでなく障害時にも期待どおりかを詰める。

**Focus**:

- offline transaction / sync conflict の再検証
- oversell, idempotency, timeout, analytics consistency の回帰確認
- Redis / RabbitMQ 障害時の degradation path を明確化

**Exit criteria**:

- 主要な取引フローに対する回帰確認が文書化されている
- 障害時に「止まる機能」と「継続できる機能」が整理されている

### 4. Maintainability & Hotspot Reduction

**Goal**: 大きすぎるファイルや責務過多の層を分割し、依存更新や機能追加のコストを下げる。

**Current hotspots**:

- `services/pos-service` の gRPC / transaction service 層
- `apps/pos-terminal` の商品一覧/カート周辺
- frontend 認証・API クライアントの重複

**Exit criteria**:

- 分割対象ごとに write scope が明確な実装タスクへ落とし込まれている
- 次の変更が大型ファイルをさらに肥大化させない

## Non-Goals For This Roadmap

- 古い日付ベース phase 計画の維持
- issue 番号を網羅した static backlog の複製
- 実装済みの項目を「未着手」のまま残すこと

## Source Of Truth

- 実装の正否: code + CI + local verification
- 個別作業: GitHub issues / pull requests
- この文書: 現在の delivery priority と exit criteria
