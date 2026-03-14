import { Outlet, useLocation } from 'react-router'
import { Header } from '@/components/header'
import { CartSidebar } from '@/components/cart-sidebar'
import { LoginScreen } from '@/components/login-screen'
import { SetupScreen } from '@/components/setup-screen'
import { Toaster } from '@/components/ui/toast'
import { hasPosRuntimeConfig } from '@/lib/runtime-config'
import { useAuthStore } from '@/stores/auth-store'

export function Layout() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const location = useLocation()
  const showSidebar = location.pathname !== '/cart'

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
      <Header />
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
