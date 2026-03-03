import { z } from "zod";

/** 金額スキーマ（銭単位: 10000 = 100円） */
export const MoneySchema = z.object({
  amount: z.number().int(),
  currency: z.string().default("JPY"),
});

export type Money = z.infer<typeof MoneySchema>;

/** 銭単位の金額を円表示用に変換 */
export function formatMoney(amount: number): string {
  const yen = amount / 100;
  return new Intl.NumberFormat("ja-JP", {
    style: "currency",
    currency: "JPY",
    minimumFractionDigits: 0,
  }).format(yen);
}
