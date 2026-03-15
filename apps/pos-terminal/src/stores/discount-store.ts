import { create } from 'zustand'
import type { Discount } from '@shared-types/openpos'

export interface AppliedCouponDiscount {
  couponCode: string
  discount: Discount
  amount: number
}

interface DiscountState {
  appliedDiscounts: AppliedCouponDiscount[]
  addDiscount: (couponCode: string, discount: Discount, subtotal: number) => void
  removeDiscount: (couponCode: string) => void
  clearDiscounts: () => void
  getTotalDiscount: () => number
}

function calculateDiscountAmount(discount: Discount, subtotal: number): number {
  if (discount.discountType === 'PERCENTAGE') {
    const rate = Number(discount.value)
    if (!Number.isFinite(rate) || rate <= 0) return 0
    return Math.floor(subtotal * rate)
  }

  if (discount.discountType === 'FIXED_AMOUNT') {
    const amount = Number.parseInt(discount.value, 10)
    if (!Number.isFinite(amount) || amount <= 0) return 0
    return Math.min(amount, subtotal)
  }

  return 0
}

export const useDiscountStore = create<DiscountState>((set, get) => ({
  appliedDiscounts: [],

  addDiscount: (couponCode, discount, subtotal) =>
    set((state) => {
      if (state.appliedDiscounts.some((d) => d.couponCode === couponCode)) {
        return state
      }
      const amount = calculateDiscountAmount(discount, subtotal)
      return {
        appliedDiscounts: [...state.appliedDiscounts, { couponCode, discount, amount }],
      }
    }),

  removeDiscount: (couponCode) =>
    set((state) => ({
      appliedDiscounts: state.appliedDiscounts.filter((d) => d.couponCode !== couponCode),
    })),

  clearDiscounts: () => set({ appliedDiscounts: [] }),

  getTotalDiscount: () => {
    return get().appliedDiscounts.reduce((sum, d) => sum + d.amount, 0)
  },
}))
