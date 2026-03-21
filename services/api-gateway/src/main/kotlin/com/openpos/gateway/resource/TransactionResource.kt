package com.openpos.gateway.resource

import com.openpos.gateway.cache.RedisCacheService
import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.paginatedResponse
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import openpos.common.v1.DateRange
import openpos.common.v1.PaginationRequest
import openpos.pos.v1.AddTransactionItemRequest
import openpos.pos.v1.ApplyDiscountRequest
import openpos.pos.v1.CreateTransactionRequest
import openpos.pos.v1.FinalizeTransactionRequest
import openpos.pos.v1.GetReceiptRequest
import openpos.pos.v1.GetTransactionRequest
import openpos.pos.v1.ListTransactionsRequest
import openpos.pos.v1.PaymentInput
import openpos.pos.v1.PaymentMethod
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.RemoveTransactionItemRequest
import openpos.pos.v1.TransactionStatus
import openpos.pos.v1.TransactionType
import openpos.pos.v1.UpdateTransactionItemRequest
import openpos.pos.v1.VoidTransactionRequest
import org.eclipse.microprofile.faulttolerance.Timeout

@Path("/api/transactions")
@Blocking
@Timeout(30000)
class TransactionResource {
    @Inject
    @GrpcClient("pos-service")
    lateinit var stub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var cache: RedisCacheService

    @POST
    fun create(body: CreateTransactionBody): Response {
        val request =
            CreateTransactionRequest
                .newBuilder()
                .setStoreId(body.storeId)
                .setTerminalId(body.terminalId)
                .setStaffId(body.staffId)
                .apply {
                    body.type?.let { setType(parseTransactionType(it)) }
                    body.clientId?.let { setClientId(it) }
                }.build()
        val response = grpc.withTenant(stub).createTransaction(request)
        return Response.status(Response.Status.CREATED).entity(response.transaction.toMap()).build()
    }

    @GET
    @Path("/{id}")
    fun get(
        @PathParam("id") id: String,
    ): Map<String, Any?> =
        grpc
            .withTenant(stub)
            .getTransaction(GetTransactionRequest.newBuilder().setId(id).build())
            .transaction
            .toMap()

