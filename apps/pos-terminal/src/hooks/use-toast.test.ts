import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { reducer, useToast, toast } from './use-toast'

describe('reducer', () => {
  const initialState = { toasts: [] as { id: string; title?: string }[] }

  it('ADD_TOAST: トーストを追加する', () => {
    const newToast = { id: '1', title: 'Hello' }
    const result = reducer(initialState, { type: 'ADD_TOAST', toast: newToast })

    expect(result.toasts).toHaveLength(1)
    expect(result.toasts[0]).toEqual(newToast)
  })

  it('ADD_TOAST: TOAST_LIMIT(1)を超えない', () => {
    const state = { toasts: [{ id: '1', title: 'First' }] }
    const newToast = { id: '2', title: 'Second' }
    const result = reducer(state, { type: 'ADD_TOAST', toast: newToast })

    expect(result.toasts).toHaveLength(1)
    expect(result.toasts[0].id).toBe('2')
  })

  it('UPDATE_TOAST: 一致するトーストを更新する', () => {
    const state = { toasts: [{ id: '1', title: 'Original' }] }
    const result = reducer(state, {
      type: 'UPDATE_TOAST',
      toast: { id: '1', title: 'Updated' },
    })

    expect(result.toasts[0].title).toBe('Updated')
  })

  it('UPDATE_TOAST: 一致しないトーストは変更しない', () => {
    const state = { toasts: [{ id: '1', title: 'Original' }] }
    const result = reducer(state, {
      type: 'UPDATE_TOAST',
      toast: { id: '99', title: 'Updated' },
    })

    expect(result.toasts[0].title).toBe('Original')
  })

  it('DISMISS_TOAST: 特定のトーストをdismissする', () => {
    vi.useFakeTimers()
    const state = { toasts: [{ id: '1', title: 'Test' }] }
    const result = reducer(state, { type: 'DISMISS_TOAST', toastId: '1' })

    expect(result.toasts).toHaveLength(1)
    vi.useRealTimers()
  })

  it('DISMISS_TOAST: toastId なしで全トーストをdismissする', () => {
    vi.useFakeTimers()
    const state = {
      toasts: [
        { id: '1', title: 'First' },
        { id: '2', title: 'Second' },
      ],
    }
    const result = reducer(state, { type: 'DISMISS_TOAST' })

    expect(result.toasts).toHaveLength(2)
    vi.useRealTimers()
  })

  it('REMOVE_TOAST: 特定のトーストを削除する', () => {
    const state = {
      toasts: [
        { id: '1', title: 'First' },
        { id: '2', title: 'Second' },
      ],
    }
    const result = reducer(state, { type: 'REMOVE_TOAST', toastId: '1' })

    expect(result.toasts).toHaveLength(1)
    expect(result.toasts[0].id).toBe('2')
  })

  it('REMOVE_TOAST: toastId が undefined で全トーストを削除する', () => {
    const state = {
      toasts: [
        { id: '1', title: 'First' },
        { id: '2', title: 'Second' },
      ],
    }
    const result = reducer(state, { type: 'REMOVE_TOAST', toastId: undefined })

    expect(result.toasts).toHaveLength(0)
  })
})

describe('toast', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('ID付きのトーストを作成し、dismiss と update を返す', () => {
    const result = toast({ title: 'Test toast' })

    expect(result.id).toBeDefined()
    expect(typeof result.dismiss).toBe('function')
    expect(typeof result.update).toBe('function')
  })
})

describe('useToast', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('toasts 配列と toast 関数を返す', () => {
    const { result } = renderHook(() => useToast())

    expect(Array.isArray(result.current.toasts)).toBe(true)
    expect(typeof result.current.toast).toBe('function')
    expect(typeof result.current.dismiss).toBe('function')
  })

  it('toast 関数でトーストを追加できる', () => {
    const { result } = renderHook(() => useToast())

    act(() => {
      result.current.toast({ title: 'New toast' })
    })

    expect(result.current.toasts).toHaveLength(1)
    expect(result.current.toasts[0].title).toBe('New toast')
  })

  it('dismiss 関数でトーストをdismissできる', () => {
    const { result } = renderHook(() => useToast())

    let toastId: string
    act(() => {
      const t = result.current.toast({ title: 'Dismissible' })
      toastId = t.id
    })

    act(() => {
      result.current.dismiss(toastId!)
    })

    // dismiss はすぐには削除しない（remove queue に追加）
    expect(result.current.toasts).toHaveLength(1)
  })
})
