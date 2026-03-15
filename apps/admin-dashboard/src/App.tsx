import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router'

import { Layout } from '@/routes/layout'
import { DashboardPage } from '@/routes/dashboard'

const ProductsPage = lazy(() =>
  import('@/routes/products').then((m) => ({ default: m.ProductsPage })),
)
const CategoriesPage = lazy(() =>
  import('@/routes/categories').then((m) => ({ default: m.CategoriesPage })),
)
const StoresPage = lazy(() => import('@/routes/stores').then((m) => ({ default: m.StoresPage })))
const StaffPage = lazy(() => import('@/routes/staff').then((m) => ({ default: m.StaffPage })))
const SettingsPage = lazy(() =>
  import('@/routes/settings').then((m) => ({ default: m.SettingsPage })),
)
const InventoryPage = lazy(() =>
  import('@/routes/inventory').then((m) => ({ default: m.InventoryPage })),
)
const PurchaseOrdersPage = lazy(() =>
  import('@/routes/purchase-orders').then((m) => ({ default: m.PurchaseOrdersPage })),
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
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route
            path="products"
            element={
              <Suspense fallback={<RouteLoading />}>
                <ProductsPage />
              </Suspense>
            }
          />
          <Route
            path="categories"
            element={
              <Suspense fallback={<RouteLoading />}>
                <CategoriesPage />
              </Suspense>
            }
          />
          <Route
            path="stores"
            element={
              <Suspense fallback={<RouteLoading />}>
                <StoresPage />
              </Suspense>
            }
          />
          <Route
            path="staff"
            element={
              <Suspense fallback={<RouteLoading />}>
                <StaffPage />
              </Suspense>
            }
          />
          <Route
            path="settings"
            element={
              <Suspense fallback={<RouteLoading />}>
                <SettingsPage />
              </Suspense>
            }
          />
          <Route
            path="inventory"
            element={
              <Suspense fallback={<RouteLoading />}>
                <InventoryPage />
              </Suspense>
            }
          />
          <Route
            path="purchase-orders"
            element={
              <Suspense fallback={<RouteLoading />}>
                <PurchaseOrdersPage />
              </Suspense>
            }
          />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
