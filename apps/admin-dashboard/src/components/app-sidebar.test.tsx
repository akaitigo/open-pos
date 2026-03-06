import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { AppSidebar } from './app-sidebar'

function renderSidebar() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <AppSidebar />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('AppSidebar', () => {
  it('OpenPOS と管理ダッシュボードを表示する', () => {
    renderSidebar()
    expect(screen.getByText('OpenPOS')).toBeInTheDocument()
    expect(screen.getByText('管理ダッシュボード')).toBeInTheDocument()
  })

  it('全6つのナビゲーション項目を表示する', () => {
    renderSidebar()
    const navItems = [
      'ダッシュボード',
      '商品管理',
      'カテゴリ管理',
      '店舗管理',
      'スタッフ管理',
      '設定',
    ]
    for (const item of navItems) {
      expect(screen.getByText(item)).toBeInTheDocument()
    }
  })

  it('メニューラベルを表示する', () => {
    renderSidebar()
    expect(screen.getByText('メニュー')).toBeInTheDocument()
  })
})
