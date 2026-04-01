# PCI DSS 対応ガイド

## 概要

PCI DSS（Payment Card Industry Data Security Standard）は、クレジットカード情報を取り扱う全ての事業者に適用されるセキュリティ基準である。PCI SSC（Payment Card Industry Security Standards Council）が策定・管理しており、現行バージョンは PCI DSS v4.0（2024年3月完全施行）。

### POS システムでの関連性

POS システムはカード決済の起点となるため、PCI DSS の適用範囲に含まれる可能性が高い。ただし、カード番号（PAN）を自社で処理・保存・伝送するかどうかで、対応範囲と負荷が大きく異なる。

| アプローチ | PCI DSS 準拠の負荷 | open-pos の方針 |
|-----------|-------------------|----------------|
| カード番号を自社で保存・処理 | 非常に高い（SAQ D） | 採用しない |
| カード番号を自社で一時処理 | 高い（SAQ C） | 採用しない |
| 外部決済ゲートウェイに完全委託 | 低い（SAQ A または SAQ A-EP） | **採用** |

## open-pos のアプローチ: カード番号を保持しない

### 基本方針

open-pos はクレジットカード番号（PAN）を一切保持・処理・伝送しない設計を採用する。カード決済は外部の PCI DSS 準拠済み決済ゲートウェイに完全委託する。

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────────┐
│ POS端末     │────→│ 決済ゲートウェイ    │────→│ カード会社 / アクワイアラ │
│ (open-pos)  │←────│ (外部サービス)     │←────│                     │
└─────────────┘     └──────────────────┘     └─────────────────────┘
      │                      │
      │  トークンのみ保存     │  カード番号を処理
      │  PAN は通過しない     │  PCI DSS Level 1 準拠
```

### 決済フロー

1. POS 端末で「カード決済」を選択
2. 決済ゲートウェイの SDK/API を呼び出し（カード情報は POS を経由しない）
3. 決済ゲートウェイがカード会社と通信し、決済を実行
4. 決済ゲートウェイからトークン（決済ID）が返却される
5. open-pos はトークンのみを `transactions` テーブルに保存

```sql
-- open-pos が保存するのはトークンのみ
-- カード番号（PAN）は一切保存しない
-- 決済情報は payments テーブルに保持（transactions ではない）
SELECT
    p.id,
    p.transaction_id,
    p.method,                   -- 'CREDIT_CARD'
    p.reference,                -- 決済ゲートウェイのトークン（例: 'pay_abc123xyz'）
    p.amount,
    t.transaction_number,
    t.total,
    t.completed_at
FROM pos_schema.payments p
JOIN pos_schema.transactions t ON t.id = p.transaction_id
WHERE p.method = 'CREDIT_CARD'
  AND p.deleted = false
  AND t.deleted = false;

-- 以下のカラムは存在しない（設計上排除）:
-- card_number, card_holder_name, cvv, expiry_date
```

## トークナイゼーション

### 方針

トークナイゼーションとは、カード番号を意味のないトークン文字列に置き換える技術である。open-pos では決済ゲートウェイが発行するトークンのみを使用する。

| 項目 | 内容 |
|------|------|
| トークン発行者 | 外部決済ゲートウェイ |
| トークン形式 | ゲートウェイ固有の文字列（例: `pay_`, `ch_` プレフィックス） |
| トークンの用途 | 決済結果の参照、返金処理 |
| トークンからの PAN 復元 | open-pos 側では不可能（ゲートウェイ側でのみ可能） |
| トークンの保存場所 | `pos_schema.payments.reference` |

### 返金処理

返金時はトークンを使って決済ゲートウェイの返金 API を呼び出す。カード番号に再度アクセスする必要はない。

```
POS端末 → 決済ゲートウェイ: POST /refunds { token: "pay_abc123xyz", amount: 100000 }
決済ゲートウェイ → POS端末: { status: "refunded", refund_id: "ref_xyz789" }
```

## SAQ（Self-Assessment Questionnaire）の適用範囲

### SAQ タイプの判定

open-pos のアーキテクチャでは、以下の SAQ タイプが適用される:

| SAQ タイプ | 条件 | open-pos での該当 |
|-----------|------|------------------|
| SAQ A | カード処理を完全に外部委託、e-commerce のみ | 対面 POS のため非該当 |
| SAQ A-EP | e-commerce でリダイレクト/iframe 方式 | 非該当 |
| SAQ B | 刻印機またはスタンドアロン端末のみ | 非該当 |
| SAQ B-IP | IP 接続のスタンドアロン決済端末 | **該当の可能性あり** |
| SAQ C | 決済アプリケーション端末 | **該当の可能性あり** |
| SAQ P2PE | P2PE 認定端末を使用 | 推奨構成 |

### 推奨構成: P2PE（Point-to-Point Encryption）

open-pos では PCI P2PE 認定の決済端末の使用を推奨する。これにより:

- カード情報は決済端末内で暗号化され、POS ソフトウェアに渡らない
- SAQ P2PE（33項目）で準拠可能（SAQ D の約320項目と比較して大幅に軽減）
- POS ソフトウェア自体が PCI DSS スコープ外となる

### 対応する決済端末の例

| 端末 | P2PE 認定 | 対応ゲートウェイ |
|------|-----------|----------------|
| Verifone P400 | あり | 各種アクワイアラ |
| Ingenico Lane/3000 | あり | 各種アクワイアラ |
| Square Terminal | 独自認定 | Square |

## セキュリティ要件チェックリスト

open-pos がカード番号を保持しない設計でも、以下のセキュリティ要件は遵守する:

- [ ] 決済ゲートウェイとの通信は TLS 1.2 以上を使用
- [ ] API キー・シークレットは環境変数で管理（ソースコードに含めない）
- [ ] 決済トークンへのアクセスは権限を持つユーザーに限定
- [ ] 決済関連のログにカード番号を含めない（マスキング済みの下4桁のみ許可）
- [ ] 決済ゲートウェイの PCI DSS 準拠証明（AOC）を年次で確認
- [ ] インシデント発生時の対応手順を整備（`docs/runbook/incident-response.md` 参照）

## 参考資料

- [PCI SSC: PCI DSS v4.0](https://www.pcisecuritystandards.org/document_library/)
- [PCI SSC: SAQ 一覧](https://www.pcisecuritystandards.org/document_library/?category=saqs)
- [日本カード情報セキュリティ協議会（JCDSC）](https://www.jcdsc.org/)
- [経済産業省: クレジットカード・セキュリティガイドライン](https://www.meti.go.jp/press/2022/03/20230315002/20230315002.html)
