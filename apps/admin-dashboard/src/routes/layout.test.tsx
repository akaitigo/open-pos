import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { Layout } from './layout'

describe('Layout', () => {
  it('サイドバーとメインコンテンツエリアをレンダリングする', () => {
    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>,
    )
    expect(screen.getByText('OpenPOS')).toBeInTheDocument()
    expect(screen.getByText('管理ダッシュボード')).toBeInTheDocument()
  })
})
