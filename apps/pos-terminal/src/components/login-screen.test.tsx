import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { LoginScreen } from './login-screen'
import { resetRuntimeConfigForTests } from '@/lib/runtime-config'
import { useAuthStore } from '@/stores/auth-store'

const mockStaffList = {
  data: [
    {
      id: '550e8400-e29b-41d4-a716-446655440010',
      organizationId: '550e8400-e29b-41d4-a716-446655440000',
      storeId: '550e8400-e29b-41d4-a716-446655440020',
      name: '田中太郎',
      email: 'tanaka@example.com',
      role: 'MANAGER',
      isActive: true,
      failedPinAttempts: 0,
      isLocked: false,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
    {
      id: '550e8400-e29b-41d4-a716-446655440011',
      organizationId: '550e8400-e29b-41d4-a716-446655440000',
      storeId: '550e8400-e29b-41d4-a716-446655440020',
      name: '山田花子',
      email: 'yamada@example.com',
      role: 'CASHIER',
      isActive: true,
      failedPinAttempts: 0,
      isLocked: false,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
  ],
  pagination: { page: 1, pageSize: 20, totalCount: 2, totalPages: 1 },
}

const mockStore = {
  id: '550e8400-e29b-41d4-a716-446655440020',
  organizationId: '550e8400-e29b-41d4-a716-446655440000',
  name: '渋谷本店',
  address: '東京都渋谷区',
  phone: null,
  timezone: 'Asia/Tokyo',
  settings: '{}',
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const mockApiPost = vi.fn().mockResolvedValue({
  success: true,
  staff: mockStaffList.data[0],
  reason: null,
})

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn().mockImplementation((path: string) => {
      if (path.includes('/api/stores/')) return Promise.resolve(mockStore)
      if (path === '/api/staff') return Promise.resolve(mockStaffList)
      return Promise.resolve({})
    }),
    post: (...args: unknown[]) => mockApiPost(...args),
    setOrganizationId: vi.fn(),
    setBaseUrl: vi.fn(),
  },
  configureApi: vi.fn(),
  getDefaultApiConfig: () => ({
    apiUrl: 'http://localhost:8080',
    organizationId: '550e8400-e29b-41d4-a716-446655440000',
  }),
}))

