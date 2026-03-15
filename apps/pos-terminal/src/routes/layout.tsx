import { useCallback, useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router'
import { Header } from '@/components/header'
import { CartSidebar } from '@/components/cart-sidebar'
import { TrainingModeBanner } from '@/components/training-mode-banner'
import { LoginScreen } from '@/components/login-screen'
import { SetupScreen } from '@/components/setup-screen'
import { Toaster } from '@/components/ui/toast'
import { hasPosRuntimeConfig } from '@/lib/runtime-config'
import { useAuthStore } from '@/stores/auth-store'
import { useAccessibilityStore } from '@/stores/accessibility-store'
import { useKeyboardNav } from '@/hooks/use-keyboard-nav'

export function Layout() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const location = useLocation()
  const showSidebar = location.pathname !== '/cart'
  const [isTraining, setIsTraining] = useState(false)

  // Initialize accessibility settings on mount
  useEffect(() => {
    useAccessibilityStore.getState().initialize()
  }, [])

  // Keyboard navigation (#153)
  const handleSearch = useCallback(() => {
    const searchInput = document.querySelector<HTMLInputElement>('input[placeholder*="検索"]')
    searchInput?.focus()
  }, [])

  useKeyboardNav({
    onSearch: handleSearch,
  })

  if (!hasPosRuntimeConfig()) {
    return (
      <>
        <SetupScreen />
        <Toaster />
      </>
    )
  }

  if (!isAuthenticated) {
    return (
      <>
        <LoginScreen />
        <Toaster />
      </>
    )
  }

  return (
    <div className="flex min-h-svh flex-col">
      <TrainingModeBanner isTraining={isTraining} />
      <Header isTraining={isTraining} onToggleTraining={() => setIsTraining((prev) => !prev)} />
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
