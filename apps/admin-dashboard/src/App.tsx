import { BrowserRouter, Routes, Route } from 'react-router'

import { ErrorBoundary } from '@/components/error-boundary'
import { Layout } from '@/routes/layout'
import { DashboardPage } from '@/routes/dashboard'
import { ProductsPage } from '@/routes/products'
import { CategoriesPage } from '@/routes/categories'
import { TaxRatesPage } from '@/routes/tax-rates'
import { StoresPage } from '@/routes/stores'
import { StaffPage } from '@/routes/staff'
import { SettingsPage } from '@/routes/settings'
import { NotFoundPage } from '@/routes/not-found'

export function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<DashboardPage />} />
            <Route path="products" element={<ProductsPage />} />
            <Route path="categories" element={<CategoriesPage />} />
            <Route path="tax-rates" element={<TaxRatesPage />} />
            <Route path="stores" element={<StoresPage />} />
            <Route path="staff" element={<StaffPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  )
}
