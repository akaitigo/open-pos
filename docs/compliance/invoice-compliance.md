# Japanese Invoice System Compliance (インボイス制度対応)

This document describes how open-pos complies with Japan's Qualified Invoice System (適格請求書等保存方式), effective from October 1, 2023, as stipulated by the Consumption Tax Act (消費税法).

## Overview

The Qualified Invoice System (通称: インボイス制度) requires businesses registered as Qualified Invoice Issuers (適格請求書発行事業者) to issue invoices containing specific information. POS receipts (レシート) are classified as Simplified Qualified Invoices (適格簡易請求書) under Article 57-4, Paragraph 2 of the Consumption Tax Act.

## Registration Number Requirements

### Format

The registration number (登録番号) follows the pattern `T` + 13 digits:

- **Corporations (法人)**: `T` + 13-digit corporate number (法人番号)
  - Example: `T1234567890123`
- **Sole proprietors (個人事業主)**: `T` + 13-digit number assigned by the tax authority

### Implementation in open-pos

The registration number is stored in the `Organization` entity:

- **Proto field**: `openpos.store.v1.Organization.invoice_number`
- **Database column**: `store_schema.organizations.invoice_number`
- **REST API**: `POST/PUT /api/organizations` with `invoice_number` field
- **Validation**: Must match the pattern `^T\d{13}$` (T followed by exactly 13 digits)

Set the registration number via:

```bash
# REST API
curl -X PUT http://localhost:8080/api/organizations/{id} \
  -H "Content-Type: application/json" \
  -d '{"invoice_number": "T1234567890123"}'
```

## Tax Rate Breakdown Requirements

### Legal Requirements

A Simplified Qualified Invoice must display tax information in one of the following formats:

1. **Tax-rate-separated amounts** (税率ごとの課税資産の譲渡等の対価の額): Show the taxable amount per tax rate
2. **Tax-rate-separated tax amounts** (税率ごとの消費税額等): Show the tax amount per tax rate

### Current Tax Rates in Japan

| Tax Rate | Name | Applies To |
|----------|------|-----------|
| 10% (標準税率) | Standard rate | General goods and services |
| 8% (軽減税率) | Reduced rate | Food and beverages (excluding dining-in and alcohol), newspapers (subscribed, published 2+/week) |

### Implementation in open-pos

Tax rate management is handled by `product-service`:

- **Proto**: `openpos.product.v1.TaxRate` with fields `rate`, `is_reduced`, `name`
- **Per-item tax tracking**: Each `TransactionItem` stores `tax_rate_name`, `tax_rate`, and `is_reduced_tax` as snapshots at transaction time
- **Tax summaries**: `Transaction.tax_summaries` (repeated `TaxSummary`) aggregates taxable amount and tax amount per rate
- **Reduced-rate marker**: Items subject to the reduced tax rate are flagged with `is_reduced_tax = true`, displayed as `※` on receipts

Example tax summary structure:

```json
{
  "tax_summaries": [
    {
      "tax_rate_name": "標準税率10%",
      "tax_rate": "0.10",
      "is_reduced": false,
      "taxable_amount": 500000,
      "tax_amount": 50000
    },
    {
      "tax_rate_name": "軽減税率8%",
      "tax_rate": "0.08",
      "is_reduced": true,
      "taxable_amount": 300000,
      "tax_amount": 24000
    }
  ]
}
```

Money values are in sen (銭) units: 10000 = 100 JPY.

## Required Receipt Fields

### Simplified Qualified Invoice (適格簡易請求書) Requirements

Per Article 57-4 of the Consumption Tax Act, a POS receipt must contain:

| # | Field | Description | open-pos Source |
|---|-------|-------------|-----------------|
| 1 | 適格請求書発行事業者の氏名又は名称及び登録番号 | Issuer name and registration number | `Organization.name` + `Organization.invoice_number` |
| 2 | 課税資産の譲渡等を行った年月日 | Transaction date | `Transaction.completed_at` |
| 3 | 課税資産の譲渡等に係る資産又は役務の内容 | Description of goods/services (note reduced-rate items) | `TransactionItem.product_name` + `※` for reduced-rate |
| 4a | 税率ごとに区分した課税資産の譲渡等の対価の額（税込） | Tax-inclusive amount per rate **OR** | `TaxSummary.taxable_amount + tax_amount` |
| 4b | 税率ごとに区分した消費税額等 | Tax amount per rate | `TaxSummary.tax_amount` |
| 5 | 税率ごとに区分した消費税額等又は適用税率 | Applied tax rate or tax amount per rate | `TaxSummary.tax_rate` |

