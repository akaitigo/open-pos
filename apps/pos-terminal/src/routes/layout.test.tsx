import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router'
import { Layout } from './layout'
import { resetRuntimeConfigForTests } from '@/lib/runtime-config'
import { useAuthStore } from '@/stores/auth-store'

const mockSetupAutoSync = vi.fn()
const mockCleanup = vi.fn()

vi.mock('@/lib/sync-manager', () => ({
  setupAutoSync: () => {
    mockSetupAutoSync()
    return mockCleanup
  },
}))

let mockIsOnline = true
vi.mock('@/hooks/use-online-status', () => ({
  useOnlineStatus: () => mockIsOnline,
}))

beforeEach(() => {
  mockSetupAutoSync.mockClear()
  mockCleanup.mockClear()
  mockIsOnline = true

  resetRuntimeConfigForTests({
    apiUrl: 'http://localhost:8080',
    organizationId: '00000000-0000-0000-0000-000000000000',
    storeId: '00000000-0000-0000-0000-000000000001',
    terminalId: '00000000-0000-0000-0000-000000000001',
  })

  vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
    const url = typeof input === 'string' ? input : input.toString()

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

    return Promise.resolve(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
  })
})

describe('Layout', () => {
  it('未認証時はログイン画面がレンダリングされる', async () => {
    useAuthStore.setState({
      isAuthenticated: false,
      staff: null,
      storeId: null,
      storeName: null,
      terminalId: null,
    })

    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>,
    )

    expect(await screen.findByText('OpenPOS Terminal')).toBeInTheDocument()
    expect(await screen.findByText('スタッフを選択してください')).toBeInTheDocument()
  })

  it('認証済み時は Header とカートサイドバーがレンダリングされる', () => {
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

    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>,
    )

    expect(screen.getByText('OpenPOS')).toBeInTheDocument()
    expect(screen.getByText('テスト店舗')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'カート' })).toBeInTheDocument()
    expect(screen.getByText('カートは空です')).toBeInTheDocument()
  })

  it('マウント時に setupAutoSync が呼び出される', () => {
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

    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>,
    )

    expect(mockSetupAutoSync).toHaveBeenCalledTimes(1)
  })

  it('オフライン時にオフラインバナーが表示される', () => {
    mockIsOnline = false
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

    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>,
    )

    expect(screen.getByTestId('offline-banner')).toBeInTheDocument()
    expect(screen.getByText(/オフラインモード/)).toBeInTheDocument()
  })

  it('オンライン時にオフラインバナーが表示されない', () => {
    mockIsOnline = true
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

    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>,
    )

    expect(screen.queryByTestId('offline-banner')).not.toBeInTheDocument()
  })

  it('organizationId のみ設定時は店舗/端末選択画面を表示する', async () => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '00000000-0000-0000-0000-000000000000',
      storeId: null,
      terminalId: null,
    })
    useAuthStore.setState({
      isAuthenticated: false,
      staff: null,
      storeId: null,
      storeName: null,
      terminalId: null,
    })

    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('store-terminal-selector')).toBeInTheDocument()
  })

  it('organizationId 未設定ならセットアップ案内を表示する', () => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: null,
      storeId: null,
      terminalId: null,
    })
    useAuthStore.setState({
      isAuthenticated: false,
      staff: null,
      storeId: null,
      storeName: null,
      terminalId: null,
    })

    render(
      <MemoryRouter>
        <Layout />
      </MemoryRouter>,
    )

    expect(
      screen.getByText('organization、store、terminal のデモ設定が未構成です。'),
    ).toBeInTheDocument()
  })
})
