# 画面別機能マトリクス

> Closes #405

open-pos の POS 端末と管理画面の画面ごとの機能一覧と実装状況をまとめたマトリクスです。

## 凡例

| 記号 | 意味 |
|------|------|
| :white_check_mark: | 実装済み |
| :construction: | 実装中 / 部分対応 |
| :calendar: | 計画済み（ロードマップに含む） |
| :x: | 未計画 |

---

## POS 端末（pos-terminal）

タブレット最適化の PWA アプリケーション。React 19 + TypeScript + Tailwind CSS + shadcn/ui で構築。

### 認証・セッション

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| PIN ログイン | :white_check_mark: | 4-6 桁の PIN でスタッフを切替 |
| OIDC/PKCE ログイン | :white_check_mark: | ORY Hydra 経由の端末認証 |
| スタッフセッション管理 | :white_check_mark: | 2 時間で自動失効 |
| ログアウト | :white_check_mark: | セッション破棄 |

### 商品選択

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| 商品グリッド表示 | :white_check_mark: | タイル形式で商品を表示 |
| カテゴリタブ（階層） | :white_check_mark: | ルート / サブカテゴリの切替 |
| 商品検索（フリーワード） | :white_check_mark: | クライアント側のリアルタイム検索 |
| ページネーション | :white_check_mark: | クライアント側ページング |
| 在庫状況表示 | :white_check_mark: | 在庫切れ商品の視覚的表示 |
| バーコードスキャン | :calendar: | html5-qrcode 統合（Phase 3） |
| お気に入り商品 | :calendar: | クイックアクセス（Phase 9） |

### カート操作

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| カートパネル | :white_check_mark: | 再利用可能なサイドパネル |
| 数量直接編集 | :white_check_mark: | インライン数量変更 |
| 明細削除 | :white_check_mark: | スワイプまたはボタンで削除 |
| 明細ごとの小計 | :white_check_mark: | 単価 x 数量の自動計算 |
| 税率区分別内訳 | :white_check_mark: | 標準 10% / 軽減 8% の分類表示 |
| 全画面カートレビュー (`/cart`) | :white_check_mark: | 確認用フルスクリーンビュー |
| 割引・クーポン適用 | :calendar: | 取引レベル / 商品レベル（Phase 7） |
| 保留取引（パーキング） | :calendar: | 取引を一時保存（Phase 7） |
| オープンプライス入力 | :calendar: | 価格未設定商品の価格入力（Phase 7） |

### 支払処理

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| 現金支払 | :white_check_mark: | 受取金額入力 + お釣り自動計算 |
| クレジットカード支払 | :white_check_mark: | 承認番号の記録 |
| QR コード支払 | :white_check_mark: | トランザクション ID の記録 |
| 複合支払（分割） | :white_check_mark: | 複数支払方法の組み合わせ |
| レシート表示 | :white_check_mark: | 取引完了後のレシートダイアログ |
| レシート再発行 | :calendar: | 過去取引のレシート再表示（Phase 7） |

### 取引管理

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| 取引履歴一覧 | :white_check_mark: | 日次の取引リスト |
| 取引詳細 | :white_check_mark: | 明細・支払・税の詳細表示 |
| VOID（当日取消） | :white_check_mark: | MANAGER 以上の権限で実行 |
| 返品 | :white_check_mark: | 元取引参照による返品 |
| 部分返品 | :calendar: | 明細単位の返品（Phase 7） |

### オフライン対応

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| オフラインバナー表示 | :white_check_mark: | ネットワーク状態の可視化 |
| ローカル DB（Dexie.js） | :white_check_mark: | IndexedDB によるマスタキャッシュ |
| オフライン取引保存 | :white_check_mark: | `client_id` 付きでローカル保存 |
| 自動同期（Background Sync） | :calendar: | オンライン復帰時の自動送信（Phase 7） |
| 未同期件数表示 | :calendar: | 保留中取引数の UI 表示（Phase 7） |

### 精算・ドロワー

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| ドロワー開閉 | :construction: | API 実装済み、UI は Phase 5 |
| 精算（レジ締め） | :calendar: | 日次精算処理（Phase 5） |
| つり銭準備金管理 | :calendar: | 営業開始時の準備金登録（Phase 5） |

### その他

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| エラーハンドリング統一 | :white_check_mark: | トースト通知 + エラーページ |
| ローディング・リトライ状態 | :white_check_mark: | リトライ可能なエラー表示 |
| レスポンシブデザイン | :construction: | タブレット / デスクトップ対応中 |
| キーボードナビゲーション | :calendar: | Phase 8 |
| 多言語対応（i18n） | :white_check_mark: | 日本語 / 英語対応済み |
| ダークモード | :calendar: | Phase 9 |

---

## 管理画面（admin-dashboard）

デスクトップ向けの SPA。React 19 + TypeScript + Tailwind CSS + shadcn/ui で構築。