### Receipt Layout Example

```
================================================
        テスト株式会社
     東京都渋谷区テスト町1-2-3
     TEL: 03-1234-5678
     登録番号: T1234567890123
================================================
2026-03-18 14:30:00          レジ1番
担当: 田中太郎
------------------------------------------------
 おにぎり（鮭）    ※      ¥150 x 2    ¥300
 ペットボトル水    ※      ¥120 x 1    ¥120
 ボールペン               ¥220 x 1    ¥220
------------------------------------------------
 小計                              ¥640
------------------------------------------------
 ※8%対象         ¥420     (税 ¥31)
 10%対象          ¥220     (税 ¥20)
------------------------------------------------
 合計(税込)                        ¥691
 (内消費税等                        ¥51)
------------------------------------------------
 現金                              ¥700
 お釣り                              ¥9
================================================
 ※は軽減税率(8%)対象商品です
================================================
```

### Key Implementation Notes

1. **Snapshot at transaction time**: Product name, unit price, and tax rate are snapshotted into `TransactionItem` when the transaction is finalized. Subsequent changes to master data do not affect past receipts.

2. **Reduced-rate marker (※)**: The `is_reduced_tax` flag on `TransactionItem` determines whether the `※` symbol is printed. This complies with the requirement to distinguish reduced-rate items.

3. **Tax calculation**: Tax is calculated per item using the formula:
   ```
   tax_amount = floor(unit_price * quantity * tax_rate)
   ```
   Fractional amounts are truncated (切り捨て) per standard Japanese business practice.

4. **Electronic journal (電子ジャーナル)**: All transactions are recorded in `pos_schema.journal_entries` for audit purposes, accessible via `PosService.ListJournalEntries`.

## Tax Rate Changes

When a tax rate changes (e.g., future rate adjustments):

1. Create a new `TaxRate` record with the new rate
2. Update products to reference the new tax rate
3. Historical transactions retain the snapshotted rate at the time of sale
4. The `TaxRate.is_reduced` flag determines the display classification

The system supports the `UpdateTaxRate` RPC for rate management. Tax rate scheduling (future effective dates) is available via the `tax_rate_schedules` table (product-service V6 migration).

## Compliance Checklist

- [x] Registration number storage (`Organization.invoice_number`)
- [x] Registration number validation (`T` + 13 digits)
- [x] Tax rate master management (standard 10% / reduced 8%)
- [x] Per-item tax rate tracking with snapshot
- [x] Reduced-rate item identification (`is_reduced_tax` flag)
- [x] Tax summary per rate on receipts (`TaxSummary`)
- [x] Receipt includes all 5 required fields for Simplified Qualified Invoice
- [x] Electronic journal for audit trail
- [x] Historical integrity (snapshots prevent retroactive changes)

## References

- [National Tax Agency: Qualified Invoice System](https://www.nta.go.jp/taxes/shiraberu/zeimokubetsu/shohi/keigenzeiritsu/invoice_about.htm)
- [Consumption Tax Act, Article 57-4](https://elaws.e-gov.go.jp/document?lawid=363AC0000000108)
- [Invoice Q&A (国税庁)](https://www.nta.go.jp/taxes/shiraberu/zeimokubetsu/shohi/keigenzeiritsu/qa_invoice.htm)

## 返品時の税率処理ルール

### 原則
返品時は**購入時の税率スナップショット**を使用して相殺する。

### 理由
- インボイス制度では適用税率の正確性が求められる
- 税率改定後に旧税率で購入した商品を返品する場合、旧税率で相殺するのが会計上正しい
- TransactionItem に tax_rate / is_reduced_tax がスナップショットとして保存済み

### 実装指針
1. RETURN 型トランザクション作成時、元取引の TransactionItem から tax_rate をコピー
2. 返品取引の tax_summaries は元取引のスナップショットを使用
3. 現在の商品マスタ税率ではなく、元取引記録の税率を参照

### 例
- 2026-01-01: 軽減税率8%で商品500円を販売 → tax 40円
- 2026-06-01: 税率改定で当該商品が10%に変更
- 2026-07-01: 返品 → **8%（購入時の税率）で相殺** → -tax 40円
