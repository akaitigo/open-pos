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
import openpos.product.v1.GetProductRequest
import openpos.product.v1.ListTaxRatesRequest
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.TaxRate
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
