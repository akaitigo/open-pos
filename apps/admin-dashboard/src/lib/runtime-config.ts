import { configureApi, getDefaultApiConfig } from '@/lib/api'

export interface AdminRuntimeConfig {
  apiUrl: string
  organizationId: string | null
}

interface DemoRuntimeConfig {
  apiUrl?: unknown
  organizationId?: unknown
}

let runtimeConfig: AdminRuntimeConfig = getDefaultApiConfig()
let initializationPromise: Promise<AdminRuntimeConfig> | null = null

function normalizeString(value: unknown): string | null {
  return typeof value === 'string' && value.trim() ? value.trim() : null
}

function mergeConfig(demoConfig?: DemoRuntimeConfig | null): AdminRuntimeConfig {
  const defaults = getDefaultApiConfig()

  return {
    apiUrl: normalizeString(demoConfig?.apiUrl) ?? defaults.apiUrl,
    organizationId: normalizeString(demoConfig?.organizationId) ?? defaults.organizationId ?? null,
  }
}

async function loadDemoRuntimeConfig(): Promise<DemoRuntimeConfig | null> {
  try {
    const response = await fetch(`/demo-config.json?ts=${Date.now()}`, {
      cache: 'no-store',
    })
    if (!response.ok) {
      return null
    }
    const data: unknown = await response.json()
    return typeof data === 'object' && data !== null ? (data as DemoRuntimeConfig) : null
  } catch {
    return null
  }
}

export async function initializeRuntimeConfig(): Promise<AdminRuntimeConfig> {
  if (!initializationPromise) {
    initializationPromise = loadDemoRuntimeConfig().then((demoConfig) => {
      runtimeConfig = mergeConfig(demoConfig)
      configureApi(runtimeConfig)
      return runtimeConfig
    })
  }

  return initializationPromise
}

export function getRuntimeConfig(): AdminRuntimeConfig {
  return runtimeConfig
}

export function hasOrganizationContext(): boolean {
  return Boolean(runtimeConfig.organizationId)
}

export function resetRuntimeConfigForTests(config?: Partial<AdminRuntimeConfig>) {
  initializationPromise = null
  runtimeConfig = {
    ...getDefaultApiConfig(),
    ...config,
  }
  configureApi(runtimeConfig)
}
