import { describe, it, expect } from "vitest";
import { MoneySchema, formatMoney } from "./money";

describe("MoneySchema", () => {
  it("有効な金額オブジェクトをパースできる", () => {
    const result = MoneySchema.parse({ amount: 10000, currency: "JPY" });

    expect(result.amount).toBe(10000);
    expect(result.currency).toBe("JPY");
  });

  it("currency のデフォルト値が JPY になる", () => {
    const result = MoneySchema.parse({ amount: 5000 });

    expect(result.currency).toBe("JPY");
  });

  it("amount が整数でない場合はバリデーションエラー", () => {
    expect(() => MoneySchema.parse({ amount: 100.5 })).toThrow();
  });

  it("amount が欠けている場合はバリデーションエラー", () => {
    expect(() => MoneySchema.parse({ currency: "JPY" })).toThrow();
  });
});

describe("formatMoney", () => {
  it("銭単位の金額を日本円表示に変換できる（10000 = 100円）", () => {
    const result = formatMoney(10000);

    // Intl.NumberFormat はロケールにより ¥ (U+00A5) または ￥ (U+FFE5) を返す
    expect(result).toMatch(/[¥￥]100$/);
  });

  it("0 を円記号付き 0 に変換できる", () => {
    const result = formatMoney(0);

    expect(result).toMatch(/[¥￥]0$/);
  });

  it("大きな金額をカンマ区切りで表示できる", () => {
    const result = formatMoney(10000000);

    expect(result).toMatch(/[¥￥]100,000$/);
  });

  it("負の金額を変換できる", () => {
    const result = formatMoney(-5000);

    expect(result).toMatch(/-[¥￥]50$/);
  });
});
