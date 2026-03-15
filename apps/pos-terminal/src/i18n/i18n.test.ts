import { describe, it, expect, beforeEach, vi } from 'vitest'
import { t, setLocale, getLocale } from './index'

const mockStorage: Record<string, string> = {}
vi.stubGlobal('localStorage', {
  getItem: (key: string) => mockStorage[key] ?? null,
  setItem: (key: string, value: string) => { mockStorage[key] = value },
  removeItem: (key: string) => { delete mockStorage[key] },
})

describe('i18n', () => {
  beforeEach(() => {
    setLocale('ja')
  })

  it('日本語のキーを解決する', () => {
    expect(t('app.name')).toBe('OpenPOS')
    expect(t('header.cart')).toBe('カート')
  })

  it('テンプレート変数を補間する', () => {
    expect(t('products.count', { count: 42 })).toBe('42 件')
  })

  it('存在しないキーはキー文字列を返す', () => {
    expect(t('nonexistent.key')).toBe('nonexistent.key')
  })

  it('ロケールを英語に変更できる', () => {
    setLocale('en')
    expect(getLocale()).toBe('en')
    expect(t('header.cart')).toBe('Cart')
  })
})
