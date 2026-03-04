package com.openpos.analytics.config

import jakarta.enterprise.context.RequestScoped
import java.util.UUID

/**
 * リクエストスコープで organization_id を保持する CDI bean。
 * gRPC Interceptor から設定され、TenantFilterService で参照される。
 */
@RequestScoped
class OrganizationIdHolder {
    var organizationId: UUID? = null
}
