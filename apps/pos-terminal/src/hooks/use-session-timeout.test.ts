import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { useAuthStore, SESSION_TIMEOUT_MS } from '@/stores/auth-store'
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

describe('auth-store session management', () => {
  beforeEach(() => {
    vi.useFakeTimers()
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

  afterEach(() => {
    vi.useRealTimers()
  })

  it('login 時にセッション開始時刻と最後の操作時刻が設定される', () => {
    // Arrange
    const now = Date.now()
    vi.setSystemTime(now)

    // Act
    useAuthStore.getState().login(mockStaff, 'store-1', '渋谷本店', 'terminal-1')

    // Assert
    const state = useAuthStore.getState()
    expect(state.sessionStartedAt).toBe(now)
    expect(state.lastActivityAt).toBe(now)
  })

  it('touch で lastActivityAt が更新される', () => {
    // Arrange
    const loginTime = Date.now()
    vi.setSystemTime(loginTime)
    useAuthStore.getState().login(mockStaff, 'store-1', '渋谷本店', 'terminal-1')

    // Act
    const laterTime = loginTime + 5 * 60 * 1000
    vi.setSystemTime(laterTime)
    useAuthStore.getState().touch()

    // Assert
    const state = useAuthStore.getState()
    expect(state.lastActivityAt).toBe(laterTime)
    expect(state.sessionStartedAt).toBe(loginTime) // 変わらない
  })

  it('30分以内は期限切れにならない', () => {
    // Arrange
    const loginTime = Date.now()
    vi.setSystemTime(loginTime)
    useAuthStore.getState().login(mockStaff, 'store-1', '渋谷本店', 'terminal-1')

    // Act — 29分経過
    vi.setSystemTime(loginTime + 29 * 60 * 1000)

    // Assert
    expect(useAuthStore.getState().isSessionExpired()).toBe(false)
  })

  it('30分経過後は期限切れになる', () => {
    // Arrange
    const loginTime = Date.now()
    vi.setSystemTime(loginTime)
    useAuthStore.getState().login(mockStaff, 'store-1', '渋谷本店', 'terminal-1')

    // Act — 31分経過
    vi.setSystemTime(loginTime + 31 * 60 * 1000)

    // Assert
    expect(useAuthStore.getState().isSessionExpired()).toBe(true)
  })

  it('touch 後にタイムアウトがリセットされる', () => {
    // Arrange
    const loginTime = Date.now()
    vi.setSystemTime(loginTime)
    useAuthStore.getState().login(mockStaff, 'store-1', '渋谷本店', 'terminal-1')

    // 20分後に touch
    vi.setSystemTime(loginTime + 20 * 60 * 1000)
    useAuthStore.getState().touch()

    // login から 31分後（touch から 11分後）
    vi.setSystemTime(loginTime + 31 * 60 * 1000)

    // Assert — touch から30分は経過していない
    expect(useAuthStore.getState().isSessionExpired()).toBe(false)
  })

  it('未認証状態ではセッション期限切れにならない', () => {
    // Arrange — 未ログイン状態

    // Act & Assert
    expect(useAuthStore.getState().isSessionExpired()).toBe(false)
  })

  it('logout 時にセッション情報がクリアされる', () => {
    // Arrange
    useAuthStore.getState().login(mockStaff, 'store-1', '渋谷本店', 'terminal-1')

    // Act
    useAuthStore.getState().logout()

    // Assert
    const state = useAuthStore.getState()
    expect(state.sessionStartedAt).toBeNull()
    expect(state.lastActivityAt).toBeNull()
    expect(state.isAuthenticated).toBe(false)
  })

  it('SESSION_TIMEOUT_MS は 30分', () => {
    expect(SESSION_TIMEOUT_MS).toBe(30 * 60 * 1000)
  })
})
