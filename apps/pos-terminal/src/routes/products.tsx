import { useEffect, useRef, useState } from 'react'
import { Html5Qrcode } from 'html5-qrcode'
import { z } from 'zod'
import { RefreshCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { api } from '@/lib/api'
import { toast } from '@/hooks/use-toast'
import { useAuthStore } from '@/stores/auth-store'
import { useCartStore } from '@/stores/cart-store'
import {
  CategorySchema,
  formatMoney,
  PaginatedProductsSchema,
  PaginatedStocksSchema,
} from '@shared-types/openpos'
import type { Category, PaginatedResponse, Product, Stock } from '@shared-types/openpos'

const PRODUCT_PAGE_SIZE = 24
const CATALOG_FETCH_PAGE_SIZE = 100

export function ProductsPage() {
  const storeId = useAuthStore((s) => s.storeId)
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [stocksByProductId, setStocksByProductId] = useState<Record<string, Stock>>({})
  const [selectedParentCategory, setSelectedParentCategory] = useState('all')
  const [selectedChildCategory, setSelectedChildCategory] = useState('all')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [scannerOpen, setScannerOpen] = useState(false)
  const [catalogLoading, setCatalogLoading] = useState(true)
  const [catalogError, setCatalogError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false

    async function loadCatalog() {
      setCatalogLoading(true)
      setCatalogError(null)

      try {
        const [loadedCategories, loadedProducts, loadedStocks] = await Promise.all([
          api.get('/api/categories', z.array(CategorySchema)),
          fetchAllPages<Product>('/api/products', PaginatedProductsSchema, {
            activeOnly: true,
            pageSize: CATALOG_FETCH_PAGE_SIZE,
          }),
          storeId
            ? fetchAllPages<Stock>('/api/inventory/stocks', PaginatedStocksSchema, {
                storeId,
                pageSize: CATALOG_FETCH_PAGE_SIZE,
              })
            : Promise.resolve([]),
        ])

        if (cancelled) return

        setCategories(sortCategories(loadedCategories))
        setProducts(sortProducts(loadedProducts))
        setStocksByProductId(indexStocksByProductId(loadedStocks))
      } catch (error) {
        if (cancelled) return
        setCatalogError(getErrorMessage(error))
      } finally {
        if (!cancelled) {
          setCatalogLoading(false)
        }
      }
    }

    void loadCatalog()

    return () => {
      cancelled = true
    }
  }, [reloadKey, storeId])

  useEffect(() => {
    if (
      selectedParentCategory !== 'all' &&
      !categories.some((category) => category.id === selectedParentCategory && category.parentId === null)
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

  function handleAddToCart(product: Product, stock?: Stock) {
    if (stock && stock.quantity <= 0) {
      toast({
        title: `${product.name} は在庫切れです`,
        description: '在庫が補充されるまでカートに追加できません。',
        variant: 'destructive',
      })
      return
    }

    useCartStore.getState().addItem(product)
    toast({ title: `${product.name} をカートに追加しました` })
  }

  const topLevelCategories = getTopLevelCategories(categories)

  return (
    <div className="flex flex-1 flex-col gap-4 p-4">
      <div className="flex flex-wrap items-center gap-2">
        <Input
          placeholder="商品名・バーコードで検索..."
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(1)
          }}
          className="min-w-[260px] flex-1"
        />
        <Button variant="outline" onClick={() => setScannerOpen(true)}>
          スキャン
        </Button>
        <Button variant="outline" onClick={() => setReloadKey((value) => value + 1)}>
          <RefreshCw className="h-4 w-4" />
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
          >
            フィルタ解除
          </Button>
        )}
      </div>

      {catalogLoading ? (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4 lg:grid-cols-6">
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
      ) : catalogError ? (
        <Card className="flex min-h-[240px] flex-col items-center justify-center gap-3 p-6 text-center">
          <p className="text-lg font-semibold">商品カタログを読み込めませんでした</p>
          <p className="max-w-md text-sm text-muted-foreground">{catalogError}</p>
          <Button onClick={() => setReloadKey((value) => value + 1)}>
            <RefreshCw className="h-4 w-4" />
            再試行
          </Button>
        </Card>
      ) : pagedProducts.length === 0 ? (
        <div className="flex min-h-[320px] flex-1 items-center justify-center text-muted-foreground">
          商品が見つかりません
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4 lg:grid-cols-6">
          {pagedProducts.map((product) => {
            const category = product.categoryId ? categoryById[product.categoryId] : undefined
            const stock = stocksByProductId[product.id]
            const isSoldOut = stock ? stock.quantity <= 0 : false
            const isLowStock = stock ? stock.quantity > 0 && stock.quantity <= stock.lowStockThreshold : false

            return (
              <Card
                key={product.id}
                className={`overflow-hidden p-3 transition-colors ${
                  isSoldOut
                    ? 'cursor-not-allowed border-dashed opacity-55'
                    : 'cursor-pointer hover:border-primary/60 hover:bg-accent'
                }`}
                aria-disabled={isSoldOut}
                onClick={() => handleAddToCart(product, stock)}
              >
                <div
                  className="mb-3 h-1.5 rounded-full bg-muted"
                  style={category?.color ? { backgroundColor: category.color } : undefined}
                />
                {product.imageUrl ? (
                  <div className="mb-3 aspect-square overflow-hidden rounded-md bg-muted">
                    <img
                      src={product.imageUrl}
                      alt={product.name}
                      className="h-full w-full object-cover"
                    />
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
          })}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={currentPage <= 1}
            onClick={() => setPage((value) => value - 1)}
          >
            前へ
          </Button>
          <span className="text-sm text-muted-foreground">
            {currentPage} / {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={currentPage >= totalPages}
            onClick={() => setPage((value) => value + 1)}
          >
            次へ
          </Button>
        </div>
      )}

      <BarcodeScannerDialog
        open={scannerOpen}
        onOpenChange={setScannerOpen}
        onScanned={handleBarcodeScanned}
      />
    </div>
  )
}

function BarcodeScannerDialog({
  open,
  onOpenChange,
  onScanned,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  onScanned: (barcode: string) => void
}) {
  const scannerRef = useRef<Html5Qrcode | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [manualBarcode, setManualBarcode] = useState('')

  useEffect(() => {
    if (!open) return

    const scannerId = 'barcode-scanner'
    let scanner: Html5Qrcode | null = null

    const startScanner = async () => {
      try {
        scanner = new Html5Qrcode(scannerId)
        scannerRef.current = scanner
        await scanner.start(
          { facingMode: 'environment' },
          { fps: 10, qrbox: { width: 250, height: 150 } },
          (decodedText) => {
            onScanned(decodedText)
          },
          () => {
            // scan failure — ignore (continuous scanning)
          },
        )
      } catch {
        setError('カメラにアクセスできません。手動でバーコードを入力してください。')
      }
    }

    const timer = setTimeout(startScanner, 100)

    return () => {
      clearTimeout(timer)
      if (scanner?.isScanning) {
        scanner.stop().catch(() => {})
      }
      scannerRef.current = null
      setError(null)
    }
  }, [open, onScanned])

  function handleManualSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (manualBarcode.trim()) {
      onScanned(manualBarcode.trim())
      setManualBarcode('')
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>バーコードスキャン</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          {error ? (
            <p className="text-sm text-destructive">{error}</p>
          ) : (
            <div id="barcode-scanner" className="w-full overflow-hidden rounded-md" />
          )}

          <form onSubmit={handleManualSubmit} className="flex items-end gap-2">
            <Input
              placeholder="バーコードを手入力..."
              value={manualBarcode}
              onChange={(e) => setManualBarcode(e.target.value)}
              className="flex-1"
            />
            <Button type="submit" variant="outline">
              検索
            </Button>
          </form>
        </div>
      </DialogContent>
    </Dialog>
  )
}

async function fetchAllPages<T>(
  path: string,
  schema: z.ZodType<PaginatedResponse<T>>,
  params: Record<string, string | number | boolean>,
): Promise<T[]> {
  const items: T[] = []
  let nextPage = 1
  let totalPages = 1

  while (nextPage <= totalPages) {
    const response = await api.get(path, schema, {
      params: {
        ...params,
        page: nextPage,
      },
    })

    items.push(...response.data)
    totalPages = response.pagination.totalPages
    nextPage += 1
  }

  return items
}

function filterProducts(
  products: Product[],
  categories: Category[],
  selectedParentCategory: string,
  selectedChildCategory: string,
  search: string,
): Product[] {
  const categoryIds = getSelectedCategoryIds(categories, selectedParentCategory, selectedChildCategory)
  const normalizedSearch = search.trim().toLowerCase()

  return sortProducts(
    products.filter((product) => {
      if (categoryIds && (!product.categoryId || !categoryIds.has(product.categoryId))) {
        return false
      }

      if (!normalizedSearch) {
        return true
      }

      return [product.name, product.barcode ?? '', product.sku ?? ''].some((value) =>
        value.toLowerCase().includes(normalizedSearch),
      )
    }),
  )
}

function getSelectedCategoryIds(
  categories: Category[],
  selectedParentCategory: string,
  selectedChildCategory: string,
): Set<string> | null {
  if (selectedChildCategory !== 'all') {
    return new Set([selectedChildCategory])
  }

  if (selectedParentCategory === 'all') {
    return null
  }

  return collectDescendantCategoryIds(categories, selectedParentCategory)
}

function collectDescendantCategoryIds(categories: Category[], rootId: string): Set<string> {
  const ids = new Set<string>([rootId])
  const queue = [rootId]

  while (queue.length > 0) {
    const currentId = queue.shift()
    if (!currentId) continue

    for (const category of categories) {
      if (category.parentId === currentId && !ids.has(category.id)) {
        ids.add(category.id)
        queue.push(category.id)
      }
    }
  }

  return ids
}

function getTopLevelCategories(categories: Category[]): Category[] {
  return categories.filter((category) => category.parentId === null)
}

function getChildCategories(categories: Category[], parentId: string): Category[] {
  if (parentId === 'all') {
    return []
  }

  return categories.filter((category) => category.parentId === parentId)
}

function indexCategoriesById(categories: Category[]): Record<string, Category> {
  return categories.reduce<Record<string, Category>>((result, category) => {
    result[category.id] = category
    return result
  }, {})
}

function indexStocksByProductId(stocks: Stock[]): Record<string, Stock> {
  return stocks.reduce<Record<string, Stock>>((result, stock) => {
    result[stock.productId] = stock
    return result
  }, {})
}

function sortCategories(categories: Category[]): Category[] {
  return [...categories].sort((left, right) => {
    if (left.displayOrder !== right.displayOrder) {
      return left.displayOrder - right.displayOrder
    }
    return left.name.localeCompare(right.name, 'ja')
  })
}

function sortProducts(products: Product[]): Product[] {
  return [...products].sort((left, right) => {
    if (left.displayOrder !== right.displayOrder) {
      return left.displayOrder - right.displayOrder
    }
    return left.name.localeCompare(right.name, 'ja')
  })
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return 'ネットワークまたは API の状態を確認してください。'
}
