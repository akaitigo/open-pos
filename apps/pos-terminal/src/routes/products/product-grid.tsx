import { RefreshCw } from 'lucide-react'
import { t } from '@/i18n'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { formatMoney } from '@shared-types/openpos'
import type { Category, Product, Stock } from '@shared-types/openpos'

interface ProductGridProps {
  loading: boolean
  error: string | null
  products: Product[]
  categoryById: Record<string, Category>
  stocksByProductId: Record<string, Stock>
  currentPage: number
  totalPages: number
  onRetry: () => void
  onAddToCart: (product: Product, stock?: Stock) => void
  onPageChange: (page: number) => void
}

export function ProductGrid({
  loading,
  error,
  products,
  categoryById,
  stocksByProductId,
  currentPage,
  totalPages,
  onRetry,
  onAddToCart,
  onPageChange,
}: ProductGridProps) {
  if (loading) {
    return (
      <div
        data-testid="product-grid"
        className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6"
      >
        {Array.from({ length: 12 }).map((_, index) => (
          <Card key={index} className="space-y-3 p-3">
            <div className="aspect-square animate-pulse rounded-md bg-muted" />
            <div className="space-y-2">
              <div className="h-4 animate-pulse rounded bg-muted" />
              <div className="h-4 w-2/3 animate-pulse rounded bg-muted" />
            </div>
          </Card>
        ))}
      </div>
    )
  }

  if (error) {
    return (
      <Card className="flex min-h-[240px] flex-col items-center justify-center gap-3 p-6 text-center">
        <p className="text-lg font-semibold">商品カタログを読み込めませんでした</p>
        <p className="max-w-md text-sm text-muted-foreground">{error}</p>
        <Button onClick={onRetry} aria-label={t('accessibility.retryAction')}>
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          再試行
        </Button>
      </Card>
    )
  }

  if (products.length === 0) {
    return (
      <div className="flex min-h-[320px] flex-1 items-center justify-center text-muted-foreground">
        商品が見つかりません
      </div>
    )
  }

  return (
    <>
      <div
        data-testid="product-grid"
        className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6"
      >
        {products.map((product) => (
          <ProductGridCard
            key={product.id}
            product={product}
            category={product.categoryId ? categoryById[product.categoryId] : undefined}
            stock={stocksByProductId[product.id]}
            onAddToCart={onAddToCart}
          />
        ))}
      </div>

      {totalPages > 1 && (
        <nav
          className="flex items-center justify-center gap-2"
          aria-label={t('accessibility.pageInfo', { current: currentPage, total: totalPages })}
        >
          <Button
            variant="outline"
            size="sm"
            disabled={currentPage <= 1}
            onClick={() => onPageChange(currentPage - 1)}
            aria-label={t('accessibility.previousPage')}
          >
            前へ
          </Button>
          <span className="text-sm text-muted-foreground" aria-current="page">
            {currentPage} / {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={currentPage >= totalPages}
            onClick={() => onPageChange(currentPage + 1)}
            aria-label={t('accessibility.nextPage')}
          >
            次へ
          </Button>
        </nav>
      )}
    </>
  )
}

interface ProductGridCardProps {
  product: Product
  category?: Category
  stock?: Stock
  onAddToCart: (product: Product, stock?: Stock) => void
}

function ProductGridCard({ product, category, stock, onAddToCart }: ProductGridCardProps) {
  const isSoldOut = stock ? stock.quantity <= 0 : false
  const isLowStock = stock ? stock.quantity > 0 && stock.quantity <= stock.lowStockThreshold : false

  return (
    <Card
      data-testid={`product-card-${product.id}`}
      className={`min-h-11 min-w-11 overflow-hidden p-3 transition-colors ${
        isSoldOut
          ? 'cursor-not-allowed border-dashed opacity-55'
          : 'cursor-pointer hover:border-primary/60 hover:bg-accent'
      }`}
      role="button"
      tabIndex={0}
      aria-disabled={isSoldOut}
      aria-label={
        isSoldOut
          ? t('accessibility.addToCartSoldOut', {
              name: product.name,
              price: formatMoney(product.price),
            })
          : t('accessibility.addToCart', {
              name: product.name,
              price: formatMoney(product.price),
            })
      }
      onClick={() => onAddToCart(product, stock)}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          onAddToCart(product, stock)
        }
      }}
    >
      <div
        className="mb-3 h-1.5 rounded-full bg-muted"
        style={category?.color ? { backgroundColor: category.color } : undefined}
      />
      {product.imageUrl ? (
        <div className="mb-3 aspect-square overflow-hidden rounded-md bg-muted">
          <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover" />
        </div>
      ) : (
        <div className="mb-3 flex aspect-square items-center justify-center rounded-md bg-muted text-3xl font-semibold text-muted-foreground">
          {product.name.charAt(0)}
        </div>
      )}
      <div className="space-y-2">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <p className="line-clamp-2 text-sm font-medium leading-tight">{product.name}</p>
          {isSoldOut ? (
            <Badge variant="destructive" className="shrink-0">
              在庫切れ
            </Badge>
          ) : isLowStock ? (
            <Badge variant="secondary" className="shrink-0">
              残り {stock?.quantity ?? 0}
            </Badge>
          ) : null}
        </div>
        {category && (
          <Badge variant="outline" className="w-fit">
            {category.name}
          </Badge>
        )}
        <p className="text-sm font-bold text-primary">{formatMoney(product.price)}</p>
        {product.barcode && (
          <Badge variant="outline" className="max-w-full text-[10px]">
            {product.barcode}
          </Badge>
        )}
      </div>
    </Card>
  )
}
