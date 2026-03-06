import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { CartPage } from './cart'

describe('CartPage', () => {
  it('Coming Soon テキストが表示される', () => {
    render(<CartPage />)

    expect(screen.getByText('カート - Coming Soon')).toBeInTheDocument()
    expect(screen.getByText('カートの内容と精算画面がここに表示されます')).toBeInTheDocument()
  })
})
