import { describe, it, expect } from 'vitest'
import {
  PlanSchema,
  SubscriptionSchema,
  AuditLogSchema,
  FavoriteProductSchema,
  ReservationSchema,
  WebhookSchema,
  ProductAlertSchema,
  SalesTargetSchema,
  DiscountReasonSchema,
  TaxRateScheduleSchema,
  StampCardSchema,
} from './enterprise'

describe('Enterprise Schemas', () => {
  it('should validate PlanSchema', () => {
    const plan = PlanSchema.parse({
      id: 'plan-1',
      name: 'スターター',
      maxStores: 3,
      maxTerminals: 10,
      maxProducts: 1000,
      monthlyPrice: 298000,
      isActive: true,
    })
    expect(plan.name).toBe('スターター')
    expect(plan.monthlyPrice).toBe(298000)
  })

  it('should validate SubscriptionSchema', () => {
    const sub = SubscriptionSchema.parse({
      id: 'sub-1',
      organizationId: 'org-1',
      planId: 'plan-1',
      status: 'ACTIVE',
      startDate: '2026-03-15T00:00:00Z',
      endDate: null,
    })
    expect(sub.status).toBe('ACTIVE')
  })

  it('should validate AuditLogSchema', () => {
    const log = AuditLogSchema.parse({
      id: 'log-1',
      organizationId: 'org-1',
      staffId: 'staff-1',
      action: 'CREATE',
      entityType: 'PRODUCT',
      entityId: 'prod-1',
      createdAt: '2026-03-15T10:00:00Z',
    })
    expect(log.action).toBe('CREATE')
  })

  it('should validate FavoriteProductSchema', () => {
    const fav = FavoriteProductSchema.parse({
      id: 'fav-1',
      staffId: 'staff-1',
      productId: 'prod-1',
      sortOrder: 0,
    })
    expect(fav.staffId).toBe('staff-1')
  })

  it('should validate ReservationSchema', () => {
    const res = ReservationSchema.parse({
      id: 'res-1',
      storeId: 'store-1',
      customerName: 'テスト太郎',
      items: [{ productId: 'prod-1', quantity: 2 }],
      reservedUntil: '2026-03-16T00:00:00Z',
      status: 'RESERVED',
      createdAt: '2026-03-15T10:00:00Z',
    })
    expect(res.status).toBe('RESERVED')
    expect(res.items).toHaveLength(1)
  })

  it('should validate WebhookSchema', () => {
    const webhook = WebhookSchema.parse({
      id: 'wh-1',
      url: 'https://example.com/webhook',
      events: ['sale.completed'],
      isActive: true,
      createdAt: '2026-03-15T10:00:00Z',
    })
    expect(webhook.events).toContain('sale.completed')
  })

  it('should validate ProductAlertSchema', () => {
    const alert = ProductAlertSchema.parse({
      id: 'alert-1',
      productId: 'prod-1',
      alertType: 'TRENDING',
      description: '売上急上昇',
      isRead: false,
      createdAt: '2026-03-15T10:00:00Z',
    })
    expect(alert.alertType).toBe('TRENDING')
  })

  it('should validate SalesTargetSchema', () => {
    const target = SalesTargetSchema.parse({
      id: 'target-1',
      storeId: 'store-1',
      targetMonth: '2026-03',
      targetAmount: 10000000,
    })
    expect(target.targetAmount).toBe(10000000)
  })

  it('should validate DiscountReasonSchema', () => {
    const reason = DiscountReasonSchema.parse({
      id: 'reason-1',
      code: 'DAMAGE',
      description: '商品損傷',
      isActive: true,
    })
    expect(reason.code).toBe('DAMAGE')
  })

  it('should validate TaxRateScheduleSchema', () => {
    const schedule = TaxRateScheduleSchema.parse({
      id: 'sched-1',
      taxRateId: 'tax-1',
      newRate: '0.1200',
      effectiveDate: '2027-04-01',
      applied: false,
    })
    expect(schedule.applied).toBe(false)
  })

  it('should validate StampCardSchema', () => {
    const card = StampCardSchema.parse({
      customerId: 'cust-1',
      totalStamps: 5,
      rewardThreshold: 10,
      isRewardAvailable: false,
    })
    expect(card.totalStamps).toBe(5)
  })
})
