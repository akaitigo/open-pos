package com.openpos.pos.grpc

import com.openpos.pos.config.OrganizationIdInterceptor
import com.openpos.pos.entity.DrawerEntity
import com.openpos.pos.entity.GiftCardEntity
import com.openpos.pos.entity.JournalEntryEntity
import com.openpos.pos.entity.SettlementEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.service.DiscountReasonService
import com.openpos.pos.service.DrawerService
import com.openpos.pos.service.GiftCardService
import com.openpos.pos.service.JournalService
import com.openpos.pos.service.PaymentInput
import com.openpos.pos.service.ReservationService
import com.openpos.pos.service.SettlementService
import com.openpos.pos.service.TransactionService
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import openpos.pos.v1.AddTransactionItemRequest
import openpos.pos.v1.AddTransactionItemResponse
import openpos.pos.v1.ApplyDiscountRequest
import openpos.pos.v1.ApplyDiscountResponse
import openpos.pos.v1.CloseDrawerRequest
import openpos.pos.v1.CloseDrawerResponse
import openpos.pos.v1.CreateSettlementRequest
import openpos.pos.v1.CreateSettlementResponse
import openpos.pos.v1.CreateTransactionRequest
import openpos.pos.v1.CreateTransactionResponse
import openpos.pos.v1.Drawer
import openpos.pos.v1.FinalizeTransactionRequest
import openpos.pos.v1.FinalizeTransactionResponse
import openpos.pos.v1.GetDrawerStatusRequest
import openpos.pos.v1.GetDrawerStatusResponse
import openpos.pos.v1.GetInvoiceReceiptRequest
import openpos.pos.v1.GetInvoiceReceiptResponse
import openpos.pos.v1.GetReceiptRequest
import openpos.pos.v1.GetReceiptResponse
import openpos.pos.v1.GetSettlementRequest
import openpos.pos.v1.GetSettlementResponse
import openpos.pos.v1.GetStaffSalesReportRequest
import openpos.pos.v1.GetStaffSalesReportResponse
import openpos.pos.v1.GetTransactionRequest
import openpos.pos.v1.GetTransactionResponse
import openpos.pos.v1.JournalEntry
import openpos.pos.v1.ListJournalEntriesRequest
import openpos.pos.v1.ListJournalEntriesResponse
import openpos.pos.v1.ListTransactionsRequest
import openpos.pos.v1.ListTransactionsResponse
import openpos.pos.v1.OpenDrawerRequest
import openpos.pos.v1.OpenDrawerResponse
import openpos.pos.v1.PaymentMethod
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.Receipt
import openpos.pos.v1.RemoveTransactionItemRequest
import openpos.pos.v1.RemoveTransactionItemResponse
import openpos.pos.v1.Settlement
import openpos.pos.v1.SyncOfflineTransactionsRequest
import openpos.pos.v1.SyncOfflineTransactionsResponse
import openpos.pos.v1.TaxSummary
import openpos.pos.v1.Transaction
import openpos.pos.v1.TransactionDiscount
import openpos.pos.v1.TransactionItem
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
    lateinit var drawerService: DrawerService

    @Inject
    lateinit var settlementService: SettlementService

    @Inject
    lateinit var giftCardService: GiftCardService

    @Inject
    lateinit var discountReasonService: DiscountReasonService

    @Inject
    lateinit var reservationService: ReservationService

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
                .setTransaction(entity.toFullProto(transactionService))
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
            val orgId = requireNotNull(tenantHelper.organizationIdHolder.organizationId) { "organizationId is not set" }
            val isCustomProduct = request.customProductName.isNotBlank()

            val entity =
                if (isCustomProduct) {
                    // アドホック商品: 商品マスタ不要、税率のみ取得
                    val taxRateId = request.customTaxRateId.toUUID()
                    val taxRateSnapshot = transactionService.fetchTaxRateSnapshot(taxRateId, orgId)
                    transactionService.addCustomItem(
                        transactionId = request.transactionId.toUUID(),
                        productName = request.customProductName,
                        unitPrice = request.customProductPrice,
                        quantity = quantity,
                        taxRateSnapshot = taxRateSnapshot,
                    )
                } else {
                    // 通常の商品追加: gRPC外部呼び出しを@Transactional外で実行（デッドロック防止 #608）
                    val productId = request.productId.toUUID()
                    val snapshot = transactionService.fetchProductSnapshot(productId, orgId)
                    transactionService.addItem(
                        transactionId = request.transactionId.toUUID(),
                        productId = productId,
                        quantity = quantity,
                        productSnapshot = snapshot,
                    )
                }
            responseObserver.onNext(
                AddTransactionItemResponse
                    .newBuilder()
                    .setTransaction(entity.toFullProto(transactionService))
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
                    .setTransaction(entity.toFullProto(transactionService))
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
                    .setTransaction(entity.toFullProto(transactionService))
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
            val resolved =
                resolveDiscountApplication(
                    request = request,
                    transactionId = transactionId,
                    transactionItemId = request.transactionItemId.uuidOrNull(),
                    loadTransaction = transactionService::getTransaction,
                    validateCoupon = productServiceClient::validateCoupon,
                    getDiscount = productServiceClient::getDiscount,
                )

            val entity =
                transactionService.applyDiscount(
                    transactionId = resolved.transactionId,
                    discountId = resolved.discountId,
                    name = resolved.name,
                    discountType = resolved.discountType,
                    value = resolved.value,
                    amount = resolved.amount,
                    transactionItemId = resolved.transactionItemId,
                )
            responseObserver.onNext(
                ApplyDiscountResponse
                    .newBuilder()
                    .setTransaction(entity.toFullProto(transactionService))
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
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
            val idempotencyKey = OrganizationIdInterceptor.IDEMPOTENCY_KEY_CTX_KEY.get()
            val entity =
                transactionService.finalizeTransaction(
                    transactionId = request.transactionId.toUUID(),
                    payments = payments,
                    idempotencyKey = idempotencyKey,
                )
            val items = transactionService.getTransactionItems(entity.id)
            val persistedPayments = transactionService.getTransactionPayments(entity.id)
            val taxSummaries = transactionService.getTransactionTaxSummaries(entity.id)
            val receipt = buildReceiptProto(entity, items, persistedPayments, taxSummaries)
            responseObserver.onNext(
                FinalizeTransactionResponse
                    .newBuilder()
                    .setTransaction(entity.toFullProto(transactionService))
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
                    .setTransaction(entity.toFullProto(transactionService))
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
                    .setTransaction(entity.toFullProto(transactionService))
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
        val query = resolveTransactionListQuery(request)
        val (transactions, totalCount) =
            transactionService.listTransactions(
                storeId = query.storeId,
                terminalId = query.terminalId,
                status = query.status,
                startDate = query.startDate,
                endDate = query.endDate,
                page = query.page,
                pageSize = query.pageSize,
            )

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
                ).setPagination(buildPaginationResponse(query.page, query.pageSize, totalCount))
                .build(),
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
            val items = transactionService.getTransactionItems(tx.id)
            val payments = transactionService.getTransactionPayments(tx.id)
            val taxSummaries = transactionService.getTransactionTaxSummaries(tx.id)

            responseObserver.onNext(
                GetReceiptResponse
                    .newBuilder()
                    .setReceipt(buildReceiptProto(tx, items, payments, taxSummaries))
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
        val query = resolveJournalEntriesQuery(request)
        val (entries, totalCount) = journalService.listEntries(query.type, query.startDate, query.endDate, query.page, query.pageSize)

        responseObserver.onNext(
            ListJournalEntriesResponse
                .newBuilder()
                .addAllEntries(entries.map { it.toProto() })
                .setPagination(buildPaginationResponse(query.page, query.pageSize, totalCount))
                .build(),
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
            val invoiceNumber =
                storeServiceClient.getInvoiceNumber(tx.organizationId)
                    ?: throw IllegalArgumentException(
                        "Invoice registration number is not configured for organization: ${tx.organizationId}",
                    )

            val receiptData = buildInvoiceReceiptData(tx, items, payments, taxSummaries, invoiceNumber)
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

    // === Drawer (#783) ===

    override fun openDrawer(
        request: OpenDrawerRequest,
        responseObserver: StreamObserver<OpenDrawerResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        try {
            val entity =
                drawerService.openDrawer(
                    storeId = request.storeId.toUUID(),
                    terminalId = request.terminalId.toUUID(),
                    openingAmount = request.openingAmount,
                )
            responseObserver.onNext(
                OpenDrawerResponse
                    .newBuilder()
                    .setDrawer(entity.toProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun closeDrawer(
        request: CloseDrawerRequest,
        responseObserver: StreamObserver<CloseDrawerResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity =
                drawerService.closeDrawer(
                    storeId = request.storeId.toUUID(),
                    terminalId = request.terminalId.toUUID(),
                )
            responseObserver.onNext(
                CloseDrawerResponse
                    .newBuilder()
                    .setDrawer(entity.toProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun getDrawerStatus(
        request: GetDrawerStatusRequest,
        responseObserver: StreamObserver<GetDrawerStatusResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity =
                drawerService.getDrawerStatus(
                    storeId = request.storeId.toUUID(),
                    terminalId = request.terminalId.toUUID(),
                )
            responseObserver.onNext(
                GetDrawerStatusResponse
                    .newBuilder()
                    .setDrawer(entity.toProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    // === Settlement (#784) ===

    override fun createSettlement(
        request: CreateSettlementRequest,
        responseObserver: StreamObserver<CreateSettlementResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        try {
            val entity =
                settlementService.createSettlement(
                    storeId = request.storeId.toUUID(),
                    terminalId = request.terminalId.toUUID(),
                    staffId = request.staffId.toUUID(),
                    cashActual = request.cashActual,
                )
            responseObserver.onNext(
                CreateSettlementResponse
                    .newBuilder()
                    .setSettlement(entity.toProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun getSettlement(
        request: GetSettlementRequest,
        responseObserver: StreamObserver<GetSettlementResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity = settlementService.getSettlement(request.id.toUUID())
            responseObserver.onNext(
                GetSettlementResponse
                    .newBuilder()
                    .setSettlement(entity.toProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    // === SyncOfflineTransactions (#785) ===

    override fun syncOfflineTransactions(
        request: SyncOfflineTransactionsRequest,
        responseObserver: StreamObserver<SyncOfflineTransactionsResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val results =
            request.transactionsList.map { offlineTx ->
                syncOfflineTransactionResult(offlineTx) { input ->
                    transactionService.syncOfflineTransaction(
                        storeId = input.storeId,
                        terminalId = input.terminalId,
                        staffId = input.staffId,
                        clientId = input.clientId,
                        items = input.items,
                        payments = input.payments,
                        createdAt = input.createdAt,
                    )
                }
            }
        responseObserver.onNext(
            SyncOfflineTransactionsResponse
                .newBuilder()
                .addAllResults(results)
                .build(),
        )
        responseObserver.onCompleted()
    }

    // === GiftCard ===

    override fun listGiftCards(
        request: openpos.pos.v1.ListGiftCardsRequest,
        responseObserver: StreamObserver<openpos.pos.v1.ListGiftCardsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val cards = giftCardService.listAll()
        val response =
            openpos.pos.v1.ListGiftCardsResponse
                .newBuilder()
                .addAllGiftCards(cards.map { it.toGiftCardProto() })
                .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getGiftCard(
        request: openpos.pos.v1.GetGiftCardRequest,
        responseObserver: StreamObserver<openpos.pos.v1.GetGiftCardResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val card = giftCardService.findByCode(request.code)
        if (card == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Gift card not found: ${request.code}").asRuntimeException())
            return
        }
        responseObserver.onNext(
            openpos.pos.v1.GetGiftCardResponse
                .newBuilder()
                .setGiftCard(card.toGiftCardProto())
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun createGiftCard(
        request: openpos.pos.v1.CreateGiftCardRequest,
        responseObserver: StreamObserver<openpos.pos.v1.CreateGiftCardResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val expiresAt = if (request.expiresAt.isNotBlank()) Instant.parse(request.expiresAt) else null
        val card = giftCardService.create(request.initialAmount, expiresAt)
        responseObserver.onNext(
            openpos.pos.v1.CreateGiftCardResponse
                .newBuilder()
                .setGiftCard(card.toGiftCardProto())
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun activateGiftCard(
        request: openpos.pos.v1.ActivateGiftCardRequest,
        responseObserver: StreamObserver<openpos.pos.v1.ActivateGiftCardResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val card = giftCardService.activate(request.code)
        responseObserver.onNext(
            openpos.pos.v1.ActivateGiftCardResponse
                .newBuilder()
                .setGiftCard(card.toGiftCardProto())
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun redeemGiftCard(
        request: openpos.pos.v1.RedeemGiftCardRequest,
        responseObserver: StreamObserver<openpos.pos.v1.RedeemGiftCardResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val card = giftCardService.redeem(request.code, request.amount)
        responseObserver.onNext(
            openpos.pos.v1.RedeemGiftCardResponse
                .newBuilder()
                .setGiftCard(card.toGiftCardProto())
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun getGiftCardBalance(
        request: openpos.pos.v1.GetGiftCardBalanceRequest,
        responseObserver: StreamObserver<openpos.pos.v1.GetGiftCardBalanceResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val card = giftCardService.getBalance(request.code)
        responseObserver.onNext(
            openpos.pos.v1.GetGiftCardBalanceResponse
                .newBuilder()
                .setCode(card.code)
                .setBalance(card.balance)
                .setStatus(card.status)
                .build(),
        )
        responseObserver.onCompleted()
    }

    // === Staff Sales Report (#1029) ===

    override fun getStaffSalesReport(
        request: GetStaffSalesReportRequest,
        responseObserver: StreamObserver<GetStaffSalesReportResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val query = resolveStaffSalesReportQuery(request)
            val aggregated = transactionService.aggregateStaffSales(query.storeId, query.startDate, query.endDate)
            val orgId = requireNotNull(tenantHelper.organizationIdHolder.organizationId) { "organizationId is not set" }
            val staffNameMap = loadStaffNameMapOrEmpty(orgId, query.storeId, storeServiceClient::getStaffNameMap)
            val items = buildStaffSalesItems(aggregated, staffNameMap)

            responseObserver.onNext(
                GetStaffSalesReportResponse
                    .newBuilder()
                    .addAllItems(items)
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    // === DiscountReason (#1034) ===

    override fun listDiscountReasons(
        request: openpos.pos.v1.ListDiscountReasonsRequest,
        responseObserver: StreamObserver<openpos.pos.v1.ListDiscountReasonsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val reasons =
                if (request.includeInactive) {
                    discountReasonService.listAll()
                } else {
                    discountReasonService.listActive()
                }
            responseObserver.onNext(
                openpos.pos.v1.ListDiscountReasonsResponse
                    .newBuilder()
                    .addAllDiscountReasons(reasons.map { it.toDiscountReasonProto() })
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun createDiscountReason(
        request: openpos.pos.v1.CreateDiscountReasonRequest,
        responseObserver: StreamObserver<openpos.pos.v1.CreateDiscountReasonResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity = discountReasonService.create(request.code, request.description)
            responseObserver.onNext(
                openpos.pos.v1.CreateDiscountReasonResponse
                    .newBuilder()
                    .setDiscountReason(entity.toDiscountReasonProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun updateDiscountReason(
        request: openpos.pos.v1.UpdateDiscountReasonRequest,
        responseObserver: StreamObserver<openpos.pos.v1.UpdateDiscountReasonResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity =
                discountReasonService.update(
                    id = request.id.toUUID(),
                    description = request.description.ifBlank { null },
                    isActive = if (request.hasIsActive()) request.isActive.value else null,
                ) ?: throw Status.NOT_FOUND.withDescription("DiscountReason not found: ${request.id}").asRuntimeException()
            responseObserver.onNext(
                openpos.pos.v1.UpdateDiscountReasonResponse
                    .newBuilder()
                    .setDiscountReason(entity.toDiscountReasonProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    // === Reservation (#1035) ===

    override fun listReservations(
        request: openpos.pos.v1.ListReservationsRequest,
        responseObserver: StreamObserver<openpos.pos.v1.ListReservationsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val storeId = request.storeId.toUUID()
            val status = request.status.ifBlank { null }
            val (reservations, _) = reservationService.listByStoreId(storeId, status, 0, 100)
            responseObserver.onNext(
                openpos.pos.v1.ListReservationsResponse
                    .newBuilder()
                    .addAllReservations(reservations.map { it.toReservationProto() })
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun createReservation(
        request: openpos.pos.v1.CreateReservationRequest,
        responseObserver: StreamObserver<openpos.pos.v1.CreateReservationResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        try {
            val entity =
                reservationService.create(
                    storeId = request.storeId.toUUID(),
                    customerName = request.customerName.ifBlank { null },
                    customerPhone = request.customerPhone.ifBlank { null },
                    items = request.items,
                    reservedUntil = Instant.parse(request.reservedUntil),
                    note = request.note.ifBlank { null },
                )
            responseObserver.onNext(
                openpos.pos.v1.CreateReservationResponse
                    .newBuilder()
                    .setReservation(entity.toReservationProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun getReservation(
        request: openpos.pos.v1.GetReservationRequest,
        responseObserver: StreamObserver<openpos.pos.v1.GetReservationResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity =
                reservationService.findById(request.id.toUUID())
                    ?: throw Status.NOT_FOUND.withDescription("Reservation not found: ${request.id}").asRuntimeException()
            responseObserver.onNext(
                openpos.pos.v1.GetReservationResponse
                    .newBuilder()
                    .setReservation(entity.toReservationProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun fulfillReservation(
        request: openpos.pos.v1.FulfillReservationRequest,
        responseObserver: StreamObserver<openpos.pos.v1.FulfillReservationResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity =
                reservationService.fulfill(request.id.toUUID())
                    ?: throw Status.NOT_FOUND.withDescription("Reservation not found: ${request.id}").asRuntimeException()
            responseObserver.onNext(
                openpos.pos.v1.FulfillReservationResponse
                    .newBuilder()
                    .setReservation(entity.toReservationProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

    override fun cancelReservation(
        request: openpos.pos.v1.CancelReservationRequest,
        responseObserver: StreamObserver<openpos.pos.v1.CancelReservationResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity =
                reservationService.cancel(request.id.toUUID())
                    ?: throw Status.NOT_FOUND.withDescription("Reservation not found: ${request.id}").asRuntimeException()
            responseObserver.onNext(
                openpos.pos.v1.CancelReservationResponse
                    .newBuilder()
                    .setReservation(entity.toReservationProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
    }

}
