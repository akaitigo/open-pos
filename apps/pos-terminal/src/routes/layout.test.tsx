import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router'
import { Layout } from './layout'

beforeEach(() => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(
    new Response(JSON.stringify([]), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
})

describe('Layout', () => {
  it('Header とメインコンテンツ領域がレンダリングされる', () => {
    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>,
    )

    expect(screen.getByText('OpenPOS Terminal')).toBeInTheDocument()
    expect(screen.getByText('本店')).toBeInTheDocument()
    expect(screen.getByText('オンライン')).toBeInTheDocument()
  })
})
