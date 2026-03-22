# open-pos v1.0.0 ロードマップ

> **マイルストーン**: [v1.0.0](https://github.com/akaitigo/open-pos/milestone/18)
> **全65件のイシューを全てクローズすれば v1.0 マイルストーン達成**
> **作成**: 2026-03-16 / Claude + Gemini + Codex による包括レビュー

## 実行フェーズと依存関係

### Phase 0: ブロッカー解除（他の全作業の前提）
> **目的**: CI green化とリポジトリの衛生確保。これが完了しないと他のフェーズが検証不能。

| # | 優先度 | タイトル | 依存 | PR状態 |
|---|--------|---------|------|--------|
| **#350** | P0 | security: dev-private-key.pem の Git 追跡削除とローテーション | なし | 未着手 |
| **#358** | P0 | verify: git 全履歴の秘密情報スキャン | #350 | 未着手 |
| **#315** | P1 | fix(infra): Flyway 版番号衝突 + RabbitMQ ポート | なし | PR #316 |
| **#311** | P1 | fix(e2e): Docker ビルド依存解決 | なし | PR #313 |
| **#351** | P1 | chore: JVM クラッシュログ除去と .gitignore 追加 | なし | 未着手 |
| **#331** | P1 | fix(infra): CI build cache 無効化の解除 | なし | 未着手 |

**マージ順序**: #315 → #311 → #350 → #351 → #331 → #358

---

### Phase 1: セキュリティ基盤（公開前必須）
> **目的**: マルチテナント分離と認証フローの堅牢化。公開リポジトリとして最低限の安全性。

| # | 優先度 | タイトル | 依存 |
|---|--------|---------|------|
| **#329** | P0 | fix(backend): analytics-service のテナントフィルタ適用漏れ | Phase 0 |
| **#328** | P0 | security: PKCE state パラメータ検証の欠落（CSRF） | なし |
| **#322** | P0 | security: マルチテナント RLS の網羅性検証 | #329 |
| **#362** | P0 | verify: マルチテナント境界の実行確認（cross-tenant leak） | #329, #322 |
| **#339** | P1 | fix(frontend): トークンリフレッシュの無限ループリスク | #328 |
| **#360** | P0 | verify: 依存脆弱性スキャン実行と Critical/High ゼロ化 | Phase 0 |
| **#333** | P1 | security: 依存脆弱性 audit の CI 失敗連動 | #360 |
| **#323** | P1 | security: CI/CD ワークフローのセキュリティ強化 | なし |
| **#332** | P1 | fix(infra): Dockerfile の非 root ユーザー実行 | なし |

---

### Phase 2: ビジネスロジック正確性（POSとしての信頼性）
> **目的**: 金額計算・決済・在庫がPOSとして正しく動作することを保証。

| # | 優先度 | タイトル | 依存 |
|---|--------|---------|------|
| **#361** | P0 | verify: 金額計算の正確性（端数処理 FE/BE 一致） | Phase 0 |
| **#334** | P0 | fix(pos): 在庫 Race Condition — オーバーセル防止 | Phase 0 |
| **#320** | P0 | fix(pos): gRPC deadline 未設定による連鎖障害リスク | Phase 0 |
| **#321** | P0 | security: 決済処理の冪等性キーとリカバリ機構 | #320 |
| **#375** | P1 | fix(pos): 割引額バリデーション欠落 | なし |
| **#376** | P1 | fix(pos): 複数決済のオーバーペイ・返金仕様 | なし |
| **#366** | P1 | fix: 割引クーポンの FE/BE 実装差分 | #375 |
| **#364** | P1 | fix: Analytics API 契約の不一致 | なし |
| **#365** | P1 | fix: void 取消時の analytics 集計ロールバック日付バグ | #364 |
| **#363** | P1 | fix: タイムゾーン処理の不一貫 | なし |
| **#356** | P1 | compliance: インボイス制度対応（適格請求書要件） | #377 |
| **#377** | P1 | fix(pos): インボイス登録番号のハードコード除去 | なし |

---

### Phase 3: 耐障害性・運用基盤
> **目的**: 本番運用に耐えるメッセージング・キャッシュ・DB接続の整備。

| # | 優先度 | タイトル | 依存 |
|---|--------|---------|------|
| **#330** | P1 | fix(backend): RabbitMQ メッセージの明示的 ack/nack | なし |
| **#318** | P1 | feat(resilience): Transactional Outbox パターン | #330 |
| **#335** | P1 | fix(backend): Redis cache-aside パターン違反 | なし |
| **#317** | P1 | fix(infra): PgBouncer が未使用 | なし |
| **#343** | P1 | fix(infra): Flyway migrate-at-start を本番で無効化 | なし |
| **#344** | P0 | fix(infra): K8s Secrets 管理 | なし |
| **#346** | P1 | feat(infra): K8s 本番構成 | #344 |
| **#349** | P1 | security: 監査ログの完全性 | なし |
| **#370** | P1 | verify: Redis/RabbitMQ ダウン時のグレースフルデグレーション | #330, #335 |

---

### Phase 4: ドキュメント・検証
> **目的**: ドキュメント整備と最終動作確認。

| # | 優先度 | タイトル | 依存 |
|---|--------|---------|------|
| **#352** | P1 | docs: README/SECURITY/アーキテクチャのバージョン整合 | Phase 2 |
| **#353** | P1 | docs: 環境変数の完全ドキュメント (.env.example) | なし |
| **#354** | P1 | docs: 本番デプロイガイドの作成 | Phase 3 |
| **#355** | P2 | docs: API ドキュメントの完全化（OpenAPI + Proto docs） | Phase 2 |
| **#357** | P2 | chore: サードパーティライセンスの確認と公開 | なし |
| **#359** | P0 | verify: make docker-demo の動作確認と E2E green 化 | Phase 0-3 |
| **#367** | P1 | verify: オフライン取引の完全同期確認 | Phase 2 |
| **#368** | P1 | verify: make db-backup / db-restore の実行確認 | Phase 0 |
| **#369** | P1 | verify: README clone→動作の新規開発者体験検証 | #352, #353 |
| **#374** | P2 | verify: 全 API エンドポイントのスモークテスト | Phase 2 |
| **#342** | P1 | fix(frontend): オフライン同期の競合解決戦略 | #367 |

---

### Phase 5: 品質向上
> **目的**: v1.0 の品質水準を上げる。必須ではないが望ましい。

| # | 優先度 | タイトル |
|---|--------|---------|
| **#312** | P2 | chore(test): カバレッジ閾値を元の水準に復元 |
| **#319** | P2 | chore: Stabilization — placeholder API の整理 |
| **#324** | P2 | feat(observability): 分散トレーシング (OpenTelemetry) |
| **#325** | P2 | chore(perf): 負荷テスト (k6) 導入 |
| **#326** | P2 | fix(infra): Gradle メモリ設定の最適化 |
| **#327** | P2 | security(privacy): GDPR/個人情報保護対応 |
| **#336** | P2 | fix(backend): N+1 クエリリスク |
| **#337** | P2 | fix(backend): gRPC Status Code の統一 |
| **#338** | P3 | fix(frontend): as 型アサーション 87箇所の削減 |
| **#340** | P2 | chore(frontend): バンドルサイズ最適化 |
| **#341** | P2 | fix(frontend): console.log 残存 |
| **#345** | P2 | fix(infra): ヘルスチェックエンドポイントの統一 |
| **#347** | P2 | feat(infra): CD パイプライン完成 |
| **#348** | P2 | fix(observability): ログ構造化の完成 |
| **#371** | P2 | verify: ログに PII が含まれていないか |
| **#372** | P2 | verify: i18n 翻訳の網羅性 |
| **#373** | P2 | verify: DB スキーマ整合性 |
| **#378** | P2 | fix: buf breaking 検証の設定修正 |

---

## 既存 PR の状態

| PR | イシュー | 内容 | CI |
|----|---------|------|-----|
| #313 | #311 | Docker ビルド依存解決修正 | Backend/FE パス, E2E 失敗(#315が原因) |
| #314 | #312 | 341テスト追加+閾値引き上げ | CI修正プッシュ済み |
| #316 | #315 | Flyway版番号衝突+RabbitMQポート修正 | CI実行中 |

## 調査ソース

| ソース | 分析手法 | 主な発見 |
|--------|---------|---------|
| Claude (5 Agent) | コード品質、テナント漏れ、CI設定、OSS完成度、実証検証 | analytics テナントフィルタ漏れ、JVMクラッシュログ |
| Gemini | POS ビジネスリスク、法的要件、OSS完了条件 | インボイス制度、Race Condition、GDPR |
| Codex | FE/BE実装差分、具体的コード箇所特定 | void日付バグ、Analytics API不一致、PgBouncer未使用 |
| ビジネスロジック Agent | 金額計算・決済・在庫の正確性検証 | 割引バリデーション欠落、複数決済オーバーペイ |
| 実証検証 Agent | git history、依存脆弱性、DB スキーマ、i18n | pnpm audit クリーン、i18n完全同期 |

## 検証済み「問題なし」項目

| 項目 | 検証方法 | 結果 |
|------|---------|------|
| 金額の型 (Float/Double使用) | 全ソースgrep | BIGINT/Long で統一、問題なし |
| TODO/FIXME コメント | 全ソースgrep | 検出ゼロ |
| ハードコード URL/ポート | 全ソースgrep | 環境変数化済み |
| i18n 翻訳漏れ | ja.json/en.json 行数比較 | 完全同期 |
| 依存脆弱性 | pnpm audit 実行 | ゼロ |
| as unknown as / as any | 全ソースgrep | 検出ゼロ |
| console.log 残存 | 全ソースgrep（test除外） | 検出ゼロ |
