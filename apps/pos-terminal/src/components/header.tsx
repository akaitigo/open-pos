import { Link, useLocation } from 'react-router'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { getCartItemCount, useCartStore } from '@/stores/cart-store'
import { useAuthStore } from '@/stores/auth-store'
import { DarkModeToggle } from '@/components/dark-mode-toggle'
import { History, LogOut, ShoppingCart } from 'lucide-react'

interface HeaderProps {
  isTraining?: boolean
  onToggleTraining?: () => void
}

export function Header({ isTraining, onToggleTraining }: HeaderProps) {
  const { staff, storeName, logout } = useAuthStore()
  const location = useLocation()
  const itemCount = useCartStore((state) => getCartItemCount(state.items))

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
        {isTraining && (
          <Badge variant="destructive" className="text-xs">
            TRAINING
          </Badge>
        )}
      </div>
      <div className="flex items-center gap-2">
        {staff && <span className="text-sm text-muted-foreground">{staff.name}</span>}
        {onToggleTraining && (
          <Button
            variant={isTraining ? 'destructive' : 'outline'}
            size="sm"
            onClick={onToggleTraining}
            aria-label="トレーニングモード切替"
          >
            {isTraining ? 'トレーニング中' : 'トレーニング'}
          </Button>
        )}
        <DarkModeToggle />
        <Button variant={location.pathname === '/cart' ? 'secondary' : 'ghost'} size="sm" asChild>
          <Link to="/cart" aria-label={itemCount > 0 ? `カート ${itemCount}` : 'カート'}>
            <ShoppingCart className="h-4 w-4" />
            カート
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
