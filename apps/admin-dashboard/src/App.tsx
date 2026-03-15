import { BrowserRouter, Routes, Route } from 'react-router'

import { Layout } from '@/routes/layout'
import { DashboardPage } from '@/routes/dashboard'
import { ProductsPage } from '@/routes/products'
import { CategoriesPage } from '@/routes/categories'
import { StoresPage } from '@/routes/stores'
import { StaffPage } from '@/routes/staff'
import { SettingsPage } from '@/routes/settings'
import { OrganizationPage } from '@/routes/organization'
import { TerminalsPage } from '@/routes/terminals'
import { DiscountsPage } from '@/routes/discounts'
import { ExportPage } from '@/routes/export'
import { ReportsPage } from '@/routes/reports'

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route path="products" element={<ProductsPage />} />
          <Route path="categories" element={<CategoriesPage />} />
          <Route path="stores" element={<StoresPage />} />
          <Route path="staff" element={<StaffPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="organization" element={<OrganizationPage />} />
          <Route path="terminals" element={<TerminalsPage />} />
          <Route path="discounts" element={<DiscountsPage />} />
          <Route path="export" element={<ExportPage />} />
          <Route path="reports" element={<ReportsPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
