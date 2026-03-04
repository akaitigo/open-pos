package com.openpos.gateway.config

import jakarta.enterprise.context.RequestScoped
import java.util.UUID

@RequestScoped
class TenantContext {
    var organizationId: UUID? = null
}
