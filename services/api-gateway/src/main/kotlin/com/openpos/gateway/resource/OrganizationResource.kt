package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
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
import openpos.store.v1.CreateOrganizationRequest
import openpos.store.v1.GetOrganizationRequest
import openpos.store.v1.StoreServiceGrpc
import openpos.store.v1.UpdateOrganizationRequest
import org.eclipse.microprofile.faulttolerance.Timeout

@Path("/api/organizations")
@Blocking
@Timeout(5000)
class OrganizationResource {
    @Inject
    @GrpcClient("store-service")
    lateinit var stub: StoreServiceGrpc.StoreServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @POST
    fun create(body: CreateOrganizationBody): Response {
        val request =
            CreateOrganizationRequest
                .newBuilder()
                .setName(body.name)
                .setBusinessType(body.businessType)
                .apply { body.invoiceNumber?.let { setInvoiceNumber(it) } }
                .build()
        val response = grpc.withTenant(stub).createOrganization(request)
        return Response.status(Response.Status.CREATED).entity(response.organization.toMap()).build()
    }

    @GET
    @Path("/{id}")
    fun get(
        @PathParam("id") id: String,
    ): Map<String, Any?> =
        grpc
            .withTenant(stub)
            .getOrganization(GetOrganizationRequest.newBuilder().setId(id).build())
            .organization
            .toMap()

    @PUT
    @Path("/{id}")
    fun update(
        @PathParam("id") id: String,
        body: UpdateOrganizationBody,
    ): Map<String, Any?> {
        val request =
            UpdateOrganizationRequest
                .newBuilder()
                .setId(id)
                .apply {
                    body.name?.let { setName(it) }
                    body.businessType?.let { setBusinessType(it) }
                    body.invoiceNumber?.let { setInvoiceNumber(it) }
                }.build()
        return grpc
            .withTenant(stub)
            .updateOrganization(request)
            .organization
            .toMap()
    }
}

data class CreateOrganizationBody(
    val name: String,
    val businessType: String,
    val invoiceNumber: String? = null,
)

data class UpdateOrganizationBody(
    val name: String? = null,
    val businessType: String? = null,
    val invoiceNumber: String? = null,
)
