import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { CartItem } from './cart-store'

const MAX_PARKED_TRANSACTIONS = 5

export interface ParkedTransaction {
  id: string
  items: CartItem[]
  parkedAt: string
  label: string
}

interface ParkingState {
  parkedTransactions: ParkedTransaction[]
  parkTransaction: (items: CartItem[], label?: string) => boolean
  resumeTransaction: (id: string) => CartItem[] | null
  removeParkedTransaction: (id: string) => void
}

function generateParkId(): string {
  return `park-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export const useParkingStore = create<ParkingState>()(
  persist(
    (set, get) => ({
      parkedTransactions: [],

      parkTransaction: (items, label) => {
        const state = get()
        if (state.parkedTransactions.length >= MAX_PARKED_TRANSACTIONS) {
          return false
        }
        if (items.length === 0) {
          return false
        }

        const parked: ParkedTransaction = {
          id: generateParkId(),
          items: structuredClone(items),
          parkedAt: new Date().toISOString(),
          label: label ?? `保留 #${state.parkedTransactions.length + 1}`,
        }

        set({ parkedTransactions: [...state.parkedTransactions, parked] })
        return true
      },

      resumeTransaction: (id) => {
        const state = get()
        const target = state.parkedTransactions.find((t) => t.id === id)
        if (!target) return null

        set({
          parkedTransactions: state.parkedTransactions.filter((t) => t.id !== id),
        })
        return target.items
      },

      removeParkedTransaction: (id) => {
        set((state) => ({
          parkedTransactions: state.parkedTransactions.filter((t) => t.id !== id),
        }))
      },
    }),
    { name: 'openpos-parking' },
  ),
)
