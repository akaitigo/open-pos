import { createApiClient } from '@shared-types/openpos'

const defaultApiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const defaultOrganizationId = import.meta.env.VITE_ORGANIZATION_ID ?? null

export const api = createApiClient(defaultApiUrl)

if (defaultOrganizationId) {
  api.setOrganizationId(defaultOrganizationId)
}

export interface AdminApiConfig {
  apiUrl: string
  organizationId?: string | null
}

export function configureApi(config: AdminApiConfig) {
  api.setBaseUrl(config.apiUrl)
  api.setOrganizationId(config.organizationId)
}

export function getDefaultApiConfig(): AdminApiConfig {
  return {
    apiUrl: defaultApiUrl,
    organizationId: defaultOrganizationId,
  }
}
