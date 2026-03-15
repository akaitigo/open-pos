import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  getRuntimeConfig,
  hasOrganizationContext,
  resetRuntimeConfigForTests,
} from './runtime-config'

vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue({
      data: [],
      pagination: { page: 1, pageSize: 1, totalCount: 0, totalPages: 0 },
    }),
    setOrganizationId: vi.fn(),
    setBaseUrl: vi.fn(),
  },
  configureApi: vi.fn(),
  getDefaultApiConfig: () => ({
    apiUrl: 'http://localhost:8080',
    organizationId: '00000000-0000-0000-0000-000000000000',
  }),
}))

describe('runtime-config', () => {
  beforeEach(() => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: '00000000-0000-0000-0000-000000000000',
    })
  })

  it('getRuntimeConfig はデフォルト設定を返す', () => {
    const config = getRuntimeConfig()
    expect(config.apiUrl).toBe('http://localhost:8080')
    expect(config.organizationId).toBe('00000000-0000-0000-0000-000000000000')
  })

  it('hasOrganizationContext は organizationId があるとき true を返す', () => {
    expect(hasOrganizationContext()).toBe(true)
  })

  it('hasOrganizationContext は organizationId が null のとき false を返す', () => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://localhost:8080',
      organizationId: null,
    })
    expect(hasOrganizationContext()).toBe(false)
  })

  it('resetRuntimeConfigForTests で設定を変更できる', () => {
    resetRuntimeConfigForTests({
      apiUrl: 'http://custom:9999',
      organizationId: 'custom-org-id',
    })
    const config = getRuntimeConfig()
    expect(config.apiUrl).toBe('http://custom:9999')
    expect(config.organizationId).toBe('custom-org-id')
  })
})
