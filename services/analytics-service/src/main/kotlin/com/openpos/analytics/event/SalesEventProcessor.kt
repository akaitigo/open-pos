package com.openpos.analytics.event

import com.openpos.analytics.entity.DailySalesEntity
import com.openpos.analytics.entity.HourlySalesEntity
import com.openpos.analytics.entity.ProductSalesEntity
import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.HourlySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * イベントペイロードを売上集計操作に変換する。
 */
@ApplicationScoped
class SalesEventProcessor {
    @Inject
    lateinit var dailySalesRepository: DailySalesRepository

    @Inject
    lateinit var productSalesRepository: ProductSalesRepository

    @Inject
    lateinit var hourlySalesRepository: HourlySalesRepository

    /**
     * 売上完了イベントを処理する。
     * 日次・商品別・時間帯別の集計を更新する。
     */
    fun processSaleCompleted(
        organizationId: UUID,
        payload: SaleCompletedPayload,
    ) {
        val storeId = UUID.fromString(payload.storeId)
        val transactedAt = Instant.parse(payload.transactedAt)
        val saleDate = transactedAt.atZone(ZoneId.of("Asia/Tokyo")).toLocalDate()
        val hour = transactedAt.atZone(ZoneId.of("Asia/Tokyo")).hour

        // 日次売上更新
        updateDailySales(organizationId, storeId, saleDate, payload.totalAmount)

        // 商品別売上更新
        for (item in payload.items) {
            updateProductSales(
                organizationId,
                storeId,
                UUID.fromString(item.productId),
                saleDate,
                item.quantity,
                item.subtotal,
                item.unitPrice,
            )
        }

        // 時間帯別売上更新
        updateHourlySales(organizationId, storeId, saleDate, hour, payload.totalAmount)
    }

    /**
     * 売上取消イベントを処理する。
     * 日次・商品別・時間帯別の集計をロールバックする。
     */
    fun processSaleVoided(
        organizationId: UUID,
        payload: SaleVoidedPayload,
    ) {
        val storeId = UUID.fromString(payload.storeId)
        val today = LocalDate.now(ZoneId.of("Asia/Tokyo"))

        // 日次売上ロールバック（取消合計を計算）
        val totalAmount = payload.items.sumOf { it.subtotal }
        rollbackDailySales(organizationId, storeId, today, totalAmount)

        // 商品別売上ロールバック
        for (item in payload.items) {
            rollbackProductSales(
                organizationId,
                storeId,
                UUID.fromString(item.productId),
                today,
                item.quantity,
                item.subtotal,
            )
        }

        // 時間帯別売上ロールバック
        val currentHour = Instant.now().atZone(ZoneId.of("Asia/Tokyo")).hour
        rollbackHourlySales(organizationId, storeId, today, currentHour, totalAmount)
    }

    private fun updateDailySales(
        organizationId: UUID,
        storeId: UUID,
        saleDate: LocalDate,
        totalAmount: Long,
    ) {
        val daily =
            dailySalesRepository.findByStoreAndDate(storeId, saleDate)
                ?: DailySalesEntity().apply {
                    this.organizationId = organizationId
                    this.storeId = storeId
                    this.saleDate = saleDate
                }
        daily.totalSales += totalAmount
        daily.transactionCount += 1
        daily.netSales = daily.totalSales - daily.taxAmount - daily.discountAmount
        dailySalesRepository.persist(daily)
    }

    private fun updateProductSales(
        organizationId: UUID,
        storeId: UUID,
        productId: UUID,
        saleDate: LocalDate,
        quantity: Int,
        subtotal: Long,
        unitPrice: Long,
    ) {
        val productSales =
            productSalesRepository.findByStoreProductAndDate(storeId, productId, saleDate)
                ?: ProductSalesEntity().apply {
                    this.organizationId = organizationId
                    this.storeId = storeId
                    this.productId = productId
                    this.saleDate = saleDate
                    this.productName = "Product-$productId"
                }
        productSales.quantitySold += quantity
        productSales.totalAmount += subtotal
        productSales.transactionCount += 1
        if (productSales.quantitySold > 0) {
            productSales.averagePrice = productSales.totalAmount / productSales.quantitySold
        }
        productSalesRepository.persist(productSales)
    }

    private fun updateHourlySales(
        organizationId: UUID,
        storeId: UUID,
        saleDate: LocalDate,
        hour: Int,
        totalAmount: Long,
    ) {
        val hourly =
            hourlySalesRepository.findByStoreAndDateAndHour(storeId, saleDate, hour)
                ?: HourlySalesEntity().apply {
                    this.organizationId = organizationId
                    this.storeId = storeId
                    this.saleDate = saleDate
                    this.hour = hour
                }
        hourly.totalSales += totalAmount
        hourly.transactionCount += 1
        hourlySalesRepository.persist(hourly)
    }

    private fun rollbackDailySales(
        organizationId: UUID,
        storeId: UUID,
        saleDate: LocalDate,
        totalAmount: Long,
    ) {
        val daily =
            dailySalesRepository.findByStoreAndDate(storeId, saleDate)
                ?: DailySalesEntity().apply {
                    this.organizationId = organizationId
                    this.storeId = storeId
                    this.saleDate = saleDate
                }
        daily.totalSales = maxOf(0, daily.totalSales - totalAmount)
        daily.voidedCount += 1
        daily.netSales = daily.totalSales - daily.taxAmount - daily.discountAmount
        dailySalesRepository.persist(daily)
    }

    private fun rollbackProductSales(
        organizationId: UUID,
        storeId: UUID,
        productId: UUID,
        saleDate: LocalDate,
        quantity: Int,
        subtotal: Long,
    ) {
        val productSales =
            productSalesRepository.findByStoreProductAndDate(storeId, productId, saleDate)
                ?: return
        productSales.quantitySold = maxOf(0, productSales.quantitySold - quantity)
        productSales.totalAmount = maxOf(0, productSales.totalAmount - subtotal)
        if (productSales.quantitySold > 0) {
            productSales.averagePrice = productSales.totalAmount / productSales.quantitySold
        } else {
            productSales.averagePrice = 0
        }
        productSalesRepository.persist(productSales)
    }

    private fun rollbackHourlySales(
        organizationId: UUID,
        storeId: UUID,
        saleDate: LocalDate,
        hour: Int,
        totalAmount: Long,
    ) {
        val hourly =
            hourlySalesRepository.findByStoreAndDateAndHour(storeId, saleDate, hour)
                ?: return
        hourly.totalSales = maxOf(0, hourly.totalSales - totalAmount)
        hourlySalesRepository.persist(hourly)
    }
}
