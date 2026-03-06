import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { App } from './App'
import { useAuthStore } from './stores/auth-store'

beforeEach(() => {
  // Reset auth store to unauthenticated state
  useAuthStore.setState({
    isAuthenticated: false,
    staff: null,
    storeId: null,
    storeName: null,
    terminalId: null,
  })

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

    if (url.includes('/api/staff')) {
      return Promise.resolve(
        new Response(
          JSON.stringify({
            data: [],
            pagination: { page: 1, pageSize: 20, totalCount: 0, totalPages: 0 },
          }),
          {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      )
    }

    if (url.includes('/api/stores/')) {
      return Promise.resolve(
        new Response(
          JSON.stringify({
            id: '00000000-0000-0000-0000-000000000001',
            organizationId: '00000000-0000-0000-0000-000000000000',
            name: 'テスト店舗',
            address: null,
            phone: null,
            timezone: 'Asia/Tokyo',
            settings: '{}',
            isActive: true,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          }),
          {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
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
  it('未認証時はログイン画面が表示される', () => {
    render(<App />)

    expect(screen.getByText('OpenPOS Terminal')).toBeInTheDocument()
    expect(screen.getByText('スタッフを選択してください')).toBeInTheDocument()
  })

  it('認証済み時は商品検索UIが表示される', async () => {
    useAuthStore.setState({
      isAuthenticated: true,
      staff: {
        id: '00000000-0000-0000-0000-000000000001',
        organizationId: '00000000-0000-0000-0000-000000000000',
        storeId: '00000000-0000-0000-0000-000000000001',
        name: 'テストスタッフ',
        email: null,
        role: 'CASHIER',
        isActive: true,
        failedPinAttempts: 0,
        isLocked: false,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
      storeId: '00000000-0000-0000-0000-000000000001',
      storeName: 'テスト店舗',
      terminalId: '00000000-0000-0000-0000-000000000001',
    })

    render(<App />)

    expect(screen.getByPlaceholderText('商品名・バーコードで検索...')).toBeInTheDocument()
    expect(screen.getByText('スキャン')).toBeInTheDocument()
  })

  it('認証済み時はヘッダーにスタッフ名が表示される', () => {
    useAuthStore.setState({
      isAuthenticated: true,
      staff: {
        id: '00000000-0000-0000-0000-000000000001',
        organizationId: '00000000-0000-0000-0000-000000000000',
        storeId: '00000000-0000-0000-0000-000000000001',
        name: 'テストスタッフ',
        email: null,
        role: 'CASHIER',
        isActive: true,
        failedPinAttempts: 0,
        isLocked: false,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
      storeId: '00000000-0000-0000-0000-000000000001',
      storeName: 'テスト店舗',
      terminalId: '00000000-0000-0000-0000-000000000001',
    })

    render(<App />)

    expect(screen.getByText('テストスタッフ')).toBeInTheDocument()
  })
})
