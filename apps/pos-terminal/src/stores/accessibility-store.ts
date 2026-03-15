import { create } from 'zustand'

interface AccessibilityState {
  highContrast: boolean
  largeFontSize: boolean
  toggleHighContrast: () => void
  toggleLargeFontSize: () => void
  initialize: () => void
}

function applyHighContrast(enabled: boolean): void {
  document.documentElement.classList.toggle('high-contrast', enabled)
}

function applyLargeFontSize(enabled: boolean): void {
  document.documentElement.classList.toggle('large-font', enabled)
}

export const useAccessibilityStore = create<AccessibilityState>((set, get) => ({
  highContrast: false,
  largeFontSize: false,
  toggleHighContrast: () => {
    const next = \!get().highContrast
    localStorage.setItem('openpos-high-contrast', String(next))
    applyHighContrast(next)
    set({ highContrast: next })
  },
  toggleLargeFontSize: () => {
    const next = \!get().largeFontSize
    localStorage.setItem('openpos-large-font', String(next))
    applyLargeFontSize(next)
    set({ largeFontSize: next })
  },
  initialize: () => {
    const hc = localStorage.getItem('openpos-high-contrast') === 'true'
    const lf = localStorage.getItem('openpos-large-font') === 'true'
    applyHighContrast(hc)
    applyLargeFontSize(lf)
    set({ highContrast: hc, largeFontSize: lf })
  },
}))