    @GET
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
        @QueryParam("storeId") storeId: String?,
        @QueryParam("terminalId") terminalId: String?,
        @QueryParam("status") status: String?,
        @QueryParam("startDate") startDate: String?,
        @QueryParam("endDate") endDate: String?,
    ): Map<String, Any> {
        val request =
            ListTransactionsRequest
                .newBuilder()
                .setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).apply {
                    storeId?.let { setStoreId(it) }
                    terminalId?.let { setTerminalId(it) }
                    status?.let { setStatus(parseTransactionStatus(it)) }
                    if (startDate != null || endDate != null) {
                        setDateRange(
                            DateRange
                                .newBuilder()
                                .apply {
                                    startDate?.let { setStart(it) }
                                    endDate?.let { setEnd(it) }
                                }.build(),
                        )
                    }
                }.build()
        val response = grpc.withTenant(stub).listTransactions(request)
        return paginatedResponse(
            data = response.transactionsList.map { it.toMap() },
            pagination = response.pagination,
        )
    }

    @POST
    @Path("/{id}/items")
    fun addItem(
        @PathParam("id") id: String,
        body: AddItemBody,
    ): Map<String, Any?> {
        val request =
            AddTransactionItemRequest
                .newBuilder()
                .setTransactionId(id)
                .setProductId(body.productId)
                .setQuantity(body.quantity)
                .build()
        return grpc
            .withTenant(stub)
            .addTransactionItem(request)
            .transaction
            .toMap()
    }

    @PUT
    @Path("/{id}/items/{itemId}")
    fun updateItem(
        @PathParam("id") id: String,
        @PathParam("itemId") itemId: String,
        body: UpdateItemBody,
    ): Map<String, Any?> {
        val request =
            UpdateTransactionItemRequest
                .newBuilder()
                .setTransactionId(id)
                .setItemId(itemId)
                .setQuantity(body.quantity)
                .build()
        return grpc
            .withTenant(stub)
            .updateTransactionItem(request)
            .transaction
            .toMap()
    }

    @DELETE
    @Path("/{id}/items/{itemId}")
    fun removeItem(
        @PathParam("id") id: String,
        @PathParam("itemId") itemId: String,
    ): Map<String, Any?> {
        val request =
            RemoveTransactionItemRequest
                .newBuilder()
                .setTransactionId(id)
                .setItemId(itemId)
                .build()
        return grpc
            .withTenant(stub)
            .removeTransactionItem(request)
            .transaction
            .toMap()
    }

    @POST
    @Path("/{id}/discount")
    fun applyDiscount(
        @PathParam("id") id: String,
        body: ApplyDiscountBody,
    ): Map<String, Any?> {
        val request =
            ApplyDiscountRequest
                .newBuilder()
                .setTransactionId(id)
                .apply {
                    body.discountId?.let { setDiscountId(it) }
                    body.couponCode?.let { setCouponCode(it) }
                    body.transactionItemId?.let { setTransactionItemId(it) }
                }.build()
        return grpc
            .withTenant(stub)
            .applyDiscount(request)
            .transaction
            .toMap()
    }

    @POST
    @Path("/{id}/finalize")
    fun finalize(
        @PathParam("id") id: String,
        @HeaderParam("Idempotency-Key") idempotencyKey: String?,
        body: FinalizeBody,
    ): Map<String, Any?> {
        val request =
            FinalizeTransactionRequest
                .newBuilder()
                .setTransactionId(id)
                .addAllPayments(
                    body.payments.map { p ->
                        PaymentInput
                            .newBuilder()
                            .setMethod(parsePaymentMethod(p.method))
                            .setAmount(p.amount)
                            .apply {
                                p.received?.let { setReceived(it) }
                                p.reference?.let { setReference(it) }
                            }.build()
                    },
                ).build()
        val tenantStub = grpc.withTenant(stub)
        val finalStub =
            if (!idempotencyKey.isNullOrBlank()) {
                grpc.withIdempotencyKey(tenantStub, idempotencyKey)
            } else {
                tenantStub
            }
        val response = finalStub.finalizeTransaction(request)
        cache.invalidatePattern("openpos:gateway:transaction:*")
        return mapOf(
            "transaction" to response.transaction.toMap(),
            "receipt" to response.receipt.toMap(),
        )
    }

    @POST
    @Path("/{id}/void")
    fun void(
        @PathParam("id") id: String,
        body: VoidBody?,
    ): Map<String, Any?> {
        val request =
            VoidTransactionRequest
                .newBuilder()
                .setTransactionId(id)
                .apply { body?.reason?.let { setReason(it) } }
                .build()
        val response = grpc.withTenant(stub).voidTransaction(request)
        cache.invalidatePattern("openpos:gateway:transaction:*")
        return response.transaction.toMap()
    }

    @GET
    @Path("/{id}/receipt")
    fun getReceipt(
        @PathParam("id") id: String,
    ): Map<String, Any?> =
        grpc
            .withTenant(stub)
            .getReceipt(GetReceiptRequest.newBuilder().setTransactionId(id).build())
            .receipt
            .toMap()
}

private fun parseTransactionType(value: String): TransactionType =
    when (value.uppercase()) {
        "SALE" -> TransactionType.TRANSACTION_TYPE_SALE
        "RETURN" -> TransactionType.TRANSACTION_TYPE_RETURN
        "VOID" -> TransactionType.TRANSACTION_TYPE_VOID
        else -> TransactionType.TRANSACTION_TYPE_UNSPECIFIED
    }

private fun parseTransactionStatus(value: String): TransactionStatus =
    when (value.uppercase()) {
        "DRAFT" -> TransactionStatus.TRANSACTION_STATUS_DRAFT
        "COMPLETED" -> TransactionStatus.TRANSACTION_STATUS_COMPLETED
        "VOIDED" -> TransactionStatus.TRANSACTION_STATUS_VOIDED
        else -> TransactionStatus.TRANSACTION_STATUS_UNSPECIFIED
    }

private fun parsePaymentMethod(value: String): PaymentMethod =
    when (value.uppercase()) {
        "CASH" -> PaymentMethod.PAYMENT_METHOD_CASH
        "CREDIT_CARD" -> PaymentMethod.PAYMENT_METHOD_CREDIT_CARD
        "QR_CODE" -> PaymentMethod.PAYMENT_METHOD_QR_CODE
        else -> PaymentMethod.PAYMENT_METHOD_UNSPECIFIED
    }

data class CreateTransactionBody(
    val storeId: String,
    val terminalId: String,
    val staffId: String,
    val type: String? = null,
    val clientId: String? = null,
)

data class AddItemBody(
    val productId: String,
    val quantity: Int = 1,
)

data class UpdateItemBody(
    val quantity: Int,
)

data class ApplyDiscountBody(
    val discountId: String? = null,
    val couponCode: String? = null,
    val transactionItemId: String? = null,
)

data class FinalizeBody(
    val payments: List<PaymentInputBody>,
)

data class PaymentInputBody(
    val method: String,
    val amount: Long,
    val received: Long? = null,
    val reference: String? = null,
)

data class VoidBody(
    val reason: String? = null,
)
