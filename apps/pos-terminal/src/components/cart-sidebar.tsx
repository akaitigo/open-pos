import { useState } from 'react'
import { CartPanel } from '@/components/cart-panel'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { ShoppingCart } from 'lucide-react'
import { getCartItemCount, useCartStore } from '@/stores/cart-store'

export function CartSidebar() {
  const [sheetOpen, setSheetOpen] = useState(false)
  const itemCount = useCartStore((state) => getCartItemCount(state.items))

  return (
    <>
      {/* デスクトップ: 固定サイドバー */}
      <CartPanel className="hidden w-[360px] shrink-0 flex-col border-l bg-muted/30 lg:flex" />

      {/* タブレット: フローティングボタン + シートドロワー */}
      <div className="fixed bottom-4 right-4 z-40 lg:hidden">
        <Button
          size="lg"
          className="h-14 gap-2 rounded-full px-6 shadow-lg"
          onClick={() => setSheetOpen(true)}
        >
          <ShoppingCart className="h-5 w-5" />
          カート
          {itemCount > 0 && (
            <Badge variant="secondary" className="ml-1">
              {itemCount}
            </Badge>
          )}
        </Button>
      </div>

      <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
        <SheetContent side="right" className="flex w-full flex-col p-0 sm:max-w-md">
          <SheetHeader className="sr-only">
            <SheetTitle>カート</SheetTitle>
            <SheetDescription>カートの中身を確認</SheetDescription>
          </SheetHeader>
          <CartPanel className="flex flex-1 flex-col overflow-hidden" />
        </SheetContent>
      </Sheet>
    </>
  )
}
