import { Link, useLocation } from 'react-router'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/stores/auth-store'
import { History, LogOut } from 'lucide-react'

export function Header() {
  const { staff, storeName, logout } = useAuthStore()
  const location = useLocation()

  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b px-4">
      <div className="flex items-center gap-3">
        <Link to="/" className="text-lg font-semibold">
          OpenPOS
        </Link>
        {storeName && (
          <Badge variant="outline" className="text-xs">
            {storeName}
          </Badge>
        )}
      </div>
      <div className="flex items-center gap-2">
        {staff && <span className="text-sm text-muted-foreground">{staff.name}</span>}
        <Button
          variant={location.pathname === '/history' ? 'secondary' : 'ghost'}
          size="sm"
          asChild
        >
          <Link to="/history">
            <History className="h-4 w-4" />
            履歴
          </Link>
        </Button>
        <Button variant="ghost" size="sm" onClick={logout}>
          <LogOut className="h-4 w-4" />
          退出
        </Button>
      </div>
    </header>
  )
}
