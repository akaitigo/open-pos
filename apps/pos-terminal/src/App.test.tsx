import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { App } from './App'

beforeEach(() => {
  vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
    const url = typeof input === 'string' ? input : input.toString()

    if (url.includes('/api/categories')) {
      return Promise.resolve(
        new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
    }

    return Promise.resolve(
      new Response(
        JSON.stringify({
          data: [],
          pagination: { page: 1, pageSize: 24, totalCount: 0, totalPages: 0 },
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    )
  })
})

describe('App', () => {
  it('クラッシュせずにレンダリングできる', () => {
    render(<App />)

    expect(screen.getByText('OpenPOS Terminal')).toBeInTheDocument()
  })

  it('商品検索UIが表示される', () => {
    render(<App />)

    expect(screen.getByPlaceholderText('商品名・バーコードで検索...')).toBeInTheDocument()
    expect(screen.getByText('スキャン')).toBeInTheDocument()
  })

  it('オンラインステータスが表示される', () => {
    render(<App />)

    expect(screen.getByText('オンライン')).toBeInTheDocument()
  })
})
