package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import openpos.product.v1.CreateCouponRequest
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.ValidateCouponRequest
import org.eclipse.microprofile.faulttolerance.Timeout

@Path("/api/coupons")
@Blocking
@Timeout(30000)
class CouponResource {
    @Inject
    @GrpcClient("product-service")
    lateinit var stub: ProductServiceGrpc.ProductServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @GET
    fun list(): List<Map<String, Any?>> {
        // product-service に ListCoupons RPC はないため、
        // ValidateCoupon を個別に呼ぶのは非効率。
        // 現時点では空リストを返し、ListCoupons RPC 追加後に正式実装する。
        // TODO: ListCoupons RPC を proto に追加して正式実装する
        return emptyList()
    }

    @POST
    fun create(body: CreateCouponBody): Response {
        val request =
            CreateCouponRequest
                .newBuilder()
                .setCode(body.code)
                .setDiscountId(body.discountId)
                .setMaxUses(body.maxUses)
                .apply {
                    body.startDate?.let { setStartDate(it) }
                    body.endDate?.let { setEndDate(it) }
                }.build()
        val response = grpc.withTenant(stub).createCoupon(request)
        return Response.status(Response.Status.CREATED).entity(response.coupon.toMap()).build()
    }

    @GET
    @Path("/validate/{code}")
    fun validate(
        @PathParam("code") code: String,
    ): Map<String, Any?> {
        val response =
            grpc
                .withTenant(stub)
                .validateCoupon(ValidateCouponRequest.newBuilder().setCode(code).build())
        return mapOf(
            "isValid" to response.isValid,
            "coupon" to if (response.hasCoupon()) response.coupon.toMap() else null,
            "discount" to if (response.hasDiscount()) response.discount.toMap() else null,
            "reason" to response.reason.ifEmpty { null },
        )
    }
}

data class CreateCouponBody(
    val code: String,
    val discountId: String,
    val maxUses: Int = 0,
    val startDate: String? = null,
    val endDate: String? = null,
)
