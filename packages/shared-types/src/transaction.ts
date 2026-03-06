import { z } from 'zod'
import { createPaginatedResponseSchema } from './api-types'

// --- トランザクション明細 ---
export const TransactionItemSchema = z.object({
  id: z.string().uuid(),
  productId: z.string().uuid(),
  productName: z.string(),
  unitPrice: z.number().int(),
  quantity: z.number().int(),
  taxRate: z.string(),
  subtotal: z.number().int(),
  taxAmount: z.number().int(),
  total: z.number().int(),
})

export type TransactionItem = z.infer<typeof TransactionItemSchema>

// --- 支払い ---
export const PaymentSchema = z.object({
  id: z.string().uuid(),
  method: z.string(),
  amount: z.number().int(),
  received: z.number().int(),
  change: z.number().int(),
  reference: z.string(),
})

export type Payment = z.infer<typeof PaymentSchema>

// --- 税率サマリー ---
export const TaxSummarySchema = z.object({
  taxRateName: z.string(),
  taxRate: z.string(),
  taxableAmount: z.number().int(),
  taxAmount: z.number().int(),
})

export type TaxSummary = z.infer<typeof TaxSummarySchema>

// --- 割引適用 ---
export const AppliedDiscountSchema = z.object({
  id: z.string().uuid(),
  discountId: z.string().uuid(),
  discountName: z.string(),
  discountType: z.string(),
  value: z.string(),
  amount: z.number().int(),
  transactionItemId: z.string().uuid(),
})

export type AppliedDiscount = z.infer<typeof AppliedDiscountSchema>

// --- トランザクション ---
export const TransactionSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  storeId: z.string().uuid(),
  terminalId: z.string().uuid(),
  staffId: z.string().uuid(),
  transactionNumber: z.string(),
  type: z.string(),
  status: z.string(),
  items: z.array(TransactionItemSchema),
  payments: z.array(PaymentSchema),
  appliedDiscounts: z.array(AppliedDiscountSchema),
  taxSummaries: z.array(TaxSummarySchema),
  subtotal: z.number().int(),
  discountTotal: z.number().int(),
  taxTotal: z.number().int(),
  total: z.number().int(),
  clientId: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Transaction = z.infer<typeof TransactionSchema>

// --- レシート ---
export const ReceiptSchema = z.object({
  id: z.string().uuid(),
  transactionId: z.string().uuid(),
  receiptNumber: z.string(),
  receiptData: z.string(),
  issuedAt: z.string(),
})

export type Receipt = z.infer<typeof ReceiptSchema>

// --- Finalize レスポンス ---
export const FinalizeTransactionResponseSchema = z.object({
  transaction: TransactionSchema,
  receipt: ReceiptSchema,
})

export type FinalizeTransactionResponse = z.infer<typeof FinalizeTransactionResponseSchema>

// --- リクエスト型 ---
export const CreateTransactionRequestSchema = z.object({
  storeId: z.string().uuid(),
  terminalId: z.string().uuid(),
  staffId: z.string().uuid(),
  type: z.string().optional(),
  clientId: z.string().optional(),
})

export type CreateTransactionRequest = z.infer<typeof CreateTransactionRequestSchema>

export const AddTransactionItemRequestSchema = z.object({
  productId: z.string().uuid(),
  quantity: z.number().int().min(1).default(1),
})

export type AddTransactionItemRequest = z.infer<typeof AddTransactionItemRequestSchema>

export const PaymentInputSchema = z.object({
  method: z.string(),
  amount: z.number().int(),
  received: z.number().int().optional(),
  reference: z.string().optional(),
})

export type PaymentInput = z.infer<typeof PaymentInputSchema>

export const FinalizeTransactionRequestSchema = z.object({
  payments: z.array(PaymentInputSchema),
})

export type FinalizeTransactionRequest = z.infer<typeof FinalizeTransactionRequestSchema>

// --- ページネーション付き ---
export const PaginatedTransactionsSchema = createPaginatedResponseSchema(TransactionSchema)
