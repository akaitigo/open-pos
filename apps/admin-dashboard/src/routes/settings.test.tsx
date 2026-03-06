import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { SettingsPage } from './settings'

function renderWithProviders() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <SettingsPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('SettingsPage', () => {
  it('設定ヘッダーを表示する', () => {
    renderWithProviders()
    expect(screen.getByText('設定')).toBeInTheDocument()
  })

  it('説明テキストを表示する', () => {
    renderWithProviders()
    expect(screen.getByText('システム設定を管理します')).toBeInTheDocument()
  })
})
