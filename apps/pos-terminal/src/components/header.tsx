import { Link, useLocation } from 'react-router'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { getCartItemCount, useCartStore } from '@/stores/cart-store'
import { useAuthStore } from '@/stores/auth-store'
import { DarkModeToggle } from '@/components/dark-mode-toggle'
import { History, LogOut, ShoppingCart, Wifi, WifiOff } from 'lucide-react'
import { t } from '@/i18n'

interface HeaderProps {
  isTraining?: boolean
  isOnline?: boolean
  onToggleTraining?: () => void
}

export function Header({ isTraining, isOnline = true, onToggleTraining }: HeaderProps) {
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
        {!isOnline && (
          <Badge
            variant="outline"
            className="gap-1 border-amber-400 text-xs text-amber-600 dark:text-amber-400"
            data-testid="offline-indicator"
          >
            <WifiOff className="h-3 w-3" aria-hidden="true" />
            オフライン
          </Badge>
        )}
        {isOnline && (
          <Wifi
            className="h-3.5 w-3.5 text-green-500"
            aria-label={t('accessibility.onlineStatus')}
            role="img"
            data-testid="online-indicator"
          />
        )}
        {isTraining && (
          <Badge variant="destructive" className="text-xs">
            TRAINING
          </Badge>
        )}
      </div>
      <nav className="flex items-center gap-2" aria-label={t('accessibility.mainNavigation')}>
        {staff && <span className="text-sm text-muted-foreground">{staff.name}</span>}
        {onToggleTraining && (
          <Button
            variant={isTraining ? 'destructive' : 'outline'}
            size="sm"
            onClick={onToggleTraining}
            aria-label={t('accessibility.trainingModeToggle')}
          >
            {isTraining ? 'トレーニング中' : 'トレーニング'}
          </Button>
        )}
        <DarkModeToggle />
        <Button variant={location.pathname === '/cart' ? 'secondary' : 'ghost'} size="sm" asChild>
          <Link
            to="/cart"
            aria-label={
              itemCount > 0
                ? t('accessibility.cartWithCount', { count: itemCount })
                : t('accessibility.cart')
            }
          >
            <ShoppingCart className="h-4 w-4" aria-hidden="true" />
            {t('header.cart')}
            {itemCount > 0 && <span className="text-xs tabular-nums">{itemCount}</span>}
          </Link>
        </Button>
        <Button
          variant={location.pathname === '/history' ? 'secondary' : 'ghost'}
          size="sm"
          asChild
        >
          <Link to="/history" aria-label={t('accessibility.historyLink')}>
            <History className="h-4 w-4" aria-hidden="true" />
            {t('header.history')}
          </Link>
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={logout}
          aria-label={t('accessibility.logoutButton')}
        >
          <LogOut className="h-4 w-4" aria-hidden="true" />
          {t('header.logout')}
        </Button>
      </nav>
    </header>
  )
}
