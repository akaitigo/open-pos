package com.openpos.inventory.grpc

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.OrganizationIdInterceptor
import com.openpos.inventory.config.TenantFilterService
import io.grpc.Status
import io.grpc.StatusRuntimeException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * gRPC コンテキストから OrganizationIdHolder へテナントIDを転送するヘルパー。
 * 各 gRPC メソッドの先頭で呼び出す。
 */
@ApplicationScoped
class GrpcTenantHelper {
    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    /**
     * gRPC Context から organizationId を取得し、
     * OrganizationIdHolder に設定してフィルターを有効化する。
     */
    fun setupTenantContext() {
        val orgId =
            OrganizationIdInterceptor.ORGANIZATION_ID_CTX_KEY.get()
                ?: throw StatusRuntimeException(
                    Status.UNAUTHENTICATED.withDescription("organization_id not found in gRPC context"),
                )
        organizationIdHolder.organizationId = orgId
        tenantFilterService.enableFilter()
    }

    /**
     * gRPC Context から organizationId を取得して OrganizationIdHolder に設定する。
     * フィルターは有効化しない（create 系メソッド用）。
     */
    fun setupTenantContextWithoutFilter() {
        val orgId =
            OrganizationIdInterceptor.ORGANIZATION_ID_CTX_KEY.get()
                ?: throw StatusRuntimeException(
                    Status.UNAUTHENTICATED.withDescription("organization_id not found in gRPC context"),
                )
        organizationIdHolder.organizationId = orgId
    }
}
