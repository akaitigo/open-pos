import { useEffect, useCallback } from 'react'
import { useAuthStore, SESSION_TIMEOUT_MS } from '@/stores/auth-store'

/**
 * セッションタイムアウトを監視するフック。
 * - ユーザー操作（click, keydown, scroll, touchstart）で lastActivityAt を更新
 * - 定期的にアイドルタイムアウトをチェックし、超過時は自動ログアウト
 */
export function useSessionTimeout() {
  const touch = useAuthStore((s) => s.touch)
  const logout = useAuthStore((s) => s.logout)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const isSessionExpired = useAuthStore((s) => s.isSessionExpired)

  const handleActivity = useCallback(() => {
    touch()
  }, [touch])

  useEffect(() => {
    if (!isAuthenticated) return

    // ユーザー操作イベントの登録
    const events = ['click', 'keydown', 'scroll', 'touchstart'] as const
    for (const event of events) {
      window.addEventListener(event, handleActivity, { passive: true })
    }

    // 定期チェック（60秒ごと）
    const intervalId = window.setInterval(() => {
      if (isSessionExpired()) {
        logout()
      }
    }, 60_000)

    return () => {
      for (const event of events) {
        window.removeEventListener(event, handleActivity)
      }
      window.clearInterval(intervalId)
    }
  }, [isAuthenticated, handleActivity, isSessionExpired, logout])

  // 初回レンダリング時に既に期限切れなら即ログアウト
  useEffect(() => {
    if (isAuthenticated && isSessionExpired()) {
      logout()
    }
  }, [isAuthenticated, isSessionExpired, logout])
}

export { SESSION_TIMEOUT_MS }
