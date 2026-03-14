import { CartPanel } from '@/components/cart-panel'

export function CartPage() {
  return (
    <div className="flex flex-1 bg-muted/20 p-4 lg:p-6">
      <CartPanel
        fullScreen
        className="mx-auto flex w-full max-w-4xl flex-col overflow-hidden rounded-2xl border bg-background shadow-sm"
      />
    </div>
  )
}
