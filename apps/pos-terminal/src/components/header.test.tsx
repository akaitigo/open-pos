import { render, screen } from '@testing-library/react'
import { describe, it, expect, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router'
import { Header } from './header'
import { useAuthStore } from '@/stores/auth-store'

beforeEach(() => {
  useAuthStore.setState({
    isAuthenticated: true,
    staff: {
      id: '00000000-0000-0000-0000-000000000001',
      organizationId: '00000000-0000-0000-0000-000000000000',
      storeId: '00000000-0000-0000-0000-000000000001',
      name: '田中太郎',
      email: null,
      role: 'CASHIER',
      isActive: true,
      failedPinAttempts: 0,
      isLocked: false,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
    storeId: '00000000-0000-0000-0000-000000000001',
    storeName: '本店',
    terminalId: '00000000-0000-0000-0000-000000000001',
  })
})

describe('Header', () => {
  it('タイトルが表示される', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>,
    )

    expect(screen.getByText('OpenPOS')).toBeInTheDocument()
  })

  it('店舗バッジが表示される', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>,
    )

    expect(screen.getByText('本店')).toBeInTheDocument()
  })

  it('スタッフ名が表示される', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>,
    )

    expect(screen.getByText('田中太郎')).toBeInTheDocument()
  })
})
