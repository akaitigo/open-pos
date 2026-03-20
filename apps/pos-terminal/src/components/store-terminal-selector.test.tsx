import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { StoreTerminalSelector } from './store-terminal-selector'

// Mock api module
const mockGet = vi.fn()
vi.mock('@/lib/api', () => ({
  api: {
    get: (...args: unknown[]) => mockGet(...args),
  },
}))

vi.mock('@/hooks/use-toast', () => ({
  toast: vi.fn(),
}))

const mockStores = {
  data: [
    {
      id: '11111111-1111-1111-1111-111111111111',
      organizationId: '00000000-0000-0000-0000-000000000001',
      name: '渋谷店',
      address: '東京都渋谷区',
      phone: '03-1234-5678',
      timezone: 'Asia/Tokyo',
      settings: '{}',
      isActive: true,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
    {
      id: '22222222-2222-2222-2222-222222222222',
      organizationId: '00000000-0000-0000-0000-000000000001',
      name: '新宿店',
      address: '東京都新宿区',
      phone: '03-9876-5432',
      timezone: 'Asia/Tokyo',
      settings: '{}',
      isActive: true,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
  ],
  pagination: { page: 1, pageSize: 100, totalCount: 2, totalPages: 1 },
}

const mockTerminals = [
  {
    id: 'aaaa-1111',
    organizationId: '00000000-0000-0000-0000-000000000001',
    storeId: '11111111-1111-1111-1111-111111111111',
    terminalCode: 'POS-001',
    name: 'レジ1',
    isActive: true,
    lastSyncAt: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'aaaa-2222',
    organizationId: '00000000-0000-0000-0000-000000000001',
    storeId: '11111111-1111-1111-1111-111111111111',
    terminalCode: 'POS-002',
    name: 'レジ2',
    isActive: true,
    lastSyncAt: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
]

describe('StoreTerminalSelector', () => {
  const onSelect = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows loading state initially', () => {
    // Arrange
    mockGet.mockReturnValue(new Promise(() => {}))

    // Act
    render(<StoreTerminalSelector onSelect={onSelect} />)

    // Assert
    expect(screen.getByTestId('store-selector-loading')).toBeInTheDocument()
  })

  it('displays store list after loading', async () => {
    // Arrange
    mockGet.mockResolvedValueOnce(mockStores)

    // Act
    render(<StoreTerminalSelector onSelect={onSelect} />)

    // Assert
    await waitFor(() => {
      expect(screen.getByText('渋谷店')).toBeInTheDocument()
      expect(screen.getByText('新宿店')).toBeInTheDocument()
    })
    expect(screen.getByTestId('store-list')).toBeInTheDocument()
  })

  it('shows terminal list when store is selected', async () => {
    // Arrange
    mockGet.mockResolvedValueOnce(mockStores).mockResolvedValueOnce(mockTerminals)
    const user = userEvent.setup()

    // Act
    render(<StoreTerminalSelector onSelect={onSelect} />)
    await waitFor(() => expect(screen.getByText('渋谷店')).toBeInTheDocument())
    await user.click(screen.getByText('渋谷店'))

    // Assert
    await waitFor(() => {
      expect(screen.getByTestId('terminal-list')).toBeInTheDocument()
      expect(screen.getByText('レジ1')).toBeInTheDocument()
      expect(screen.getByText('レジ2')).toBeInTheDocument()
    })
  })

  it('calls onSelect when terminal is clicked', async () => {
    // Arrange
    mockGet.mockResolvedValueOnce(mockStores).mockResolvedValueOnce(mockTerminals)
    const user = userEvent.setup()

    // Act
    render(<StoreTerminalSelector onSelect={onSelect} />)
    await waitFor(() => expect(screen.getByText('渋谷店')).toBeInTheDocument())
    await user.click(screen.getByText('渋谷店'))
    await waitFor(() => expect(screen.getByText('レジ1')).toBeInTheDocument())
    await user.click(screen.getByText('レジ1'))

    // Assert
    expect(onSelect).toHaveBeenCalledWith(
      '11111111-1111-1111-1111-111111111111',
      '渋谷店',
      'aaaa-1111',
    )
  })

  it('navigates back to store list from terminal list', async () => {
    // Arrange
    mockGet.mockResolvedValueOnce(mockStores).mockResolvedValueOnce(mockTerminals)
    const user = userEvent.setup()

    // Act
    render(<StoreTerminalSelector onSelect={onSelect} />)
    await waitFor(() => expect(screen.getByText('渋谷店')).toBeInTheDocument())
    await user.click(screen.getByText('渋谷店'))
    await waitFor(() => expect(screen.getByTestId('terminal-list')).toBeInTheDocument())
    await user.click(screen.getByTestId('back-to-stores'))

    // Assert
    await waitFor(() => {
      expect(screen.getByTestId('store-list')).toBeInTheDocument()
    })
  })

  it('shows message when no stores available', async () => {
    // Arrange
    mockGet.mockResolvedValueOnce({
      data: [],
      pagination: { page: 1, pageSize: 100, totalCount: 0, totalPages: 0 },
    })

    // Act
    render(<StoreTerminalSelector onSelect={onSelect} />)

    // Assert
    await waitFor(() => {
      expect(screen.getByText('利用可能な店舗がありません')).toBeInTheDocument()
    })
  })

  it('shows message when no terminals available', async () => {
    // Arrange
    mockGet.mockResolvedValueOnce(mockStores).mockResolvedValueOnce([])
    const user = userEvent.setup()

    // Act
    render(<StoreTerminalSelector onSelect={onSelect} />)
    await waitFor(() => expect(screen.getByText('渋谷店')).toBeInTheDocument())
    await user.click(screen.getByText('渋谷店'))

    // Assert
    await waitFor(() => {
      expect(screen.getByText('利用可能な端末がありません')).toBeInTheDocument()
    })
  })
})
