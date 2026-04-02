package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionDiscountEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import com.openpos.pos.event.EventPublisher
import com.openpos.pos.event.SaleCompletedEventDto
import com.openpos.pos.event.SaleItemDto
import com.openpos.pos.event.SalePaymentDto
import com.openpos.pos.event.SaleVoidedEventDto
import com.openpos.pos.grpc.BusinessPreconditionException
import com.openpos.pos.grpc.InvalidInputException
import com.openpos.pos.grpc.ProductServiceClient
import com.openpos.pos.grpc.ProductSnapshot
import com.openpos.pos.grpc.ResourceNotFoundException
import com.openpos.pos.grpc.TaxRateSnapshot
import com.openpos.pos.repository.PaymentRepository
import com.openpos.pos.repository.TaxSummaryRepository
import com.openpos.pos.repository.TransactionDiscountRepository
import com.openpos.pos.repository.TransactionItemRepository
import com.openpos.pos.repository.TransactionRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@ApplicationScoped
class TransactionService {
    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var meterRegistry: MeterRegistry

    private val transactionsCompletedCounter: Counter by lazy {
        Counter
            .builder("openpos_transactions_completed_total")
            .description("Total number of completed transactions")
            .register(meterRegistry)
    }

    private val transactionsVoidedCounter: Counter by lazy {
        Counter
            .builder("openpos_transactions_voided_total")
            .description("Total number of voided transactions")
            .register(meterRegistry)
    }

    private val revenueCounter: Counter by lazy {
        Counter
            .builder("openpos_revenue_total")
            .description("Total revenue in sen (smallest currency unit)")
            .register(meterRegistry)
    }

    @Inject
    lateinit var itemRepository: TransactionItemRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var discountRepository: TransactionDiscountRepository

    @Inject
    lateinit var taxSummaryRepository: TaxSummaryRepository

    @Inject
    lateinit var taxCalculationService: TaxCalculationService

    @Inject
    lateinit var productServiceClient: ProductServiceClient

    @Inject
    lateinit var eventPublisher: EventPublisher

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    // === Create ===

