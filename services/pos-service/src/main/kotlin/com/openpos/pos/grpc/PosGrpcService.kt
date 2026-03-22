package com.openpos.pos.grpc

import com.openpos.pos.entity.JournalEntryEntity
import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionDiscountEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import com.openpos.pos.service.JournalService
import com.openpos.pos.service.PaymentInput
import com.openpos.pos.service.TransactionService
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import openpos.common.v1.PaginationResponse
import openpos.pos.v1.AddTransactionItemRequest
import openpos.pos.v1.AddTransactionItemResponse
import openpos.pos.v1.ApplyDiscountRequest
import openpos.pos.v1.ApplyDiscountResponse
import openpos.pos.v1.CreateTransactionRequest
import openpos.pos.v1.CreateTransactionResponse
import openpos.pos.v1.FinalizeTransactionRequest
import openpos.pos.v1.FinalizeTransactionResponse
import openpos.pos.v1.GetInvoiceReceiptRequest
import openpos.pos.v1.GetInvoiceReceiptResponse
import openpos.pos.v1.GetReceiptRequest
import openpos.pos.v1.GetReceiptResponse
import openpos.pos.v1.GetTransactionRequest
import openpos.pos.v1.GetTransactionResponse
import openpos.pos.v1.JournalEntry
import openpos.pos.v1.ListJournalEntriesRequest
import openpos.pos.v1.ListJournalEntriesResponse
import openpos.pos.v1.ListTransactionsRequest
import openpos.pos.v1.ListTransactionsResponse
import openpos.pos.v1.Payment
import openpos.pos.v1.PaymentMethod
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.Receipt
import openpos.pos.v1.RemoveTransactionItemRequest
import openpos.pos.v1.RemoveTransactionItemResponse
import openpos.pos.v1.TaxSummary
import openpos.pos.v1.Transaction
import openpos.pos.v1.TransactionDiscount
import openpos.pos.v1.TransactionItem
import openpos.pos.v1.TransactionStatus
import openpos.pos.v1.TransactionType
import openpos.pos.v1.UpdateTransactionItemRequest
import openpos.pos.v1.UpdateTransactionItemResponse
import openpos.pos.v1.VoidTransactionRequest
import openpos.pos.v1.VoidTransactionResponse
import java.time.Instant
import java.util.UUID

@GrpcService
@Blocking
class PosGrpcService : PosServiceGrpc.PosServiceImplBase() {
    @Inject
    lateinit var transactionService: TransactionService

    @Inject
    lateinit var journalService: JournalService

    @Inject
    lateinit var productServiceClient: ProductServiceClient

    @Inject
    lateinit var tenantHelper: GrpcTenantHelper

    @Inject
    lateinit var storeServiceClient: StoreServiceClient

    // === Create ===

    override fun createTransaction(
        request: CreateTransactionRequest,
        responseObserver: StreamObserver<CreateTransactionResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val type =
            when (request.type) {
                TransactionType.TRANSACTION_TYPE_RETURN -> "RETURN"
                TransactionType.TRANSACTION_TYPE_VOID -> "VOID"
                else -> "SALE"
            }
        val entity =
            transactionService.createTransaction(
                storeId = request.storeId.toUUID(),
                terminalId = request.terminalId.toUUID(),
                staffId = request.staffId.toUUID(),
                type = type,
                clientId = request.clientId.ifBlank { null },
            )
        responseObserver.onNext(
            CreateTransactionResponse
                .newBuilder()
                .setTransaction(entity.toFullProto())
                .build(),
        )
        responseObserver.onCompleted()
    }

    // === Item Operations ===

