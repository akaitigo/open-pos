import { BrowserRouter, Routes, Route } from 'react-router'

import { Layout } from '@/routes/layout'
import { DashboardPage } from '@/routes/dashboard'
import { ProductsPage } from '@/routes/products'
import { CategoriesPage } from '@/routes/categories'
import { StoresPage } from '@/routes/stores'
import { StaffPage } from '@/routes/staff'
import { SettingsPage } from '@/routes/settings'
import { CustomersPage } from '@/routes/customers'
import { NotificationsPage } from '@/routes/notifications'

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
          <Route path="customers" element={<CustomersPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
