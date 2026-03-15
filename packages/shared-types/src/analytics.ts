import { z } from 'zod'

/** 日次売上スキーマ（金額は銭単位: 10000 = 100円） */
export const DailySalesSchema = z.object({
  date: z.string(),
  storeId: z.string(),
  grossAmount: z.number(),
  netAmount: z.number(),
  taxAmount: z.number(),
  discountAmount: z.number().optional().default(0),
  transactionCount: z.number().int(),
  cashAmount: z.number().optional().default(0),
  cardAmount: z.number().optional().default(0),
  qrAmount: z.number().optional().default(0),
})

export type DailySales = z.infer<typeof DailySalesSchema>

/** 売上サマリースキーマ（金額は銭単位: 10000 = 100円） */
export const SalesSummarySchema = z.object({
  totalGross: z.number(),
  totalNet: z.number(),
  totalTax: z.number(),
  totalDiscount: z.number().optional().default(0),
  totalTransactions: z.number().int(),
  averageTransaction: z.number(),
})

export type SalesSummary = z.infer<typeof SalesSummarySchema>

/** 時間帯別売上スキーマ（金額は銭単位: 10000 = 100円） */
export const HourlySalesSchema = z.object({
  hour: z.number().int(),
  amount: z.number(),
  transactionCount: z.number().int(),
})

export type HourlySales = z.infer<typeof HourlySalesSchema>
