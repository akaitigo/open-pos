import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { Header } from './header'

function renderHeader(title: string) {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <Header title={title} />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('Header', () => {
  it('タイトルを表示する', () => {
    renderHeader('テストタイトル')
    expect(screen.getByText('テストタイトル')).toBeInTheDocument()
  })

  it('異なるタイトルを表示する', () => {
    renderHeader('商品管理')
    expect(screen.getByText('商品管理')).toBeInTheDocument()
  })
})
