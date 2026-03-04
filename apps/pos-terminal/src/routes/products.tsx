import { useEffect, useRef, useState } from 'react'
import { Html5Qrcode } from 'html5-qrcode'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { api } from '@/lib/api'
import { formatMoney } from '@shared-types/openpos'
import type { Product, Category, PaginatedResponse } from '@shared-types/openpos'
import { CategorySchema, PaginatedProductsSchema } from '@shared-types/openpos'
import { z } from 'zod'

export function ProductsPage() {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [selectedCategory, setSelectedCategory] = useState<string>('all')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(0)
  const [scannerOpen, setScannerOpen] = useState(false)

  useEffect(() => {
    api.get('/api/categories', z.array(CategorySchema)).then(setCategories)
  }, [])

  useEffect(() => {
    let cancelled = false
    api
      .get<PaginatedResponse<Product>>('/api/products', PaginatedProductsSchema, {
        params: {
          page,
          pageSize: 24,
          categoryId: selectedCategory !== 'all' ? selectedCategory : undefined,
          search: search || undefined,
          activeOnly: true,
        },
      })
      .then((result) => {
        if (!cancelled) {
          setProducts(result.data)
          setTotalPages(result.pagination.totalPages)
        }
      })
    return () => {
      cancelled = true
    }
  }, [page, selectedCategory, search])

  function handleCategoryChange(value: string) {
    setSelectedCategory(value)
    setPage(1)
  }

  function handleBarcodeScanned(barcode: string) {
    setScannerOpen(false)
    setSearch(barcode)
    setPage(1)
  }

  function handleAddToCart(product: Product) {
    // TODO: Zustand store でカートに追加
    console.log('Add to cart:', product.id, product.name)
  }

  return (
    <div className="flex flex-1 flex-col gap-4 p-4">
      <div className="flex items-center gap-2">
        <Input
          placeholder="商品名・バーコードで検索..."
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(1)
          }}
          className="flex-1"
        />
        <Button variant="outline" onClick={() => setScannerOpen(true)}>
          スキャン
        </Button>
      </div>

      {categories.length > 0 && (
        <Tabs value={selectedCategory} onValueChange={handleCategoryChange}>
          <TabsList className="flex h-auto flex-wrap gap-1">
            <TabsTrigger value="all">すべて</TabsTrigger>
            {categories.map((cat) => (
              <TabsTrigger key={cat.id} value={cat.id}>
                {cat.name}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>
      )}

      <div className="grid grid-cols-3 gap-3 md:grid-cols-4 lg:grid-cols-6">
        {products.map((product) => (
          <Card
            key={product.id}
            className="cursor-pointer p-3 transition-colors hover:bg-accent"
            onClick={() => handleAddToCart(product)}
          >
            {product.imageUrl ? (
              <div className="mb-2 aspect-square overflow-hidden rounded-md bg-muted">
                <img
                  src={product.imageUrl}
                  alt={product.name}
                  className="h-full w-full object-cover"
                />
              </div>
            ) : (
              <div className="mb-2 flex aspect-square items-center justify-center rounded-md bg-muted text-2xl text-muted-foreground">
                {product.name.charAt(0)}
              </div>
            )}
            <div className="space-y-1">
              <p className="line-clamp-2 text-sm font-medium leading-tight">{product.name}</p>
              <p className="text-sm font-bold text-primary">{formatMoney(product.price)}</p>
              {product.barcode && (
                <Badge variant="outline" className="text-[10px]">
                  {product.barcode}
                </Badge>
              )}
            </div>
          </Card>
        ))}
      </div>

      {products.length === 0 && (
        <div className="flex flex-1 items-center justify-center text-muted-foreground">
          商品が見つかりません
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page <= 1}
            onClick={() => setPage((p) => p - 1)}
          >
            前へ
          </Button>
          <span className="text-sm text-muted-foreground">
            {page} / {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages}
            onClick={() => setPage((p) => p + 1)}
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

    // Small delay to ensure the DOM element is rendered
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

  function handleManualSubmit(e: React.FormEvent) {
    e.preventDefault()
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
