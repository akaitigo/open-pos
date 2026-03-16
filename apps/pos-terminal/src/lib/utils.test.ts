import { describe, it, expect } from 'vitest'
import { cn } from './utils'

describe('cn', () => {
  it('単一のクラス名をそのまま返す', () => {
    expect(cn('text-red-500')).toBe('text-red-500')
  })

  it('複数のクラス名を結合する', () => {
    const result = cn('px-4', 'py-2')
    expect(result).toContain('px-4')
    expect(result).toContain('py-2')
  })

  it('Tailwind の競合するクラスをマージする', () => {
    // twMerge は後のクラスを優先
    const result = cn('px-4', 'px-6')
    expect(result).toBe('px-6')
  })

  it('条件付きクラス（falsy 値）を除外する', () => {
    const result = cn('base', false && 'hidden', null, undefined, 'active')
    expect(result).toContain('base')
    expect(result).toContain('active')
    expect(result).not.toContain('hidden')
  })

  it('空の入力で空文字列を返す', () => {
    expect(cn()).toBe('')
  })

  it('配列形式のクラス名を処理する', () => {
    const result = cn(['px-4', 'py-2'])
    expect(result).toContain('px-4')
    expect(result).toContain('py-2')
  })

  it('オブジェクト形式のクラス名を処理する', () => {
    const result = cn({ 'text-red-500': true, 'text-blue-500': false })
    expect(result).toContain('text-red-500')
    expect(result).not.toContain('text-blue-500')
  })
})
