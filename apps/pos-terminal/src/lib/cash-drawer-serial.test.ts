import { describe, it, expect, vi, beforeEach } from 'vitest'
import { SerialCashDrawer, createCashDrawer, DummyCashDrawer } from './cash-drawer'

vi.mock('@/lib/logger', () => ({
  createLogger: () => ({
    debug: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
  }),
}))

describe('SerialCashDrawer', () => {
  it('ポート未接続時は isConnected が false', () => {
    const drawer = new SerialCashDrawer()
    expect(drawer.isConnected()).toBe(false)
  })

  it('ポート未接続時の getStatus は UNKNOWN', async () => {
    const drawer = new SerialCashDrawer()
    const status = await drawer.getStatus()
    expect(status).toBe('UNKNOWN')
  })

  it('Web Serial API が未対応の場合 open は false を返す', async () => {
    const drawer = new SerialCashDrawer()
    // jsdom has no serial API
    const result = await drawer.open()
    expect(result).toBe(false)
  })
})

describe('createCashDrawer', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('useSerial=false の場合 DummyCashDrawer を返す', () => {
    const drawer = createCashDrawer(false)
    expect(drawer).toBeInstanceOf(DummyCashDrawer)
  })

  it('useSerial=true だが serial 未対応なら DummyCashDrawer を返す', () => {
    const drawer = createCashDrawer(true)
    expect(drawer).toBeInstanceOf(DummyCashDrawer)
  })

  it('serial API 対応時に useSerial=true なら SerialCashDrawer を返す', () => {
    Object.defineProperty(navigator, 'serial', {
      value: { requestPort: vi.fn() },
      configurable: true,
    })
    const drawer = createCashDrawer(true)
    expect(drawer).toBeInstanceOf(SerialCashDrawer)
    // cleanup
    Object.defineProperty(navigator, 'serial', { value: undefined, configurable: true })
  })
})
