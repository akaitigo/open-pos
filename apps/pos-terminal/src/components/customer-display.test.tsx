import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { CustomerDisplayButton } from './customer-display'
import { useCartStore } from '@/stores/cart-store'

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue([]),
    post: vi.fn().mockResolvedValue({}),
    setOrganizationId: vi.fn(),
  },
}))

vi.mock('@/stores/auth-store', () => {
  const state = {
    isAuthenticated: true,
    staff: null,
    storeId: null,
    storeName: null,
    terminalId: null,
    login: vi.fn(),
    logout: vi.fn(),
  }
  const fn = (selector?: (s: typeof state) => unknown) => (selector ? selector(state) : state)
  fn.getState = () => state
  fn.setState = vi.fn()
  fn.subscribe = vi.fn()
  fn.destroy = vi.fn()
  return { useAuthStore: fn }
})

describe('CustomerDisplayButton', () => {
  beforeEach(() => {
    useCartStore.setState({ items: [] })
    vi.clearAllMocks()
  })

  it('顧客ディスプレイボタンを表示する', () => {
    render(<CustomerDisplayButton />)
    expect(screen.getByTestId('customer-display-button')).toBeInTheDocument()
    expect(screen.getByText('顧客ディスプレイ')).toBeInTheDocument()
  })

  it('ボタンクリックで window.open を呼ぶ', async () => {
    const mockOpen = vi.fn().mockReturnValue(null)
    vi.stubGlobal('open', mockOpen)

    render(<CustomerDisplayButton />)
    await userEvent.click(screen.getByTestId('customer-display-button'))

    expect(mockOpen).toHaveBeenCalledWith(
      '',
      'customer-display',
      'width=600,height=800,menubar=no,toolbar=no,location=no,status=no',
    )

    vi.unstubAllGlobals()
  })

  it('window.open が null を返した場合にエラーが発生しない', async () => {
    const mockOpen = vi.fn().mockReturnValue(null)
    vi.stubGlobal('open', mockOpen)

    render(<CustomerDisplayButton />)
    // null が返されても例外なく動作する
    await userEvent.click(screen.getByTestId('customer-display-button'))

    vi.unstubAllGlobals()
  })
})
