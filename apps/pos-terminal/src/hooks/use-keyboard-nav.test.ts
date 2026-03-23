import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useKeyboardNav } from './use-keyboard-nav'

describe('useKeyboardNav', () => {
  let onSearch: () => void
  let onCheckout: () => void
  let onCancel: () => void

  beforeEach(() => {
    onSearch = vi.fn()
    onCheckout = vi.fn()
    onCancel = vi.fn()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function fireKeyDown(key: string, target?: Partial<HTMLElement>) {
    const event = new KeyboardEvent('keydown', {
      key,
      bubbles: true,
      cancelable: true,
    })
    if (target) {
      Object.defineProperty(event, 'target', { value: target })
    }
    window.dispatchEvent(event)
  }

  it('F1キーで onSearch が呼ばれる', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    fireKeyDown('F1')
    expect(onSearch).toHaveBeenCalledTimes(1)
  })

  it('F2キーで onCheckout が呼ばれる', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    fireKeyDown('F2')
    expect(onCheckout).toHaveBeenCalledTimes(1)
  })

  it('Escapeキーで onCancel が呼ばれる', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    fireKeyDown('Escape')
    expect(onCancel).toHaveBeenCalledTimes(1)
  })

  it('INPUT要素内ではショートカットが無効化される', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    fireKeyDown('F1', { tagName: 'INPUT' } as HTMLElement)
    expect(onSearch).not.toHaveBeenCalled()
  })

  it('TEXTAREA要素内ではショートカットが無効化される', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    fireKeyDown('F2', { tagName: 'TEXTAREA' } as HTMLElement)
    expect(onCheckout).not.toHaveBeenCalled()
  })

  it('contentEditable要素内ではショートカットが無効化される', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    fireKeyDown('F1', { tagName: 'DIV', isContentEditable: true } as unknown as HTMLElement)
    expect(onSearch).not.toHaveBeenCalled()
  })

  it('INPUT要素内でEscapeを押すとblurとonCancelが呼ばれる', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    const blurFn = vi.fn()
    fireKeyDown('Escape', { tagName: 'INPUT', blur: blurFn } as unknown as HTMLElement)
    expect(blurFn).toHaveBeenCalled()
    expect(onCancel).toHaveBeenCalled()
  })

  it('未登録のキーは何も呼ばない', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    fireKeyDown('a')
    expect(onSearch).not.toHaveBeenCalled()
    expect(onCheckout).not.toHaveBeenCalled()
    expect(onCancel).not.toHaveBeenCalled()
  })

  it('アンマウント後はイベントリスナーが削除される', () => {
    const { unmount } = renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    unmount()
    fireKeyDown('F1')
    expect(onSearch).not.toHaveBeenCalled()
  })
})
