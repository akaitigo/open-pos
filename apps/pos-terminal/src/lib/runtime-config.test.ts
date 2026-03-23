import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  getRuntimeConfig,
  hasPosRuntimeConfig,
  updateStoreTerminal,
  needsStoreTerminalSelection,
  resetRuntimeConfigForTests,
  initializeRuntimeConfig,
} from './runtime-config'

vi.mock('@/lib/api', () => ({
  api: {
    setBaseUrl: vi.fn(),
    setOrganizationId: vi.fn(),
  },
  configureApi: vi.fn(),
  getDefaultApiConfig: () => ({
    apiUrl: 'http://localhost:8080',
    organizationId: null,
  }),
}))

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: {
    getState: vi.fn().mockReturnValue({
      isAuthenticated: false,
      storeId: null,
      terminalId: null,
      logout: vi.fn(),
    }),
  },
}))

describe('runtime-config', () => {
  beforeEach(() => {
    resetRuntimeConfigForTests()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('getRuntimeConfig', () => {
    it('デフォルト設定を返す', () => {
      const config = getRuntimeConfig()
      expect(config.apiUrl).toBe('http://localhost:8080')
    })
  })

  describe('hasPosRuntimeConfig', () => {
    it('orgId, storeId, terminalId が全て設定済みなら true', () => {
      resetRuntimeConfigForTests({
        organizationId: 'org-1',
        storeId: 'store-1',
        terminalId: 'terminal-1',
      })
      expect(hasPosRuntimeConfig()).toBe(true)
    })

    it('organizationId がない場合は false', () => {
      resetRuntimeConfigForTests({
        organizationId: null,
        storeId: 'store-1',
        terminalId: 'terminal-1',
      })
      expect(hasPosRuntimeConfig()).toBe(false)
    })

    it('storeId がない場合は false', () => {
      resetRuntimeConfigForTests({
        organizationId: 'org-1',
        storeId: null,
        terminalId: 'terminal-1',
      })
      expect(hasPosRuntimeConfig()).toBe(false)
    })

    it('terminalId がない場合は false', () => {
      resetRuntimeConfigForTests({
        organizationId: 'org-1',
        storeId: 'store-1',
        terminalId: null,
      })
      expect(hasPosRuntimeConfig()).toBe(false)
    })
  })

  describe('updateStoreTerminal', () => {
    it('storeId と terminalId を更新する', () => {
      resetRuntimeConfigForTests({ organizationId: 'org-1' })
      updateStoreTerminal('store-new', 'terminal-new')
      const config = getRuntimeConfig()
      expect(config.storeId).toBe('store-new')
      expect(config.terminalId).toBe('terminal-new')
    })

    it('organizationId は変更されない', () => {
      resetRuntimeConfigForTests({ organizationId: 'org-1' })
      updateStoreTerminal('store-new', 'terminal-new')
      expect(getRuntimeConfig().organizationId).toBe('org-1')
    })
  })

  describe('needsStoreTerminalSelection', () => {
    it('orgId はあるが storeId/terminalId がない場合 true', () => {
      resetRuntimeConfigForTests({
        organizationId: 'org-1',
        storeId: null,
        terminalId: null,
      })
      expect(needsStoreTerminalSelection()).toBe(true)
    })

    it('orgId がない場合は false', () => {
      resetRuntimeConfigForTests({
        organizationId: null,
        storeId: null,
        terminalId: null,
      })
      expect(needsStoreTerminalSelection()).toBe(false)
    })

    it('全て設定済みなら false', () => {
      resetRuntimeConfigForTests({
        organizationId: 'org-1',
        storeId: 'store-1',
        terminalId: 'terminal-1',
      })
      expect(needsStoreTerminalSelection()).toBe(false)
    })

    it('storeId のみ欠如でも true', () => {
      resetRuntimeConfigForTests({
        organizationId: 'org-1',
        storeId: null,
        terminalId: 'terminal-1',
      })
      expect(needsStoreTerminalSelection()).toBe(true)
    })
  })

  describe('initializeRuntimeConfig', () => {
    it('fetch 失敗時はデフォルト設定を使う', async () => {
      vi.spyOn(globalThis, 'fetch').mockRejectedValueOnce(new Error('network'))
      resetRuntimeConfigForTests()

      const config = await initializeRuntimeConfig()
      expect(config.apiUrl).toBe('http://localhost:8080')
    })

    it('fetch が non-ok を返した場合はデフォルト設定を使う', async () => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: false,
      } as Response)
      resetRuntimeConfigForTests()

      const config = await initializeRuntimeConfig()
      expect(config.apiUrl).toBe('http://localhost:8080')
    })

    it('正常な demo-config.json を読み込む', async () => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: () =>
          Promise.resolve({
            apiUrl: 'http://demo:9999',
            organizationId: 'demo-org',
            storeId: 'demo-store',
            terminalId: 'demo-terminal',
          }),
      } as Response)
      resetRuntimeConfigForTests()

      const config = await initializeRuntimeConfig()
      expect(config.apiUrl).toBe('http://demo:9999')
      expect(config.organizationId).toBe('demo-org')
      expect(config.storeId).toBe('demo-store')
      expect(config.terminalId).toBe('demo-terminal')
    })

    it('二回目の呼び出しは fetch を一度しか呼ばない（キャッシュ）', async () => {
      const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue({
        ok: false,
      } as Response)
      resetRuntimeConfigForTests()

      await initializeRuntimeConfig()
      await initializeRuntimeConfig()
      expect(fetchSpy).toHaveBeenCalledTimes(1)
    })

    it('null の demo config ではデフォルトを使う', async () => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(null),
      } as Response)
      resetRuntimeConfigForTests()

      const config = await initializeRuntimeConfig()
      expect(config.apiUrl).toBe('http://localhost:8080')
    })
  })
})
