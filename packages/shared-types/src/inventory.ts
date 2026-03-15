import { z } from 'zod'
import { createPaginatedResponseSchema } from './api-types'

export const StockSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  storeId: z.string().uuid(),
  productId: z.string().uuid(),
  quantity: z.number().int(),
  lowStockThreshold: z.number().int(),
  updatedAt: z.string(),
})

export type Stock = z.infer<typeof StockSchema>

export const StockMovementSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  storeId: z.string().uuid(),
  productId: z.string().uuid(),
  movementType: z.string(),
  quantity: z.number().int(),
  referenceId: z.string().nullable().optional(),
  note: z.string().nullable().optional(),
  createdAt: z.string(),
})

export type StockMovement = z.infer<typeof StockMovementSchema>

export const PurchaseOrderItemSchema = z.object({
  id: z.string().uuid(),
  productId: z.string().uuid(),
  orderedQuantity: z.number().int(),
  receivedQuantity: z.number().int(),
  unitCost: z.number().int(),
})

export type PurchaseOrderItem = z.infer<typeof PurchaseOrderItemSchema>

export const PurchaseOrderSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  storeId: z.string().uuid(),
  status: z.string(),
  items: z.array(PurchaseOrderItemSchema),
  supplierName: z.string(),
  note: z.string().nullable().optional(),
  orderedAt: z.string().nullable().optional(),
  receivedAt: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type PurchaseOrder = z.infer<typeof PurchaseOrderSchema>

export const PaginatedStockMovementsSchema = createPaginatedResponseSchema(StockMovementSchema)
export const PaginatedPurchaseOrdersSchema = createPaginatedResponseSchema(PurchaseOrderSchema)
