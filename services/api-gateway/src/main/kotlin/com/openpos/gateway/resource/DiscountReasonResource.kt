package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import openpos.pos.v1.CreateDiscountReasonRequest
import openpos.pos.v1.ListDiscountReasonsRequest
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.UpdateDiscountReasonRequest
import org.eclipse.microprofile.faulttolerance.Timeout
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

/**
 * 値引き理由コード REST リソース（#216）。
 * RBAC: 一覧は全ロール、作成/更新は OWNER/MANAGER のみ。
 */
@Path("/api/discount-reasons")
@Blocking
@Timeout(30000)
@Tag(name = "Discount Reasons", description = "値引き理由コード管理API")
class DiscountReasonResource {
    @Inject
    @GrpcClient("pos-service")
    lateinit var stub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    @Operation(summary = "値引き理由コード一覧を取得する")
    fun list(): Map<String, Any?> {
        val response = grpc.withTenant(stub).listDiscountReasons(ListDiscountReasonsRequest.getDefaultInstance())
        return mapOf("data" to response.discountReasonsList.map { it.toMap() })
    }

    @POST
    @Operation(summary = "値引き理由コードを作成する")
    fun create(body: CreateDiscountReasonBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            CreateDiscountReasonRequest
                .newBuilder()
                .setCode(body.code)
                .setDescription(body.description)
                .build()
        val response = grpc.withTenant(stub).createDiscountReason(request)
        return Response.status(Response.Status.CREATED).entity(response.discountReason.toMap()).build()
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "値引き理由コードを更新する")
    fun update(
        @PathParam("id") id: String,
        body: UpdateDiscountReasonBody,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val builder = UpdateDiscountReasonRequest.newBuilder().setId(id)
        body.description?.let { builder.setDescription(it) }
        body.isActive?.let {
            builder.setIsActive(
                com.google.protobuf.BoolValue
                    .of(it),
            )
        }
        val response = grpc.withTenant(stub).updateDiscountReason(builder.build())
        return response.discountReason.toMap()
    }
}

data class CreateDiscountReasonBody(
    val code: String,
    val description: String,
)

data class UpdateDiscountReasonBody(
    val description: String? = null,
    val isActive: Boolean? = null,
)
