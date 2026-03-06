package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import openpos.pos.v1.OfflineTransaction
import openpos.pos.v1.OfflineTransactionItem
import openpos.pos.v1.PaymentInput
import openpos.pos.v1.PaymentMethod
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.SyncOfflineTransactionsRequest

@Path("/api/sync")
@Blocking
class SyncResource {
    @Inject
    @GrpcClient("pos-service")
    lateinit var stub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @POST
    @Path("/transactions")
    fun syncTransactions(body: SyncTransactionsBody): Map<String, Any> {
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
