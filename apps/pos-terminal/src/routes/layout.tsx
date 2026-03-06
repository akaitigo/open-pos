import { Outlet } from 'react-router'
import { Header } from '@/components/header'
import { CartSidebar } from '@/components/cart-sidebar'
import { LoginScreen } from '@/components/login-screen'
import { Toaster } from '@/components/ui/toast'
import { useAuthStore } from '@/stores/auth-store'

export function Layout() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

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
        <CartSidebar />
      </div>
      <Toaster />
    </div>
  )
}
