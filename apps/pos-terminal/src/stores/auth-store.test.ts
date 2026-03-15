import { describe, it, expect, beforeEach } from 'vitest'
import { useAuthStore } from './auth-store'
import type { Staff } from '@shared-types/openpos'

const mockStaff: Staff = {
  id: '550e8400-e29b-41d4-a716-446655440010',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  storeId: '550e8400-e29b-41d4-a716-446655440020',
  name: '田中太郎',
  email: 'tanaka@example.com',
  role: 'MANAGER',
  isActive: true,
  failedPinAttempts: 0,
  isLocked: false,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('auth-store', () => {
  beforeEach(() => {
    useAuthStore.setState({
      isAuthenticated: false,
      staff: null,
      storeId: null,
      storeName: null,
      terminalId: null,
      sessionStartedAt: null,
      lastActivityAt: null,
    })
  })

  it('初期状態は未認証', () => {
    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(false)
    expect(state.staff).toBeNull()
    expect(state.storeId).toBeNull()
  })

  it('login で認証状態になる', () => {
    useAuthStore.getState().login(mockStaff, 'store-1', '渋谷本店', 'terminal-1')
    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(true)
    expect(state.staff).toEqual(mockStaff)
    expect(state.storeId).toBe('store-1')
    expect(state.storeName).toBe('渋谷本店')
    expect(state.terminalId).toBe('terminal-1')
  })

  it('logout で未認証状態に戻る', () => {
    useAuthStore.getState().login(mockStaff, 'store-1', '渋谷本店', 'terminal-1')
    useAuthStore.getState().logout()
    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(false)
    expect(state.staff).toBeNull()
    expect(state.storeId).toBeNull()
    expect(state.storeName).toBeNull()
    expect(state.terminalId).toBeNull()
  })
})
