import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { SidebarProvider } from '@/components/ui/sidebar'
import { DiscountsPage } from './discounts'
import { beforeEach, describe, it, expect, vi } from 'vitest'
import { resetRuntimeConfigForTests } from '@/lib/runtime-config'

const mockDiscounts = [
  {
    id: '44444444-4444-4444-4444-444444444444',
    organizationId: '00000000-0000-0000-0000-000000000000',
    name: '10%オフ',
    discountType: 'PERCENTAGE' as const,
    value: '10',
    startDate: null,
    endDate: null,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

const mockCoupons = [
  {
    id: '55555555-5555-5555-5555-555555555555',
    organizationId: '00000000-0000-0000-0000-000000000000',
    code: 'WELCOME2024',
    discountId: '44444444-4444-4444-4444-444444444444',
    maxUses: 100,
    currentUses: 5,
    startDate: null,
    endDate: null,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

const mockApi = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
  setOrganizationId: vi.fn(),
  setBaseUrl: vi.fn(),
}))

vi.mock('@/lib/api', () => ({
  api: mockApi,
  configureApi: vi.fn(),
  getDefaultApiConfig: () => ({
    apiUrl: 'http://localhost:8080',
    organizationId: '00000000-0000-0000-0000-000000000000',
  }),
}))

function renderPage() {
  return render(
    <MemoryRouter>
      <SidebarProvider>
        <DiscountsPage />
      </SidebarProvider>
    </MemoryRouter>,
  )
}

describe('DiscountsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/coupons')) return Promise.resolve(mockCoupons)
      return Promise.resolve(mockDiscounts)
    })
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '00000000-0000-0000-0000-000000000000',
    })
  })

  it('割引・クーポン管理ヘッダーを表示する', () => {
    renderPage()
    expect(screen.getByText('割引・クーポン管理')).toBeInTheDocument()
  })

  it('割引タブに割引一覧を表示する', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('10%オフ')).toBeInTheDocument()
    })
    expect(screen.getByText('パーセント')).toBeInTheDocument()
    expect(screen.getByText('10%')).toBeInTheDocument()
  })

  it('割引を追加ボタンを表示する', () => {
    renderPage()
    expect(screen.getByText('割引を追加')).toBeInTheDocument()
  })

  it('タブを切り替えてクーポン一覧を表示する', async () => {
    const user = userEvent.setup()
    renderPage()
    // クーポンタブをクリック
    const couponTab = screen.getByRole('tab', { name: 'クーポン' })
    await user.click(couponTab)
    await waitFor(() => {
      expect(screen.getByText('WELCOME2024')).toBeInTheDocument()
    })
  })

  it('割引を追加ダイアログでフォーム送信する', async () => {
    const user = userEvent.setup()
    mockApi.post.mockResolvedValue(mockDiscounts[0])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('10%オフ')).toBeInTheDocument()
    })
    await user.click(screen.getByText('割引を追加'))
    await waitFor(() => {
      expect(screen.getByText('割引を追加', { selector: 'h2' })).toBeInTheDocument()
    })
    await user.type(screen.getByLabelText('名称 *'), '20%オフ')
    await user.type(screen.getByLabelText('値 *'), '20')
    await user.click(screen.getByRole('button', { name: '追加' }))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith(
        '/api/discounts',
        expect.objectContaining({ name: '20%オフ', value: '20' }),
        expect.anything(),
      )
    })
  })

  it('割引を編集ダイアログでフォーム送信する', async () => {
    const user = userEvent.setup()
    mockApi.put.mockResolvedValue(mockDiscounts[0])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('10%オフ')).toBeInTheDocument()
    })
    await user.click(screen.getByText('編集'))
    await waitFor(() => {
      expect(screen.getByText('割引を編集')).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('10%オフ')).toBeInTheDocument()
    expect(screen.getByDisplayValue('10')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '更新' }))
    await waitFor(() => {
      expect(mockApi.put).toHaveBeenCalledWith(
        `/api/discounts/${mockDiscounts[0]!.id}`,
        expect.objectContaining({ name: '10%オフ' }),
        expect.anything(),
      )
    })
  })

  it('割引を削除する', async () => {
    const user = userEvent.setup()
    mockApi.delete.mockResolvedValue(undefined)
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('10%オフ')).toBeInTheDocument()
    })
    await user.click(screen.getByText('削除'))
    await waitFor(() => {
      expect(mockApi.delete).toHaveBeenCalledWith(`/api/discounts/${mockDiscounts[0]!.id}`)
    })
  })

  it('割引が空の場合に空メッセージを表示する', async () => {
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/coupons')) return Promise.resolve([])
      return Promise.resolve([])
    })
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('割引が登録されていません')).toBeInTheDocument()
    })
  })

  it('固定額の割引を正しく表示する', async () => {
    const fixedDiscount = {
      ...mockDiscounts[0]!,
      discountType: 'FIXED_AMOUNT' as const,
      value: '10000',
    }
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/coupons')) return Promise.resolve([])
      return Promise.resolve([fixedDiscount])
    })
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('固定額')).toBeInTheDocument()
    })
    expect(screen.getByText('100円')).toBeInTheDocument()
  })

  it('期間付き割引を表示する', async () => {
    const datedDiscount = {
      ...mockDiscounts[0]!,
      startDate: '2026-01-01T00:00:00Z',
      endDate: '2026-12-31T00:00:00Z',
    }
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/coupons')) return Promise.resolve([])
      return Promise.resolve([datedDiscount])
    })
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('2026-01-01 ~ 2026-12-31')).toBeInTheDocument()
    })
  })

  it('無効な割引に無効バッジを表示する', async () => {
    const inactiveDiscount = { ...mockDiscounts[0]!, isActive: false }
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/coupons')) return Promise.resolve([])
      return Promise.resolve([inactiveDiscount])
    })
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('無効')).toBeInTheDocument()
    })
  })

  it('クーポンタブに空メッセージを表示する', async () => {
    const user = userEvent.setup()
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/coupons')) return Promise.resolve([])
      return Promise.resolve(mockDiscounts)
    })
    renderPage()
    const couponTab = screen.getByRole('tab', { name: 'クーポン' })
    await user.click(couponTab)
    await waitFor(() => {
      expect(screen.getByText('クーポンが登録されていません')).toBeInTheDocument()
    })
  })

  it('クーポン追加ダイアログを表示してフォーム送信する', async () => {
    const user = userEvent.setup()
    mockApi.post.mockResolvedValue(mockCoupons[0])
    renderPage()
    const couponTab = screen.getByRole('tab', { name: 'クーポン' })
    await user.click(couponTab)
    await waitFor(() => {
      expect(screen.getByText('WELCOME2024')).toBeInTheDocument()
    })
    await user.click(screen.getByText('クーポンを追加'))
    await waitFor(() => {
      expect(screen.getByText('クーポンを追加', { selector: 'h2' })).toBeInTheDocument()
    })
    await user.type(screen.getByLabelText('クーポンコード *'), 'SUMMER2026')
    await user.click(screen.getByRole('button', { name: '追加' }))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith(
        '/api/coupons',
        expect.objectContaining({ code: 'SUMMER2026' }),
        expect.anything(),
      )
    })
  })

  it('クーポンの期間を表示する', async () => {
    const user = userEvent.setup()
    const datedCoupon = {
      ...mockCoupons[0]!,
      startDate: '2026-01-01T00:00:00Z',
      endDate: '2026-06-30T00:00:00Z',
    }
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/coupons')) return Promise.resolve([datedCoupon])
      return Promise.resolve(mockDiscounts)
    })
    renderPage()
    const couponTab = screen.getByRole('tab', { name: 'クーポン' })
    await user.click(couponTab)
    await waitFor(() => {
      expect(screen.getByText('2026-01-01 ~ 2026-06-30')).toBeInTheDocument()
    })
  })

  it('クーポン追加ダイアログで全フィールドを入力して送信する', async () => {
    const user = userEvent.setup()
    mockApi.post.mockResolvedValue(mockCoupons[0])
    renderPage()
    const couponTab = screen.getByRole('tab', { name: 'クーポン' })
    await user.click(couponTab)
    await waitFor(() => {
      expect(screen.getByText('WELCOME2024')).toBeInTheDocument()
    })
    await user.click(screen.getByText('クーポンを追加'))
    await waitFor(() => {
      expect(screen.getByText('クーポンを追加', { selector: 'h2' })).toBeInTheDocument()
    })
    await user.type(screen.getByLabelText('クーポンコード *'), 'WINTER2026')
    await user.type(screen.getByLabelText('利用上限'), '50')
    // 日付フィールドに入力
    fireEvent.change(screen.getByLabelText('開始日'), { target: { value: '2026-01-01' } })
    fireEvent.change(screen.getByLabelText('終了日'), { target: { value: '2026-12-31' } })
    await user.click(screen.getByRole('button', { name: '追加' }))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith(
        '/api/coupons',
        expect.objectContaining({
          code: 'WINTER2026',
          maxUses: 50,
          startDate: '2026-01-01',
          endDate: '2026-12-31',
        }),
        expect.anything(),
      )
    })
  })

  it('割引追加ダイアログで日付フィールドを入力して送信する', async () => {
    const user = userEvent.setup()
    mockApi.post.mockResolvedValue(mockDiscounts[0])
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('10%オフ')).toBeInTheDocument()
    })
    await user.click(screen.getByText('割引を追加'))
    await waitFor(() => {
      expect(screen.getByText('割引を追加', { selector: 'h2' })).toBeInTheDocument()
    })
    await user.type(screen.getByLabelText('名称 *'), '期間限定')
    await user.type(screen.getByLabelText('値 *'), '15')
    fireEvent.change(screen.getByLabelText('開始日'), { target: { value: '2026-04-01' } })
    fireEvent.change(screen.getByLabelText('終了日'), { target: { value: '2026-04-30' } })
    await user.click(screen.getByRole('button', { name: '追加' }))
    await waitFor(() => {
      expect(mockApi.post).toHaveBeenCalledWith(
        '/api/discounts',
        expect.objectContaining({
          name: '期間限定',
          value: '15',
          startDate: '2026-04-01',
          endDate: '2026-04-30',
        }),
        expect.anything(),
      )
    })
  })

  it('割引追加ダイアログのキャンセルボタンで閉じる', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('10%オフ')).toBeInTheDocument()
    })
    await user.click(screen.getByText('割引を追加'))
    await waitFor(() => {
      expect(screen.getByText('割引を追加', { selector: 'h2' })).toBeInTheDocument()
    })
    await user.click(screen.getByText('キャンセル'))
    await waitFor(() => {
      expect(screen.queryByText('割引を追加', { selector: 'h2' })).not.toBeInTheDocument()
    })
  })

  it('クーポン追加ダイアログのキャンセルボタンで閉じる', async () => {
    const user = userEvent.setup()
    renderPage()
    const couponTab = screen.getByRole('tab', { name: 'クーポン' })
    await user.click(couponTab)
    await waitFor(() => {
      expect(screen.getByText('WELCOME2024')).toBeInTheDocument()
    })
    await user.click(screen.getByText('クーポンを追加'))
    await waitFor(() => {
      expect(screen.getByText('クーポンを追加', { selector: 'h2' })).toBeInTheDocument()
    })
    await user.click(screen.getByText('キャンセル'))
    await waitFor(() => {
      expect(screen.queryByText('クーポンを追加', { selector: 'h2' })).not.toBeInTheDocument()
    })
  })

  it('無効なクーポンに無効バッジを表示する', async () => {
    const user = userEvent.setup()
    const inactiveCoupon = { ...mockCoupons[0]!, isActive: false }
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/coupons')) return Promise.resolve([inactiveCoupon])
      return Promise.resolve(mockDiscounts)
    })
    renderPage()
    const couponTab = screen.getByRole('tab', { name: 'クーポン' })
    await user.click(couponTab)
    await waitFor(() => {
      expect(screen.getByText('無効')).toBeInTheDocument()
    })
  })

  it('無制限利用のクーポンを表示する', async () => {
    const user = userEvent.setup()
    const unlimitedCoupon = { ...mockCoupons[0]!, maxUses: null }
    mockApi.get.mockImplementation((path: string) => {
      if (path.includes('/coupons')) return Promise.resolve([unlimitedCoupon])
      return Promise.resolve(mockDiscounts)
    })
    renderPage()
    const couponTab = screen.getByRole('tab', { name: 'クーポン' })
    await user.click(couponTab)
    await waitFor(() => {
      expect(screen.getByText('無制限')).toBeInTheDocument()
    })
  })
})
