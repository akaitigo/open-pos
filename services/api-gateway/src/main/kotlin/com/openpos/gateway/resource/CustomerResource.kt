package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.paginatedResponse
import com.openpos.gateway.config.requireValidPage
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import openpos.common.v1.PaginationRequest
import openpos.store.v1.CreateCustomerRequest
import openpos.store.v1.EarnPointsRequest
import openpos.store.v1.GetCustomerRequest
import openpos.store.v1.ListCustomersRequest
import openpos.store.v1.RedeemPointsRequest
import openpos.store.v1.StoreServiceGrpc
import openpos.store.v1.UpdateCustomerRequest
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@Path("/api/customers")
@Blocking
@Tag(name = "Customers", description = "顧客管理・ポイントAPI")
class CustomerResource {
    @Inject
    @GrpcClient("store-service")
    lateinit var stub: StoreServiceGrpc.StoreServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @POST
    @Operation(summary = "顧客を作成する")
    fun create(body: CreateCustomerBody): Response {
        val request =
            CreateCustomerRequest
                .newBuilder()
                .setName(body.name)
                .apply {
                    body.email?.let { setEmail(it) }
                    body.phone?.let { setPhone(it) }
                    body.notes?.let { setNotes(it) }
                }.build()
        val response = grpc.withTenant(stub).createCustomer(request)
        return Response.status(Response.Status.CREATED).entity(response.customer.toMap()).build()
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "顧客を取得する")
    fun get(
        @PathParam("id") id: String,
    ): Map<String, Any?> =
        grpc
            .withTenant(stub)
            .getCustomer(GetCustomerRequest.newBuilder().setId(id).build())
            .customer
            .toMap()

    @GET
    @Operation(summary = "顧客一覧を取得する")
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
        @QueryParam("search") search: String?,
    ): Map<String, Any> {
        requireValidPage(page)
        val request =
            ListCustomersRequest
                .newBuilder()
                .setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).apply {
                    search?.let { setSearch(it) }
                }.build()
        val response = grpc.withTenant(stub).listCustomers(request)
        return paginatedResponse(
            data = response.customersList.map { it.toMap() },
            pagination = response.pagination,
        )
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "顧客を更新する")
    fun update(
        @PathParam("id") id: String,
        body: UpdateCustomerBody,
    ): Map<String, Any?> {
        val request =
            UpdateCustomerRequest
                .newBuilder()
                .setId(id)
                .apply {
                    body.name?.let { setName(it) }
                    body.email?.let { setEmail(it) }
                    body.phone?.let { setPhone(it) }
                    body.notes?.let { setNotes(it) }
                }.build()
        return grpc
            .withTenant(stub)
            .updateCustomer(request)
            .customer
            .toMap()
    }

    @POST
    @Path("/{id}/earn-points")
    @Operation(summary = "ポイントを付与する")
    fun earnPoints(
        @PathParam("id") id: String,
        body: EarnPointsBody,
    ): Map<String, Any?> {
        val request =
            EarnPointsRequest
                .newBuilder()
                .setCustomerId(id)
                .setTransactionTotal(body.transactionTotal)
                .apply {
                    body.transactionId?.let { setTransactionId(it) }
                }.build()
        val response = grpc.withTenant(stub).earnPoints(request)
        return mapOf(
            "earnedPoints" to response.earnedPoints,
            "customer" to response.customer.toMap(),
        )
    }

    @POST
    @Path("/{id}/redeem-points")
    @Operation(summary = "ポイントを利用する")
    fun redeemPoints(
        @PathParam("id") id: String,
        body: RedeemPointsBody,
    ): Map<String, Any?> {
        val request =
            RedeemPointsRequest
                .newBuilder()
                .setCustomerId(id)
                .setPoints(body.points)
                .apply {
                    body.transactionId?.let { setTransactionId(it) }
                }.build()
        val response = grpc.withTenant(stub).redeemPoints(request)
        return mapOf(
            "success" to response.success,
            "customer" to response.customer.toMap(),
        )
    }
}

data class CreateCustomerBody(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val notes: String? = null,
)

data class UpdateCustomerBody(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val notes: String? = null,
)

data class EarnPointsBody(
    val transactionTotal: Long,
    val transactionId: String? = null,
)

data class RedeemPointsBody(
    val points: Long,
    val transactionId: String? = null,
)
