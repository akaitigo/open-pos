import { useCallback, useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router'
import { Header } from '@/components/header'
import { CartSidebar } from '@/components/cart-sidebar'
import { TrainingModeBanner } from '@/components/training-mode-banner'
import { LoginScreen } from '@/components/login-screen'
import { SetupScreen } from '@/components/setup-screen'
import { StoreTerminalSelector } from '@/components/store-terminal-selector'
import { Toaster } from '@/components/ui/toast'
import { getRuntimeConfig, updateStoreTerminal } from '@/lib/runtime-config'
import { setupAutoSync } from '@/lib/sync-manager'
import { useAuthStore } from '@/stores/auth-store'
import { useAccessibilityStore } from '@/stores/accessibility-store'
import { useKeyboardNav } from '@/hooks/use-keyboard-nav'
import { useOnlineStatus } from '@/hooks/use-online-status'

export function Layout() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const location = useLocation()
  const showSidebar = location.pathname !== '/cart'
  const [isTraining, setIsTraining] = useState(false)
  const isOnline = useOnlineStatus()

  // Initialize accessibility settings on mount
  useEffect(() => {
    useAccessibilityStore.getState().initialize()
  }, [])

  // Setup auto-sync for offline transactions on mount
  useEffect(() => {
    const cleanup = setupAutoSync()
    return cleanup
  }, [])

  // Keyboard navigation (#153)
  const handleSearch = useCallback(() => {
    const searchInput = document.querySelector<HTMLInputElement>('input[placeholder*="検索"]')
    searchInput?.focus()
  }, [])

  useKeyboardNav({
    onSearch: handleSearch,
  })

  // 店舗/端末の動的選択後に再レンダーするためのキー
  const [configVersion, setConfigVersion] = useState(0)
  const config = getRuntimeConfig()
  const hasOrgButNoStore = Boolean(config.organizationId) && (!config.storeId || !config.terminalId)

  if (!config.organizationId) {
    return (
      <>
        <SetupScreen />
        <Toaster />
      </>
    )
  }

  if (hasOrgButNoStore) {
    return (
      <>
        <StoreTerminalSelector
          onSelect={(storeId, _storeName, terminalId) => {
            updateStoreTerminal(storeId, terminalId)
            setConfigVersion((v) => v + 1)
          }}
        />
        <Toaster />
      </>
    )
  }

  if (!isAuthenticated) {
    return (
      <>
        <LoginScreen key={configVersion} />
        <Toaster />
      </>
    )
  }

  return (
    <div className="flex min-h-svh flex-col">
      <TrainingModeBanner isTraining={isTraining} />
      <Header
        isTraining={isTraining}
        isOnline={isOnline}
        onToggleTraining={() => setIsTraining((prev) => !prev)}
      />
      {!isOnline && (
        <div
          className="flex items-center justify-center gap-2 bg-amber-500 px-4 py-1.5 text-sm font-medium text-white"
          role="status"
          data-testid="offline-banner"
        >
          <span className="inline-block h-2 w-2 rounded-full bg-white/80" aria-hidden="true" />
          オフラインモード — 取引はローカルに保存され、オンライン復帰時に自動同期されます
        </div>
      )}
      <div className="flex flex-1 overflow-hidden">
        <main className="flex flex-1 flex-col overflow-auto">
          <Outlet />
        </main>
        {showSidebar && <CartSidebar />}
      </div>
      <Toaster />
    </div>
  )
}
