import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router'

import { ErrorBoundary } from '@/components/error-boundary'
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
const ActivityLogsPage = lazy(() =>
  import('@/routes/activity-logs').then((m) => ({ default: m.ActivityLogsPage })),
)
const OnboardingPage = lazy(() =>
  import('@/routes/onboarding').then((m) => ({ default: m.OnboardingPage })),
)
const CustomersPage = lazy(() =>
  import('@/routes/customers').then((m) => ({ default: m.CustomersPage })),
)
const NotificationsPage = lazy(() =>
  import('@/routes/notifications').then((m) => ({ default: m.NotificationsPage })),
)
const TaxRatesPage = lazy(() =>
  import('@/routes/tax-rates').then((m) => ({ default: m.TaxRatesPage })),
)
const OrganizationPage = lazy(() =>
  import('@/routes/organization').then((m) => ({ default: m.OrganizationPage })),
)
const DiscountsPage = lazy(() =>
  import('@/routes/discounts').then((m) => ({ default: m.DiscountsPage })),
)
const ReportsPage = lazy(() => import('@/routes/reports').then((m) => ({ default: m.ReportsPage })))
const ExportPage = lazy(() => import('@/routes/export').then((m) => ({ default: m.ExportPage })))
const TerminalsPage = lazy(() =>
  import('@/routes/terminals').then((m) => ({ default: m.TerminalsPage })),
)
const CallbackPage = lazy(() =>
  import('@/routes/callback').then((m) => ({ default: m.CallbackPage })),
)
const NotFoundPage = lazy(() =>
  import('@/routes/not-found').then((m) => ({ default: m.NotFoundPage })),
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
          <Route
            path="callback"
            element={
              <Suspense fallback={<RouteLoading />}>
                <CallbackPage />
              </Suspense>
            }
          />
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
            <Route
              path="activity-logs"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <ActivityLogsPage />
                </Suspense>
              }
            />
            <Route
              path="onboarding"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <OnboardingPage />
                </Suspense>
              }
            />
            <Route
              path="customers"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <CustomersPage />
                </Suspense>
              }
            />
            <Route
              path="notifications"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <NotificationsPage />
                </Suspense>
              }
            />
            <Route
              path="tax-rates"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <TaxRatesPage />
                </Suspense>
              }
            />
            <Route
              path="organization"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <OrganizationPage />
                </Suspense>
              }
            />
            <Route
              path="discounts"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <DiscountsPage />
                </Suspense>
              }
            />
            <Route
              path="reports"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <ReportsPage />
                </Suspense>
              }
            />
            <Route
              path="export"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <ExportPage />
                </Suspense>
              }
            />
            <Route
              path="terminals"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <TerminalsPage />
                </Suspense>
              }
            />
            <Route
              path="*"
              element={
                <Suspense fallback={<RouteLoading />}>
                  <NotFoundPage />
                </Suspense>
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  )
}
