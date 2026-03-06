import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Staff } from '@shared-types/openpos'

interface AuthState {
  isAuthenticated: boolean
  staff: Staff | null
  storeId: string | null
  storeName: string | null
  terminalId: string | null
  login: (staff: Staff, storeId: string, storeName: string, terminalId: string) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      isAuthenticated: false,
      staff: null,
      storeId: null,
      storeName: null,
      terminalId: null,
      login: (staff, storeId, storeName, terminalId) =>
        set({ isAuthenticated: true, staff, storeId, storeName, terminalId }),
      logout: () =>
        set({
          isAuthenticated: false,
          staff: null,
          storeId: null,
          storeName: null,
          terminalId: null,
        }),
    }),
    { name: 'openpos-auth' },
  ),
)
