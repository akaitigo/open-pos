import { createApiClient } from '@shared-types/openpos'

export const api = createApiClient(import.meta.env.VITE_API_URL || 'http://localhost:8080')

const orgId = import.meta.env.VITE_ORGANIZATION_ID
if (orgId) {
  api.setOrganizationId(orgId)
}
