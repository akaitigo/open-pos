package com.openpos.gateway.resource

import com.google.protobuf.BoolValue
import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.SessionTokenService
import com.openpos.gateway.config.paginatedResponse
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
import openpos.store.v1.AuthenticateByPinRequest
import openpos.store.v1.CreateStaffRequest
import openpos.store.v1.GetStaffRequest
import openpos.store.v1.ListStaffRequest
import openpos.store.v1.StaffRole
import openpos.store.v1.StoreServiceGrpc
import openpos.store.v1.UpdateStaffRequest
import org.eclipse.microprofile.config.inject.ConfigProperty

@Path("/api/staff")
@Blocking
class StaffResource {
    @Inject
    @GrpcClient("store-service")
    lateinit var stub: StoreServiceGrpc.StoreServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var sessionTokenService: SessionTokenService

    @ConfigProperty(name = "openpos.auth.enabled", defaultValue = "true")
    var authEnabled: Boolean = true

    @POST
    fun create(body: CreateStaffBody): Response {
        val request =
            CreateStaffRequest
                .newBuilder()
                .setStoreId(body.storeId)
                .setName(body.name)
                .setEmail(body.email.orEmpty())
                .setRole(body.role.toProtoRole())
                .setPin(body.pin)
                .build()
        val response = grpc.withTenant(stub).createStaff(request)
        return Response.status(Response.Status.CREATED).entity(response.staff.toMap()).build()
    }

    @GET
    @Path("/{id}")
    fun get(
        @PathParam("id") id: String,
    ): Map<String, Any?> =
        grpc
            .withTenant(stub)
            .getStaff(GetStaffRequest.newBuilder().setId(id).build())
            .staff
            .toMap()

    @GET
    fun list(
        @QueryParam("storeId") storeId: String,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> {
        val request =
            ListStaffRequest
                .newBuilder()
                .setStoreId(storeId)
                .setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).build()
        val response = grpc.withTenant(stub).listStaff(request)
        return paginatedResponse(
            data = response.staffList.map { it.toMap() },
            pagination = response.pagination,
        )
    }

    @PUT
    @Path("/{id}")
    fun update(
        @PathParam("id") id: String,
        body: UpdateStaffBody,
    ): Map<String, Any?> {
        val request =
            UpdateStaffRequest
                .newBuilder()
                .setId(id)
                .apply {
                    body.name?.let { setName(it) }
                    body.email?.let { setEmail(it) }
                    body.role?.let { setRole(it.toProtoRole()) }
                    body.pin?.let { setPin(it) }
                    body.isActive?.let { setIsActiveValue(BoolValue.of(it)) }
                }.build()
        return grpc
            .withTenant(stub)
            .updateStaff(request)
            .staff
            .toMap()
    }

    @POST
    @Path("/{id}/authenticate")
    fun authenticate(
        @PathParam("id") id: String,
        body: AuthenticateBody,
    ): Map<String, Any?> {
        val request =
            AuthenticateByPinRequest
                .newBuilder()
                .setStoreId(body.storeId)
                .setStaffId(id)
                .setPin(body.pin)
                .build()
        val response = grpc.withTenant(stub).authenticateByPin(request)
        val result = mutableMapOf<String, Any?>("success" to response.success)
        if (response.hasStaff()) {
            val staff = response.staff
            result["staff"] = staff.toMap()

            // 認証成功時にセッショントークンを発行
            if (response.success && authEnabled) {
                val roleName =
                    when (staff.role) {
                        StaffRole.STAFF_ROLE_OWNER -> "OWNER"
                        StaffRole.STAFF_ROLE_MANAGER -> "MANAGER"
                        StaffRole.STAFF_ROLE_CASHIER -> "CASHIER"
                        else -> "CASHIER"
                    }
                val token =
                    sessionTokenService.generateToken(
                        staffId = staff.id,
                        staffRole = roleName,
                        storeId = staff.storeId,
                        organizationId = staff.organizationId,
                    )
                result["token"] = token
            }
        }
        if (response.reason.isNotEmpty()) result["reason"] = response.reason
        return result
    }

    private fun String.toProtoRole(): StaffRole =
        when (this.uppercase()) {
            "OWNER" -> StaffRole.STAFF_ROLE_OWNER
            "MANAGER" -> StaffRole.STAFF_ROLE_MANAGER
            "CASHIER" -> StaffRole.STAFF_ROLE_CASHIER
            else -> StaffRole.STAFF_ROLE_CASHIER
        }
}

data class CreateStaffBody(
    val storeId: String,
    val name: String,
    val email: String? = null,
    val role: String = "CASHIER",
    val pin: String,
)

data class UpdateStaffBody(
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val pin: String? = null,
    val isActive: Boolean? = null,
)

data class AuthenticateBody(
    val storeId: String,
    val pin: String,
)
