import { useEffect, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import { t } from '@/i18n'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { toast } from '@/hooks/use-toast'
import { useAuthStore } from '@/stores/auth-store'
import { useCartStore } from '@/stores/cart-store'
import type { Product, Stock } from '@shared-types/openpos'
import { BarcodeScannerDialog } from './products/barcode-scanner-dialog'
import {
  filterProducts,
  getChildCategories,
  getTopLevelCategories,
  indexCategoriesById,
} from './products/catalog'
import { OpenPriceDialog } from './products/open-price-dialog'
import { ProductGrid } from './products/product-grid'
import { useProductCatalog } from './products/use-product-catalog'

const PRODUCT_PAGE_SIZE = 24

export function ProductsPage() {
  const storeId = useAuthStore((s) => s.storeId)
  const [selectedParentCategory, setSelectedParentCategory] = useState('all')
  const [selectedChildCategory, setSelectedChildCategory] = useState('all')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [scannerOpen, setScannerOpen] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const {
    products,
    categories,
    stocksByProductId,
    loading: catalogLoading,
    error: catalogError,
  } = useProductCatalog(storeId, reloadKey)

  useEffect(() => {
    if (
      selectedParentCategory !== 'all' &&
      !categories.some(
        (category) => category.id === selectedParentCategory && category.parentId === null,
      )
    ) {
      setSelectedParentCategory('all')
    }
  }, [categories, selectedParentCategory])

  const childCategories = getChildCategories(categories, selectedParentCategory)

  useEffect(() => {
    if (
      selectedChildCategory !== 'all' &&
      !childCategories.some((category) => category.id === selectedChildCategory)
    ) {
      setSelectedChildCategory('all')
    }
  }, [childCategories, selectedChildCategory])

  const categoryById = indexCategoriesById(categories)
  const filteredProducts = filterProducts(
    products,
    categories,
    selectedParentCategory,
    selectedChildCategory,
    search,
  )
  const totalPages =
    filteredProducts.length === 0 ? 0 : Math.ceil(filteredProducts.length / PRODUCT_PAGE_SIZE)
  const currentPage = totalPages === 0 ? 1 : Math.min(page, totalPages)
  const pagedProducts = filteredProducts.slice(
    (currentPage - 1) * PRODUCT_PAGE_SIZE,
    currentPage * PRODUCT_PAGE_SIZE,
  )

  useEffect(() => {
    if (totalPages > 0 && page > totalPages) {
      setPage(totalPages)
    }
  }, [page, totalPages])

  function handleParentCategoryChange(value: string) {
    setSelectedParentCategory(value)
    setSelectedChildCategory('all')
    setPage(1)
  }

  function handleChildCategoryChange(value: string) {
    setSelectedChildCategory(value)
    setPage(1)
  }

  function handleBarcodeScanned(barcode: string) {
    setScannerOpen(false)
    setSearch(barcode)
    setPage(1)
  }

  const [openPriceProduct, setOpenPriceProduct] = useState<Product | null>(null)
  const [openPriceDialogOpen, setOpenPriceDialogOpen] = useState(false)

  function handleAddToCart(product: Product, stock?: Stock) {
    if (stock && stock.quantity <= 0) {
      toast({
        title: `${product.name} は在庫切れです`,
        description: '在庫が補充されるまでカートに追加できません。',
        variant: 'destructive',
      })
      return
    }

    if (product.price === 0) {
      setOpenPriceProduct(product)
      setOpenPriceDialogOpen(true)
      return
    }

    useCartStore.getState().addItem(product)
    toast({ title: `${product.name} をカートに追加しました` })
  }

  function handleOpenPriceSubmit(priceInSen: number) {
    if (!openPriceProduct) return
    const productWithPrice: Product = { ...openPriceProduct, price: priceInSen }
    useCartStore.getState().addItem(productWithPrice)
    toast({ title: `${openPriceProduct.name} をカートに追加しました` })
    setOpenPriceDialogOpen(false)
    setOpenPriceProduct(null)
  }

  const topLevelCategories = getTopLevelCategories(categories)

  return (
    <div className="flex flex-1 flex-col gap-4 p-4">
      <div className="flex flex-wrap items-center gap-2">
        <Input
          data-testid="product-search-input"
          placeholder="商品名・バーコードで検索..."
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(1)
          }}
          className="min-w-[260px] flex-1"
          aria-label={t('accessibility.productSearch')}
        />
        <Button
          variant="outline"
          className="min-h-11 min-w-11"
          onClick={() => setScannerOpen(true)}
          aria-label={t('accessibility.openBarcodeScanner')}
        >
          スキャン
        </Button>
        <Button
          variant="outline"
          className="min-h-11 min-w-11"
          onClick={() => setReloadKey((value) => value + 1)}
          aria-label={t('accessibility.reloadProducts')}
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          再読込
        </Button>
      </div>

      {topLevelCategories.length > 0 && (
        <Tabs value={selectedParentCategory} onValueChange={handleParentCategoryChange}>
          <TabsList className="flex h-auto flex-wrap gap-1">
            <TabsTrigger value="all">すべて</TabsTrigger>
            {topLevelCategories.map((category) => (
              <TabsTrigger key={category.id} value={category.id}>
                {category.name}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>
      )}

      {childCategories.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted-foreground">
            サブカテゴリ
          </p>
          <Tabs value={selectedChildCategory} onValueChange={handleChildCategoryChange}>
            <TabsList className="flex h-auto flex-wrap gap-1">
              <TabsTrigger value="all">すべて</TabsTrigger>
              {childCategories.map((category) => (
                <TabsTrigger key={category.id} value={category.id}>
                  {category.name}
                </TabsTrigger>
              ))}
            </TabsList>
          </Tabs>
        </div>
      )}

      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <span>{filteredProducts.length} 件</span>
        {(selectedParentCategory !== 'all' || selectedChildCategory !== 'all' || search) && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setSelectedParentCategory('all')
              setSelectedChildCategory('all')
              setSearch('')
              setPage(1)
            }}
            aria-label={t('accessibility.clearFilters')}
          >
            フィルタ解除
          </Button>
        )}
      </div>

      <ProductGrid
        loading={catalogLoading}
        error={catalogError}
        products={pagedProducts}
        categoryById={categoryById}
        stocksByProductId={stocksByProductId}
        currentPage={currentPage}
        totalPages={totalPages}
        onRetry={() => setReloadKey((value) => value + 1)}
        onAddToCart={handleAddToCart}
        onPageChange={setPage}
      />

      <BarcodeScannerDialog
        open={scannerOpen}
        onOpenChange={setScannerOpen}
        onScanned={handleBarcodeScanned}
      />

      <OpenPriceDialog
        open={openPriceDialogOpen}
        onOpenChange={setOpenPriceDialogOpen}
        productName={openPriceProduct?.name ?? ''}
        onSubmit={handleOpenPriceSubmit}
      />
    </div>
  )
}
