import { Outlet } from 'react-router'

import { SidebarInset, SidebarProvider } from '@/components/ui/sidebar'
import { AppSidebar } from '@/components/app-sidebar'
import { SetupScreen } from '@/components/setup-screen'
import { Toaster } from '@/components/ui/toast'
import { hasOrganizationContext } from '@/lib/runtime-config'

export function Layout() {
  if (!hasOrganizationContext()) {
    return <SetupScreen />
  }

  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        <Outlet />
      </SidebarInset>
      <Toaster />
    </SidebarProvider>
  )
}
