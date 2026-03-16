import { renderHook } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useKeyboardNav } from './use-keyboard-nav'

describe('useKeyboardNav', () => {
  const onSearch = vi.fn()
  const onCheckout = vi.fn()
  const onCancel = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function fireKeyDown(key: string, target?: Partial<HTMLElement>) {
    const event = new KeyboardEvent('keydown', { key, bubbles: true })
    if (target) {
      Object.defineProperty(event, 'target', { value: target })
    }
    window.dispatchEvent(event)
  }

  it('F1 キーで onSearch を呼ぶ', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    fireKeyDown('F1')
    expect(onSearch).toHaveBeenCalledOnce()
  })

  it('F2 キーで onCheckout を呼ぶ', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    fireKeyDown('F2')
    expect(onCheckout).toHaveBeenCalledOnce()
  })

  it('Escape キーで onCancel を呼ぶ', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    fireKeyDown('Escape')
    expect(onCancel).toHaveBeenCalledOnce()
  })

  it('INPUT 要素内では F1/F2 が無視される', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    const inputTarget = { tagName: 'INPUT', isContentEditable: false, blur: vi.fn() }
    fireKeyDown('F1', inputTarget)
    fireKeyDown('F2', inputTarget)
    expect(onSearch).not.toHaveBeenCalled()
    expect(onCheckout).not.toHaveBeenCalled()
  })

  it('TEXTAREA 要素内では F1/F2 が無視される', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    const textareaTarget = { tagName: 'TEXTAREA', isContentEditable: false, blur: vi.fn() }
    fireKeyDown('F1', textareaTarget)
    expect(onSearch).not.toHaveBeenCalled()
  })

  it('INPUT 内で Escape を押すと blur + onCancel が呼ばれる', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    const blurFn = vi.fn()
    const inputTarget = { tagName: 'INPUT', isContentEditable: false, blur: blurFn }
    fireKeyDown('Escape', inputTarget)
    expect(blurFn).toHaveBeenCalledOnce()
    expect(onCancel).toHaveBeenCalledOnce()
  })

  it('contentEditable 要素内では F1/F2 が無視される', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    const editableTarget = { tagName: 'DIV', isContentEditable: true, blur: vi.fn() }
    fireKeyDown('F1', editableTarget)
    expect(onSearch).not.toHaveBeenCalled()
  })

  it('アンマウント時にイベントリスナーが解除される', () => {
    const { unmount } = renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    unmount()
    fireKeyDown('F1')
    expect(onSearch).not.toHaveBeenCalled()
  })

  it('未設定のコールバックは呼ばれない', () => {
    renderHook(() => useKeyboardNav({}))
    // 例外なく動作する
    fireKeyDown('F1')
    fireKeyDown('F2')
    fireKeyDown('Escape')
  })

  it('関係ないキーではコールバックが呼ばれない', () => {
    renderHook(() => useKeyboardNav({ onSearch, onCheckout, onCancel }))
    fireKeyDown('Enter')
    fireKeyDown('a')
    fireKeyDown('F3')
    expect(onSearch).not.toHaveBeenCalled()
    expect(onCheckout).not.toHaveBeenCalled()
    expect(onCancel).not.toHaveBeenCalled()
  })
})
