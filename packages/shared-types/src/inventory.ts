import { z } from 'zod'

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
