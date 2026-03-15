import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router'

import { Layout } from '@/routes/layout'
import { ProductsPage } from '@/routes/products'

const HistoryPage = lazy(() => import('@/routes/history').then((m) => ({ default: m.HistoryPage })))
const CartPage = lazy(() => import('@/routes/cart').then((m) => ({ default: m.CartPage })))

function RouteLoading() {
  return (
    <div className="flex flex-1 items-center justify-center p-8">
      <div className="text-sm text-muted-foreground">読み込み中...</div>
    </div>
  )
}

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<ProductsPage />} />
          <Route
            path="cart"
            element={
              <Suspense fallback={<RouteLoading />}>
                <CartPage />
              </Suspense>
            }
          />
          <Route
            path="history"
            element={
              <Suspense fallback={<RouteLoading />}>
                <HistoryPage />
              </Suspense>
            }
          />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
