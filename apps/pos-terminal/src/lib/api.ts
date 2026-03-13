import { createApiClient } from '@shared-types/openpos'

const defaultApiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const defaultOrganizationId = import.meta.env.VITE_ORGANIZATION_ID ?? null

export const api = createApiClient(defaultApiUrl)

if (defaultOrganizationId) {
  api.setOrganizationId(defaultOrganizationId)
}

export interface PosApiConfig {
  apiUrl: string
  organizationId: string | null
}

export function configureApi(config: PosApiConfig) {
  api.setBaseUrl(config.apiUrl)
  api.setOrganizationId(config.organizationId)
}

export function getDefaultApiConfig(): PosApiConfig {
  return {
    apiUrl: defaultApiUrl,
    organizationId: defaultOrganizationId,
  }
}
