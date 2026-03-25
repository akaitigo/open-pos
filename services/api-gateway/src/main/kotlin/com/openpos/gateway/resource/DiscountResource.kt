package com.openpos.gateway.resource

import com.google.protobuf.BoolValue
import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import openpos.product.v1.CreateDiscountRequest
import openpos.product.v1.DeleteDiscountRequest
import openpos.product.v1.DiscountType
import openpos.product.v1.ListDiscountsRequest
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.UpdateDiscountRequest
import org.eclipse.microprofile.faulttolerance.Timeout

@Path("/api/discounts")
@Blocking
@Timeout(30000)
class DiscountResource {
    @Inject
    @GrpcClient("product-service")
    lateinit var stub: ProductServiceGrpc.ProductServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @POST
    fun create(body: CreateDiscountBody): Response {
        val request =
            CreateDiscountRequest
                .newBuilder()
                .setName(body.name)
                .setDiscountType(body.discountType.toProtoDiscountType())
                .setValue(body.value)
                .apply {
                    body.startDate?.let { setStartDate(it) }
                    body.endDate?.let { setEndDate(it) }
                }.build()
        val response = grpc.withTenant(stub).createDiscount(request)
        return Response.status(Response.Status.CREATED).entity(response.discount.toMap()).build()
    }

    @GET
    fun list(): List<Map<String, Any?>> =
        grpc
            .withTenant(stub)
            .listDiscounts(ListDiscountsRequest.getDefaultInstance())
            .discountsList
            .map { it.toMap() }

    @PUT
    @Path("/{id}")
    fun update(
        @PathParam("id") id: String,
        body: UpdateDiscountBody,
    ): Map<String, Any?> {
        val request =
            UpdateDiscountRequest
                .newBuilder()
                .setId(id)
                .apply {
                    body.name?.let { setName(it) }
                    body.discountType?.let { setDiscountType(it.toProtoDiscountType()) }
                    body.value?.let { setValue(it) }
                    body.startDate?.let { setStartDate(it) }
                    body.endDate?.let { setEndDate(it) }
                    body.isActive?.let { setIsActiveValue(BoolValue.of(it)) }
                }.build()
        return grpc
            .withTenant(stub)
            .updateDiscount(request)
            .discount
            .toMap()
    }

    @DELETE
    @Path("/{id}")
    fun delete(
        @PathParam("id") id: String,
    ): Response {
        grpc.withTenant(stub).deleteDiscount(DeleteDiscountRequest.newBuilder().setId(id).build())
        return Response.noContent().build()
    }
}

data class CreateDiscountBody(
    val name: String,
    val discountType: String,
    val value: String,
    val startDate: String? = null,
    val endDate: String? = null,
)

data class UpdateDiscountBody(
    val name: String? = null,
    val discountType: String? = null,
    val value: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isActive: Boolean? = null,
)

private fun String.toProtoDiscountType(): DiscountType =
    when (uppercase()) {
        "PERCENTAGE" -> DiscountType.DISCOUNT_TYPE_PERCENTAGE
        "FIXED_AMOUNT" -> DiscountType.DISCOUNT_TYPE_FIXED_AMOUNT
        else -> DiscountType.DISCOUNT_TYPE_UNSPECIFIED
    }
