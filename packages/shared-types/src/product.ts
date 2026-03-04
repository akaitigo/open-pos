import { z } from 'zod'

/** 商品スキーマ（price は銭単位: 10000 = 100円） */
export const ProductSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  description: z.string().optional(),
  barcode: z.string().optional(),
  sku: z.string().optional(),
  price: z.number().int().nonnegative(),
  categoryId: z.string().uuid().optional(),
  taxRateId: z.string().uuid().optional(),
  imageUrl: z.string().optional(),
  displayOrder: z.number().int(),
  isActive: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Product = z.infer<typeof ProductSchema>

export const CreateProductRequestSchema = z.object({
  name: z.string().min(1),
  description: z.string().optional(),
  barcode: z.string().optional(),
  sku: z.string().optional(),
  price: z.number().int().nonnegative(),
  categoryId: z.string().uuid().optional(),
  taxRateId: z.string().uuid().optional(),
  imageUrl: z.string().optional(),
  displayOrder: z.number().int().optional(),
})

export type CreateProductRequest = z.infer<typeof CreateProductRequestSchema>

export const UpdateProductRequestSchema = z.object({
  name: z.string().min(1).optional(),
  description: z.string().optional(),
  barcode: z.string().optional(),
  sku: z.string().optional(),
  price: z.number().int().nonnegative().optional(),
  categoryId: z.string().uuid().optional(),
  taxRateId: z.string().uuid().optional(),
  imageUrl: z.string().optional(),
  displayOrder: z.number().int().optional(),
  isActive: z.boolean().optional(),
})

export type UpdateProductRequest = z.infer<typeof UpdateProductRequestSchema>

/** カテゴリスキーマ（階層構造: parentId で親子関係） */
export const CategorySchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  parentId: z.string().uuid().nullable(),
  color: z.string().nullable(),
  icon: z.string().nullable(),
  displayOrder: z.number().int(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Category = z.infer<typeof CategorySchema>

export const CreateCategoryRequestSchema = z.object({
  name: z.string().min(1),
  parentId: z.string().uuid().optional(),
  color: z.string().optional(),
  icon: z.string().optional(),
  displayOrder: z.number().int().optional(),
})

export type CreateCategoryRequest = z.infer<typeof CreateCategoryRequestSchema>

export const UpdateCategoryRequestSchema = z.object({
  name: z.string().min(1).optional(),
  parentId: z.string().uuid().nullable().optional(),
  color: z.string().nullable().optional(),
  icon: z.string().nullable().optional(),
  displayOrder: z.number().int().optional(),
})

export type UpdateCategoryRequest = z.infer<typeof UpdateCategoryRequestSchema>

/** 税率スキーマ（rate は小数: 0.10 = 10%） */
export const TaxRateSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  rate: z.string(),
  isReduced: z.boolean(),
  isDefault: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type TaxRate = z.infer<typeof TaxRateSchema>

export const CreateTaxRateRequestSchema = z.object({
  name: z.string().min(1),
  rate: z.string(),
  isReduced: z.boolean().optional(),
  isDefault: z.boolean().optional(),
})

export type CreateTaxRateRequest = z.infer<typeof CreateTaxRateRequestSchema>

export const UpdateTaxRateRequestSchema = z.object({
  name: z.string().min(1).optional(),
  rate: z.string().optional(),
  isReduced: z.boolean().optional(),
  isDefault: z.boolean().optional(),
})

export type UpdateTaxRateRequest = z.infer<typeof UpdateTaxRateRequestSchema>

export const DiscountType = {
  PERCENTAGE: 'PERCENTAGE',
  FIXED_AMOUNT: 'FIXED_AMOUNT',
} as const

export type DiscountType = (typeof DiscountType)[keyof typeof DiscountType]

/** 割引スキーマ（value は銭単位 or パーセント） */
export const DiscountSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  discountType: z.enum(['PERCENTAGE', 'FIXED_AMOUNT']),
  value: z.string(),
  startDate: z.string().nullable(),
  endDate: z.string().nullable(),
  isActive: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Discount = z.infer<typeof DiscountSchema>

export const CreateDiscountRequestSchema = z.object({
  name: z.string().min(1),
  discountType: z.enum(['PERCENTAGE', 'FIXED_AMOUNT']),
  value: z.string(),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
})

export type CreateDiscountRequest = z.infer<typeof CreateDiscountRequestSchema>

export const UpdateDiscountRequestSchema = z.object({
  name: z.string().min(1).optional(),
  discountType: z.enum(['PERCENTAGE', 'FIXED_AMOUNT']).optional(),
  value: z.string().optional(),
  startDate: z.string().nullable().optional(),
  endDate: z.string().nullable().optional(),
  isActive: z.boolean().optional(),
})

export type UpdateDiscountRequest = z.infer<typeof UpdateDiscountRequestSchema>

/** クーポンスキーマ */
export const CouponSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  code: z.string(),
  discountId: z.string().uuid(),
  maxUses: z.number().int().nullable(),
  currentUses: z.number().int(),
  startDate: z.string().nullable(),
  endDate: z.string().nullable(),
  isActive: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Coupon = z.infer<typeof CouponSchema>

export const CreateCouponRequestSchema = z.object({
  code: z.string().min(1),
  discountId: z.string().uuid(),
  maxUses: z.number().int().nonnegative().optional(),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
})

export type CreateCouponRequest = z.infer<typeof CreateCouponRequestSchema>

export const ValidateCouponResponseSchema = z.object({
  isValid: z.boolean(),
  coupon: CouponSchema.nullable(),
  discount: DiscountSchema.nullable(),
  reason: z.string().nullable(),
})

export type ValidateCouponResponse = z.infer<typeof ValidateCouponResponseSchema>
