import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { OnboardingPage } from './onboarding'

const mockPost = vi.fn()
const mockSetOrganizationId = vi.fn()

vi.mock('@/lib/api', () => ({
  api: {
    post: (...args: unknown[]) => mockPost(...args),
    setOrganizationId: (...args: unknown[]) => mockSetOrganizationId(...args),
  },
}))

vi.mock('@/hooks/use-toast', () => ({
  toast: vi.fn(),
}))

describe('OnboardingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the wizard with initial ORG_INFO step', () => {
    // Act
    render(<OnboardingPage />)

    // Assert
    expect(screen.getByTestId('onboarding-wizard')).toBeInTheDocument()
    expect(screen.getByTestId('input-org-name')).toBeInTheDocument()
    expect(screen.getByText('組織情報')).toBeInTheDocument()
  })

  it('disables next button when orgName is empty', () => {
    // Act
    render(<OnboardingPage />)

    // Assert
    expect(screen.getByTestId('step-next')).toBeDisabled()
  })

  it('enables next button when orgName has a value', async () => {
    // Arrange
    const user = userEvent.setup()

    // Act
    render(<OnboardingPage />)
    await user.type(screen.getByTestId('input-org-name'), 'テスト組織')

    // Assert
    expect(screen.getByTestId('step-next')).toBeEnabled()
  })

  it('navigates through all steps', async () => {
    // Arrange
    const user = userEvent.setup()

    // Act
    render(<OnboardingPage />)

    // Step 1: ORG_INFO
    await user.type(screen.getByTestId('input-org-name'), 'テスト組織')
    await user.click(screen.getByTestId('step-next'))

    // Step 2: STORE_SETUP
    await waitFor(() => expect(screen.getByTestId('input-store-name')).toBeInTheDocument())
    await user.type(screen.getByTestId('input-store-name'), 'テスト店舗')
    await user.click(screen.getByTestId('step-next'))

    // Step 3: PRODUCTS
    await waitFor(() => expect(screen.getByTestId('input-product-0')).toBeInTheDocument())
    await user.click(screen.getByTestId('step-next'))

    // Step 4: STAFF
    await waitFor(() => expect(screen.getByTestId('input-staff-name')).toBeInTheDocument())
    await user.type(screen.getByTestId('input-staff-name'), '山田太郎')
    await user.type(screen.getByTestId('input-staff-email'), 'test@example.com')
    await user.type(screen.getByTestId('input-staff-pin'), '1234')
    await user.click(screen.getByTestId('step-next'))

    // Step 5: REVIEW
    await waitFor(() => expect(screen.getByTestId('review-summary')).toBeInTheDocument())

    // Assert
    expect(screen.getByText('テスト組織')).toBeInTheDocument()
    expect(screen.getByText('テスト店舗')).toBeInTheDocument()
    expect(screen.getByText('山田太郎')).toBeInTheDocument()
  })

  it('navigates back from STORE_SETUP to ORG_INFO', async () => {
    // Arrange
    const user = userEvent.setup()

    // Act
    render(<OnboardingPage />)
    await user.type(screen.getByTestId('input-org-name'), 'テスト組織')
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-store-name')).toBeInTheDocument())
    await user.click(screen.getByTestId('step-back'))

    // Assert
    await waitFor(() => expect(screen.getByTestId('input-org-name')).toBeInTheDocument())
  })

  it('calls API to provision on complete', async () => {
    // Arrange
    const user = userEvent.setup()
    mockPost
      .mockResolvedValueOnce({
        id: 'org-001',
        name: 'テスト組織',
        businessType: 'RETAIL',
        invoiceNumber: null,
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01',
      })
      .mockResolvedValueOnce({
        id: 'store-001',
        organizationId: 'org-001',
        name: 'テスト店舗',
        address: null,
        phone: null,
        timezone: 'Asia/Tokyo',
        settings: '{}',
        isActive: true,
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01',
      })
      .mockResolvedValueOnce({
        id: 'staff-001',
        organizationId: 'org-001',
        storeId: 'store-001',
        name: '山田太郎',
        email: 'test@example.com',
        role: 'OWNER',
        isActive: true,
        failedPinAttempts: 0,
        isLocked: false,
        createdAt: '2026-01-01',
        updatedAt: '2026-01-01',
      })

    // Act
    render(<OnboardingPage />)

    // Navigate through all steps
    await user.type(screen.getByTestId('input-org-name'), 'テスト組織')
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-store-name')).toBeInTheDocument())
    await user.type(screen.getByTestId('input-store-name'), 'テスト店舗')
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-product-0')).toBeInTheDocument())
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-staff-name')).toBeInTheDocument())
    await user.type(screen.getByTestId('input-staff-name'), '山田太郎')
    await user.type(screen.getByTestId('input-staff-email'), 'test@example.com')
    await user.type(screen.getByTestId('input-staff-pin'), '1234')
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('review-summary')).toBeInTheDocument())
    await user.click(screen.getByTestId('complete-setup'))

    // Assert
    await waitFor(() => expect(screen.getByTestId('onboarding-complete')).toBeInTheDocument())
    expect(mockPost).toHaveBeenCalledTimes(3) // org + store + staff
    expect(mockSetOrganizationId).toHaveBeenCalledWith('org-001')
  })

  it('shows error when provisioning fails', async () => {
    // Arrange
    const user = userEvent.setup()
    mockPost.mockRejectedValueOnce(new Error('ネットワークエラー'))

    // Act
    render(<OnboardingPage />)
    await user.type(screen.getByTestId('input-org-name'), 'テスト組織')
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-store-name')).toBeInTheDocument())
    await user.type(screen.getByTestId('input-store-name'), 'テスト店舗')
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-product-0')).toBeInTheDocument())
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-staff-name')).toBeInTheDocument())
    await user.type(screen.getByTestId('input-staff-name'), '山田太郎')
    await user.type(screen.getByTestId('input-staff-email'), 'test@example.com')
    await user.type(screen.getByTestId('input-staff-pin'), '1234')
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('review-summary')).toBeInTheDocument())
    await user.click(screen.getByTestId('complete-setup'))

    // Assert
    await waitFor(() => expect(screen.getByTestId('provision-error')).toBeInTheDocument())
    expect(screen.getByText('ネットワークエラー')).toBeInTheDocument()
  })

  it('validates PIN is 4-8 digits on STAFF step', async () => {
    // Arrange
    const user = userEvent.setup()

    // Act
    render(<OnboardingPage />)
    await user.type(screen.getByTestId('input-org-name'), 'テスト組織')
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-store-name')).toBeInTheDocument())
    await user.type(screen.getByTestId('input-store-name'), 'テスト店舗')
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-product-0')).toBeInTheDocument())
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-staff-name')).toBeInTheDocument())
    await user.type(screen.getByTestId('input-staff-name'), '山田太郎')
    await user.type(screen.getByTestId('input-staff-email'), 'test@example.com')
    await user.type(screen.getByTestId('input-staff-pin'), '12') // Too short

    // Assert - button should be disabled (PIN < 4 digits)
    expect(screen.getByTestId('step-next')).toBeDisabled()

    // Fix the PIN
    await user.clear(screen.getByTestId('input-staff-pin'))
    await user.type(screen.getByTestId('input-staff-pin'), '12345678')
    expect(screen.getByTestId('step-next')).toBeEnabled()
  })

  it('adds product input fields with + button', async () => {
    // Arrange
    const user = userEvent.setup()

    // Act
    render(<OnboardingPage />)
    await user.type(screen.getByTestId('input-org-name'), 'テスト組織')
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-store-name')).toBeInTheDocument())
    await user.type(screen.getByTestId('input-store-name'), 'テスト店舗')
    await user.click(screen.getByTestId('step-next'))
    await waitFor(() => expect(screen.getByTestId('input-product-0')).toBeInTheDocument())
    await user.click(screen.getByTestId('add-product-button'))

    // Assert
    expect(screen.getByTestId('input-product-1')).toBeInTheDocument()
  })
})
