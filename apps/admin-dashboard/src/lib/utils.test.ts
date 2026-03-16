import { describe, it, expect } from 'vitest'
import { cn } from './utils'

describe('cn', () => {
  it('単一クラスを返す', () => {
    expect(cn('foo')).toBe('foo')
  })

  it('複数クラスを結合する', () => {
    expect(cn('foo', 'bar')).toBe('foo bar')
  })

  it('Tailwind のクラスをマージする', () => {
    const result = cn('px-2 py-1', 'px-4')
    expect(result).toBe('py-1 px-4')
  })

  it('falsy 値を無視する', () => {
    expect(cn('foo', false, null, undefined, 'bar')).toBe('foo bar')
  })

  it('条件付きクラスをサポートする', () => {
    const isActive = true
    const result = cn('base', isActive && 'active')
    expect(result).toBe('base active')
  })

  it('条件が false の場合はクラスを含めない', () => {
    const isActive = false
    const result = cn('base', isActive && 'active')
    expect(result).toBe('base')
  })

  it('空の入力で空文字を返す', () => {
    expect(cn()).toBe('')
  })

  it('重複するTailwindクラスは後者が優先される', () => {
    const result = cn('text-red-500', 'text-blue-500')
    expect(result).toBe('text-blue-500')
  })

  it('オブジェクト形式のクラスをサポートする', () => {
    const result = cn({ foo: true, bar: false, baz: true })
    expect(result).toBe('foo baz')
  })

  it('配列形式のクラスをサポートする', () => {
    const result = cn(['foo', 'bar'])
    expect(result).toBe('foo bar')
  })
})
