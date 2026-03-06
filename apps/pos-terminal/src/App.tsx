import { BrowserRouter, Routes, Route } from 'react-router'

import { Layout } from '@/routes/layout'
import { ProductsPage } from '@/routes/products'
import { HistoryPage } from '@/routes/history'

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<ProductsPage />} />
          <Route path="history" element={<HistoryPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