### 認証

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| OIDC/PKCE ログイン | :white_check_mark: | ORY Hydra 経由 |
| ロールベースアクセス制御 | :white_check_mark: | Owner / Manager で表示切替 |

### 商品管理

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| 商品一覧 | :white_check_mark: | フィルタ・ソート・ページネーション |
| 商品作成 | :white_check_mark: | フォームバリデーション付き |
| 商品編集 | :white_check_mark: | インライン編集 |
| 商品削除（論理削除） | :white_check_mark: | ソフトデリート |
| 商品一括操作 | :calendar: | 複数選択→一括処理（Phase 3） |
| 商品 CSV インポート | :calendar: | 一括登録（Phase 7） |
| 商品画像管理 | :calendar: | Cloud Storage 統合（Phase 9） |
| バーコードラベル印刷 | :calendar: | Phase 9 |

### カテゴリ管理

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| カテゴリ一覧（ツリービュー） | :white_check_mark: | 階層表示 |
| カテゴリ作成 / 編集 | :white_check_mark: | 色・アイコン設定 |
| カテゴリ削除 | :white_check_mark: | 紐付き商品チェック付き |

### 税率管理

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| 税率一覧 | :white_check_mark: | 標準 / 軽減の区分表示 |
| 税率作成 / 編集 | :white_check_mark: | 新レコード追加方式 |
| 税率スケジューリング | :calendar: | 将来の税率変更予約（Phase 9） |

### 割引・クーポン管理

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| 割引一覧 | :construction: | API 実装済み、UI は Phase 5 |
| 割引作成 / 編集 | :calendar: | Phase 5 |
| クーポン管理 | :calendar: | Phase 5 |

### 在庫管理

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| 在庫一覧 | :white_check_mark: | 店舗別の在庫状況 |
| 在庫調整（棚卸） | :white_check_mark: | 手動数量変更 |
| 在庫移動履歴 | :white_check_mark: | 移動種別ごとのログ |
| 低在庫アラート | :construction: | イベント発行済み、UI 通知は Phase 6 |
| 発注作成 / 管理 | :white_check_mark: | DRAFT→ORDERED→RECEIVED フロー |
| 棚卸し（在庫実査） | :calendar: | Phase 7 |

### 店舗管理

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| 店舗一覧 | :white_check_mark: | 組織内の店舗リスト |
| 店舗作成 / 編集 | :white_check_mark: | Owner 権限 |
| 店舗設定 | :construction: | JSONB 設定（Phase 5） |
| 端末管理 | :calendar: | 端末登録 / 無効化（Phase 5） |

### スタッフ管理

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| スタッフ一覧 | :white_check_mark: | ロール別表示 |
| スタッフ作成 / 編集 | :white_check_mark: | Owner 権限、RBAC 設定 |
| PIN リセット | :white_check_mark: | ロック解除 |
| スタッフ活動ログ | :calendar: | Phase 9 |

### 売上分析

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| 売上ダッシュボード | :construction: | KPI カード表示、チャートは Phase 5 |
| 日次売上グラフ | :calendar: | Phase 5 |
| 商品別売上 | :calendar: | Phase 5 |
| 時間帯別売上 | :calendar: | Phase 5 |
| ABC 分析 | :calendar: | Phase 8 |
| 粗利レポート | :calendar: | Phase 8 |
| CSV/Excel エクスポート | :calendar: | Phase 5 |
| 日報・月報 | :calendar: | Phase 5 |

### 組織設定

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| 組織情報編集 | :construction: | API 実装済み、UI は Phase 5 |
| インボイス番号設定 | :white_check_mark: | 適格請求書発行事業者番号 |
| システム設定 | :calendar: | テナントレベル設定（Phase 7） |

### ナビゲーション

| 画面 / 機能 | 状態 | 説明 |
|------------|------|------|
| サイドバーナビゲーション | :white_check_mark: | メニュー構造 |
| レスポンシブサイドバー | :calendar: | 折りたたみ対応（Phase 3） |
| 多言語対応（i18n） | :white_check_mark: | 日本語 / 英語対応済み |

---

## 横断的な機能

| 機能 | 状態 | 説明 |
|------|------|------|
| マルチテナント分離 | :white_check_mark: | `organization_id` + Hibernate Filter |
| gRPC サービス間通信 | :white_check_mark: | proto3 + buf ツールチェーン |
| RabbitMQ イベント駆動 | :white_check_mark: | `sale.completed`、`sale.voided` 等 |
| Redis キャッシュ | :construction: | cache-aside パターン（Phase 7 で強化） |
| インボイス制度対応 | :white_check_mark: | 税率別集計 + 登録番号 |
| 金額は BIGINT 銭単位 | :white_check_mark: | 浮動小数点禁止 |
| CI/CD | :white_check_mark: | GitHub Actions、CodeQL、Dependabot |
| E2E テスト | :white_check_mark: | Playwright + Page Object Model |
| Docker Compose 開発環境 | :white_check_mark: | `make local-demo` / `make docker-demo` |
