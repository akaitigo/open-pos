import { describe, it, expect, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useDarkMode } from './use-dark-mode'

describe('useDarkMode', () => {
  beforeEach(() => {
    localStorage.removeItem('openpos-theme')
    document.documentElement.classList.remove('dark')
  })

  it('デフォルト値は isDark: false', () => {
    const { result } = renderHook(() => useDarkMode())
    expect(result.current.isDark).toBe(false)
  })

  it('toggle でダークモードに切り替わる', () => {
    const { result } = renderHook(() => useDarkMode())

    act(() => {
      result.current.toggle()
    })

    expect(result.current.isDark).toBe(true)
    expect(localStorage.getItem('openpos-theme')).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('toggle 2回でライトモードに戻る', () => {
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

  it('localStorage にダークが保存済みの場合は isDark: true', () => {
    localStorage.setItem('openpos-theme', 'dark')
    const { result } = renderHook(() => useDarkMode())
    expect(result.current.isDark).toBe(true)
  })
})
