package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response
import openpos.store.v1.GetOrganizationRequest
import openpos.store.v1.StoreServiceGrpc
import openpos.store.v1.UpdateOrganizationRequest
import org.eclipse.microprofile.faulttolerance.Timeout

/**
 * X-Organization-Id ヘッダーで特定される「現在の組織」を操作する補助エンドポイント。
 * Admin ダッシュボードの組織設定画面は /api/organization (単数形) を呼び出すため、
 * 組織 ID をパスに含めずに自分の組織を取得・更新できるようにする。
 */
@Path("/api/organization")
@Blocking
@Timeout(30000)
class CurrentOrganizationResource {
    @Inject
    @GrpcClient("store-service")
    lateinit var stub: StoreServiceGrpc.StoreServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    fun getCurrent(): Response {
        val orgId =
            tenantContext.organizationId
                ?: return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(mapOf("error" to "MISSING_ORGANIZATION_ID", "message" to "X-Organization-Id header is required"))
                    .build()
        val response =
            grpc
                .withTenant(stub)
                .getOrganization(GetOrganizationRequest.newBuilder().setId(orgId.toString()).build())
        return Response.ok(response.organization.toMap()).build()
    }

    @PUT
    fun updateCurrent(body: UpdateOrganizationBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val orgId =
            tenantContext.organizationId
                ?: return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(mapOf("error" to "MISSING_ORGANIZATION_ID", "message" to "X-Organization-Id header is required"))
                    .build()
        val request =
            UpdateOrganizationRequest
                .newBuilder()
                .setId(orgId.toString())
                .apply {
                    body.name?.let { setName(it) }
                    body.businessType?.let { setBusinessType(it) }
                    body.invoiceNumber?.let { setInvoiceNumber(it) }
                }.build()
        val response = grpc.withTenant(stub).updateOrganization(request)
        return Response.ok(response.organization.toMap()).build()
    }
}
