package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockEntity
import com.openpos.inventory.entity.StockMovementEntity
import com.openpos.inventory.event.StockLowEventPublisher
import com.openpos.inventory.repository.StockMovementRepository
import com.openpos.inventory.repository.StockRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Instant
import java.util.UUID

/**
 * 在庫のビジネスロジック層。
 * 在庫取得、一覧、調整（+移動履歴記録）、移動履歴一覧を提供する。
 * 在庫が閾値以下になった場合に StockLowEvent を発行する。
 */
@ApplicationScoped
class StockService {
    @Inject
    lateinit var stockRepository: StockRepository

    @Inject
    lateinit var movementRepository: StockMovementRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Inject
    lateinit var stockLowEventPublisher: StockLowEventPublisher

    @ConfigProperty(name = "inventory.stock.low-threshold", defaultValue = "10")
    var defaultLowStockThreshold: Int = 10

    /**
     * 店舗×商品の在庫を取得する。
     */
    fun getStock(
        storeId: UUID,
        productId: UUID,
    ): StockEntity? {
        tenantFilterService.enableFilter()
        return stockRepository.findByStoreAndProduct(storeId, productId)
    }

    /**
     * 店舗の在庫一覧を取得する（ページネーション対応）。
     *
     * @return Pair<在庫リスト, 総件数>
     */
    fun listStocks(
        storeId: UUID,
        lowStockOnly: Boolean,
        page: Int,
        pageSize: Int,
    ): Pair<List<StockEntity>, Long> {
        tenantFilterService.enableFilter()
        val panachePage = Page.of(page, pageSize)
        return if (lowStockOnly) {
            val stocks = stockRepository.listLowStock(storeId, panachePage)
            val totalCount = stockRepository.countLowStock(storeId)
            Pair(stocks, totalCount)
        } else {
            val stocks = stockRepository.listByStoreId(storeId, panachePage)
            val totalCount = stockRepository.countByStoreId(storeId)
            Pair(stocks, totalCount)
        }
    }

    /**
     * 在庫を調整する。
     * 1. stocks テーブルから store_id + product_id を SELECT FOR UPDATE で検索（なければ新規作成）
     * 2. quantity を更新（結果が負になる場合は FAILED_PRECONDITION 例外）
     * 3. stock_movements に記録
     *
     * @return 調整後の在庫エンティティ
     * @throws InsufficientStockException 在庫不足の場合（gRPC FAILED_PRECONDITION にマッピング）
     */
    @Transactional
    fun adjustStock(
        storeId: UUID,
        productId: UUID,
        quantityChange: Int,
        movementType: String,
        referenceId: String?,
        note: String?,
    ): StockEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        tenantFilterService.enableFilter()

        // 悲観的ロック（SELECT FOR UPDATE）で在庫行を取得し Race Condition を防止
        val stock =
            stockRepository.findByStoreAndProductForUpdate(storeId, productId)
                ?: StockEntity().apply {
                    this.organizationId = orgId
                    this.storeId = storeId
                    this.productId = productId
                    this.quantity = 0
                }

        val newQuantity = stock.quantity + quantityChange
        if (newQuantity < 0) {
            throw InsufficientStockException(
                "Insufficient stock: current=${stock.quantity}, requested change=$quantityChange, product=$productId, store=$storeId",
            )
        }

        val previousQuantity = stock.quantity
        stock.quantity = newQuantity
        stockRepository.persist(stock)

        val movement =
            StockMovementEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.productId = productId
                this.movementType = movementType
                this.quantity = quantityChange
                this.referenceId = referenceId
                this.note = note
            }
        movementRepository.persist(movement)

        // 在庫が閾値以下に下がった場合に StockLowEvent を発行
        val threshold = stock.lowStockThreshold
        if (newQuantity <= threshold && previousQuantity > threshold) {
            stockLowEventPublisher.publish(
                organizationId = orgId,
                productId = productId,
                storeId = storeId,
                currentQuantity = newQuantity,
                threshold = threshold,
            )
        }

        return stock
    }

    /**
     * 在庫移動履歴を取得する（ページネーション対応）。
     *
     * @return Pair<移動履歴リスト, 総件数>
     */
    fun listMovements(
        storeId: UUID,
        productId: UUID?,
        startDate: Instant?,
        endDate: Instant?,
        page: Int,
        pageSize: Int,
    ): Pair<List<StockMovementEntity>, Long> {
        tenantFilterService.enableFilter()
        val panachePage = Page.of(page, pageSize)
        val movements = movementRepository.listByStoreAndProduct(storeId, productId, startDate, endDate, panachePage)
        val totalCount = movementRepository.countByStoreAndProduct(storeId, productId, startDate, endDate)
        return Pair(movements, totalCount)
    }
}

/**
 * 在庫不足例外。
 * gRPC レイヤーで FAILED_PRECONDITION ステータスにマッピングされる。
 */
class InsufficientStockException(
    message: String,
) : RuntimeException(message)