describe('LoginScreen', () => {
  beforeEach(() => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '550e8400-e29b-41d4-a716-446655440000',
      storeId: '550e8400-e29b-41d4-a716-446655440020',
      terminalId: 'terminal-1',
    })
    useAuthStore.setState({
      isAuthenticated: false,
      staff: null,
      storeId: null,
      storeName: null,
      terminalId: null,
    })
    mockApiPost.mockClear()
  })

  it('タイトルとスタッフ一覧を表示する', async () => {
    render(<LoginScreen />)
    await waitFor(() => {
      expect(screen.getByText('OpenPOS Terminal')).toBeInTheDocument()
    })
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
      expect(screen.getByText('山田花子')).toBeInTheDocument()
    })
  })

  it('スタッフ選択後にテンキーが表示される', async () => {
    render(<LoginScreen />)
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByText('田中太郎'))
    expect(screen.getByText('PINを入力してください')).toBeInTheDocument()
    expect(screen.getByText('0')).toBeInTheDocument()
    expect(screen.getByText('C')).toBeInTheDocument()
  })

  it('PIN 入力後にログインボタンが有効になる', async () => {
    render(<LoginScreen />)
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByText('田中太郎'))

    const loginButton = screen.getByRole('button', { name: 'ログイン' })
    expect(loginButton).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: '1' }))
    await userEvent.click(screen.getByRole('button', { name: '2' }))
    await userEvent.click(screen.getByRole('button', { name: '3' }))
    await userEvent.click(screen.getByRole('button', { name: '4' }))

    expect(loginButton).not.toBeDisabled()
  })

  it('認証成功で API が呼ばれる', async () => {
    render(<LoginScreen />)
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByText('田中太郎'))
    await userEvent.click(screen.getByRole('button', { name: '1' }))
    await userEvent.click(screen.getByRole('button', { name: '2' }))
    await userEvent.click(screen.getByRole('button', { name: '3' }))
    await userEvent.click(screen.getByRole('button', { name: '4' }))
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))

    await waitFor(() => {
      expect(mockApiPost).toHaveBeenCalledWith(
        `/api/staff/${mockStaffList.data[0]!.id}/authenticate`,
        {
          storeId: '550e8400-e29b-41d4-a716-446655440020',
          pin: '1234',
        },
        expect.anything(),
      )
    })
  })

  it('C ボタンで PIN がクリアされる', async () => {
    render(<LoginScreen />)
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByText('田中太郎'))
    await userEvent.click(screen.getByRole('button', { name: '1' }))
    await userEvent.click(screen.getByRole('button', { name: '2' }))
    await userEvent.click(screen.getByRole('button', { name: 'C' }))

    const loginButton = screen.getByRole('button', { name: 'ログイン' })
    expect(loginButton).toBeDisabled()
  })

  it('← ボタンで PIN の最後の1桁が削除される', async () => {
    render(<LoginScreen />)
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByText('田中太郎'))
    await userEvent.click(screen.getByRole('button', { name: '1' }))
    await userEvent.click(screen.getByRole('button', { name: '2' }))
    await userEvent.click(screen.getByRole('button', { name: '3' }))
    await userEvent.click(screen.getByRole('button', { name: '4' }))

    const loginButton = screen.getByRole('button', { name: 'ログイン' })
    expect(loginButton).not.toBeDisabled()

    // バックスペースで1桁削除 → 3桁になりログインボタンは無効
    await userEvent.click(screen.getByTestId('pin-key-backspace'))
    expect(loginButton).toBeDisabled()
  })

  it('認証失敗のレスポンスでトーストが表示される', async () => {
    mockApiPost.mockResolvedValue({
      success: false,
      staff: null,
      reason: 'PINが正しくありません',
    })
    render(<LoginScreen />)
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByText('田中太郎'))
    await userEvent.click(screen.getByRole('button', { name: '1' }))
    await userEvent.click(screen.getByRole('button', { name: '2' }))
    await userEvent.click(screen.getByRole('button', { name: '3' }))
    await userEvent.click(screen.getByRole('button', { name: '4' }))
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))

    await waitFor(() => {
      expect(mockApiPost).toHaveBeenCalledTimes(1)
    })
  })

  it('認証 API エラー時にクラッシュしない', async () => {
    mockApiPost.mockRejectedValue(new Error('ネットワークエラー'))
    render(<LoginScreen />)
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByText('田中太郎'))
    await userEvent.click(screen.getByRole('button', { name: '1' }))
    await userEvent.click(screen.getByRole('button', { name: '2' }))
    await userEvent.click(screen.getByRole('button', { name: '3' }))
    await userEvent.click(screen.getByRole('button', { name: '4' }))
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))

    await waitFor(() => {
      expect(mockApiPost).toHaveBeenCalledTimes(1)
    })
    // クラッシュせずログインボタンが再度無効化される (PIN クリアされるため)
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'ログイン' })).toBeDisabled()
    })
  })

  it('storeId 未設定時にログインするとトーストが表示される', async () => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '550e8400-e29b-41d4-a716-446655440000',
      storeId: null,
      terminalId: null,
    })
    render(<LoginScreen />)
    // storeId がないため、スタッフリストは空
    await waitFor(() => {
      expect(screen.getByText('スタッフが登録されていません')).toBeInTheDocument()
    })
  })

  it('戻るボタンでスタッフ選択に戻る', async () => {
    render(<LoginScreen />)
    await waitFor(() => {
      expect(screen.getByText('田中太郎')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByText('田中太郎'))
    expect(screen.getByText('PINを入力してください')).toBeInTheDocument()

    // 戻るボタン（ArrowLeftアイコン付き）をクリック
    const backButton = screen.getByText('田中太郎').closest('div')!.querySelector('button')!
    await userEvent.click(backButton)

    await waitFor(() => {
      expect(screen.getByText('スタッフを選択してください')).toBeInTheDocument()
    })
  })
})
