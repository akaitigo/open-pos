import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Staff } from '@shared-types/openpos'

/** セッションタイムアウト: 30分（ミリ秒） */
const SESSION_TIMEOUT_MS = 30 * 60 * 1000

interface AuthState {
  isAuthenticated: boolean
  staff: Staff | null
  storeId: string | null
  storeName: string | null
  terminalId: string | null
  /** セッション開始時刻（Unix ms） */
  sessionStartedAt: number | null
  /** 最後のユーザー操作時刻（Unix ms） */
  lastActivityAt: number | null
  login: (
    staff: Staff,
    storeId: string,
    storeName: string,
    terminalId: string,
    token?: string,
  ) => void
  logout: () => void
  /** ユーザー操作があったことを記録する */
  touch: () => void
  /** アイドルタイムアウトを超えているか判定する */
  isSessionExpired: () => boolean
  /** セッショントークン（メモリのみ、persist 対象外） */
  _sessionToken: string | null
  getSessionToken: () => string | null
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      isAuthenticated: false,
      staff: null,
      storeId: null,
      storeName: null,
      terminalId: null,
      sessionStartedAt: null,
      lastActivityAt: null,
      _sessionToken: null,
      login: (staff, storeId, storeName, terminalId, token) => {
        const now = Date.now()
        set({
          isAuthenticated: true,
          staff,
          storeId,
          storeName,
          terminalId,
          sessionStartedAt: now,
          lastActivityAt: now,
          _sessionToken: token ?? null,
        })
      },
      logout: () =>
        set({
          isAuthenticated: false,
          staff: null,
          storeId: null,
          storeName: null,
          terminalId: null,
          sessionStartedAt: null,
          lastActivityAt: null,
          _sessionToken: null,
        }),
      touch: () => set({ lastActivityAt: Date.now() }),
      isSessionExpired: () => {
        const { lastActivityAt, isAuthenticated } = get()
        if (!isAuthenticated || lastActivityAt === null) return false
        return Date.now() - lastActivityAt > SESSION_TIMEOUT_MS
      },
      getSessionToken: () => get()._sessionToken,
    }),
    {
      name: 'openpos-auth',
      // トークンは localStorage に保存しない（セキュリティ要件）
      partialize: (state) => ({
        isAuthenticated: state.isAuthenticated,
        staff: state.staff,
        storeId: state.storeId,
        storeName: state.storeName,
        terminalId: state.terminalId,
        sessionStartedAt: state.sessionStartedAt,
        lastActivityAt: state.lastActivityAt,
      }),
    },
  ),
)

export { SESSION_TIMEOUT_MS }
