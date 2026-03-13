import { configureApi, getDefaultApiConfig } from '@/lib/api'
import { useAuthStore } from '@/stores/auth-store'

export interface PosRuntimeConfig {
  apiUrl: string
  organizationId: string | null
  storeId: string | null
  terminalId: string | null
}

interface DemoRuntimeConfig {
  apiUrl?: unknown
  organizationId?: unknown
  storeId?: unknown
  terminalId?: unknown
}

let runtimeConfig: PosRuntimeConfig = {
  ...getDefaultApiConfig(),
  storeId: import.meta.env.VITE_DEFAULT_STORE_ID ?? null,
  terminalId: import.meta.env.VITE_DEFAULT_TERMINAL_ID ?? null,
}
let initializationPromise: Promise<PosRuntimeConfig> | null = null

function normalizeString(value: unknown): string | null {
  return typeof value === 'string' && value.trim() ? value.trim() : null
}

function mergeConfig(demoConfig?: DemoRuntimeConfig | null): PosRuntimeConfig {
  return {
    apiUrl: normalizeString(demoConfig?.apiUrl) ?? getDefaultApiConfig().apiUrl,
    organizationId:
      normalizeString(demoConfig?.organizationId) ?? getDefaultApiConfig().organizationId ?? null,
    storeId: normalizeString(demoConfig?.storeId) ?? import.meta.env.VITE_DEFAULT_STORE_ID ?? null,
    terminalId:
      normalizeString(demoConfig?.terminalId) ?? import.meta.env.VITE_DEFAULT_TERMINAL_ID ?? null,
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

function syncAuthStateWithRuntimeConfig(config: PosRuntimeConfig) {
  const authState = useAuthStore.getState()
  const hasConfiguredTerminal = Boolean(config.storeId && config.terminalId)

  if (!hasConfiguredTerminal) {
    if (authState.isAuthenticated) {
      authState.logout()
    }
    return
  }

  if (
    authState.isAuthenticated &&
    (authState.storeId !== config.storeId || authState.terminalId !== config.terminalId)
  ) {
    authState.logout()
  }
}

export async function initializeRuntimeConfig(): Promise<PosRuntimeConfig> {
  if (!initializationPromise) {
    initializationPromise = loadDemoRuntimeConfig().then((demoConfig) => {
      runtimeConfig = mergeConfig(demoConfig)
      configureApi(runtimeConfig)
      syncAuthStateWithRuntimeConfig(runtimeConfig)
      return runtimeConfig
    })
  }

  return initializationPromise
}

export function getRuntimeConfig(): PosRuntimeConfig {
  return runtimeConfig
}

export function hasPosRuntimeConfig(): boolean {
  return Boolean(runtimeConfig.organizationId && runtimeConfig.storeId && runtimeConfig.terminalId)
}

export function resetRuntimeConfigForTests(config?: Partial<PosRuntimeConfig>) {
  initializationPromise = null
  runtimeConfig = {
    ...getDefaultApiConfig(),
    storeId: import.meta.env.VITE_DEFAULT_STORE_ID ?? null,
    terminalId: import.meta.env.VITE_DEFAULT_TERMINAL_ID ?? null,
    ...config,
  }
  configureApi(runtimeConfig)
}
