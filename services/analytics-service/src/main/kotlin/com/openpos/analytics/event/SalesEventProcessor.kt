package com.openpos.analytics.event

import com.openpos.analytics.entity.DailySalesEntity
import com.openpos.analytics.entity.HourlySalesEntity
import com.openpos.analytics.entity.ProductSalesEntity
import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.HourlySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.config.inject.ConfigProperty
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

    @ConfigProperty(name = "openpos.analytics.timezone", defaultValue = "Asia/Tokyo")
    lateinit var timezoneName: String

    val timezone: ZoneId
        get() = ZoneId.of(timezoneName)

    /**
     * 売上完了イベントを処理する。
     * 日次・商品別・時間帯別の集計を更新する。
     */
    @Transactional
    fun processSaleCompleted(
        organizationId: UUID,
        payload: SaleCompletedPayload,
    ) {
        val storeId = UUID.fromString(payload.storeId)
        val transactedAt = Instant.parse(payload.transactedAt)
        val saleDate = transactedAt.atZone(timezone).toLocalDate()
        val hour = transactedAt.atZone(timezone).hour

        // 日次売上更新
        updateDailySales(organizationId, storeId, saleDate, payload.totalAmount, payload.taxTotal, payload.discountTotal, payload.payments)

        // 商品別売上更新（カスタムアイテム = productId null/blank はスキップ）
        for (item in payload.items) {
            val pid = item.productId
            if (pid.isNullOrBlank()) continue
            updateProductSales(
                organizationId,
                storeId,
                UUID.fromString(pid),
                item.productName,
                item.categoryName,
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
    @Transactional
    fun processSaleVoided(
        organizationId: UUID,
        payload: SaleVoidedPayload,
    ) {
        val storeId = UUID.fromString(payload.storeId)
        val originalTransactedAt = Instant.parse(payload.originalTransactedAt)
        val originalDate = originalTransactedAt.atZone(timezone).toLocalDate()
        val originalHour = originalTransactedAt.atZone(timezone).hour

        // 日次売上ロールバック（取消合計を計算）
        val totalAmount = payload.items.sumOf { it.subtotal }
        rollbackDailySales(organizationId, storeId, originalDate, totalAmount)

        // 商品別売上ロールバック（カスタムアイテム = productId null/blank はスキップ）
        for (item in payload.items) {
            val pid = item.productId
            if (pid.isNullOrBlank()) continue
            rollbackProductSales(
                organizationId,
                storeId,
                UUID.fromString(pid),
                originalDate,
                item.quantity,
                item.subtotal,
            )
        }

        // 時間帯別売上ロールバック
        rollbackHourlySales(organizationId, storeId, originalDate, originalHour, totalAmount)
    }

    private fun updateDailySales(
        organizationId: UUID,
        storeId: UUID,
        saleDate: LocalDate,
        totalAmount: Long,
        taxTotal: Long = 0,
        discountTotal: Long = 0,
        payments: List<SalePaymentPayload>? = null,
    ) {
        val daily =
            dailySalesRepository.findByStoreAndDate(storeId, saleDate)
                ?: DailySalesEntity().apply {
                    this.organizationId = organizationId
                    this.storeId = storeId
                    this.date = saleDate
                }
        daily.grossAmount += totalAmount
        daily.taxAmount += taxTotal
        daily.discountAmount += discountTotal
        daily.transactionCount += 1
        daily.netAmount = daily.grossAmount - daily.taxAmount - daily.discountAmount

        // 支払方法別売上の更新
        payments?.forEach { payment ->
            when (payment.method.uppercase()) {
                "CASH" -> daily.cashAmount += payment.amount
                "CREDIT_CARD" -> daily.cardAmount += payment.amount
                "QR_CODE" -> daily.qrAmount += payment.amount
            }
        }

        dailySalesRepository.persist(daily)
    }

    private fun updateProductSales(
        organizationId: UUID,
        storeId: UUID,
        productId: UUID,
        productName: String?,
        categoryName: String?,
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
                    this.date = saleDate
                    this.productName = productName?.ifBlank { "Product-$productId" } ?: "Product-$productId"
                    this.categoryName = categoryName?.ifBlank { "" } ?: ""
                }
        productSales.quantitySold += quantity
        productSales.totalAmount += subtotal
        productSales.transactionCount += 1
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
                    this.date = saleDate
                }
        daily.grossAmount = maxOf(0, daily.grossAmount - totalAmount)
        daily.transactionCount = maxOf(0, daily.transactionCount - 1)
        daily.netAmount = daily.grossAmount - daily.taxAmount - daily.discountAmount
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
