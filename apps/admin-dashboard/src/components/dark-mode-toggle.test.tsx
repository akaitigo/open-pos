import { render, screen, fireEvent } from '@testing-library/react'
import { DarkModeToggle } from './dark-mode-toggle'

const mockToggle = vi.fn()

vi.mock('@/hooks/use-dark-mode', () => ({
  useDarkMode: () => ({
    isDark: false,
    toggle: mockToggle,
  }),
}))

describe('DarkModeToggle (ライトモード)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('ダークモードに切替のaria-labelを表示する', () => {
    render(<DarkModeToggle />)
    expect(screen.getByLabelText('ダークモードに切替')).toBeInTheDocument()
  })

  it('クリックでtoggleが呼ばれる', () => {
    render(<DarkModeToggle />)
    fireEvent.click(screen.getByLabelText('ダークモードに切替'))
    expect(mockToggle).toHaveBeenCalledTimes(1)
  })
})
