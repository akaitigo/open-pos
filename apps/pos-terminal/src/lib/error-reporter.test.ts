import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { initErrorReporter, reportError, onErrorReport } from './error-reporter'

describe('error-reporter', () => {
  let cleanup: (() => void) | undefined

  beforeEach(() => {
    cleanup = undefined
  })

  afterEach(() => {
    cleanup?.()
  })

  it('reportErrorでリスナーに通知される', () => {
    const listener = vi.fn()
    const unsub = onErrorReport(listener)

    reportError(new Error('test error'))

    expect(listener).toHaveBeenCalledOnce()
    expect(listener.mock.calls[0]?.[0]).toMatchObject({
      message: 'test error',
    })

    unsub()
  })

  it('文字列エラーも報告できる', () => {
    const listener = vi.fn()
    const unsub = onErrorReport(listener)

    reportError('string error')

    expect(listener).toHaveBeenCalledOnce()
    expect(listener.mock.calls[0]?.[0]).toMatchObject({
      message: 'string error',
    })

    unsub()
  })

  it('unsubscribeでリスナーが解除される', () => {
    const listener = vi.fn()
    const unsub = onErrorReport(listener)
    unsub()

    reportError('after unsub')

    expect(listener).not.toHaveBeenCalled()
  })

  it('initErrorReporterがクリーンアップ関数を返す', () => {
    cleanup = initErrorReporter()
    expect(typeof cleanup).toBe('function')
  })

  it('ErrorReportにタイムスタンプとURLが含まれる', () => {
    const listener = vi.fn()
    const unsub = onErrorReport(listener)

    reportError(new Error('with metadata'))

    const report = listener.mock.calls[0]?.[0]
    expect(report).toHaveProperty('timestamp')
    expect(report).toHaveProperty('url')
    expect(report).toHaveProperty('userAgent')

    unsub()
  })
})
