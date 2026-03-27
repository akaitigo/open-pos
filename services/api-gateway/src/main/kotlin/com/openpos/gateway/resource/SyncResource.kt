package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import openpos.pos.v1.OfflineTransaction
import openpos.pos.v1.OfflineTransactionItem
import openpos.pos.v1.PaymentInput
import openpos.pos.v1.PaymentMethod
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.SyncOfflineTransactionsRequest
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.faulttolerance.Timeout
import org.jboss.logging.Logger

/**
 * オフライン取引同期エンドポイント。
 *
 * unitPrice のバリデーション:
 * - unitPrice <= 0 は即座に拒否（BadRequest）
 * - unitPrice > maxUnitPrice は WARNING ログ出力後に拒否（BadRequest）
 *
 * NOTE: クライアント送信の unitPrice/taxRate はオフライン時のスナップショットであり、
 * 商品マスタの最新価格との突き合わせ検証は v1.1 で実装予定。
 */
@Path("/api/sync")
@Blocking
@Timeout(30000)
class SyncResource {
    companion object {
        private val log = Logger.getLogger(SyncResource::class.java)
    }

    @Inject
    @GrpcClient("pos-service")
    lateinit var stub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @ConfigProperty(name = "openpos.sync.max-unit-price", defaultValue = "10000000")
    var maxUnitPrice: Long = 10_000_000

    @ConfigProperty(name = "openpos.sync.max-transactions", defaultValue = "100")
    var maxTransactions: Int = 100

    @ConfigProperty(name = "openpos.sync.max-items-per-transaction", defaultValue = "500")
    var maxItemsPerTransaction: Int = 500

    @POST
    @Path("/transactions")
    fun syncTransactions(body: SyncTransactionsBody): Map<String, Any> {
        if (body.transactions.size > maxTransactions) {
            throw BadRequestException(
                "Too many transactions: ${body.transactions.size} exceeds maximum of $maxTransactions",
            )
        }
        body.transactions.forEachIndexed { index, t ->
            if (t.items.size > maxItemsPerTransaction) {
                throw BadRequestException(
                    "Transaction[$index] has too many items: ${t.items.size} exceeds maximum of $maxItemsPerTransaction",
                )
            }
        }
        val request =
            SyncOfflineTransactionsRequest
                .newBuilder()
                .addAllTransactions(
                    body.transactions.map { t ->
                        OfflineTransaction
                            .newBuilder()
                            .setClientId(t.clientId)
                            .setStoreId(t.storeId)
                            .setTerminalId(t.terminalId)
                            .setStaffId(t.staffId)
                            .apply { t.createdAt?.let { setCreatedAt(it) } }
                            .addAllItems(
                                t.items.map { item ->
                                    validateUnitPrice(item, t.clientId)
                                    OfflineTransactionItem
                                        .newBuilder()
                                        .setProductId(item.productId)
                                        .setProductName(item.productName)
                                        .setUnitPrice(item.unitPrice)
                                        .setQuantity(item.quantity)
                                        .setTaxRateName(item.taxRateName)
                                        .setTaxRate(item.taxRate)
                                        .setIsReducedTax(item.isReducedTax)
                                        .build()
                                },
                            ).addAllPayments(
                                t.payments.map { p ->
                                    PaymentInput
                                        .newBuilder()
                                        .setMethod(
                                            when (p.method.uppercase()) {
                                                "CASH" -> PaymentMethod.PAYMENT_METHOD_CASH
                                                "CREDIT_CARD" -> PaymentMethod.PAYMENT_METHOD_CREDIT_CARD
                                                "QR_CODE" -> PaymentMethod.PAYMENT_METHOD_QR_CODE
                                                else -> PaymentMethod.PAYMENT_METHOD_UNSPECIFIED
                                            },
                                        ).setAmount(p.amount)
                                        .apply {
                                            p.received?.let { setReceived(it) }
                                            p.reference?.let { setReference(it) }
                                        }.build()
                                },
                            ).build()
                    },
                ).build()
        val response = grpc.withTenant(stub).syncOfflineTransactions(request)
        return mapOf(
            "results" to
                response.resultsList.map { r ->
                    mapOf(
                        "clientId" to r.clientId,
                        "success" to r.success,
                        "transactionId" to r.transactionId.ifEmpty { null },
                        "error" to r.error.ifEmpty { null },
                    )
                },
        )
    }

    private fun validateUnitPrice(
        item: OfflineItemBody,
        clientId: String,
    ) {
        if (item.unitPrice <= 0) {
            log.warnf(
                "Invalid offline item price: productId=%s, unitPrice=%d, clientId=%s",
                item.productId,
                item.unitPrice,
                clientId,
            )
            throw BadRequestException(
                "Invalid unitPrice for product ${item.productId}: must be greater than 0",
            )
        }
        if (item.unitPrice > maxUnitPrice) {
            log.warnf(
                "Offline item price exceeds maximum: productId=%s, unitPrice=%d, maxUnitPrice=%d, clientId=%s",
                item.productId,
                item.unitPrice,
                maxUnitPrice,
                clientId,
            )
            throw BadRequestException(
                "unitPrice for product ${item.productId} exceeds maximum allowed price",
            )
        }
    }
}

data class SyncTransactionsBody(
    val transactions: List<OfflineTransactionBody>,
)

data class OfflineTransactionBody(
    val clientId: String,
    val storeId: String,
    val terminalId: String,
    val staffId: String,
    val items: List<OfflineItemBody>,
    val payments: List<PaymentInputBody>,
    val createdAt: String? = null,
)

data class OfflineItemBody(
    val productId: String,
    val productName: String,
    val unitPrice: Long,
    val quantity: Int,
    val taxRateName: String,
    val taxRate: String,
    val isReducedTax: Boolean = false,
)
