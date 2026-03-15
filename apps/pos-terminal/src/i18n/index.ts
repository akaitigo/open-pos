import ja from './ja.json'
import en from './en.json'

export type Locale = 'ja' | 'en'

const resources: Record<Locale, Record<string, unknown>> = { ja, en }

/** ネストされたキーからテンプレート文字列を返す i18n ヘルパー */
function getNestedValue(obj: Record<string, unknown>, path: string): string | undefined {
  const keys = path.split('.')
  let current: unknown = obj
  for (const key of keys) {
    if (current == null || typeof current !== 'object') return undefined
    current = (current as Record<string, unknown>)[key]
  }
  return typeof current === 'string' ? current : undefined
}

/** テンプレート変数を置換 */
function interpolate(template: string, vars?: Record<string, string | number>): string {
  if (!vars) return template
  return template.replace(/\{\{(\w+)\}\}/g, (_, key: string) =>
    vars[key] != null ? String(vars[key]) : `{{${key}}}`,
  )
}

let currentLocale: Locale = (localStorage.getItem('openpos-locale') as Locale) ?? 'ja'
const listeners = new Set<() => void>()

export function getLocale(): Locale {
  return currentLocale
}

export function setLocale(locale: Locale): void {
  currentLocale = locale
  localStorage.setItem('openpos-locale', locale)
  listeners.forEach((fn) => fn())
}

export function onLocaleChange(fn: () => void): () => void {
  listeners.add(fn)
  return () => listeners.delete(fn)
}

export function t(key: string, vars?: Record<string, string | number>): string {
  const value = getNestedValue(resources[currentLocale] as Record<string, unknown>, key)
  if (value == null) return key
  return interpolate(value, vars)
}
