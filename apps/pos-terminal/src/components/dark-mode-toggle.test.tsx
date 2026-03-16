import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { DarkModeToggle } from './dark-mode-toggle'

const mockToggle = vi.fn()
let mockIsDark = false

vi.mock('@/hooks/use-dark-mode', () => ({
  useDarkMode: () => ({
    isDark: mockIsDark,
    toggle: mockToggle,
  }),
}))

describe('DarkModeToggle', () => {
  beforeEach(() => {
    mockIsDark = false
    vi.clearAllMocks()
  })

  it('ライトモード時に「ダークモードに切替」ラベルを持つ', () => {
    mockIsDark = false
    render(<DarkModeToggle />)
    expect(screen.getByLabelText('ダークモードに切替')).toBeInTheDocument()
  })

  it('ダークモード時に「ライトモードに切替」ラベルを持つ', () => {
    mockIsDark = true
    render(<DarkModeToggle />)
    expect(screen.getByLabelText('ライトモードに切替')).toBeInTheDocument()
  })

  it('クリックで toggle を呼ぶ', async () => {
    render(<DarkModeToggle />)
    await userEvent.click(screen.getByRole('button'))
    expect(mockToggle).toHaveBeenCalledOnce()
  })
})
