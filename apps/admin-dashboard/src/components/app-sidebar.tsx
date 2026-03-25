import { NavLink } from 'react-router'
import {
  LayoutDashboard,
  Package,
  FolderTree,
  Store,
  Users,
  Settings,
  Warehouse,
  ClipboardList,
  ScrollText,
  UserRound,
  Bell,
  Receipt,
} from 'lucide-react'

import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
} from '@/components/ui/sidebar'

const navItems = [
  { title: 'ダッシュボード', url: '/', icon: LayoutDashboard },
  { title: '商品管理', url: '/products', icon: Package },
  { title: 'カテゴリ管理', url: '/categories', icon: FolderTree },
  { title: '在庫管理', url: '/inventory', icon: Warehouse },
  { title: '発注管理', url: '/purchase-orders', icon: ClipboardList },
  { title: '店舗管理', url: '/stores', icon: Store },
  { title: 'スタッフ管理', url: '/staff', icon: Users },
  { title: '顧客管理', url: '/customers', icon: UserRound },
  { title: '税率管理', url: '/tax-rates', icon: Receipt },
  { title: '操作履歴', url: '/activity-logs', icon: ScrollText },
  { title: '通知', url: '/notifications', icon: Bell },
  { title: '設定', url: '/settings', icon: Settings },
]

export function AppSidebar() {
  return (
    <Sidebar>
      <SidebarHeader>
        <div className="flex items-center gap-2 px-2 py-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Package className="h-4 w-4" />
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-semibold">OpenPOS</span>
            <span className="text-xs text-muted-foreground">管理ダッシュボード</span>
          </div>
        </div>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>メニュー</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {navItems.map((item) => (
                <SidebarMenuItem key={item.title}>
                  <SidebarMenuButton asChild tooltip={item.title}>
                    <NavLink
                      to={item.url}
                      end={item.url === '/'}
                      className={({ isActive }) => (isActive ? 'data-[active=true]' : '')}
                    >
                      {({ isActive }) => (
                        <>
                          <item.icon className={isActive ? 'text-sidebar-primary' : ''} />
                          <span>{item.title}</span>
                        </>
                      )}
                    </NavLink>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
      <SidebarRail />
    </Sidebar>
  )
}
