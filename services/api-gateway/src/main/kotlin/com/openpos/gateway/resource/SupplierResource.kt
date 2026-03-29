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
import openpos.inventory.v1.CreateSupplierRequest
import openpos.inventory.v1.GetSupplierRequest
import openpos.inventory.v1.InventoryServiceGrpc
import openpos.inventory.v1.ListSuppliersRequest
import openpos.inventory.v1.UpdateSupplierRequest
import org.eclipse.microprofile.faulttolerance.Timeout

@Path("/api/suppliers")
@Blocking
@Timeout(30000)
class SupplierResource {
    @Inject
    @GrpcClient("inventory-service")
    lateinit var stub: InventoryServiceGrpc.InventoryServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> {
        requireValidPage(page)
        val request =
            ListSuppliersRequest
                .newBuilder()
                .setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).build()
        val response = grpc.withTenant(stub).listSuppliers(request)
        return paginatedResponse(
            data = response.suppliersList.map { it.toMap() },
            pagination = response.pagination,
        )
    }

    @POST
    fun create(body: CreateSupplierBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            CreateSupplierRequest
                .newBuilder()
                .setName(body.name)
                .setContactPerson(body.contactPerson.orEmpty())
                .setEmail(body.email.orEmpty())
                .setPhone(body.phone.orEmpty())
                .setAddress(body.address.orEmpty())
                .build()
        val response = grpc.withTenant(stub).createSupplier(request)
        return Response.status(Response.Status.CREATED).entity(response.supplier.toMap()).build()
    }

    @GET
    @Path("/{id}")
    fun get(
        @PathParam("id") id: String,
    ): Map<String, Any?> {
        val request = GetSupplierRequest.newBuilder().setId(id).build()
        return grpc
            .withTenant(stub)
            .getSupplier(request)
            .supplier
            .toMap()
    }

    @PUT
    @Path("/{id}")
    fun update(
        @PathParam("id") id: String,
        body: UpdateSupplierBody,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            UpdateSupplierRequest
                .newBuilder()
                .setId(id)
                .setName(body.name.orEmpty())
                .setContactPerson(body.contactPerson.orEmpty())
                .setEmail(body.email.orEmpty())
                .setPhone(body.phone.orEmpty())
                .setAddress(body.address.orEmpty())
                .build()
        val response = grpc.withTenant(stub).updateSupplier(request)
        return response.supplier.toMap()
    }
}

data class CreateSupplierBody(
    val name: String,
    val contactPerson: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
)

data class UpdateSupplierBody(
    val name: String? = null,
    val contactPerson: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
)
