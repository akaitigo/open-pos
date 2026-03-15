import { Link, useLocation } from 'react-router'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { getCartItemCount, useCartStore } from '@/stores/cart-store'
import { useAuthStore } from '@/stores/auth-store'
import { History, LogOut, ShoppingCart } from 'lucide-react'

export function Header() {
  const { staff, storeName, logout } = useAuthStore()
  const location = useLocation()
  const itemCount = useCartStore((state) => getCartItemCount(state.items))

  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b px-3 md:px-4">
      <div className="flex items-center gap-2 md:gap-3">
        <Link to="/" className="text-base font-semibold md:text-lg">
          OpenPOS
        </Link>
        {storeName && (
          <Badge variant="outline" className="hidden text-xs sm:inline-flex">
            {storeName}
          </Badge>
        )}
      </div>
      <div className="flex items-center gap-1 md:gap-2">
        {staff && (
          <span className="hidden text-sm text-muted-foreground sm:inline">{staff.name}</span>
        )}
        <Button
          variant={location.pathname === '/cart' ? 'secondary' : 'ghost'}
          size="sm"
          asChild
          className="lg:hidden"
        >
          <Link to="/cart" aria-label={itemCount > 0 ? `カート ${itemCount}` : 'カート'}>
            <ShoppingCart className="h-4 w-4" />
            <span className="hidden sm:inline">カート</span>
            {itemCount > 0 && <span className="text-xs tabular-nums">{itemCount}</span>}
          </Link>
        </Button>
        <Button
          variant={location.pathname === '/history' ? 'secondary' : 'ghost'}
          size="sm"
          asChild
        >
          <Link to="/history">
            <History className="h-4 w-4" />
            <span className="hidden sm:inline">履歴</span>
          </Link>
        </Button>
        <Button variant="ghost" size="sm" onClick={logout}>
          <LogOut className="h-4 w-4" />
          <span className="hidden sm:inline">退出</span>
        </Button>
      </div>
    </header>
  )
}
