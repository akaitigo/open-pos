# open-pos Product Roadmap

> **Last Updated**: 2026-03-05
> **Status**: Active
> **Owner**: Product Team

## Overview

open-pos は汎用 POS システムのモノリポプロジェクト。Kotlin/Quarkus + React で構築する、マルチテナント・オフライン対応の SaaS 型 POS。

本ロードマップは [Harness Engineering](https://openai.com/index/harness-engineering/) の原則に基づき設計:
- **Phase = Milestone** — 各フェーズは GitHub Milestone に対応
- **1 Phase = 1 つのデリバリー可能な価値** — 各フェーズ完了時にデモ可能
- **依存関係の明示** — Phase 間の依存を最小化、Phase 内は並列可能
- **機械的に検証可能** — 各 Phase に CI/テストの完了条件

---

## Phase Summary

| Phase | Name | Target | Key Deliverable |
|-------|------|--------|-----------------|
| ~~1~~ | ~~Foundation~~ | ~~Done~~ | ~~Proto, Store/Product, CI~~ |
| **1.5** | **Development Infrastructure** | 2026-03-19 | テスト基盤・CI/CD・品質ゲート |
| **2** | **Core POS Engine** | 2026-04-30 | 取引エンジン・在庫・イベント駆動 |
| **3** | **Frontend Integration** | 2026-06-15 | POS端末・管理画面のAPI接続 |
| **4** | **Auth & Security** | 2026-07-31 | 認証・認可・セキュリティ強化 |
| **5** | **Analytics & Reporting** | 2026-08-31 | 売上分析・精算・レポート |
| **6** | **Operations & Observability** | 2026-09-30 | 監視・CI/CD・デプロイ |
| **7** | **Business Features** | 2026-11-30 | オフライン・帳票・在庫高度化 |
| **8** | **Scale & Polish** | 2027-01-31 | 性能・a11y・i18n・クラウド |
| **9** | **Enterprise** | 2027-03-31 | 顧客管理・ポイント・SaaS化 |

---

## Phase 1.5: Development Infrastructure (〜2026-03-19)

**ゴール**: Phase 2 開発を安全に開始できるテスト基盤・CI/CD・品質ゲートが整う

**背景**: リポジトリ網羅的レビューにより、テスト基盤 25%・品質ゲート 0% が判明。Phase 2 開始前の基盤整備が必須。

### 1.5.1 即時対応（P0）
| Issue | Title | Priority |
|-------|-------|----------|
| #232 | 未コミットファイルの確定（testing docs, branching strategy, CLAUDE.md, CI workflow） | P0 |
| #233 | CLAUDE.md 強化（トラブルシューティング、レイヤー図、テナント実装例、rules参照） | P0 |
| #240 | Testcontainers 結合テスト基盤構築（PostgreSQL + Redis + RabbitMQ） | P0 |
| #234 | JaCoCo + Vitest カバレッジ計測基盤（80%品質ゲート） | P0 |
| #241 | Playwright E2E テスト基盤構築（Page Object Model + 5シナリオ） | P0 |

### 1.5.2 品質ゲート（P1）
| Issue | Title | Priority |
|-------|-------|----------|
| #235 | GitHub ブランチ保護ルール設定（テスト pass + approve 必須） | P1 |
| #236 | CI パイプライン拡張（機能/結合テスト step + カバレッジ PR コメント） | P1 |
| #237 | Docker Compose 認証情報の外部化（.env + .env.example） | P1 |
| #238 | テストテンプレート作成（単体/機能/結合/E2E 実装例） | P1 |

### 1.5.3 改善（P2）
| Issue | Title | Priority |
|-------|-------|----------|
| #239 | Dependabot 設定最適化（directory拡張 + grouping） | P2 |

**依存グラフ**:
```
#232 → #233 (未コミット確定 → CLAUDE.md 強化)
#240 → #236 (Testcontainers → CI パイプライン拡張)
#234 → #236 (カバレッジ計測 → CI パイプライン拡張)
#241 → #236 (E2E 基盤 → CI パイプライン拡張)
#236 → #235 (CI 拡張 → ブランチ保護)
```

**完了条件**:
- [ ] `./gradlew test -Dquarkus.test.profile=integration` で結合テスト pass
- [ ] `pnpm --filter e2e test` で E2E テスト pass
- [ ] JaCoCo/Vitest カバレッジレポート生成
- [ ] CI で全テストレベルが自動実行
- [ ] main ブランチ保護ルール有効

---

## Phase 2: Core POS Engine (〜2026-04-30)

**ゴール**: gRPCurl で取引フルフロー（作成→明細追加→確定→レシート→在庫減算）が実行できる

### 2.1 取引エンジン（pos-service）
| Issue | Title | Priority |
|-------|-------|----------|
| #40 | Transaction エンティティ + Flyway マイグレーション | P1 |
| #49 | 税額計算エンジン（標準10%/軽減8%） | P1 |
| #41 | CreateTransaction + AddTransactionItem RPC | P1 |
| #42 | UpdateTransactionItem + RemoveTransactionItem RPC | P1 |
| #43 | ApplyDiscount RPC | P1 |
| #44 | FinalizeTransaction RPC（支払処理・取引確定） | P1 |
| #45 | VoidTransaction RPC（取引無効化・返品） | P1 |
| #46 | GetTransaction + ListTransactions RPC | P2 |
| #47 | GetReceipt RPC（レシート生成） | P1 |

### 2.2 在庫管理（inventory-service）
| Issue | Title | Priority |
|-------|-------|----------|
| #50 | Stock エンティティ + Flyway マイグレーション | P1 |
| #51 | GetStock + ListStocks + AdjustStock RPC | P1 |
| #52 | ListStockMovements RPC（在庫移動履歴） | P2 |

### 2.3 イベント駆動基盤
| Issue | Title | Priority |
|-------|-------|----------|
| #89 | RabbitMQ Exchange/Queue 定義 + SaleCompletedEvent Publisher | P1 |
| #90 | SaleVoidedEvent + ProductUpdatedEvent Publisher | P1 |
| #92 | イベント冪等性フレームワーク（ProcessedEvent テーブル） | P1 |
| #54 | SaleCompletedEvent 購読 → 在庫自動減算 | P1 |
| #55 | SaleVoidedEvent 購読 → 在庫自動戻し | P1 |

### 2.4 API Gateway 拡張
| Issue | Title | Priority |
|-------|-------|----------|
| #62 | POS取引 REST エンドポイント追加 | P1 |
| #63 | 在庫管理 REST エンドポイント追加 | P2 |

### 2.5 テスト・DX
| Issue | Title | Priority |
|-------|-------|----------|
| #96 | pos-service ユニット/統合テスト | P1 |
| #97 | inventory-service ユニット/統合テスト | P1 |
| #119 | CI パイプライン改善（全サービステスト + キャッシュ） | P2 |
| #151 | 開発用シードデータ | P2 |
| #169 | Makefile コマンド拡充 | P2 |

**依存グラフ**:
```
#40 → #49 → #41 → #42,#43 → #44 → #45,#46,#47
#89 → #92 → #54,#55
#50 → #51 → #54
#44 → #89 (FinalizeTransaction が SaleCompletedEvent を発行)
```

**完了条件**:
- [ ] `make test` で pos-service, inventory-service 全テスト pass
- [ ] gRPCurl で取引フルフローが実行可能
- [ ] RabbitMQ 経由で在庫が自動減算される

---

## Phase 3: Frontend Integration (〜2026-06-15)

**ゴール**: POS 端末で実際に取引操作できる、管理画面で商品・店舗管理ができる

### 3.1 POS 端末
| Issue | Title | Priority |
|-------|-------|----------|
| #37 | 商品グリッド + バーコードスキャン | P1 |
| #65 | 商品グリッド表示 + カテゴリナビゲーション | P1 |
| #66 | バーコードスキャナー統合（html5-qrcode） | P1 |
| #67 | カート管理UI（数量変更・削除・合計表示） | P1 |
| #68 | 支払処理画面（現金・クレジット・QR） | P1 |
| #69 | レシート表示・印刷画面 | P1 |
| #70 | 取引履歴画面（一覧・詳細・返品） | P2 |
| #163 | レスポンシブデザイン（タブレット/デスクトップ） | P2 |
| #165 | エラーハンドリング統一（トースト・エラーページ） | P2 |
| #166 | 確認ダイアログ統一（破壊的操作） | P2 |

### 3.2 管理画面
| Issue | Title | Priority |
|-------|-------|----------|
| #35 | 商品管理画面 | P1 |
| #74 | 商品管理画面（CRUD + 一括操作） | P1 |
| #75 | カテゴリ管理画面（ツリービュー） | P2 |
| #76 | 税率管理画面 | P2 |
| #36 | 店舗・スタッフ管理画面 | P1 |
| #78 | 店舗管理画面 | P1 |
| #79 | スタッフ管理画面（RBAC） | P1 |
| #164 | レスポンシブサイドバーナビゲーション | P2 |

### 3.3 テスト
| Issue | Title | Priority |
|-------|-------|----------|
| #102 | pos-terminal コンポーネントテスト拡充 | P2 |
| #103 | admin-dashboard コンポーネントテスト | P2 |
| #99 | api-gateway REST 統合テスト拡充 | P2 |

**完了条件**:
- [ ] POS 端末で商品選択→カート→支払→レシート表示が可能
- [ ] 管理画面で商品 CRUD / 店舗・スタッフ管理が可能
- [ ] フロントエンド全テスト pass

---

## Phase 4: Auth & Security (〜2026-07-31)

**ゴール**: PIN ログインでPOS操作、ロールベースのアクセス制御が機能する

### 4.1 認証基盤
| Issue | Title | Priority |
|-------|-------|----------|
| #85 | ORY Hydra OAuth2 クライアント登録 | P1 |
| #86 | スタッフログインフロー（PIN → Hydra トークン発行） | P1 |
| #87 | API Gateway 認証ミドルウェア（JWT 検証） | P1 |
| #88 | フロントエンド認証フロー（PKCE + トークン管理） | P1 |
| #72 | POS端末 PIN ログイン画面 + スタッフセッション管理 | P1 |

### 4.2 セキュリティ強化
| Issue | Title | Priority |
|-------|-------|----------|
| #105 | 入力バリデーション強化（全gRPCサービス） | P1 |
| #106 | gRPC メタデータサニタイゼーション | P1 |
| #108 | CORS 設定（api-gateway） | P2 |
| #107 | API レートリミット（api-gateway） | P2 |
| #110 | リクエストサイズ制限 + タイムアウト | P2 |
| #111 | Content Security Policy + セキュリティヘッダー | P2 |
| #109 | 監査ログ（重要操作の記録） | P2 |
| #167 | Dependabot / Renovate Bot 設定 | P2 |

### 4.3 コンプライアンス
| Issue | Title | Priority |
|-------|-------|----------|
| #124 | 適格請求書（インボイス）制度完全対応 | P1 |
| #126 | 個人情報保護（スタッフ・顧客データ） | P2 |

**完了条件**:
- [ ] PIN ログイン → POS 操作 → ログアウト が動作する
- [ ] CASHIER ロールでスタッフ管理 API にアクセスすると 403
- [ ] OWASP Top 10 の主要項目をカバー

---

## Phase 5: Analytics & Reporting (〜2026-08-31)

**ゴール**: 管理画面で売上分析・精算が可能

### 5.1 Analytics Service
| Issue | Title | Priority |
|-------|-------|----------|
| #57 | 売上集計エンティティ + Flyway マイグレーション | P1 |
| #58 | SaleCompletedEvent 購読 → 売上集計更新 | P1 |
| #59 | SaleVoidedEvent 購読 → 売上集計ロールバック | P1 |
| #60 | GetDailySales + GetSalesSummary RPC | P1 |
| #61 | GetProductSales + GetHourlySales RPC | P2 |
| #64 | 売上分析 REST エンドポイント追加 | P2 |
| #98 | analytics-service ユニット/統合テスト | P1 |

### 5.2 精算・レポート
| Issue | Title | Priority |
|-------|-------|----------|
| #127 | 精算（レジ締め）機能 | P1 |
| #128 | つり銭準備金管理 | P2 |
| #129 | 日報・月報レポート生成 | P2 |
| #130 | CSV/Excel エクスポート | P2 |
| #82 | 管理画面 売上ダッシュボード（日次チャート・KPI） | P1 |

### 5.3 補助
| Issue | Title | Priority |
|-------|-------|----------|
| #77 | 管理画面 割引・クーポン管理画面 | P2 |
| #80 | 管理画面 端末管理画面 | P2 |
| #81 | 管理画面 組織設定画面 | P2 |

**完了条件**:
- [ ] 管理画面に売上ダッシュボード表示
- [ ] レジ締め（精算）が POS 端末から実行可能
- [ ] CSV エクスポートが動作する

---

## Phase 6: Operations & Observability (〜2026-09-30)

**ゴール**: 本番運用に必要な可観測性・CI/CD・インフラが整う

### Issues
| Issue | Title | Priority |
|-------|-------|----------|
| #112 | 構造化ログ（JSON + 相関ID） | P1 |
| #113 | 分散トレーシング（OpenTelemetry） | P2 |
| #114 | メトリクス収集（Micrometer + Prometheus） | P2 |
| #115 | Grafana ダッシュボード | P2 |
| #116 | ヘルスチェック改善（深いヘルスチェック） | P2 |
| #117 | データベースインデックス戦略 | P2 |
| #122 | データベースバックアップ戦略 | P2 |
| #123 | シークレット管理（GCP Secret Manager） | P2 |
| #198 | Flyway マイグレーション CI 検証 | P2 |
| #91 | Dead Letter Queue ハンドリング + リトライ | P2 |
| #56 | StockLowEvent 発行（低在庫アラート） | P2 |
| #100 | 取引フルフロー E2E テスト | P1 |
| #150 | OpenAPI / Swagger ドキュメント自動生成 | P2 |
| #177 | アーキテクチャドキュメント更新 | P2 |
| #211 | 開発環境セットアップ自動化スクリプト | P2 |
| #185 | OWASP Top 10 セキュリティ監査 | P2 |

**完了条件**:
- [ ] Grafana で全サービスの監視ダッシュボード表示
- [ ] E2E テストが CI で pass
- [ ] OpenAPI ドキュメントが生成される

---

## Phase 7: Business Features (〜2026-11-30)

**ゴール**: 実店舗で運用可能な業務機能が揃う

### Issues
| Issue | Title | Priority |
|-------|-------|----------|
| #71 | オフラインモード（Dexie + Service Worker） | P1 |
| #48 | SyncOfflineTransactions RPC | P1 |
| #73 | 割引・クーポン適用UI | P2 |
| #197 | レシートプリンター連携（ESC/POS） | P2 |
| #194 | 領収書発行機能 | P2 |
| #179 | レシート再発行機能 | P2 |
| #180 | 部分返品（明細単位） | P2 |
| #181 | 保留取引（パーキング） | P2 |
| #136 | オープンプライス商品 | P2 |
| #131 | 商品CSVインポート | P2 |
| #53 | 発注管理 CRUD RPC | P2 |
| #83 | 管理画面 在庫管理画面 | P2 |
| #84 | 管理画面 発注管理画面 | P2 |
| #146 | 棚卸し（在庫実査）機能 | P2 |
| #217 | 商品のソフトデリート（論理削除） | P2 |
| #218 | 楽観的ロック（同時更新の競合防止） | P2 |
| #204 | システム設定管理（テナントレベル） | P2 |
| #209 | 電子ジャーナル（取引ジャーナル保存） | P2 |
| #125 | 電子帳簿保存法対応 | P2 |
| #101 | オフライン同期 E2E テスト | P2 |
| #93 | product-service Redis キャッシュ層 | P2 |
| #94 | store-service Redis キャッシュ層 | P2 |
| #95 | api-gateway Redis レスポンスキャッシュ | P2 |
| #154 | タッチターゲットサイズ最適化 | P2 |

**完了条件**:
- [ ] オフラインで取引作成→オンライン復帰で同期
- [ ] レシートプリンターで印刷可能
- [ ] 棚卸し・発注管理が動作する

---

## Phase 8: Scale & Polish (〜2027-01-31)

**ゴール**: 本番デプロイ可能な品質と性能

### Issues
| Issue | Title | Priority |
|-------|-------|----------|
| #157 | フロントエンドバンドル最適化 | P2 |
| #158 | gRPC 接続プーリング + keep-alive | P2 |
| #159 | N+1 クエリ防止の検証・最適化 | P2 |
| #118 | pgBouncer 接続プーリング | P3 |
| #104 | 負荷テスト（取引処理スループット） | P3 |
| #120 | GKE / Cloud Run デプロイ設定 | P3 |
| #121 | CD パイプライン（ステージング→本番） | P3 |
| #178 | Quarkus ネイティブビルド対応 | P3 |
| #168 | コンテナセキュリティスキャン | P3 |
| #153 | POS端末 キーボードナビゲーション | P3 |
| #155 | ハイコントラストモード + フォントサイズ調整 | P3 |
| #156 | 多言語対応基盤（i18n） | P3 |
| #160 | カーソルベースページネーション | P3 |
| #190 | ログ集約（Loki + Grafana） | P3 |
| #210 | データアーカイブ・パーティショニング | P3 |
| #182 | 売上予測・トレンド分析 | P3 |
| #183 | ABC 分析 | P3 |
| #184 | 粗利管理（原価登録・粗利レポート） | P3 |
| #152 | Postman / gRPCurl コレクション | P3 |
| #212 | gRPC リフレクション有効化 | P3 |
| #215 | Docker Compose プロファイル分離 | P3 |
| #202 | 災害復旧計画（DR） | P3 |
| #221 | Proto ドキュメント自動生成 | P3 |

**完了条件**:
- [ ] 負荷テストで性能目標を達成（p99 < 500ms）
- [ ] Cloud Run へデプロイ可能
- [ ] Lighthouse Performance > 90

---

## Phase 9: Enterprise (〜2027-03-31)

**ゴール**: 多業態・多店舗展開に対応する SaaS 機能

### Issues
| Issue | Title | Priority |
|-------|-------|----------|
| #140 | 顧客管理基盤 | P3 |
| #141 | ポイント/ロイヤルティシステム | P3 |
| #142 | ギフトカード / プリペイドカード | P3 |
| #132 | 電子レシート（メール送信） | P3 |
| #133 | 商品バリアント（サイズ・カラー） | P3 |
| #134 | セット商品（バンドル販売） | P3 |
| #135 | 計量販売商品（重量/量り売り） | P3 |
| #138 | 年齢確認プロンプト | P3 |
| #139 | トレーニングモード | P3 |
| #143 | タイムセール / 時間帯別価格 | P3 |
| #144 | 仕入先管理 | P3 |
| #145 | 多店舗間在庫移動 | P3 |
| #147 | バーコードラベル印刷 | P3 |
| #148 | 免税販売対応 | P3 |
| #149 | レシートカスタマイズ | P3 |
| #161 | ダークモード | P3 |
| #162 | サウンドフィードバック | P3 |
| #170 | 複数通貨対応 | P3 |
| #171 | 勤怠管理 | P3 |
| #172 | シフト管理 | P3 |
| #173 | 商品画像管理 | P3 |
| #174 | 通知システム | P3 |
| #175 | WebSocket / SSE リアルタイム更新 | P3 |
| #176 | マルチテナント プラン・課金管理 | P3 |
| #186 | 外部決済端末連携 | P3 |
| #187 | スタッフ活動ログ | P3 |
| #188 | お気に入り商品 | P3 |
| #189 | 在庫自動発注 | P3 |
| #191 | テナント オンボーディングウィザード | P3 |
| #192 | マルチ端末カート共有 | P3 |
| #193 | 予約注文・取り置き | P3 |
| #195 | 会計ソフト連携 | P3 |
| #196 | ドロワー連携 | P3 |
| #199 | 在庫有効期限管理 | P3 |
| #200 | テーブルオーダー / 飲食店モード | P3 |
| #201 | セルフレジモード | P3 |
| #203 | Webhook 連携 | P3 |
| #205 | 売上速報メール | P3 |
| #206 | サービスメッシュ検討 | P3 |
| #207 | 商品アラート | P3 |
| #208 | API バージョニング戦略 | P3 |
| #213 | 顧客表示ディスプレイ | P3 |
| #214 | 売上目標管理 | P3 |
| #216 | 値引き理由コード管理 | P3 |
| #219 | 税率変更スケジューリング | P3 |
| #220 | テナント データエクスポート | P3 |
| #222 | スタンプカード | P3 |
| #223 | POS端末 マルチウィンドウ | P3 |
| #137 | 取引メモ・備考 | P3 |

**完了条件**:
- [ ] 顧客管理 + ポイントシステムが動作
- [ ] 飲食店モード（テーブルオーダー）が使用可能
- [ ] 外部決済端末と連携可能

---

## Principles (Harness Engineering)

### 1. Delegate-Review-Own
- **Delegate**: 明確な仕様のタスク → AI エージェントに委任
- **Review**: PR レビュー → 品質・整合性の人間検証
- **Own**: アーキテクチャ・戦略的決定 → 人間が所有

### 2. Golden Principles
- 金額は **BIGINT 銭単位**（浮動小数点禁止）
- 全テーブルに **organization_id**（テナント分離）
- イベントは **EventEnvelope** でラップ（冪等性キー付き）
- テストは **Red → Green**（実装前に失敗確認）

### 3. Session Protocol
1. 作業ディレクトリ確認
2. Git ログ + 進捗ファイル読み取り
3. 未完了の最優先タスク選択
4. 既存不具合を先に修正
5. 1 セッション = 1 機能に集中

### 4. Branching Strategy
→ [docs/guides/branching-strategy.md](../guides/branching-strategy.md)
