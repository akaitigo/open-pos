import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { HistoryPage } from './history'

describe('HistoryPage', () => {
  it('Coming Soon テキストが表示される', () => {
    render(<HistoryPage />)

    expect(screen.getByText('取引履歴 - Coming Soon')).toBeInTheDocument()
    expect(screen.getByText('過去の取引履歴がここに表示されます')).toBeInTheDocument()
  })
})
