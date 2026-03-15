package com.openpos.analytics.grpc

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.OrganizationIdInterceptor
import com.openpos.analytics.config.TenantFilterService
import io.grpc.Status
import io.grpc.StatusRuntimeException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class GrpcTenantHelper {
    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    fun setupTenantContext() {
        val orgId =
            OrganizationIdInterceptor.ORGANIZATION_ID_CTX_KEY.get()
                ?: throw StatusRuntimeException(
                    Status.UNAUTHENTICATED.withDescription("organization_id not found in gRPC context"),
                )
        organizationIdHolder.organizationId = orgId
        tenantFilterService.enableFilter()
    }
}
