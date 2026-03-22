# 顧客PII同意フロー設計

## 概要
個人情報保護法第15条に基づき、顧客の個人情報（名前、メール、電話番号）を
収集する際に利用目的の明示と同意取得が必要。

## 現状
- `CustomerService.create()` は同意確認なしで顧客を作成
- `DataProcessingConsentEntity` は組織単位の同意のみ（顧客単位なし）

## 設計方針

### Phase 1: 同意フラグ追加（v1.1）
1. `CustomerEntity` に `consentGiven: Boolean` フィールド追加
2. `CustomerService.create()` で `consentGiven = true` を必須化
3. Proto の `CreateCustomerRequest` に `consent_given` フィールド追加
4. フロントエンドの顧客登録フォームに同意チェックボックス追加

### Phase 2: 同意管理（v1.2）
1. `customer_consents` テーブル新設（顧客単位の同意履歴）
2. 同意の撤回（`revokeConsent`）RPC追加
3. 同意撤回時の PII 匿名化フロー

### 利用目的
以下の利用目的を顧客に明示:
- ポイント管理・付与
- 購買履歴の管理
- レシート・領収書の発行
- マーケティング（オプトイン）

### API変更
```protobuf
message CreateCustomerRequest {
  string name = 1;
  string email = 2;
  string phone = 3;
  bool consent_given = 4;  // 追加: true 必須
}
```
