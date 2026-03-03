# 認証・認可 機能要件

## 認証基盤

ORY Hydra を OAuth2/OIDC プロバイダーとして使用する。

```
ブラウザ → ORY Hydra (OIDC) → api-gateway (トークン検証) → バックエンドサービス
```

## フロント認証フロー（PKCE）

1. ユーザーがログインボタンをクリック
2. PKCE `code_verifier` / `code_challenge` を生成
3. ORY Hydra の認証エンドポイントへリダイレクト
4. 認証完了後、`authorization_code` を受け取る
5. `code_verifier` を添えてトークンエンドポイントへリクエスト
6. `access_token` / `refresh_token` / `id_token` を取得
7. `access_token` を httpOnly cookie に保存（`refresh_token` も同様）
8. `localStorage` へのトークン保存は禁止

### トークン更新
- `access_token` 有効期限（デフォルト: 15分）を確認してからAPI呼び出し
- 期限切れ時は `refresh_token` で自動更新（リフレッシュローテーション対応）
- `refresh_token` が無効な場合は再ログイン画面へ

## api-gateway でのトークン検証

```
リクエスト → api-gateway
  → Authorization: Bearer {access_token} を検証（Hydra Introspection）
  → organization_id を JWT クレームから抽出
  → gRPC metadata に注入（x-organization-id, x-staff-id, x-role）
  → バックエンドサービスへ転送
```

### gRPC metadata 伝播

| メタデータキー | 値 | 設定元 |
|--------------|-----|-------|
| `authorization` | Bearer トークン | クライアント |
| `x-organization-id` | organization_id | gateway 注入 |
| `x-staff-id` | staff_id | gateway 注入 |
| `x-role` | OWNER/MANAGER/CASHIER | gateway 注入 |
| `x-request-id` | 相関ID | gateway 生成 |

## ロール定義

| ロール | 説明 |
|--------|------|
| `OWNER` | 組織全体の管理者 |
| `MANAGER` | 店舗管理者（組織設定・スタッフ管理以外） |
| `CASHIER` | POS会計のみ |

- ロールはJWTクレームに含める（`custom_claims.role`）
- バックエンドは `x-role` メタデータでロールチェック

## PINログイン（端末スタッフ切替）

- 用途: 端末共有時のスタッフ切替（フルログインは不要）
- フロー: PINコード(4-6桁)入力 → `POST /api/auth/pin-login` → 短期セッショントークン発行
- PINハッシュ: bcrypt（cost=12）
- 5回失敗でロック（`store-mgmt.md` 参照）
- PINセッションは2時間で自動失効

## CSRF対策

- `SameSite=Strict` cookie 属性設定
- `Origin` ヘッダー検証（api-gateway）
- カスタムヘッダー（`X-Requested-With: XMLHttpRequest`）必須

## 受け入れ条件

- [ ] 未認証リクエストは401を返す
- [ ] 権限外リソースへのアクセスは403を返す
- [ ] access_token は httpOnly cookie に保存される
- [ ] 異なる organization_id のリソースにはアクセスできない
- [ ] refresh_token ローテーション後、旧トークンは無効化される
