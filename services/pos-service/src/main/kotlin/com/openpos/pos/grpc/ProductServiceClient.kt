package com.openpos.pos.grpc

import com.fasterxml.jackson.databind.ObjectMapper
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.StatusRuntimeException
import io.quarkus.grpc.GrpcClient
import io.quarkus.redis.datasource.RedisDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import openpos.product.v1.BatchGetProductsRequest
import openpos.product.v1.DiscountType
import openpos.product.v1.GetProductRequest
import openpos.product.v1.ListDiscountsRequest
import openpos.product.v1.ListTaxRatesRequest
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.TaxRate
import openpos.product.v1.ValidateCouponRequest
import org.eclipse.microprofile.faulttolerance.CircuitBreaker
import org.eclipse.microprofile.faulttolerance.Retry
import org.jboss.logging.Logger
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * product-service の gRPC クライアント。
 * cache-aside パターンで Redis キャッシュを利用する。
 * キー形式: openpos:pos-service:{orgId}:product-snapshot:{productId}
 * TTL: 3600 秒（1 時間）
 */
@ApplicationScoped
class ProductServiceClient {
    @Inject
    @GrpcClient("product-service")
    lateinit var channel: Channel

    @Inject
    lateinit var redis: RedisDataSource

    @Inject
    lateinit var objectMapper: ObjectMapper

    private val log = Logger.getLogger(ProductServiceClient::class.java)

    companion object {
        const val PRODUCT_SNAPSHOT_TTL_SECONDS = 3600L
        private const val PREFIX = "openpos:pos-service"

        /** gRPC 呼び出しのデフォルトタイムアウト（秒） */
        private const val GRPC_DEADLINE_SECONDS = 5L
    }

    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 10000)
    @Retry(maxRetries = 2, delay = 500)
    fun getProductSnapshot(
        productId: UUID,
        organizationId: UUID,
    ): ProductSnapshot {
        val cacheKey = "$PREFIX:$organizationId:product-snapshot:$productId"

        // cache-aside: まずキャッシュを確認
        val cached = getCached(cacheKey)
        if (cached != null) {
            return cached
        }

        // cache miss: gRPC で取得
        val snapshot = fetchProductSnapshotFromService(productId, organizationId)

        // キャッシュに書き込み（TTL 付き）
        setCache(cacheKey, snapshot)

        return snapshot
    }

    private fun fetchProductSnapshotFromService(
        productId: UUID,
        organizationId: UUID,
    ): ProductSnapshot {
        val stub =
            ProductServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
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
                .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
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
                .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
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

    /**
     * 複数商品のスナップショットを一括取得する（N+1 クエリ防止）。
     * 税率一覧は 1 回だけ取得し、商品ごとの個別取得をまとめて実行する。
     */
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 10000)
    @Retry(maxRetries = 2, delay = 500)
    fun getProductSnapshots(
        productIds: List<UUID>,
        organizationId: UUID,
    ): Map<UUID, ProductSnapshot> {
        if (productIds.isEmpty()) return emptyMap()

        val stub =
            ProductServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .withInterceptors(TenantHeaderInterceptor(organizationId))

        // 税率一覧を 1 回だけ取得
        val taxRatesResponse = stub.listTaxRates(ListTaxRatesRequest.getDefaultInstance())
        val taxRatesById = taxRatesResponse.taxRatesList.associateBy { it.id }

        // BatchGetProducts で一括取得（N+1 RPC 防止）
        val distinctIds = productIds.distinct()
        val batchRequest =
            BatchGetProductsRequest
                .newBuilder()
                .addAllIds(distinctIds.map { it.toString() })
                .build()
        val batchResponse = stub.batchGetProducts(batchRequest)

        return batchResponse.productsList
            .mapNotNull { product ->
                val productId =
                    try {
                        UUID.fromString(product.id)
                    } catch (_: IllegalArgumentException) {
                        return@mapNotNull null
                    }
                val matchedTaxRate = taxRatesById[product.taxRateId] ?: return@mapNotNull null

                productId to
                    ProductSnapshot(
                        name = product.name,
                        price = product.price,
                        taxRateName = matchedTaxRate.name,
                        taxRate = matchedTaxRate.rate,
                        isReduced = matchedTaxRate.isReduced,
                    )
            }.toMap()
    }

    private fun DiscountType.toDbValue(): String =
        when (this) {
            DiscountType.DISCOUNT_TYPE_PERCENTAGE -> "PERCENTAGE"
            DiscountType.DISCOUNT_TYPE_FIXED_AMOUNT -> "FIXED_AMOUNT"
            else -> "UNSPECIFIED"
        }

    private fun getCached(key: String): ProductSnapshot? =
        try {
            val json = redis.value(String::class.java).get(key)
            if (json != null) {
                objectMapper.readValue(json, ProductSnapshot::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            log.warnf("Redis GET failed for key=%s: %s", key, e.message)
            null
        }

    private fun setCache(
        key: String,
        snapshot: ProductSnapshot,
    ) {
        try {
            val json = objectMapper.writeValueAsString(snapshot)
            redis.value(String::class.java).setex(key, PRODUCT_SNAPSHOT_TTL_SECONDS, json)
        } catch (e: Exception) {
            log.warnf("Redis SET failed for key=%s: %s", key, e.message)
        }
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
