import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createLogger, setLogLevel, addLogTransport } from './logger'

describe('logger', () => {
  beforeEach(() => {
    setLogLevel('debug')
    vi.restoreAllMocks()
  })

  it('タグ付きロガーを生成できる', () => {
    const logger = createLogger('TestTag')
    expect(logger).toHaveProperty('debug')
    expect(logger).toHaveProperty('info')
    expect(logger).toHaveProperty('warn')
    expect(logger).toHaveProperty('error')
  })

  it('debug レベルのメッセージを出力する', () => {
    const spy = vi.spyOn(console, 'debug').mockImplementation(() => {})
    const logger = createLogger('Test')
    logger.debug('debug message')
    expect(spy).toHaveBeenCalledWith('[Test]', 'debug message', '')
  })

  it('info レベルのメッセージを出力する', () => {
    const spy = vi.spyOn(console, 'info').mockImplementation(() => {})
    const logger = createLogger('Test')
    logger.info('info message')
    expect(spy).toHaveBeenCalledWith('[Test]', 'info message', '')
  })

  it('warn レベルのメッセージを出力する', () => {
    const spy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    const logger = createLogger('Test')
    logger.warn('warn message')
    expect(spy).toHaveBeenCalledWith('[Test]', 'warn message', '')
  })

  it('error レベルのメッセージを出力する', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    const logger = createLogger('Test')
    logger.error('error message')
    expect(spy).toHaveBeenCalledWith('[Test]', 'error message', '')
  })

  it('data 付きのメッセージを出力する', () => {
    const spy = vi.spyOn(console, 'info').mockImplementation(() => {})
    const logger = createLogger('Test')
    const data = { key: 'value' }
    logger.info('with data', data)
    expect(spy).toHaveBeenCalledWith('[Test]', 'with data', data)
  })

  it('ログレベルに満たないメッセージは抑制される', () => {
    const spy = vi.spyOn(console, 'debug').mockImplementation(() => {})
    setLogLevel('warn')
    const logger = createLogger('Test')
    logger.debug('should not appear')
    expect(spy).not.toHaveBeenCalled()
  })

  it('warn レベル設定時に error は出力される', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    setLogLevel('warn')
    const logger = createLogger('Test')
    logger.error('should appear')
    expect(spy).toHaveBeenCalled()
  })

  it('カスタムトランスポートが呼ばれる', () => {
    const transport = vi.fn()
    addLogTransport(transport)
    const logger = createLogger('Custom')
    logger.info('transported')
    expect(transport).toHaveBeenCalledWith(
      expect.objectContaining({
        level: 'info',
        tag: 'Custom',
        message: 'transported',
      }),
    )
  })
})
