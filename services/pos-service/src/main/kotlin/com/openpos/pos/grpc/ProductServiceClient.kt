package com.openpos.pos.grpc

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.StatusRuntimeException
import io.quarkus.grpc.GrpcClient
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import openpos.product.v1.DiscountType
import openpos.product.v1.GetProductRequest
import openpos.product.v1.ListDiscountsRequest
import openpos.product.v1.ListTaxRatesRequest
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.TaxRate
import openpos.product.v1.ValidateCouponRequest
import java.util.UUID

@ApplicationScoped
class ProductServiceClient {
    @Inject
    @GrpcClient("product-service")
    lateinit var channel: Channel

    fun getProductSnapshot(
        productId: UUID,
        organizationId: UUID,
    ): ProductSnapshot {
        val stub =
            ProductServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(TenantHeaderInterceptor(organizationId))

        val productResponse =
            stub.getProduct(
                GetProductRequest.newBuilder().setId(productId.toString()).build(),
            )
        val product = productResponse.product

        val taxRatesResponse = stub.listTaxRates(ListTaxRatesRequest.getDefaultInstance())
        val matchedTaxRate: TaxRate =
            taxRatesResponse.taxRatesList.firstOrNull { tr -> tr.id == product.taxRateId }
                ?: throw StatusRuntimeException(
                    io.grpc.Status.INTERNAL
                        .withDescription("Tax rate not found for product: $productId"),
                )

        return ProductSnapshot(
            name = product.name,
            price = product.price,
            taxRateName = matchedTaxRate.name,
            taxRate = matchedTaxRate.rate,
            isReduced = matchedTaxRate.isReduced,
        )
    }

    /**
     * クーポンコードを検証し、紐付く割引情報を取得する。
     */
    fun validateCoupon(
        couponCode: String,
        organizationId: UUID,
    ): CouponValidationResult {
        val stub =
            ProductServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(TenantHeaderInterceptor(organizationId))

        val response =
            stub.validateCoupon(
                ValidateCouponRequest.newBuilder().setCode(couponCode).build(),
            )

        if (!response.isValid) {
            return CouponValidationResult(
                isValid = false,
                reason = response.reason,
            )
        }

        val discount = response.discount
        return CouponValidationResult(
            isValid = true,
            discountId = UUID.fromString(discount.id),
            discountName = discount.name,
            discountType = discount.discountType.toDbValue(),
            discountValue = discount.value,
        )
    }

    /**
     * 割引IDから割引マスタ情報を取得する。
     */
    fun getDiscount(
        discountId: UUID,
        organizationId: UUID,
    ): DiscountInfo {
        val stub =
            ProductServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(TenantHeaderInterceptor(organizationId))

        val response =
            stub.listDiscounts(
                ListDiscountsRequest.newBuilder().setActiveOnly(false).build(),
            )

        val discount =
            response.discountsList.firstOrNull { it.id == discountId.toString() }
                ?: throw StatusRuntimeException(
                    io.grpc.Status.NOT_FOUND
                        .withDescription("Discount not found: $discountId"),
                )

        return DiscountInfo(
            name = discount.name,
            discountType = discount.discountType.toDbValue(),
            value = discount.value,
        )
    }

    private fun DiscountType.toDbValue(): String =
        when (this) {
            DiscountType.DISCOUNT_TYPE_PERCENTAGE -> "PERCENTAGE"
            DiscountType.DISCOUNT_TYPE_FIXED_AMOUNT -> "FIXED_AMOUNT"
            else -> "UNSPECIFIED"
        }

    private class TenantHeaderInterceptor(
        private val organizationId: UUID,
    ) : ClientInterceptor {
        override fun <ReqT, RespT> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel,
        ): ClientCall<ReqT, RespT> =
            object :
                ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions),
                ) {
                override fun start(
                    responseListener: Listener<RespT>,
                    headers: Metadata,
                ) {
                    headers.put(
                        Metadata.Key.of("x-organization-id", Metadata.ASCII_STRING_MARSHALLER),
                        organizationId.toString(),
                    )
                    super.start(responseListener, headers)
                }
            }
    }
}

data class ProductSnapshot(
    val name: String,
    val price: Long,
    val taxRateName: String,
    val taxRate: String,
    val isReduced: Boolean,
)

data class CouponValidationResult(
    val isValid: Boolean,
    val discountId: UUID? = null,
    val discountName: String = "",
    val discountType: String = "",
    val discountValue: String = "",
    val reason: String? = null,
)

data class DiscountInfo(
    val name: String,
    val discountType: String,
    val value: String,
)
