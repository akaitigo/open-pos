# セキュリティ要件

## テナント分離

- 全テーブルに `organization_id` カラムを持つ
- Hibernate Filter で全クエリに `organization_id = :orgId` 条件を自動付与
- api-gateway で JWT から `organization_id` を抽出し、gRPC metadata に注入
- バックエンドサービスは metadata の `x-organization-id` を信頼（gateway 経由のみ）

**リスク**: Hibernate Filter 未適用のクエリが存在すると別テナントデータが漏洩する。
全エンティティに Filter 適用を静的解析でチェック（CI）。

## インジェクション防止

### SQLインジェクション
- 全クエリでパラメータバインド必須（文字列連結クエリ禁止）
- Panache / HQL / JPQL のプレースホルダ使用
- ネイティブクエリも `:param` バインド必須

### XSS防止
- React のデフォルトエスケープを活用（JSX の変数展開は自動エスケープ）
- `dangerouslySetInnerHTML` 使用禁止（sanitize-html 経由のみ許可）
- Content-Security-Policy ヘッダーを設定（`script-src 'self'`）

## 認証・認可

- アクセストークン: httpOnly cookie（`Secure`, `SameSite=Strict`）
- `localStorage` へのトークン保存禁止
- 全 API エンドポイントで認証チェック必須（許可リスト方式）
- 認可チェックはバックエンドで実施（フロントの表示切替のみでは不十分）

## CSRF対策

- `SameSite=Strict` cookie により CSRF リクエストをブロック
- `Origin` ヘッダー検証（api-gateway ミドルウェア）
- Preflight OPTIONS リクエストへの適切なCORS設定

## 通信暗号化

- 全通信 TLS 1.2以上（HTTP禁止）
- サービス間 gRPC も mTLS を推奨（Cloud Run マネージドTLS）
- DB接続は SSL 必須（Cloud SQL）

## シークレット管理

- 認証情報・APIキーのハードコード禁止
- `application.properties` は `${ENV_VAR}` プレースホルダのみ
- 本番環境は GCP Secret Manager 経由
- ソースコードへのシークレット混入を git-secrets / gitleaks でCI検知

## 監査ログ

- 全取引操作（作成・VOID・返品）を監査ログに記録
- スタッフログイン・ログアウト記録
- 管理操作（商品・スタッフ変更）記録
- 保存期間: 7年（会計記録保存要件）

## 脆弱性管理

- Dependabot / Renovate で依存関係の自動更新
- OWASP Top 10 を年次レビュー
- ペネトレーションテスト: 本番リリース前に実施

## 受け入れ条件

- [ ] 別テナントのデータにアクセスできないことをテストで確認
- [ ] SQLインジェクション文字列（`' OR '1'='1`等）がエラーなく処理される
- [ ] access_token が localStorage に保存されない
- [ ] CSPヘッダーが全レスポンスに付与される