    override fun addTransactionItem(
        request: AddTransactionItemRequest,
        responseObserver: StreamObserver<AddTransactionItemResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val quantity = if (request.quantity > 0) request.quantity else 1
        try {
            val entity =
                transactionService.addItem(
                    transactionId = request.transactionId.toUUID(),
                    productId = request.productId.toUUID(),
                    quantity = quantity,
                )
            responseObserver.onNext(
                AddTransactionItemResponse
                    .newBuilder()
                    .setTransaction(entity.toFullProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun updateTransactionItem(
        request: UpdateTransactionItemRequest,
        responseObserver: StreamObserver<UpdateTransactionItemResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity =
                transactionService.updateItem(
                    transactionId = request.transactionId.toUUID(),
                    itemId = request.itemId.toUUID(),
                    quantity = request.quantity,
                )
            responseObserver.onNext(
                UpdateTransactionItemResponse
                    .newBuilder()
                    .setTransaction(entity.toFullProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun removeTransactionItem(
        request: RemoveTransactionItemRequest,
        responseObserver: StreamObserver<RemoveTransactionItemResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity =
                transactionService.removeItem(
                    transactionId = request.transactionId.toUUID(),
                    itemId = request.itemId.toUUID(),
                )
            responseObserver.onNext(
                RemoveTransactionItemResponse
                    .newBuilder()
                    .setTransaction(entity.toFullProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    // === Discount ===

    override fun applyDiscount(
        request: ApplyDiscountRequest,
        responseObserver: StreamObserver<ApplyDiscountResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val transactionId = request.transactionId.toUUID()
            val couponCode = request.couponCode.ifBlank { null }
            val discountId = request.discountId.uuidOrNull()

            val resolvedName: String
            val resolvedType: String
            val resolvedValue: String
            val resolvedAmount: Long
            val resolvedDiscountId: UUID?

            if (couponCode != null) {
                // クーポンコード経由: product-service に問い合わせて割引情報を取得
                val tx = transactionService.getTransaction(transactionId)
                val couponResult = productServiceClient.validateCoupon(couponCode, tx.organizationId)
                if (!couponResult.isValid) {
                    throw IllegalArgumentException(
                        "Coupon is not valid: ${couponResult.reason}",
                    )
                }
                resolvedDiscountId = couponResult.discountId
                resolvedName = couponResult.discountName
                resolvedType = couponResult.discountType
                resolvedValue = couponResult.discountValue
                resolvedAmount =
                    computeDiscountAmount(
                        couponResult.discountType,
                        couponResult.discountValue,
                        transactionId,
                    )
            } else if (discountId != null) {
                // discount_id 直接指定: product-service から割引マスタ取得
                val tx = transactionService.getTransaction(transactionId)
                val discountInfo = productServiceClient.getDiscount(discountId, tx.organizationId)
                resolvedDiscountId = discountId
                resolvedName = discountInfo.name
                resolvedType = discountInfo.discountType
                resolvedValue = discountInfo.value
                resolvedAmount =
                    computeDiscountAmount(
                        discountInfo.discountType,
                        discountInfo.value,
                        transactionId,
                    )
            } else {
                throw IllegalArgumentException("Either coupon_code or discount_id must be specified")
            }

            val entity =
                transactionService.applyDiscount(
                    transactionId = transactionId,
                    discountId = resolvedDiscountId,
                    name = resolvedName,
                    discountType = resolvedType,
                    value = resolvedValue,
                    amount = resolvedAmount,
                    transactionItemId = request.transactionItemId.uuidOrNull(),
                )
            responseObserver.onNext(
                ApplyDiscountResponse
                    .newBuilder()
                    .setTransaction(entity.toFullProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    /**
     * 割引タイプと値から実際の割引金額（銭単位）を計算する。
     * PERCENTAGE の場合は取引小計に対するパーセンテージ、
     * FIXED_AMOUNT の場合はそのまま値を返す。
     */
    private fun computeDiscountAmount(
        discountType: String,
        discountValue: String,
        transactionId: UUID,
    ): Long =
        when (discountType) {
            "PERCENTAGE" -> {
                val tx = transactionService.getTransaction(transactionId)
                val percentage = discountValue.toBigDecimal()
                (tx.subtotal.toBigDecimal() * percentage / 100.toBigDecimal()).toLong()
            }

            "FIXED_AMOUNT" -> {
                discountValue.toLong()
            }

            else -> {
                0L
            }
        }

    // === Finalize ===

    override fun finalizeTransaction(
        request: FinalizeTransactionRequest,
        responseObserver: StreamObserver<FinalizeTransactionResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val payments =
                request.paymentsList.map { p ->
                    PaymentInput(
                        method = p.method.toDbValue(),
                        amount = p.amount,
                        received = if (p.received > 0) p.received else null,
                        reference = p.reference.ifBlank { null },
                    )
                }
            val entity =
                transactionService.finalizeTransaction(
                    transactionId = request.transactionId.toUUID(),
                    payments = payments,
                )
            val receipt = buildReceipt(entity)
            responseObserver.onNext(
                FinalizeTransactionResponse
                    .newBuilder()
                    .setTransaction(entity.toFullProto())
                    .setReceipt(receipt)
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    // === Void ===

    override fun voidTransaction(
        request: VoidTransactionRequest,
        responseObserver: StreamObserver<VoidTransactionResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity =
                transactionService.voidTransaction(
                    transactionId = request.transactionId.toUUID(),
                    reason = request.reason,
                )
            responseObserver.onNext(
                VoidTransactionResponse
                    .newBuilder()
                    .setTransaction(entity.toFullProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    // === Query ===

    override fun getTransaction(
        request: GetTransactionRequest,
        responseObserver: StreamObserver<GetTransactionResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity = transactionService.getTransaction(request.id.toUUID())
            responseObserver.onNext(
                GetTransactionResponse
                    .newBuilder()
                    .setTransaction(entity.toFullProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun listTransactions(
        request: ListTransactionsRequest,
        responseObserver: StreamObserver<ListTransactionsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val page = if (request.hasPagination()) request.pagination.page - 1 else 0
        val pageSize = if (request.hasPagination() && request.pagination.pageSize > 0) request.pagination.pageSize.coerceAtMost(100) else 20

        val statusFilter =
            when (request.status) {
                TransactionStatus.TRANSACTION_STATUS_DRAFT -> "DRAFT"
                TransactionStatus.TRANSACTION_STATUS_COMPLETED -> "COMPLETED"
                TransactionStatus.TRANSACTION_STATUS_VOIDED -> "VOIDED"
                else -> null
            }

        val startDate =
            if (request.hasDateRange() && request.dateRange.start.isNotBlank()) {
                Instant.parse(request.dateRange.start)
            } else {
                null
            }
        val endDate =
            if (request.hasDateRange() && request.dateRange.end.isNotBlank()) {
                Instant.parse(request.dateRange.end)
            } else {
                null
            }

        val (transactions, totalCount) =
            transactionService.listTransactions(
                storeId = request.storeId.uuidOrNull(),
                terminalId = request.terminalId.uuidOrNull(),
                status = statusFilter,
                startDate = startDate,
                endDate = endDate,
                page = page,
                pageSize = pageSize,
            )
        val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0

        val relations = transactionService.batchLoadRelations(transactions.map { it.id })

        responseObserver.onNext(
            ListTransactionsResponse
                .newBuilder()
                .addAllTransactions(
                    transactions.map { tx ->
                        tx.toBatchProto(
                            items = relations.items[tx.id] ?: emptyList(),
                            payments = relations.payments[tx.id] ?: emptyList(),
                            discounts = relations.discounts[tx.id] ?: emptyList(),
                            taxSummaries = relations.taxSummaries[tx.id] ?: emptyList(),
                        )
                    },
                ).setPagination(
                    PaginationResponse
                        .newBuilder()
                        .setPage(page + 1)
                        .setPageSize(pageSize)
                        .setTotalCount(totalCount)
                        .setTotalPages(totalPages)
                        .build(),
                ).build(),
        )
        responseObserver.onCompleted()
    }

    // === Receipt ===

    override fun getReceipt(
        request: GetReceiptRequest,
        responseObserver: StreamObserver<GetReceiptResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val tx = transactionService.getTransaction(request.transactionId.toUUID())
            require(tx.status == "COMPLETED" || tx.status == "VOIDED") {
                "Receipt is only available for COMPLETED or VOIDED transactions"
            }

            responseObserver.onNext(
                GetReceiptResponse
                    .newBuilder()
                    .setReceipt(buildReceipt(tx))
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    // === Journal Entries (#209) ===

    override fun listJournalEntries(
        request: ListJournalEntriesRequest,
        responseObserver: StreamObserver<ListJournalEntriesResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val page = if (request.hasPagination()) request.pagination.page - 1 else 0
        val pageSize = if (request.hasPagination() && request.pagination.pageSize > 0) request.pagination.pageSize.coerceAtMost(100) else 20

        val typeFilter = request.type.ifBlank { null }
        val startDate =
            if (request.hasDateRange() && request.dateRange.start.isNotBlank()) {
                Instant.parse(request.dateRange.start)
            } else {
                null
            }
        val endDate =
            if (request.hasDateRange() && request.dateRange.end.isNotBlank()) {
                Instant.parse(request.dateRange.end)
            } else {
                null
            }

        val (entries, totalCount) = journalService.listEntries(typeFilter, startDate, endDate, page, pageSize)
        val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0

        responseObserver.onNext(
            ListJournalEntriesResponse
                .newBuilder()
                .addAllEntries(entries.map { it.toProto() })
                .setPagination(
                    PaginationResponse
                        .newBuilder()
                        .setPage(page + 1)
                        .setPageSize(pageSize)
                        .setTotalCount(totalCount)
                        .setTotalPages(totalPages)
                        .build(),
                ).build(),
        )
        responseObserver.onCompleted()
    }

    // === Invoice Receipt (#194) ===

    override fun getInvoiceReceipt(
        request: GetInvoiceReceiptRequest,
        responseObserver: StreamObserver<GetInvoiceReceiptResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val tx = transactionService.getTransaction(request.transactionId.toUUID())
            require(tx.status == "COMPLETED") {
                "Invoice receipt is only available for COMPLETED transactions"
            }

            val items = transactionService.getTransactionItems(tx.id)
            val payments = transactionService.getTransactionPayments(tx.id)
            val taxSummaries = transactionService.getTransactionTaxSummaries(tx.id)

            val receiptData = buildInvoiceReceiptData(tx, items, payments, taxSummaries)
            responseObserver.onNext(
                GetInvoiceReceiptResponse
                    .newBuilder()
                    .setReceiptData(receiptData)
                    .setReceiptType("INVOICE")
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    private fun buildInvoiceReceiptData(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
        payments: List<PaymentEntity>,
        taxSummaries: List<TaxSummaryEntity>,
    ): String {
        // 組織のインボイス登録番号を store-service から取得
        val invoiceNumber =
            storeServiceClient.getInvoiceNumber(tx.organizationId)
                ?: throw IllegalArgumentException(
                    "Invoice registration number is not configured for organization: ${tx.organizationId}",
                )

        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine("        領 収 書")
        sb.appendLine("  （適格簡易請求書）")
        sb.appendLine("================================")
        sb.appendLine("取引番号: ${tx.transactionNumber}")
        sb.appendLine("日時: ${tx.completedAt}")
        sb.appendLine("登録番号: $invoiceNumber")
        sb.appendLine("--------------------------------")
        for (item in items) {
            val reducedMark = if (item.isReducedTax) " ※" else ""
            sb.appendLine("${item.productName}$reducedMark")
            sb.appendLine("  ${item.unitPrice / 100}円 x ${item.quantity}  ${item.total / 100}円")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("小計:     ${tx.subtotal / 100}円")
        sb.appendLine("消費税:   ${tx.taxTotal / 100}円")
        if (tx.discountTotal > 0) {
            sb.appendLine("割引:    -${tx.discountTotal / 100}円")
        }
        sb.appendLine("合計:     ${tx.total / 100}円")
        sb.appendLine("--------------------------------")
        for (payment in payments) {
            sb.appendLine("${payment.method}: ${payment.amount / 100}円")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("【税率別内訳（税込）】")
        for (summary in taxSummaries) {
            val reducedMark = if (summary.isReduced) "※" else ""
            sb.appendLine("$reducedMark${summary.taxRateName}")
            sb.appendLine("  対象: ${summary.taxableAmount / 100}円  税: ${summary.taxAmount / 100}円")
        }
        if (taxSummaries.any { it.isReduced }) {
            sb.appendLine("※は軽減税率(8%)対象商品")
        }
        sb.appendLine("================================")
        sb.appendLine("上記正に領収いたしました")
        sb.appendLine("================================")
        return sb.toString()
    }

    private fun JournalEntryEntity.toProto(): JournalEntry =
        JournalEntry
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setType(type)
            .setTransactionId(transactionId?.toString().orEmpty())
            .setStaffId(staffId.toString())
            .setTerminalId(terminalId.toString())
            .setDetails(details)
            .setCreatedAt(createdAt.toString())
            .build()

    private fun buildReceipt(tx: TransactionEntity): Receipt {
        val items = transactionService.getTransactionItems(tx.id)
        val payments = transactionService.getTransactionPayments(tx.id)
        val taxSummaries = transactionService.getTransactionTaxSummaries(tx.id)

        val receiptData = buildReceiptData(tx, items, payments, taxSummaries)

        return Receipt
            .newBuilder()
            .setId(UUID.randomUUID().toString())
            .setTransactionId(tx.id.toString())
            .setReceiptData(receiptData)
            .setCreatedAt(Instant.now().toString())
            .build()
    }

    // === Mapper Extensions ===

    private fun TransactionEntity.toFullProto(): Transaction {
        val items = transactionService.getTransactionItems(id)
        val payments = transactionService.getTransactionPayments(id)
        val discounts = transactionService.getTransactionDiscounts(id)
        val taxSummaries = transactionService.getTransactionTaxSummaries(id)

        return Transaction
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setStoreId(storeId.toString())
            .setTerminalId(terminalId.toString())
            .setStaffId(staffId.toString())
            .setTransactionNumber(transactionNumber)
            .setType(type.toProtoTransactionType())
            .setStatus(status.toProtoTransactionStatus())
            .setClientId(clientId.orEmpty())
            .addAllItems(items.map { it.toProto() })
            .addAllDiscounts(discounts.map { it.toProto() })
            .addAllPayments(payments.map { it.toProto() })
            .addAllTaxSummaries(taxSummaries.map { it.toProto() })
            .setSubtotal(subtotal)
            .setTaxTotal(taxTotal)
            .setDiscountTotal(discountTotal)
            .setTotal(total)
            .setChangeAmount(changeAmount)
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .setCompletedAt(completedAt?.toString().orEmpty())
            .build()
    }

    private fun TransactionEntity.toBatchProto(
        items: List<TransactionItemEntity>,
        payments: List<PaymentEntity>,
        discounts: List<TransactionDiscountEntity>,
        taxSummaries: List<TaxSummaryEntity>,
    ): Transaction =
        Transaction
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setStoreId(storeId.toString())
            .setTerminalId(terminalId.toString())
            .setStaffId(staffId.toString())
            .setTransactionNumber(transactionNumber)
            .setType(type.toProtoTransactionType())
            .setStatus(status.toProtoTransactionStatus())
            .setClientId(clientId.orEmpty())
            .addAllItems(items.map { it.toProto() })
            .addAllDiscounts(discounts.map { it.toProto() })
            .addAllPayments(payments.map { it.toProto() })
            .addAllTaxSummaries(taxSummaries.map { it.toProto() })
            .setSubtotal(subtotal)
            .setTaxTotal(taxTotal)
            .setDiscountTotal(discountTotal)
            .setTotal(total)
            .setChangeAmount(changeAmount)
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .setCompletedAt(completedAt?.toString().orEmpty())
            .build()

    private fun TransactionItemEntity.toProto(): TransactionItem =
        TransactionItem
            .newBuilder()
            .setId(id.toString())
            .setProductId(productId.toString())
            .setProductName(productName)
            .setUnitPrice(unitPrice)
            .setQuantity(quantity)
            .setTaxRateName(taxRateName)
            .setTaxRate(taxRate)
            .setIsReducedTax(isReducedTax)
            .setSubtotal(subtotal)
            .setTaxAmount(taxAmount)
            .setTotal(total)
            .build()

    private fun PaymentEntity.toProto(): Payment =
        Payment
            .newBuilder()
            .setId(id.toString())
            .setMethod(method.toProtoPaymentMethod())
            .setAmount(amount)
            .setReceived(received ?: 0)
            .setChange(change ?: 0)
            .setReference(reference.orEmpty())
            .build()

    private fun TransactionDiscountEntity.toProto(): TransactionDiscount =
        TransactionDiscount
            .newBuilder()
            .setId(id.toString())
            .setDiscountId(discountId?.toString().orEmpty())
            .setName(name)
            .setDiscountType(discountType)
            .setValue(value)
            .setAmount(amount)
            .setTransactionItemId(transactionItemId?.toString().orEmpty())
            .build()

    private fun TaxSummaryEntity.toProto(): TaxSummary =
        TaxSummary
            .newBuilder()
            .setTaxRateName(taxRateName)
            .setTaxRate(taxRate)
            .setIsReduced(isReduced)
            .setTaxableAmount(taxableAmount)
            .setTaxAmount(taxAmount)
            .build()

    // === Utility Extensions ===

    private fun String.toUUID(): UUID =
        try {
            UUID.fromString(this)
        } catch (e: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT.withDescription("Invalid UUID: $this").asRuntimeException()
        }

    private fun String.uuidOrNull(): UUID? = if (isBlank()) null else toUUID()

    private fun String.toProtoTransactionType(): TransactionType =
        when (this) {
            "SALE" -> TransactionType.TRANSACTION_TYPE_SALE
            "RETURN" -> TransactionType.TRANSACTION_TYPE_RETURN
            "VOID" -> TransactionType.TRANSACTION_TYPE_VOID
            else -> TransactionType.TRANSACTION_TYPE_UNSPECIFIED
        }

    private fun String.toProtoTransactionStatus(): TransactionStatus =
        when (this) {
            "DRAFT" -> TransactionStatus.TRANSACTION_STATUS_DRAFT
            "COMPLETED" -> TransactionStatus.TRANSACTION_STATUS_COMPLETED
            "VOIDED" -> TransactionStatus.TRANSACTION_STATUS_VOIDED
            else -> TransactionStatus.TRANSACTION_STATUS_UNSPECIFIED
        }

    private fun String.toProtoPaymentMethod(): PaymentMethod =
        when (this) {
            "CASH" -> PaymentMethod.PAYMENT_METHOD_CASH
            "CREDIT_CARD" -> PaymentMethod.PAYMENT_METHOD_CREDIT_CARD
            "QR_CODE" -> PaymentMethod.PAYMENT_METHOD_QR_CODE
            else -> PaymentMethod.PAYMENT_METHOD_UNSPECIFIED
        }

    private fun PaymentMethod.toDbValue(): String =
        when (this) {
            PaymentMethod.PAYMENT_METHOD_CASH -> "CASH"
            PaymentMethod.PAYMENT_METHOD_CREDIT_CARD -> "CREDIT_CARD"
            PaymentMethod.PAYMENT_METHOD_QR_CODE -> "QR_CODE"
            else -> throw Status.INVALID_ARGUMENT.withDescription("payment method is required").asRuntimeException()
        }

    private fun buildReceiptData(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
        payments: List<PaymentEntity>,
        taxSummaries: List<TaxSummaryEntity>,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine("        領 収 書")
        sb.appendLine("================================")
        sb.appendLine("取引番号: ${tx.transactionNumber}")
        sb.appendLine("日時: ${tx.completedAt}")
        sb.appendLine("--------------------------------")
        for (item in items) {
            val reducedMark = if (item.isReducedTax) " ※" else ""
            sb.appendLine("${item.productName}$reducedMark")
            sb.appendLine("  ${item.unitPrice / 100}円 x ${item.quantity}  ${item.total / 100}円")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("小計:     ${tx.subtotal / 100}円")
        sb.appendLine("消費税:   ${tx.taxTotal / 100}円")
        if (tx.discountTotal > 0) {
            sb.appendLine("割引:    -${tx.discountTotal / 100}円")
        }
        sb.appendLine("合計:     ${tx.total / 100}円")
        sb.appendLine("--------------------------------")
        for (payment in payments) {
            sb.appendLine("${payment.method}: ${payment.amount / 100}円")
            val changeVal = payment.change ?: 0
            if (payment.method == "CASH" && changeVal > 0) {
                sb.appendLine("お釣り: ${changeVal / 100}円")
            }
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("【税率別内訳】")
        for (summary in taxSummaries) {
            val reducedMark = if (summary.isReduced) "※" else ""
            sb.appendLine("$reducedMark${summary.taxRateName}: 対象${summary.taxableAmount / 100}円 税${summary.taxAmount / 100}円")
        }
        if (taxSummaries.any { it.isReduced }) {
            sb.appendLine("※は軽減税率対象商品")
        }
        sb.appendLine("================================")
        return sb.toString()
    }
}
