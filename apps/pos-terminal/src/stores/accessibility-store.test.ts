import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAccessibilityStore } from './accessibility-store'

// Mock localStorage
const mockStorage: Record<string, string> = {}
vi.stubGlobal('localStorage', {
  getItem: (key: string) => mockStorage[key] ?? null,
  setItem: (key: string, value: string) => { mockStorage[key] = value },
  removeItem: (key: string) => { delete mockStorage[key] },
})

describe('accessibility-store', () => {
  beforeEach(() => {
    Object.keys(mockStorage).forEach((key) => delete mockStorage[key])
    useAccessibilityStore.setState({ highContrast: false, largeFontSize: false })
  })

  it('toggleHighContrast でハイコントラストを切り替える', () => {
    expect(useAccessibilityStore.getState().highContrast).toBe(false)
    useAccessibilityStore.getState().toggleHighContrast()
    expect(useAccessibilityStore.getState().highContrast).toBe(true)
    expect(mockStorage['openpos-high-contrast']).toBe('true')
  })

  it('toggleLargeFontSize でフォントサイズを切り替える', () => {
    expect(useAccessibilityStore.getState().largeFontSize).toBe(false)
    useAccessibilityStore.getState().toggleLargeFontSize()
    expect(useAccessibilityStore.getState().largeFontSize).toBe(true)
    expect(mockStorage['openpos-large-font']).toBe('true')
  })

  it('initialize で localStorage から設定を復元する', () => {
    mockStorage['openpos-high-contrast'] = 'true'
    mockStorage['openpos-large-font'] = 'true'
    useAccessibilityStore.getState().initialize()
    expect(useAccessibilityStore.getState().highContrast).toBe(true)
    expect(useAccessibilityStore.getState().largeFontSize).toBe(true)
  })
})
