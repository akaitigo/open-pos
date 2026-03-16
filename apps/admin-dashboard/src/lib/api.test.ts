import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@shared-types/openpos', () => {
  const mockClient = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
    setOrganizationId: vi.fn(),
    setBaseUrl: vi.fn(),
  }
  return {
    createApiClient: vi.fn(() => mockClient),
  }
})

describe('api module', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  it('api クライアントをエクスポートする', async () => {
    const mod = await import('./api')
    expect(mod.api).toBeDefined()
    expect(mod.api.get).toBeDefined()
    expect(mod.api.post).toBeDefined()
    expect(mod.api.put).toBeDefined()
    expect(mod.api.delete).toBeDefined()
  })

  it('configureApi で api のベースURLと組織IDを設定する', async () => {
    const mod = await import('./api')
    mod.configureApi({ apiUrl: 'http://custom:9999', organizationId: 'org-123' })
    expect(mod.api.setBaseUrl).toHaveBeenCalledWith('http://custom:9999')
    expect(mod.api.setOrganizationId).toHaveBeenCalledWith('org-123')
  })

  it('configureApi で organizationId が null の場合もセットする', async () => {
    const mod = await import('./api')
    mod.configureApi({ apiUrl: 'http://localhost:8080', organizationId: null })
    expect(mod.api.setOrganizationId).toHaveBeenCalledWith(null)
  })

  it('getDefaultApiConfig はデフォルト設定を返す', async () => {
    const mod = await import('./api')
    const config = mod.getDefaultApiConfig()
    expect(config).toHaveProperty('apiUrl')
    expect(config).toHaveProperty('organizationId')
    expect(typeof config.apiUrl).toBe('string')
  })

  it('configureApi は apiUrl と organizationId を個別にセットする', async () => {
    const mod = await import('./api')
    mod.configureApi({ apiUrl: 'http://test:8080', organizationId: 'test-org' })
    expect(mod.api.setBaseUrl).toHaveBeenCalledWith('http://test:8080')
    expect(mod.api.setOrganizationId).toHaveBeenCalledWith('test-org')
  })
})
