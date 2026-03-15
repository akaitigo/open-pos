import { z } from 'zod'

// --- #176 プラン・課金管理 ---

export const PlanSchema = z.object({
  id: z.string(),
  name: z.string(),
  maxStores: z.number().int(),
  maxTerminals: z.number().int(),
  maxProducts: z.number().int(),
  monthlyPrice: z.number().int(),
  isActive: z.boolean(),
})

export type Plan = z.infer<typeof PlanSchema>

export const SubscriptionSchema = z.object({
  id: z.string(),
  organizationId: z.string(),
  planId: z.string(),
  status: z.string(),
  startDate: z.string(),
  endDate: z.string().nullable().optional(),
})

export type Subscription = z.infer<typeof SubscriptionSchema>

// --- #187 スタッフ活動ログ ---

export const AuditLogSchema = z.object({
  id: z.string(),
  organizationId: z.string(),
  staffId: z.string().nullable().optional(),
  action: z.string(),
  entityType: z.string(),
  entityId: z.string().nullable().optional(),
  details: z.string().optional(),
  ipAddress: z.string().nullable().optional(),
  createdAt: z.string(),
})

export type AuditLog = z.infer<typeof AuditLogSchema>

// --- #188 お気に入り商品 ---

export const FavoriteProductSchema = z.object({
  id: z.string(),
  staffId: z.string(),
  productId: z.string(),
  sortOrder: z.number().int(),
})

export type FavoriteProduct = z.infer<typeof FavoriteProductSchema>

// --- #193 予約注文 ---

export const ReservationItemSchema = z.object({
  productId: z.string(),
  quantity: z.number().int(),
})

export type ReservationItem = z.infer<typeof ReservationItemSchema>

export const ReservationSchema = z.object({
  id: z.string(),
  storeId: z.string(),
  customerName: z.string().nullable().optional(),
  customerPhone: z.string().nullable().optional(),
  items: z.array(ReservationItemSchema),
  reservedUntil: z.string(),
  status: z.string(),
  note: z.string().nullable().optional(),
  createdAt: z.string(),
})

export type Reservation = z.infer<typeof ReservationSchema>

// --- #200 テーブルオーダー ---

export const TableStatus = {
  OPEN: 'OPEN',
  OCCUPIED: 'OCCUPIED',
  BILL_REQUESTED: 'BILL_REQUESTED',
} as const

export type TableStatus = (typeof TableStatus)[keyof typeof TableStatus]

// --- #203 Webhook ---

export const WebhookSchema = z.object({
  id: z.string(),
  url: z.string(),
  events: z.array(z.string()),
  isActive: z.boolean(),
  createdAt: z.string(),
})

export type Webhook = z.infer<typeof WebhookSchema>

// --- #207 商品アラート ---

export const ProductAlertSchema = z.object({
  id: z.string(),
  productId: z.string(),
  alertType: z.string(),
  description: z.string(),
  isRead: z.boolean(),
  createdAt: z.string(),
})

export type ProductAlert = z.infer<typeof ProductAlertSchema>

// --- #214 売上目標 ---

export const SalesTargetSchema = z.object({
  id: z.string(),
  storeId: z.string().nullable().optional(),
  targetMonth: z.string(),
  targetAmount: z.number().int(),
})

export type SalesTarget = z.infer<typeof SalesTargetSchema>

// --- #216 値引き理由コード ---

export const DiscountReasonSchema = z.object({
  id: z.string(),
  code: z.string(),
  description: z.string(),
  isActive: z.boolean(),
})

export type DiscountReason = z.infer<typeof DiscountReasonSchema>

// --- #219 税率変更スケジュール ---

export const TaxRateScheduleSchema = z.object({
  id: z.string(),
  taxRateId: z.string(),
  newRate: z.string(),
  effectiveDate: z.string(),
  applied: z.boolean(),
})

export type TaxRateSchedule = z.infer<typeof TaxRateScheduleSchema>

// --- #222 スタンプカード ---

export const StampCardSchema = z.object({
  customerId: z.string(),
  totalStamps: z.number().int(),
  rewardThreshold: z.number().int(),
  isRewardAvailable: z.boolean(),
})

export type StampCard = z.infer<typeof StampCardSchema>
