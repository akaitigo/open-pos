import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { Header } from './header'

describe('Header', () => {
  it('タイトルが表示される', () => {
    render(<Header />)

    expect(screen.getByText('OpenPOS Terminal')).toBeInTheDocument()
  })

  it('店舗バッジが表示される', () => {
    render(<Header />)

    expect(screen.getByText('本店')).toBeInTheDocument()
  })

  it('オンラインステータスが表示される', () => {
    render(<Header />)

    expect(screen.getByText('オンライン')).toBeInTheDocument()
  })
})
