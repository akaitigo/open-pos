import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { App } from './App'

describe('App', () => {
  it('クラッシュせずにレンダリングできる', () => {
    render(<App />)

    expect(screen.getByText('OpenPOS')).toBeInTheDocument()
  })

  it('サイドバーにダッシュボードリンクが表示される', () => {
    render(<App />)

    expect(screen.getByText('ダッシュボード')).toBeInTheDocument()
  })

  it('ダッシュボードページが表示される', () => {
    render(<App />)

    expect(screen.getByText('Dashboard - Coming Soon')).toBeInTheDocument()
  })
})
