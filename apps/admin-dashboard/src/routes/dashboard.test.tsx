import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { DashboardPage } from './dashboard'

function renderWithProviders() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <DashboardPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('DashboardPage', () => {
  it('Coming Soon テキストを表示する', () => {
    renderWithProviders()
    expect(screen.getByText('Dashboard - Coming Soon')).toBeInTheDocument()
  })

  it('ダッシュボードヘッダーを表示する', () => {
    renderWithProviders()
    expect(screen.getByText('ダッシュボード')).toBeInTheDocument()
  })

  it('説明テキストを表示する', () => {
    renderWithProviders()
    expect(screen.getByText('売上・在庫・注文の概要がここに表示されます')).toBeInTheDocument()
  })
})
