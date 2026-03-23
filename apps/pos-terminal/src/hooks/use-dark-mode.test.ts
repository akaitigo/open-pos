import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'

const mockStorage: Record<string, string> = {}
vi.stubGlobal('localStorage', {
  getItem: (key: string) => mockStorage[key] ?? null,
  setItem: (key: string, value: string) => {
    mockStorage[key] = value
  },
  removeItem: (key: string) => {
    delete mockStorage[key]
  },
})

describe('dark mode settings', () => {
  beforeEach(() => {
    Object.keys(mockStorage).forEach((key) => delete mockStorage[key])
    document.documentElement.classList.remove('dark')
  })

  it('デフォルトはライトモード', () => {
    const theme = localStorage.getItem('openpos-theme') ?? 'light'
    expect(theme).toBe('light')
  })

  it('ダークモードに切り替えられる', () => {
    localStorage.setItem('openpos-theme', 'dark')
    expect(localStorage.getItem('openpos-theme')).toBe('dark')
  })
})

describe('useDarkMode hook', () => {
  beforeEach(() => {
    Object.keys(mockStorage).forEach((key) => delete mockStorage[key])
    document.documentElement.classList.remove('dark')
  })

  it('デフォルト値は isDark: false', async () => {
    const { useDarkMode } = await import('./use-dark-mode')
    const { result } = renderHook(() => useDarkMode())
    expect(result.current.isDark).toBe(false)
  })

  it('toggle でダークモードに切り替わる', async () => {
    const { useDarkMode } = await import('./use-dark-mode')
    const { result } = renderHook(() => useDarkMode())

    act(() => {
      result.current.toggle()
    })

    expect(result.current.isDark).toBe(true)
    expect(localStorage.getItem('openpos-theme')).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('toggle 2回でライトモードに戻る', async () => {
    const { useDarkMode } = await import('./use-dark-mode')
    const { result } = renderHook(() => useDarkMode())

    act(() => {
      result.current.toggle()
    })
    act(() => {
      result.current.toggle()
    })

    expect(result.current.isDark).toBe(false)
    expect(localStorage.getItem('openpos-theme')).toBe('light')
  })

  it('localStorage にダークが保存済みの場合は isDark: true', async () => {
    localStorage.setItem('openpos-theme', 'dark')
    const { useDarkMode } = await import('./use-dark-mode')
    const { result } = renderHook(() => useDarkMode())
    expect(result.current.isDark).toBe(true)
  })
})
