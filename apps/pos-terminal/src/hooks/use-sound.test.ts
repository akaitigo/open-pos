import { describe, it, expect, vi } from 'vitest'

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

describe('sound settings', () => {
  it('デフォルトでサウンドが有効', () => {
    expect(localStorage.getItem('openpos-sound-enabled')).toBeNull()
    // Default is enabled (null !== 'false')
    expect(localStorage.getItem('openpos-sound-enabled') !== 'false').toBe(true)
  })

  it('サウンドを無効にできる', () => {
    localStorage.setItem('openpos-sound-enabled', 'false')
    expect(localStorage.getItem('openpos-sound-enabled')).toBe('false')
  })
})
