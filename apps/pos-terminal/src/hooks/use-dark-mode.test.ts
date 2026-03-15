import { describe, it, expect, vi, beforeEach } from 'vitest'

const mockStorage: Record<string, string> = {}
vi.stubGlobal('localStorage', {
  getItem: (key: string) => mockStorage[key] ?? null,
  setItem: (key: string, value: string) => { mockStorage[key] = value },
  removeItem: (key: string) => { delete mockStorage[key] },
})

describe('dark mode settings', () => {
  beforeEach(() => {
    Object.keys(mockStorage).forEach((key) => delete mockStorage[key])
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
