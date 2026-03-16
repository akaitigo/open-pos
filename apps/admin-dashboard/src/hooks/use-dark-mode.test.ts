import { renderHook, act } from '@testing-library/react'
import { useDarkMode } from './use-dark-mode'

describe('useDarkMode', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  it('デフォルトはライトモード', () => {
    const { result } = renderHook(() => useDarkMode())
    expect(result.current.isDark).toBe(false)
  })

  it('toggle でダークモードに切り替える', () => {
    const { result } = renderHook(() => useDarkMode())
    act(() => {
      result.current.toggle()
    })
    expect(result.current.isDark).toBe(true)
    expect(localStorage.getItem('openpos-theme')).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('ダークモードから toggle でライトモードに戻す', () => {
    localStorage.setItem('openpos-theme', 'dark')
    const { result } = renderHook(() => useDarkMode())
    expect(result.current.isDark).toBe(true)
    act(() => {
      result.current.toggle()
    })
    expect(result.current.isDark).toBe(false)
    expect(localStorage.getItem('openpos-theme')).toBe('light')
  })

  it('localStorage の値を読み込む', () => {
    localStorage.setItem('openpos-theme', 'dark')
    const { result } = renderHook(() => useDarkMode())
    expect(result.current.isDark).toBe(true)
  })

  it('localStorage に値がない場合はライトモード', () => {
    localStorage.removeItem('openpos-theme')
    const { result } = renderHook(() => useDarkMode())
    expect(result.current.isDark).toBe(false)
  })
})
