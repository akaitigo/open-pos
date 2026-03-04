import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { App } from './App'

describe('App', () => {
  it('クラッシュせずにレンダリングできる', () => {
    render(<App />)

    expect(screen.getByText('OpenPOS Admin')).toBeInTheDocument()
  })

  it('サブタイトルが表示される', () => {
    render(<App />)

    expect(screen.getByText('管理ダッシュボード')).toBeInTheDocument()
  })
})