    /**
     * 新規取引を作成する。
     *
     * clientId が指定されている場合、同一 clientId の既存取引があればそれを返す（冪等性保証）。
     *
     * @param storeId 店舗ID
     * @param terminalId 端末ID
     * @param staffId スタッフID
     * @param type 取引種別（空白の場合は "SALE"）
     * @param clientId クライアント側で生成した一意ID（オフライン対応用、省略可）
     * @return 作成または既存の取引エンティティ
     */
    @Transactional
    fun createTransaction(
        storeId: UUID,
        terminalId: UUID,
        staffId: UUID,
        type: String,
        clientId: String?,
    ): TransactionEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }

        if (!clientId.isNullOrBlank()) {
            val existing = transactionRepository.findByClientId(clientId, orgId)
            if (existing != null) return existing
        }

        val entity =
            TransactionEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.terminalId = terminalId
                this.staffId = staffId
                this.transactionNumber = generateTransactionNumber()
                this.type = type.ifBlank { "SALE" }
                this.status = "DRAFT"
                this.clientId = clientId?.ifBlank { null }
            }
        transactionRepository.persist(entity)
        return entity
    }

    // === Item Operations ===

    /**
     * 商品スナップショットを取得する（@Transactional外で呼ぶこと）。
     *
     * gRPC 外部呼び出しのためDBロック保持中に実行しない。
     *
     * @param productId 商品ID
     * @param organizationId 組織ID
     * @return 商品スナップショット
     */
    fun fetchProductSnapshot(
        productId: UUID,
        organizationId: UUID,
    ): ProductSnapshot = productServiceClient.getProductSnapshot(productId, organizationId)

    fun fetchTaxRateSnapshot(
        taxRateId: UUID,
        organizationId: UUID,
    ): TaxRateSnapshot = productServiceClient.getTaxRateSnapshot(taxRateId, organizationId)

    /**
     * 取引に商品を追加する。
     *
     * 同一商品が既に存在する場合は数量を加算する。追加後、取引合計を再計算する。
     *
     * @param transactionId 取引ID
     * @param productId 商品ID
     * @param quantity 追加数量（1以上）
     * @param productSnapshot 事前取得済みの商品スナップショット（省略時は内部で取得）
     * @return 更新後の取引エンティティ
     * @throws InvalidInputException 数量が0以下の場合
     * @throws ResourceNotFoundException 取引が存在しない場合
     * @throws BusinessPreconditionException 取引がDRAFTステータスでない場合
     */
    @Transactional
    fun addItem(
        transactionId: UUID,
        productId: UUID,
        quantity: Int,
        productSnapshot: ProductSnapshot? = null,
    ): TransactionEntity {
        if (quantity <= 0) throw InvalidInputException("quantity must be positive")
        val tx = getWritableTransaction(transactionId)
        val orgId = tx.organizationId

        val product = productSnapshot ?: productServiceClient.getProductSnapshot(productId, orgId)

        val existingItem = itemRepository.findByTransactionAndProduct(transactionId, productId)
        if (existingItem != null) {
            existingItem.quantity += quantity
            val taxResult =
                taxCalculationService.calculateItemTax(
                    existingItem.unitPrice,
                    existingItem.quantity,
                    existingItem.taxRate,
                )
            existingItem.subtotal = taxResult.subtotal
            existingItem.taxAmount = taxResult.taxAmount
            existingItem.total = taxResult.total
            itemRepository.persist(existingItem)
        } else {
            val taxResult = taxCalculationService.calculateItemTax(product.price, quantity, product.taxRate)
            val item =
                TransactionItemEntity().apply {
                    this.organizationId = orgId
                    this.transactionId = transactionId
                    this.productId = productId
                    this.productName = product.name
                    this.unitPrice = product.price
                    this.quantity = quantity
                    this.taxRateName = product.taxRateName
                    this.taxRate = product.taxRate
                    this.isReducedTax = product.isReduced
                    this.subtotal = taxResult.subtotal
                    this.taxAmount = taxResult.taxAmount
                    this.total = taxResult.total
                }
            itemRepository.persist(item)
        }

        recalculateTransactionTotals(tx)
        return tx
    }

    @Transactional
    fun addCustomItem(
        transactionId: UUID,
        productName: String,
        unitPrice: Long,
        quantity: Int,
        taxRateSnapshot: TaxRateSnapshot,
    ): TransactionEntity {
        if (quantity <= 0) throw InvalidInputException("quantity must be positive")
        if (unitPrice < 0) throw InvalidInputException("unit price must not be negative")
        if (productName.isBlank()) throw InvalidInputException("custom product name must not be blank")

        val tx = getWritableTransaction(transactionId)
        val orgId = tx.organizationId

        val taxResult = taxCalculationService.calculateItemTax(unitPrice, quantity, taxRateSnapshot.rate)
        val item =
            TransactionItemEntity().apply {
                this.organizationId = orgId
                this.transactionId = transactionId
                this.productId = null
                this.productName = productName
                this.unitPrice = unitPrice
                this.quantity = quantity
                this.taxRateName = taxRateSnapshot.name
                this.taxRate = taxRateSnapshot.rate
                this.isReducedTax = taxRateSnapshot.isReduced
                this.subtotal = taxResult.subtotal
                this.taxAmount = taxResult.taxAmount
                this.total = taxResult.total
            }
        itemRepository.persist(item)

        recalculateTransactionTotals(tx)
        return tx
    }

    /**
     * 取引内の商品の数量を更新する。
     *
     * 税額・小計を再計算し、取引合計も再計算する。
     *
     * @param transactionId 取引ID
     * @param itemId 取引明細ID
     * @param quantity 新しい数量（1以上）
     * @return 更新後の取引エンティティ
     * @throws InvalidInputException 数量が0以下の場合
     * @throws ResourceNotFoundException 取引または明細が存在しない場合
     * @throws BusinessPreconditionException 明細が指定取引に属さない場合、または取引がDRAFTでない場合
     */
    @Transactional
    fun updateItem(
        transactionId: UUID,
        itemId: UUID,
        quantity: Int,
    ): TransactionEntity {
        if (quantity <= 0) throw InvalidInputException("quantity must be positive, use removeItem for deletion")
        val tx = getWritableTransaction(transactionId)

        val item =
            itemRepository.findById(itemId)
                ?: throw ResourceNotFoundException("Item not found: $itemId")
        if (item.transactionId != transactionId) throw BusinessPreconditionException("Item does not belong to this transaction")

        item.quantity = quantity
        val taxResult = taxCalculationService.calculateItemTax(item.unitPrice, quantity, item.taxRate)
        item.subtotal = taxResult.subtotal
        item.taxAmount = taxResult.taxAmount
        item.total = taxResult.total
        itemRepository.persist(item)

        recalculateTransactionTotals(tx)
        return tx
    }

    /**
     * 取引から商品を削除する。
     *
     * 削除後、取引合計を再計算する。
     *
     * @param transactionId 取引ID
     * @param itemId 取引明細ID
     * @return 更新後の取引エンティティ
     * @throws ResourceNotFoundException 取引または明細が存在しない場合
     * @throws BusinessPreconditionException 明細が指定取引に属さない場合、または取引がDRAFTでない場合
     */
    @Transactional
    fun removeItem(
        transactionId: UUID,
        itemId: UUID,
    ): TransactionEntity {
        val tx = getWritableTransaction(transactionId)

        val item =
            itemRepository.findById(itemId)
                ?: throw ResourceNotFoundException("Item not found: $itemId")
        if (item.transactionId != transactionId) throw BusinessPreconditionException("Item does not belong to this transaction")

        itemRepository.delete(item)
        recalculateTransactionTotals(tx)
        return tx
    }

    // === Discount ===

    /**
     * 取引に割引を適用する。
     *
     * PERCENTAGE または FIXED_AMOUNT の割引種別をサポートする。
     * 同一 discountId の重複適用は拒否される。適用後、取引合計を再計算する。
     *
     * @param transactionId 取引ID
     * @param discountId 割引マスタID（省略可）
     * @param name 割引名称
     * @param discountType 割引種別（"PERCENTAGE" または "FIXED_AMOUNT"）
     * @param value 割引値（パーセンテージの場合は "0"〜"100" の文字列）
     * @param amount 割引額（銭単位）
     * @param transactionItemId 明細単位の割引の場合の対象明細ID（省略可）
     * @return 更新後の取引エンティティ
     * @throws IllegalArgumentException 割引種別が不正、値が範囲外、または割引額が対象金額を超過する場合
     * @throws BusinessPreconditionException 取引がDRAFTでない場合
     */
    @Transactional
    fun applyDiscount(
        transactionId: UUID,
        discountId: UUID?,
        name: String,
        discountType: String,
        value: String,
        amount: Long,
        transactionItemId: UUID?,
    ): TransactionEntity {
        val tx = getWritableTransaction(transactionId)

        // バリデーション: 負の割引額を拒否
        require(amount >= 0) { "Discount amount must not be negative: $amount" }

        // 同一割引の重複適用を防止
        if (discountId != null) {
            val existingDiscounts = discountRepository.findByTransactionId(transactionId)
            require(existingDiscounts.none { it.discountId == discountId }) {
                "Discount $discountId is already applied to this transaction"
            }
        }

        // 割引種別ごとのバリデーション
        when (discountType) {
            "PERCENTAGE" -> {
                // パーセンテージは 0〜100（文字列 "0"〜"100"）の範囲
                val pct =
                    value.toBigDecimalOrNull()
                        ?: throw IllegalArgumentException("Invalid percentage value: $value")
                require(pct >= java.math.BigDecimal.ZERO && pct <= java.math.BigDecimal("100")) {
                    "Percentage discount must be between 0 and 100, got: $value"
                }
            }

            "FIXED_AMOUNT" -> {
                // 固定額割引: 対象金額を超えてはならない
                val targetSubtotal =
                    if (transactionItemId != null) {
                        val item =
                            itemRepository.findById(transactionItemId)
                                ?: throw IllegalArgumentException("Transaction item not found: $transactionItemId")
                        require(item.transactionId == transactionId) {
                            "Item does not belong to this transaction"
                        }
                        item.subtotal
                    } else {
                        val items = itemRepository.findByTransactionId(transactionId)
                        items.sumOf { it.subtotal }
                    }
                require(amount <= targetSubtotal) {
                    "Discount amount ($amount) exceeds subtotal ($targetSubtotal)"
                }
            }

            else -> {
                throw IllegalArgumentException("Unknown discount type: $discountType")
            }
        }

        val discount =
            TransactionDiscountEntity().apply {
                this.organizationId = tx.organizationId
                this.transactionId = transactionId
                this.discountId = discountId
                this.name = name
                this.discountType = discountType
                this.value = value
                this.amount = amount
                this.transactionItemId = transactionItemId
            }
        discountRepository.persist(discount)

        recalculateTransactionTotals(tx)
        return tx
    }

    // === Finalize ===

    /**
     * 取引を確定する。
     *
     * 支払い情報を登録し、税サマリを集計し、コンテンツハッシュを計算してステータスを COMPLETED に遷移する。
     * 確定後に sale.completed イベントを発行し、ビジネスメトリクスを更新する。
     *
     * @param transactionId 取引ID
     * @param payments 支払い情報のリスト（合計額が取引合計以上であること）
     * @return 確定済みの取引エンティティ
     * @throws ResourceNotFoundException 取引が存在しない場合
     * @throws BusinessPreconditionException 取引がDRAFTでない場合、明細が空の場合、または支払い不足の場合
     */
    @Transactional
    fun finalizeTransaction(
        transactionId: UUID,
        payments: List<PaymentInput>,
        idempotencyKey: String? = null,
    ): TransactionEntity {
        tenantFilterService.enableFilter()

        // Idempotency check: return existing COMPLETED transaction for same key
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = transactionRepository.findByIdempotencyKey(idempotencyKey)
            if (existing != null && existing.status == "COMPLETED") {
                return existing
            }
        }

        val tx =
            transactionRepository.findById(transactionId)
                ?: throw ResourceNotFoundException("Transaction not found: $transactionId")
        if (tx.status !=
            "DRAFT"
        ) {
            throw BusinessPreconditionException("Transaction must be in DRAFT status to finalize, current: ${tx.status}")
        }

        val items = itemRepository.findByTransactionId(transactionId)
        if (items.isEmpty()) throw BusinessPreconditionException("Transaction has no items")

        recalculateTransactionTotals(tx)

        val paymentTotal = payments.sumOf { it.amount }
        if (paymentTotal <
            tx.total
        ) {
            throw BusinessPreconditionException("Payment total ($paymentTotal) is less than transaction total (${tx.total})")
        }

        // オーバーペイ分（複数決済での余剰金額）を現金お釣りとして返金
        val overpayAmount = paymentTotal - tx.total
        tx.changeAmount = overpayAmount

        for (input in payments) {
            val payment =
                PaymentEntity().apply {
                    this.organizationId = tx.organizationId
                    this.transactionId = transactionId
                    this.method = input.method
                    this.amount = input.amount
                    if (input.method == "CASH") {
                        this.received = input.received ?: input.amount
                        this.change = (input.received ?: input.amount) - input.amount
                    }
                    this.reference = input.reference
                }
            paymentRepository.persist(payment)
        }

        taxSummaryRepository.deleteByTransactionId(transactionId)
        val taxableItems =
            items.map {
                TaxableItem(
                    taxRateName = it.taxRateName,
                    taxRate = it.taxRate,
                    isReducedTax = it.isReducedTax,
                    subtotal = it.subtotal,
                )
            }
        val summaries = taxCalculationService.aggregateTaxSummaries(taxableItems)
        for (summary in summaries) {
            val entity =
                TaxSummaryEntity().apply {
                    this.organizationId = tx.organizationId
                    this.transactionId = transactionId
                    this.taxRateName = summary.taxRateName
                    this.taxRate = summary.taxRate
                    this.isReduced = summary.isReduced
                    this.taxableAmount = summary.taxableAmount
                    this.taxAmount = summary.taxAmount
                }
            taxSummaryRepository.persist(entity)
        }

        tx.status = "COMPLETED"
        tx.completedAt = Instant.now()
        tx.contentHash = computeContentHash(tx, items)
        if (!idempotencyKey.isNullOrBlank()) {
            tx.idempotencyKey = idempotencyKey
        }
        transactionRepository.persist(tx)

        publishSaleCompletedEvent(tx, items)

        // ビジネスメトリクス更新
        transactionsCompletedCounter.increment()
        // Micrometer Counter.increment() accepts double — acceptable for metrics (not financial calc)
        revenueCounter.increment(tx.total.toDouble())

        return tx
    }

    // === Void ===

    /**
     * 完了済み取引を無効化（VOID）する。
     *
     * ステータスを VOIDED に変更し、sale.voided イベントを発行する。
     *
     * @param transactionId 取引ID
     * @param reason 無効化理由（必須）
     * @return 無効化後の取引エンティティ
     * @throws ResourceNotFoundException 取引が存在しない場合
     * @throws BusinessPreconditionException 取引がCOMPLETEDでない場合
     * @throws InvalidInputException 理由が空白の場合
     */
    @Transactional
    fun voidTransaction(
        transactionId: UUID,
        reason: String,
    ): TransactionEntity {
        tenantFilterService.enableFilter()
        val tx =
            transactionRepository.findById(transactionId)
                ?: throw ResourceNotFoundException("Transaction not found: $transactionId")
        if (tx.status !=
            "COMPLETED"
        ) {
            throw BusinessPreconditionException("Only COMPLETED transactions can be voided, current: ${tx.status}")
        }
        if (reason.isBlank()) throw InvalidInputException("Void reason is required")

        tx.status = "VOIDED"
        transactionRepository.persist(tx)

        val items = itemRepository.findByTransactionId(transactionId)
        publishSaleVoidedEvent(tx, items)

        transactionsVoidedCounter.increment()

        return tx
    }

    // === Query ===

    /**
     * 取引をIDで取得する。
     *
     * @param transactionId 取引ID
     * @return 取引エンティティ
     * @throws ResourceNotFoundException 取引が存在しない場合
     */
    fun getTransaction(transactionId: UUID): TransactionEntity {
        tenantFilterService.enableFilter()
        return transactionRepository.findById(transactionId)
            ?: throw ResourceNotFoundException("Transaction not found: $transactionId")
    }

    /**
     * 取引に紐づく明細一覧を取得する。
     *
     * @param transactionId 取引ID
     * @return 取引明細のリスト
     */
    fun getTransactionItems(transactionId: UUID): List<TransactionItemEntity> = itemRepository.findByTransactionId(transactionId)

    /**
     * 取引に紐づく支払い一覧を取得する。
     *
     * @param transactionId 取引ID
     * @return 支払いのリスト
     */
    fun getTransactionPayments(transactionId: UUID): List<PaymentEntity> = paymentRepository.findByTransactionId(transactionId)

    /**
     * 取引に紐づく割引一覧を取得する。
     *
     * @param transactionId 取引ID
     * @return 割引のリスト
     */
    fun getTransactionDiscounts(transactionId: UUID): List<TransactionDiscountEntity> =
        discountRepository.findByTransactionId(transactionId)

    /**
     * 取引に紐づく税サマリ一覧を取得する。
     *
     * @param transactionId 取引ID
     * @return 税サマリのリスト
     */
    fun getTransactionTaxSummaries(transactionId: UUID): List<TaxSummaryEntity> = taxSummaryRepository.findByTransactionId(transactionId)

    /**
     * 取引一覧をフィルタ条件付きで取得する（ページネーション対応）。
     *
     * @param storeId 店舗IDフィルタ（省略可）
     * @param terminalId 端末IDフィルタ（省略可）
     * @param status ステータスフィルタ（省略可）
     * @param startDate 開始日時フィルタ（省略可）
     * @param endDate 終了日時フィルタ（省略可）
     * @param page ページ番号（0始まり）
     * @param pageSize ページサイズ
     * @return Pair<取引リスト, 総件数>
     */
    fun listTransactions(
        storeId: UUID?,
        terminalId: UUID?,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
        page: Int,
        pageSize: Int,
    ): Pair<List<TransactionEntity>, Long> {
        tenantFilterService.enableFilter()
        val panachePage = Page.of(page, pageSize)
        val transactions = transactionRepository.listByFilters(storeId, terminalId, status, startDate, endDate, panachePage)
        val totalCount = transactionRepository.countByFilters(storeId, terminalId, status, startDate, endDate)
        return Pair(transactions, totalCount)
    }

    // === Batch Query (N+1 防止) ===

    /**
     * 複数取引の関連エンティティを一括取得する（N+1 クエリ防止）。
     * ListTransactions で取引一覧を返す際に、個別の取引ごとにクエリする代わりに
     * IN 句で一括取得して Map にグループ化する。
     */
    data class TransactionRelations(
        val items: Map<UUID, List<TransactionItemEntity>>,
        val payments: Map<UUID, List<PaymentEntity>>,
        val discounts: Map<UUID, List<TransactionDiscountEntity>>,
        val taxSummaries: Map<UUID, List<TaxSummaryEntity>>,
    )

    /**
     * 複数取引の関連エンティティ（明細・支払い・割引・税サマリ）を一括取得する。
     *
     * @param transactionIds 取引IDのリスト
     * @return 取引IDをキーとした関連エンティティのマップ群
     */
    fun batchLoadRelations(transactionIds: List<UUID>): TransactionRelations {
        if (transactionIds.isEmpty()) {
            return TransactionRelations(emptyMap(), emptyMap(), emptyMap(), emptyMap())
        }
        val items = itemRepository.findByTransactionIds(transactionIds).groupBy { it.transactionId }
        val payments = paymentRepository.findByTransactionIds(transactionIds).groupBy { it.transactionId }
        val discounts = discountRepository.findByTransactionIds(transactionIds).groupBy { it.transactionId }
        val taxSummaries = taxSummaryRepository.findByTransactionIds(transactionIds).groupBy { it.transactionId }
        return TransactionRelations(items, payments, discounts, taxSummaries)
    }

    // === Offline Sync ===

    /**
     * オフライン取引を一括同期する。
     *
     * クライアント端末がオフライン中に記録した取引データを受け取り、
     * アイテム作成・支払い登録・税サマリ集計・ステータス COMPLETED 化を一括で実行する。
     * clientId による冪等性を保証し、重複送信時は既存取引を返す。
     */
    @Transactional
    fun syncOfflineTransaction(
        storeId: UUID,
        terminalId: UUID,
        staffId: UUID,
        clientId: String,
        items: List<OfflineItemInput>,
        payments: List<PaymentInput>,
        createdAt: Instant?,
    ): TransactionEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }

        // 冪等性: 同一 clientId の既存取引があればそのまま返す
        if (clientId.isNotBlank()) {
            val existing = transactionRepository.findByClientId(clientId, orgId)
            if (existing != null) return existing
        }

        require(items.isNotEmpty()) { "Offline transaction must have at least one item" }
        require(payments.isNotEmpty()) { "Offline transaction must have at least one payment" }

        // 1. 取引作成
        val tx =
            TransactionEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.terminalId = terminalId
                this.staffId = staffId
                this.transactionNumber = generateTransactionNumber()
                this.type = "SALE"
                this.status = "DRAFT"
                this.clientId = clientId.ifBlank { null }
            }
        transactionRepository.persist(tx)

        // 2. アイテム作成（端末キャッシュのスナップショットをそのまま使用）
        for (offlineItem in items) {
            val taxResult =
                taxCalculationService.calculateItemTax(
                    offlineItem.unitPrice,
                    offlineItem.quantity,
                    offlineItem.taxRate,
                )
            val item =
                TransactionItemEntity().apply {
                    this.organizationId = orgId
                    this.transactionId = tx.id
                    this.productId = offlineItem.productId
                    this.productName = offlineItem.productName
                    this.unitPrice = offlineItem.unitPrice
                    this.quantity = offlineItem.quantity
                    this.taxRateName = offlineItem.taxRateName
                    this.taxRate = offlineItem.taxRate
                    this.isReducedTax = offlineItem.isReducedTax
                    this.subtotal = taxResult.subtotal
                    this.taxAmount = taxResult.taxAmount
                    this.total = taxResult.total
                }
            itemRepository.persist(item)
        }

        // 3. 取引合計を計算
        val savedItems = itemRepository.findByTransactionId(tx.id)
        tx.subtotal = savedItems.sumOf { it.subtotal }
        tx.taxTotal = savedItems.sumOf { it.taxAmount }
        tx.total = tx.subtotal + tx.taxTotal

        // 4. 支払い検証・登録
        val paymentTotal = payments.sumOf { it.amount }
        require(paymentTotal >= tx.total) {
            "Payment total ($paymentTotal) is less than transaction total (${tx.total})"
        }
        tx.changeAmount = paymentTotal - tx.total

        for (input in payments) {
            val payment =
                PaymentEntity().apply {
                    this.organizationId = orgId
                    this.transactionId = tx.id
                    this.method = input.method
                    this.amount = input.amount
                    if (input.method == "CASH") {
                        this.received = input.received ?: input.amount
                        this.change = (input.received ?: input.amount) - input.amount
                    }
                    this.reference = input.reference
                }
            paymentRepository.persist(payment)
        }

        // 5. 税サマリ集計
        val taxableItems =
            savedItems.map {
                TaxableItem(
                    taxRateName = it.taxRateName,
                    taxRate = it.taxRate,
                    isReducedTax = it.isReducedTax,
                    subtotal = it.subtotal,
                )
            }
        val summaries = taxCalculationService.aggregateTaxSummaries(taxableItems)
        for (summary in summaries) {
            val entity =
                TaxSummaryEntity().apply {
                    this.organizationId = orgId
                    this.transactionId = tx.id
                    this.taxRateName = summary.taxRateName
                    this.taxRate = summary.taxRate
                    this.isReduced = summary.isReduced
                    this.taxableAmount = summary.taxableAmount
                    this.taxAmount = summary.taxAmount
                }
            taxSummaryRepository.persist(entity)
        }

        // 6. ステータスを COMPLETED に遷移
        tx.status = "COMPLETED"
        tx.completedAt = createdAt ?: Instant.now()
        tx.contentHash = computeContentHash(tx, savedItems)
        transactionRepository.persist(tx)

        // 7. イベント発行 + メトリクス
        publishSaleCompletedEvent(tx, savedItems)
        transactionsCompletedCounter.increment()
        // Micrometer Counter.increment() accepts double — acceptable for metrics (not financial calc)
        revenueCounter.increment(tx.total.toDouble())

        return tx
    }

    // === Internal Helpers ===

    private fun getWritableTransaction(transactionId: UUID): TransactionEntity {
        tenantFilterService.enableFilter()
        val tx =
            transactionRepository.findById(transactionId)
                ?: throw ResourceNotFoundException("Transaction not found: $transactionId")
        if (tx.status != "DRAFT") throw BusinessPreconditionException("Transaction is not in DRAFT status: ${tx.status}")
        return tx
    }

    private fun recalculateTransactionTotals(tx: TransactionEntity) {
        val items = itemRepository.findByTransactionId(tx.id)
        val discounts = discountRepository.findByTransactionId(tx.id)

        tx.subtotal = items.sumOf { it.subtotal }
        tx.taxTotal = items.sumOf { it.taxAmount }
        tx.discountTotal = discounts.sumOf { it.amount }
        tx.total = tx.subtotal + tx.taxTotal - tx.discountTotal
        if (tx.total < 0) tx.total = 0
        transactionRepository.persist(tx)
    }

    private fun generateTransactionNumber(): String {
        val date = LocalDate.now(ZoneOffset.UTC)
        val seq = UUID.randomUUID().toString().take(8)
        return "T-$date-$seq"
    }

    /**
     * 取引内容のSHA-256ハッシュを計算する（電子帳簿保存法 真正性確保）。
     */
    private fun computeContentHash(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
    ): String {
        val content =
            buildString {
                append(tx.id)
                append(tx.transactionNumber)
                append(tx.subtotal)
                append(tx.taxTotal)
                append(tx.discountTotal)
                append(tx.total)
                items.sortedBy { it.id }.forEach { item ->
                    append(item.productId?.toString().orEmpty())
                    append(item.productName)
                    append(item.quantity)
                    append(item.unitPrice)
                    append(item.subtotal)
                }
            }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(content.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun publishSaleCompletedEvent(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
    ) {
        val payments = paymentRepository.findByTransactionId(tx.id)
        val event =
            SaleCompletedEventDto(
                transactionId = tx.id.toString(),
                storeId = tx.storeId.toString(),
                terminalId = tx.terminalId.toString(),
                items =
                    items.map { item ->
                        SaleItemDto(
                            productId = item.productId?.toString(),
                            productName = item.productName,
                            quantity = item.quantity,
                            unitPrice = item.unitPrice,
                            subtotal = item.subtotal,
                        )
                    },
                totalAmount = tx.total,
                taxTotal = tx.taxTotal,
                discountTotal = tx.discountTotal,
                payments =
                    payments.map { p ->
                        SalePaymentDto(
                            method = p.method,
                            amount = p.amount,
                        )
                    },
                transactedAt = tx.completedAt.toString(),
            )
        eventPublisher.publish("sale.completed", tx.organizationId, event)
    }

    fun aggregateStaffSales(
        storeId: java.util.UUID,
        startDate: java.time.Instant,
        endDate: java.time.Instant,
    ): List<Array<Any>> {
        tenantFilterService.enableFilter()
        return transactionRepository.aggregateStaffSales(storeId, startDate, endDate)
    }

    private fun publishSaleVoidedEvent(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
    ) {
        val event =
            SaleVoidedEventDto(
                originalTransactionId = tx.id.toString(),
                voidTransactionId = tx.id.toString(),
                storeId = tx.storeId.toString(),
                items =
                    items.map { item ->
                        SaleItemDto(
                            productId = item.productId?.toString(),
                            productName = item.productName,
                            quantity = item.quantity,
                            unitPrice = item.unitPrice,
                            subtotal = item.subtotal,
                        )
                    },
                originalTransactedAt = (tx.completedAt ?: tx.createdAt).toString(),
            )
        eventPublisher.publish("sale.voided", tx.organizationId, event)
    }
}

data class PaymentInput(
    val method: String,
    val amount: Long,
    val received: Long?,
    val reference: String?,
)

data class OfflineItemInput(
    val productId: UUID,
    val productName: String,
    val unitPrice: Long,
    val quantity: Int,
    val taxRateName: String,
    val taxRate: String,
    val isReducedTax: Boolean,
)
