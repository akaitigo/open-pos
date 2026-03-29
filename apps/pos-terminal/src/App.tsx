import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router'

import { ErrorBoundary } from '@/components/error-boundary'
import { Layout } from '@/routes/layout'
import { ProductsPage } from '@/routes/products'

const HistoryPage = lazy(() => import('@/routes/history').then((m) => ({ default: m.HistoryPage })))
const CartPage = lazy(() => import('@/routes/cart').then((m) => ({ default: m.CartPage })))
const CallbackPage = lazy(() =>
  import('@/routes/callback').then((m) => ({ default: m.CallbackPage })),
)

function RouteLoading() {
  return (
    <div className="flex flex-1 items-center justify-center p-8">
      <div className="text-sm text-muted-foreground">読み込み中...</div>
    </div>
  )
}

export function App() {
  return (
    <ErrorBoundary>
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
            <Route
              path="callback"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <CallbackPage />
                </Suspense>
              }
            />
            <Route
              path="*"
              element={
                <div className="flex flex-1 flex-col items-center justify-center gap-4 p-8">
                  <h1 className="text-2xl font-bold">ページが見つかりません</h1>
                  <p className="text-muted-foreground">指定されたURLは存在しません。</p>
                </div>
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  )
}
