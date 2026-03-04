import { Outlet } from 'react-router'

import { Header } from '@/components/header'
import { Toaster } from '@/components/ui/toast'

export function Layout() {
  return (
    <div className="flex min-h-svh flex-col">
      <Header />
      <main className="flex flex-1 flex-col">
        <Outlet />
      </main>
      <Toaster />
    </div>
  )
}
