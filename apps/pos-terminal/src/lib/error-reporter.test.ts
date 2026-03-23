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

  it('initErrorReporter で window error イベントがキャプチャされる', () => {
    cleanup = initErrorReporter()
    const listener = vi.fn()
    const unsub = onErrorReport(listener)

    const errorEvent = new ErrorEvent('error', {
      error: new Error('global error'),
      message: 'global error',
      filename: 'test.ts',
      lineno: 1,
      colno: 1,
    })
    window.dispatchEvent(errorEvent)

    expect(listener).toHaveBeenCalledOnce()
    expect(listener.mock.calls[0]?.[0]).toMatchObject({
      message: 'global error',
    })

    unsub()
  })

  it('initErrorReporter で unhandledrejection がキャプチャされる', () => {
    cleanup = initErrorReporter()
    const listener = vi.fn()
    const unsub = onErrorReport(listener)

    const event = new Event('unhandledrejection') as PromiseRejectionEvent
    Object.defineProperty(event, 'reason', { value: new Error('rejected') })
    window.dispatchEvent(event)

    expect(listener).toHaveBeenCalledOnce()
    expect(listener.mock.calls[0]?.[0]).toMatchObject({
      message: 'rejected',
    })

    unsub()
  })

  it('initErrorReporter の二重初期化は警告を出す', () => {
    cleanup = initErrorReporter()
    const cleanup2 = initErrorReporter()
    expect(typeof cleanup2).toBe('function')
    cleanup2()
  })

  it('リスナーがエラーを投げても他のリスナーには影響しない', () => {
    const badListener = vi.fn().mockImplementation(() => {
      throw new Error('listener error')
    })
    const goodListener = vi.fn()
    const unsub1 = onErrorReport(badListener)
    const unsub2 = onErrorReport(goodListener)

    reportError(new Error('test'))

    expect(badListener).toHaveBeenCalled()
    expect(goodListener).toHaveBeenCalled()

    unsub1()
    unsub2()
  })

  it('reportError に context を渡せる', () => {
    const listener = vi.fn()
    const unsub = onErrorReport(listener)

    reportError(new Error('ctx error'), { componentStack: 'SomeComponent' })

    expect(listener).toHaveBeenCalledOnce()
    unsub()
  })
})
