import { z } from 'zod'

/** 組織スキーマ（テナントルート） */
export const OrganizationSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  businessType: z.string(),
  invoiceNumber: z.string().nullable(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Organization = z.infer<typeof OrganizationSchema>

export const CreateOrganizationRequestSchema = z.object({
  name: z.string().min(1),
  businessType: z.string().min(1),
  invoiceNumber: z.string().optional(),
})

export type CreateOrganizationRequest = z.infer<typeof CreateOrganizationRequestSchema>

export const UpdateOrganizationRequestSchema = z.object({
  name: z.string().min(1).optional(),
  businessType: z.string().optional(),
  invoiceNumber: z.string().nullable().optional(),
})

export type UpdateOrganizationRequest = z.infer<typeof UpdateOrganizationRequestSchema>

/** 店舗スキーマ */
export const StoreSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  name: z.string(),
  address: z.string().nullable(),
  phone: z.string().nullable(),
  timezone: z.string(),
  settings: z.string(),
  isActive: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Store = z.infer<typeof StoreSchema>

export const CreateStoreRequestSchema = z.object({
  name: z.string().min(1),
  address: z.string().optional(),
  phone: z.string().optional(),
  timezone: z.string().optional(),
  settings: z.string().optional(),
})

export type CreateStoreRequest = z.infer<typeof CreateStoreRequestSchema>

export const UpdateStoreRequestSchema = z.object({
  name: z.string().min(1).optional(),
  address: z.string().nullable().optional(),
  phone: z.string().nullable().optional(),
  timezone: z.string().optional(),
  settings: z.string().optional(),
  isActive: z.boolean().optional(),
})

export type UpdateStoreRequest = z.infer<typeof UpdateStoreRequestSchema>

/** 端末スキーマ */
export const TerminalSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  storeId: z.string().uuid(),
  terminalCode: z.string(),
  name: z.string().nullable(),
  isActive: z.boolean(),
  lastSyncAt: z.string().nullable(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Terminal = z.infer<typeof TerminalSchema>

export const RegisterTerminalRequestSchema = z.object({
  storeId: z.string().uuid(),
  terminalCode: z.string().min(1),
  name: z.string().min(1),
})

export type RegisterTerminalRequest = z.infer<typeof RegisterTerminalRequestSchema>

export const StaffRole = {
  OWNER: 'OWNER',
  MANAGER: 'MANAGER',
  CASHIER: 'CASHIER',
} as const

export type StaffRole = (typeof StaffRole)[keyof typeof StaffRole]

/** スタッフスキーマ */
export const StaffSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  storeId: z.string().uuid(),
  name: z.string(),
  email: z.string().nullable(),
  role: z.enum(['OWNER', 'MANAGER', 'CASHIER']),
  isActive: z.boolean(),
  failedPinAttempts: z.number().int(),
  isLocked: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export type Staff = z.infer<typeof StaffSchema>

export const CreateStaffRequestSchema = z.object({
  storeId: z.string().uuid(),
  name: z.string().min(1),
  email: z.string().email(),
  role: z.enum(['OWNER', 'MANAGER', 'CASHIER']),
  pin: z.string().min(4).max(8).regex(/^\d+$/),
})

export type CreateStaffRequest = z.infer<typeof CreateStaffRequestSchema>

export const UpdateStaffRequestSchema = z.object({
  name: z.string().min(1).optional(),
  email: z.string().email().optional(),
  role: z.enum(['OWNER', 'MANAGER', 'CASHIER']).optional(),
  pin: z.string().min(4).max(8).regex(/^\d+$/).optional(),
  isActive: z.boolean().optional(),
})

export type UpdateStaffRequest = z.infer<typeof UpdateStaffRequestSchema>

export const AuthenticateByPinRequestSchema = z.object({
  storeId: z.string().uuid(),
  pin: z.string().min(4).max(8),
})

export type AuthenticateByPinRequest = z.infer<typeof AuthenticateByPinRequestSchema>

export const AuthenticateByPinResponseSchema = z.object({
  success: z.boolean(),
  staff: StaffSchema.nullish(),
  reason: z.string().nullish(),
})

export type AuthenticateByPinResponse = z.infer<typeof AuthenticateByPinResponseSchema>
