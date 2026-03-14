package com.openpos.gateway.resource

import com.google.protobuf.BoolValue
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
import openpos.product.v1.CreateTaxRateRequest
import openpos.product.v1.ListTaxRatesRequest
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.UpdateTaxRateRequest

@Path("/api/tax-rates")
@Blocking
class TaxRateResource {
    @Inject
    @GrpcClient("product-service")
    lateinit var stub: ProductServiceGrpc.ProductServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @POST
    fun create(body: CreateTaxRateBody): Response {
        val request =
            CreateTaxRateRequest
                .newBuilder()
                .setName(body.name)
                .setRate(body.rate)
                .setIsReduced(body.isReduced)
                .setIsDefault(body.isDefault)
                .build()
        val response = grpc.withTenant(stub).createTaxRate(request)
        return Response.status(Response.Status.CREATED).entity(response.taxRate.toMap()).build()
    }

    @GET
    fun list(): List<Map<String, Any?>> =
        grpc
            .withTenant(stub)
            .listTaxRates(ListTaxRatesRequest.getDefaultInstance())
            .taxRatesList
            .map { it.toMap() }

    @PUT
    @Path("/{id}")
    fun update(
        @PathParam("id") id: String,
        body: UpdateTaxRateBody,
    ): Map<String, Any?> {
        val request =
            UpdateTaxRateRequest
                .newBuilder()
                .setId(id)
                .apply {
                    body.name?.let { setName(it) }
                    body.rate?.let { setRate(it) }
                    body.isReduced?.let { setIsReducedValue(BoolValue.of(it)) }
                    body.isDefault?.let { setIsDefaultValue(BoolValue.of(it)) }
                }.build()
        return grpc
            .withTenant(stub)
            .updateTaxRate(request)
            .taxRate
            .toMap()
    }
}

data class CreateTaxRateBody(
    val name: String,
    val rate: String,
    val isReduced: Boolean = false,
    val isDefault: Boolean = false,
)

data class UpdateTaxRateBody(
    val name: String? = null,
    val rate: String? = null,
    val isReduced: Boolean? = null,
    val isDefault: Boolean? = null,
)
