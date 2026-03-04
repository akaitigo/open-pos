import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { App } from './App'

describe('App', () => {
  it('クラッシュせずにレンダリングできる', () => {
    render(<App />)

    expect(screen.getByText('OpenPOS Terminal')).toBeInTheDocument()
  })

  it('商品選択ページが表示される', () => {
    render(<App />)

    expect(screen.getByText('商品選択')).toBeInTheDocument()
  })

  it('オンラインステータスが表示される', () => {
    render(<App />)

    expect(screen.getByText('オンライン')).toBeInTheDocument()
  })
})
