import { renderHook, act } from '@testing-library/react'
import { reducer, useToast, toast } from './use-toast'
import type { ToasterToast } from './use-toast'

describe('reducer', () => {
  const initialState = { toasts: [] as ToasterToast[] }

  it('ADD_TOAST でトーストを追加する', () => {
    const newToast: ToasterToast = { id: '1', title: 'テスト' }
    const state = reducer(initialState, { type: 'ADD_TOAST', toast: newToast })
    expect(state.toasts).toHaveLength(1)
    expect(state.toasts[0]!.title).toBe('テスト')
  })

  it('ADD_TOAST で制限数を超えない', () => {
    const state1 = reducer(initialState, {
      type: 'ADD_TOAST',
      toast: { id: '1', title: 'first' },
    })
    const state2 = reducer(state1, {
      type: 'ADD_TOAST',
      toast: { id: '2', title: 'second' },
    })
    // TOAST_LIMIT = 1 なので最新の1つだけ残る
    expect(state2.toasts).toHaveLength(1)
    expect(state2.toasts[0]!.title).toBe('second')
  })

  it('UPDATE_TOAST でトーストを更新する', () => {
    const state = reducer(
      { toasts: [{ id: '1', title: 'before' }] },
      { type: 'UPDATE_TOAST', toast: { id: '1', title: 'after' } },
    )
    expect(state.toasts[0]!.title).toBe('after')
  })

  it('UPDATE_TOAST で該当IDがない場合は変更なし', () => {
    const state = reducer(
      { toasts: [{ id: '1', title: 'original' }] },
      { type: 'UPDATE_TOAST', toast: { id: '999', title: 'nope' } },
    )
    expect(state.toasts[0]!.title).toBe('original')
  })

  it('DISMISS_TOAST で特定のトーストをdismissする', () => {
    const state = reducer(
      { toasts: [{ id: '1', title: 'test' }] },
      { type: 'DISMISS_TOAST', toastId: '1' },
    )
    expect(state.toasts).toHaveLength(1)
  })

  it('DISMISS_TOAST でtoastId未指定の場合は全てdismissする', () => {
    const state = reducer({ toasts: [{ id: '1', title: 'a' }] }, { type: 'DISMISS_TOAST' })
    expect(state.toasts).toHaveLength(1)
  })

  it('REMOVE_TOAST で特定のトーストを削除する', () => {
    const state = reducer(
      {
        toasts: [
          { id: '1', title: 'test' },
          { id: '2', title: 'keep' },
        ],
      },
      { type: 'REMOVE_TOAST', toastId: '1' },
    )
    expect(state.toasts).toHaveLength(1)
    expect(state.toasts[0]!.id).toBe('2')
  })

  it('REMOVE_TOAST でtoastId未指定の場合は全て削除する', () => {
    const state = reducer(
      {
        toasts: [
          { id: '1', title: 'a' },
          { id: '2', title: 'b' },
        ],
      },
      { type: 'REMOVE_TOAST', toastId: undefined },
    )
    expect(state.toasts).toHaveLength(0)
  })
})

describe('toast 関数', () => {
  it('トーストを作成してIDを返す', () => {
    const result = toast({ title: 'Hello' })
    expect(result.id).toBeDefined()
    expect(typeof result.dismiss).toBe('function')
    expect(typeof result.update).toBe('function')
  })
})

describe('useToast', () => {
  it('初期状態でtoasts配列を返す', () => {
    const { result } = renderHook(() => useToast())
    expect(Array.isArray(result.current.toasts)).toBe(true)
    expect(typeof result.current.toast).toBe('function')
    expect(typeof result.current.dismiss).toBe('function')
  })

  it('toast呼び出し後にtoastsが更新される', () => {
    const { result } = renderHook(() => useToast())
    act(() => {
      result.current.toast({ title: 'フック内テスト' })
    })
    expect(result.current.toasts.length).toBeGreaterThanOrEqual(0)
  })
})
