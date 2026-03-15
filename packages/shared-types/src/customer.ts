import { z } from 'zod'

/** 顧客スキーマ */
export const CustomerSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  email: z.string().nullable().optional(),
  phone: z.string().nullable().optional(),
  points: z.number().int(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Customer = z.infer<typeof CustomerSchema>

export const CreateCustomerRequestSchema = z.object({
  name: z.string().min(1),
  email: z.string().email().nullable().optional(),
  phone: z.string().nullable().optional(),
})

export type CreateCustomerRequest = z.infer<typeof CreateCustomerRequestSchema>

export const UpdateCustomerRequestSchema = z.object({
  name: z.string().min(1).optional(),
  email: z.string().email().nullable().optional(),
  phone: z.string().nullable().optional(),
})

export type UpdateCustomerRequest = z.infer<typeof UpdateCustomerRequestSchema>

/** ポイント取引スキーマ */
export const PointTransactionSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  customerId: z.string().uuid(),
  points: z.number().int(),
  type: z.enum(['EARN', 'REDEEM', 'ADJUST']),
  transactionId: z.string().uuid().nullable().optional(),
  note: z.string().nullable().optional(),
  createdAt: z.string(),
})

export type PointTransaction = z.infer<typeof PointTransactionSchema>

/** ギフトカードスキーマ（金額は銭単位） */
export const GiftCardSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  code: z.string(),
  balance: z.number().int(),
  initialBalance: z.number().int(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'EXHAUSTED']),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type GiftCard = z.infer<typeof GiftCardSchema>

export const CreateGiftCardRequestSchema = z.object({
  code: z.string().min(1),
  initialBalance: z.number().int().nonnegative(),
})

export type CreateGiftCardRequest = z.infer<typeof CreateGiftCardRequestSchema>

/** タイムセールスキーマ（金額は銭単位） */
export const TimeSaleSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  productId: z.string().uuid(),
  salePrice: z.number().int().nonnegative(),
  startTime: z.string(),
  endTime: z.string(),
  isActive: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type TimeSale = z.infer<typeof TimeSaleSchema>

/** 商品バリアントスキーマ */
export const ProductVariantSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  productId: z.string().uuid(),
  name: z.string(),
  sku: z.string().nullable().optional(),
  price: z.number().int().nonnegative(),
  attributes: z.string().nullable().optional(),
  isActive: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type ProductVariant = z.infer<typeof ProductVariantSchema>

/** 商品バンドルスキーマ */
export const ProductBundleSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  bundlePrice: z.number().int().nonnegative(),
  isActive: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type ProductBundle = z.infer<typeof ProductBundleSchema>

/** レシートテンプレートスキーマ */
export const ReceiptTemplateSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  header: z.string().nullable().optional(),
  footer: z.string().nullable().optional(),
  logoUrl: z.string().nullable().optional(),
  showBarcode: z.boolean(),
  isDefault: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type ReceiptTemplate = z.infer<typeof ReceiptTemplateSchema>

/** 仕入先スキーマ */
export const SupplierSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  contactPerson: z.string().nullable().optional(),
  email: z.string().nullable().optional(),
  phone: z.string().nullable().optional(),
  address: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Supplier = z.infer<typeof SupplierSchema>

/** 在庫移動スキーマ */
export const StockTransferSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  fromStoreId: z.string().uuid(),
  toStoreId: z.string().uuid(),
  items: z.string(),
  status: z.enum(['PENDING', 'IN_TRANSIT', 'COMPLETED', 'CANCELLED']),
  note: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type StockTransfer = z.infer<typeof StockTransferSchema>

/** 勤怠スキーマ */
export const AttendanceSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  staffId: z.string().uuid(),
  storeId: z.string().uuid(),
  date: z.string(),
  clockIn: z.string(),
  clockOut: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Attendance = z.infer<typeof AttendanceSchema>

/** シフトスキーマ */
export const ShiftSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  staffId: z.string().uuid(),
  storeId: z.string().uuid(),
  date: z.string(),
  startTime: z.string(),
  endTime: z.string(),
  note: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Shift = z.infer<typeof ShiftSchema>

/** 通知スキーマ */
export const NotificationSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  type: z.string(),
  title: z.string(),
  message: z.string(),
  isRead: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Notification = z.infer<typeof NotificationSchema>
