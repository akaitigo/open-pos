package com.openpos.analytics.integration

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.DailySalesEntity
import com.openpos.analytics.entity.HourlySalesEntity
import com.openpos.analytics.entity.ProductAlertEntity
import com.openpos.analytics.entity.ProductSalesEntity
import com.openpos.analytics.entity.SalesTargetEntity
import com.openpos.analytics.service.AnalyticsQueryService
import com.openpos.analytics.service.AnalyticsService
import com.openpos.analytics.service.ProductAlertService
import com.openpos.analytics.service.SalesTargetService
import io.quarkus.test.TestTransaction
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

/**
 * Analytics Service テナント隔離 E2E 統合テスト。
 *
 * サービス層（AnalyticsService, AnalyticsQueryService, SalesTargetService,
 * ProductAlertService）を経由した cross-tenant データ漏洩がないことを検証する。
 *
 * 検証観点:
 * 1. org1 のデータが org2 のコンテキストから見えないこと（全サービスメソッド）
 * 2. org2 のデータが org1 のコンテキストから見えないこと（全サービスメソッド）
 * 3. テナントフィルタ未設定時に IllegalArgumentException がスローされること（フェイルセーフ）
 * 4. ProductAlertService.markAsRead() の手動テナントチェックが正しく機能すること
 * 5. テナント切り替えを繰り返してもデータ漏洩がないこと
 *
 * セキュリティ評価 P0-2 対応。
 */
@QuarkusTest
class TenantIsolationIntegrationTest {
    @Inject
    lateinit var em: EntityManager

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var analyticsService: AnalyticsService

    @Inject
    lateinit var analyticsQueryService: AnalyticsQueryService

    @Inject
    lateinit var salesTargetService: SalesTargetService

    @Inject
    lateinit var productAlertService: ProductAlertService

    companion object {
        private val ORG1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
        private val ORG2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
        private val STORE_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        private val STORE_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        private val PRODUCT_X = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        private val PRODUCT_Y = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        private val DATE = LocalDate.of(2026, 4, 1)
    }

    /**
     * テナントコンテキストを切り替える。
     * 既存のフィルターを無効化してから、指定テナントのフィルターを有効化する。
     */
    private fun switchTenant(orgId: UUID) {
        val session = em.unwrap(org.hibernate.Session::class.java)
        session.disableFilter("organizationFilter")
        organizationIdHolder.organizationId = orgId
        tenantFilterService.enableFilter()
    }

    // --- AnalyticsService: getDailySales ---

    @Test
    @TestTransaction
    fun `getDailySales - org1はorg2の日次売上を取得できない`() {
        // Arrange
        em.persist(
            DailySalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                date = DATE
                grossAmount = 100000
                netAmount = 90000
                taxAmount = 10000
                transactionCount = 5
            },
        )
        em.persist(
            DailySalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_A
                date = DATE
                grossAmount = 999999
                netAmount = 899999
                taxAmount = 100000
                transactionCount = 50
            },
        )
        em.flush()

        // Act: org1 のコンテキストで取得
        switchTenant(ORG1)
        val org1Results = analyticsService.getDailySales(STORE_A, DATE, DATE)

        // Assert: org1 のデータのみ返る
        assertEquals(1, org1Results.size)
        assertTrue(org1Results.all { it.organizationId == ORG1 })
        assertEquals(100000L, org1Results[0].grossAmount)

        // Act: org2 のコンテキストで取得
        switchTenant(ORG2)
        val org2Results = analyticsService.getDailySales(STORE_A, DATE, DATE)

        // Assert: org2 のデータのみ返る
        assertEquals(1, org2Results.size)
        assertTrue(org2Results.all { it.organizationId == ORG2 })
        assertEquals(999999L, org2Results[0].grossAmount)
    }

    // --- AnalyticsService: getSalesSummary ---

    @Test
    @TestTransaction
    fun `getSalesSummary - org1のサマリーにorg2のデータが混入しない`() {
        // Arrange
        em.persist(
            DailySalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                date = DATE
                grossAmount = 50000
                netAmount = 45000
                taxAmount = 5000
                transactionCount = 3
            },
        )
        em.persist(
            DailySalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_A
                date = DATE
                grossAmount = 9000000
                netAmount = 8100000
                taxAmount = 900000
                transactionCount = 100
            },
        )
        em.flush()

        // Act: org1 のコンテキストでサマリー取得
        switchTenant(ORG1)
        val summary = analyticsService.getSalesSummary(STORE_A, DATE, DATE)

        // Assert: org2 の 9,000,000 が含まれていないこと
        assertEquals(50000L, summary.totalGross)
        assertEquals(3, summary.totalTransactions)
    }

    // --- AnalyticsService: getHourlySales ---

    @Test
    @TestTransaction
    fun `getHourlySales - org間で時間帯別売上が混在しない`() {
        // Arrange
        em.persist(
            HourlySalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                saleDate = DATE
                hour = 10
                totalSales = 30000
                transactionCount = 3
            },
        )
        em.persist(
            HourlySalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_A
                saleDate = DATE
                hour = 10
                totalSales = 888888
                transactionCount = 88
            },
        )
        em.flush()

        // Act: org1 のコンテキストで取得
        switchTenant(ORG1)
        val org1Hourly = analyticsService.getHourlySales(STORE_A, DATE)

        // Assert: 24時間分返り、10時台のデータは org1 のもののみ
        assertEquals(24, org1Hourly.size)
        assertEquals(30000L, org1Hourly[10].totalSales)
        assertEquals(3, org1Hourly[10].transactionCount)

        // Act: org2 のコンテキストで取得
        switchTenant(ORG2)
        val org2Hourly = analyticsService.getHourlySales(STORE_A, DATE)

        // Assert: org2 のデータのみ
        assertEquals(888888L, org2Hourly[10].totalSales)
        assertEquals(88, org2Hourly[10].transactionCount)
    }

    // --- AnalyticsService: getProductSales ---

    @Test
    @TestTransaction
    fun `getProductSales - org1の商品別売上にorg2のデータが含まれない`() {
        // Arrange
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                date = DATE
                productId = PRODUCT_X
                productName = "org1のコーヒー"
                quantitySold = 10
                totalAmount = 30000
                costAmount = 15000
                transactionCount = 8
            },
        )
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_A
                date = DATE
                productId = PRODUCT_X
                productName = "org2のコーヒー"
                quantitySold = 999
                totalAmount = 9999999
                costAmount = 5000000
                transactionCount = 500
            },
        )
        em.flush()

        // Act: org1 のコンテキストで取得
        switchTenant(ORG1)
        val (org1Results, org1Count) =
            analyticsService.getProductSales(STORE_A, DATE, DATE, null, null, 0, 20)

        // Assert: org1 のデータのみ
        assertEquals(1, org1Results.size)
        assertEquals(1L, org1Count)
        assertTrue(org1Results.all { it.organizationId == ORG1 })
        assertEquals(10, org1Results[0].quantitySold)

        // Act: org2 のコンテキストで取得
        switchTenant(ORG2)
        val (org2Results, org2Count) =
            analyticsService.getProductSales(STORE_A, DATE, DATE, null, null, 0, 20)

        // Assert: org2 のデータのみ
        assertEquals(1, org2Results.size)
        assertEquals(1L, org2Count)
        assertTrue(org2Results.all { it.organizationId == ORG2 })
        assertEquals(999, org2Results[0].quantitySold)
    }

    // --- AnalyticsQueryService: getAbcAnalysis ---

    @Test
    @TestTransaction
    fun `getAbcAnalysis - org2のABC分析にorg1の商品が含まれない`() {
        // Arrange: org1 に高額商品、org2 に低額商品
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                date = DATE
                productId = PRODUCT_X
                productName = "org1の高額商品"
                quantitySold = 100
                totalAmount = 10000000
                costAmount = 5000000
                transactionCount = 80
            },
        )
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_A
                date = DATE
                productId = PRODUCT_Y
                productName = "org2の低額商品"
                quantitySold = 5
                totalAmount = 10000
                costAmount = 5000
                transactionCount = 3
            },
        )
        em.flush()

        // Act: org2 のコンテキストで ABC 分析
        switchTenant(ORG2)
        val org2Abc = analyticsQueryService.getAbcAnalysis(STORE_A, DATE, DATE)

        // Assert: org1 の高額商品がランキングに混入していないこと
        assertEquals(1, org2Abc.size)
        assertEquals("org2の低額商品", org2Abc[0].productName)
        assertEquals(10000L, org2Abc[0].revenue)

        // Act: org1 のコンテキストで ABC 分析
        switchTenant(ORG1)
        val org1Abc = analyticsQueryService.getAbcAnalysis(STORE_A, DATE, DATE)

        // Assert: org1 のデータのみ
        assertEquals(1, org1Abc.size)
        assertEquals("org1の高額商品", org1Abc[0].productName)
        assertEquals(10000000L, org1Abc[0].revenue)
    }

    // --- AnalyticsQueryService: getGrossProfitReport ---

    @Test
    @TestTransaction
    fun `getGrossProfitReport - org間で粗利レポートが混在しない`() {
        // Arrange
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                date = DATE
                productId = PRODUCT_X
                productName = "org1商品"
                quantitySold = 10
                totalAmount = 100000
                costAmount = 40000
                transactionCount = 8
            },
        )
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_A
                date = DATE
                productId = PRODUCT_Y
                productName = "org2商品"
                quantitySold = 20
                totalAmount = 500000
                costAmount = 300000
                transactionCount = 15
            },
        )
        em.flush()

        // Act: org1
        switchTenant(ORG1)
        val org1Report = analyticsQueryService.getGrossProfitReport(STORE_A, DATE, DATE)

        // Assert: org2 のデータが含まれていない
        assertEquals(1, org1Report.items.size)
        assertEquals(100000L, org1Report.totalRevenue)
        assertEquals(40000L, org1Report.totalCost)
        assertEquals(60000L, org1Report.totalGrossProfit)

        // Act: org2
        switchTenant(ORG2)
        val org2Report = analyticsQueryService.getGrossProfitReport(STORE_A, DATE, DATE)

        // Assert
        assertEquals(1, org2Report.items.size)
        assertEquals(500000L, org2Report.totalRevenue)
    }

    // --- AnalyticsQueryService: getSalesForecast ---

    @Test
    @TestTransaction
    fun `getSalesForecast - org2の売上予測にorg1のデータが混入しない`() {
        // Arrange: 3日分のデータを両テナントに投入
        for (day in 1..3) {
            em.persist(
                DailySalesEntity().apply {
                    organizationId = ORG1
                    storeId = STORE_A
                    date = DATE.plusDays(day.toLong() - 1)
                    grossAmount = 10000L * day
                    netAmount = 9000L * day
                    taxAmount = 1000L * day
                    transactionCount = day
                },
            )
            em.persist(
                DailySalesEntity().apply {
                    organizationId = ORG2
                    storeId = STORE_A
                    date = DATE.plusDays(day.toLong() - 1)
                    grossAmount = 99999L * day
                    netAmount = 89999L * day
                    taxAmount = 10000L * day
                    transactionCount = day * 10
                },
            )
        }
        em.flush()

        val startDate = DATE
        val endDate = DATE.plusDays(2)

        // Act: org1
        switchTenant(ORG1)
        val org1Forecast = analyticsQueryService.getSalesForecast(STORE_A, startDate, endDate, 3)

        // Assert: org1 のデータのみ（1日目: 10000, 2日目: 20000, 3日目: 30000）
        assertEquals(3, org1Forecast.size)
        assertEquals(10000L, org1Forecast[0].actualAmount)
        assertEquals(20000L, org1Forecast[1].actualAmount)
        assertEquals(30000L, org1Forecast[2].actualAmount)

        // Act: org2
        switchTenant(ORG2)
        val org2Forecast = analyticsQueryService.getSalesForecast(STORE_A, startDate, endDate, 3)

        // Assert: org2 のデータのみ
        assertEquals(3, org2Forecast.size)
        assertEquals(99999L, org2Forecast[0].actualAmount)
        assertEquals(199998L, org2Forecast[1].actualAmount)
        assertEquals(299997L, org2Forecast[2].actualAmount)
    }

    // --- AnalyticsQueryService: getCategorySalesReport ---

    @Test
    @TestTransaction
    fun `getCategorySalesReport - org間でカテゴリ別売上が混在しない`() {
        // Arrange
        val categoryId = UUID.randomUUID()
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                date = DATE
                productId = PRODUCT_X
                productName = "org1のドリンク"
                this.categoryId = categoryId
                categoryName = "ドリンク"
                quantitySold = 10
                totalAmount = 30000
                costAmount = 15000
                transactionCount = 8
            },
        )
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_A
                date = DATE
                productId = PRODUCT_Y
                productName = "org2のフード"
                this.categoryId = categoryId
                categoryName = "フード"
                quantitySold = 50
                totalAmount = 500000
                costAmount = 250000
                transactionCount = 40
            },
        )
        em.flush()

        // Act: org1
        switchTenant(ORG1)
        val org1Categories = analyticsQueryService.getCategorySalesReport(STORE_A, DATE, DATE)

        // Assert
        assertEquals(1, org1Categories.size)
        assertEquals("ドリンク", org1Categories[0].categoryName)
        assertEquals(30000L, org1Categories[0].totalAmount)

        // Act: org2
        switchTenant(ORG2)
        val org2Categories = analyticsQueryService.getCategorySalesReport(STORE_A, DATE, DATE)

        // Assert
        assertEquals(1, org2Categories.size)
        assertEquals("フード", org2Categories[0].categoryName)
        assertEquals(500000L, org2Categories[0].totalAmount)
    }

    // --- SalesTargetService: listAll ---

    @Test
    @TestTransaction
    fun `SalesTarget listAll - org1はorg2の売上目標を取得できない`() {
        // Arrange
        em.persist(
            SalesTargetEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                targetMonth = DATE
                targetAmount = 1000000
            },
        )
        em.persist(
            SalesTargetEntity().apply {
                organizationId = ORG2
                storeId = STORE_B
                targetMonth = DATE
                targetAmount = 9999999
            },
        )
        em.flush()

        // Act: org1
        switchTenant(ORG1)
        val org1Targets = salesTargetService.listAll()

        // Assert
        assertEquals(1, org1Targets.size)
        assertTrue(org1Targets.all { it.organizationId == ORG1 })
        assertEquals(1000000L, org1Targets[0].targetAmount)

        // Act: org2
        switchTenant(ORG2)
        val org2Targets = salesTargetService.listAll()

        // Assert
        assertEquals(1, org2Targets.size)
        assertTrue(org2Targets.all { it.organizationId == ORG2 })
        assertEquals(9999999L, org2Targets[0].targetAmount)
    }

    // --- SalesTargetService: findByStoreAndMonth ---

    @Test
    @TestTransaction
    fun `SalesTarget findByStoreAndMonth - 同一storeかつ同一monthでもorg間で分離される`() {
        // Arrange: 同一 store + 同一 month を両テナントに投入
        em.persist(
            SalesTargetEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                targetMonth = DATE
                targetAmount = 500000
            },
        )
        em.persist(
            SalesTargetEntity().apply {
                organizationId = ORG2
                storeId = STORE_A
                targetMonth = DATE
                targetAmount = 7777777
            },
        )
        em.flush()

        // Act: org1
        switchTenant(ORG1)
        val org1Target = salesTargetService.findByStoreAndMonth(STORE_A, DATE)

        // Assert
        assertNotNull(org1Target)
        assertEquals(ORG1, requireNotNull(org1Target).organizationId)
        assertEquals(500000L, org1Target.targetAmount)

        // Act: org2
        switchTenant(ORG2)
        val org2Target = salesTargetService.findByStoreAndMonth(STORE_A, DATE)

        // Assert
        assertNotNull(org2Target)
        assertEquals(ORG2, requireNotNull(org2Target).organizationId)
        assertEquals(7777777L, org2Target.targetAmount)
    }

    // --- SalesTargetService: findById (cross-tenant by ID) ---

    @Test
    @TestTransaction
    fun `SalesTarget findById - org1のIDでorg2のコンテキストからはnullが返る`() {
        // Arrange
        val org1Entity =
            SalesTargetEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                targetMonth = DATE
                targetAmount = 300000
            }
        em.persist(org1Entity)
        em.flush()
        val org1TargetId = org1Entity.id

        // Act: org2 のコンテキストで org1 の ID を検索
        switchTenant(ORG2)
        val result = salesTargetService.findById(org1TargetId)

        // Assert: テナントフィルターにより null が返る
        assertNull(result)
    }

    // --- ProductAlertService: markAsRead (cross-tenant protection) ---

    @Test
    @TestTransaction
    fun `ProductAlert markAsRead - 他テナントのアラートは既読にできない`() {
        // Arrange: org1 のアラートを作成
        val org1Alert =
            ProductAlertEntity().apply {
                organizationId = ORG1
                productId = PRODUCT_X
                alertType = "TRENDING"
                description = "org1: コーヒーの売上急上昇"
            }
        em.persist(org1Alert)
        em.flush()
        val alertId = org1Alert.id

        // Act: org2 のコンテキストで org1 のアラートを既読にしようとする
        switchTenant(ORG2)
        val result = productAlertService.markAsRead(alertId)

        // Assert: null が返る（操作拒否）
        assertNull(result)

        // Verify: org1 のアラートは未読のまま
        switchTenant(ORG1)
        val org1AlertAfter = em.find(ProductAlertEntity::class.java, alertId)
        assertNotNull(org1AlertAfter)
        assertEquals(false, requireNotNull(org1AlertAfter).isRead)
    }

    @Test
    @TestTransaction
    fun `ProductAlert markAsRead - 自テナントのアラートは正常に既読にできる`() {
        // Arrange
        val org1Alert =
            ProductAlertEntity().apply {
                organizationId = ORG1
                productId = PRODUCT_X
                alertType = "DECLINING"
                description = "org1: 紅茶の売上急降下"
            }
        em.persist(org1Alert)
        em.flush()
        val alertId = org1Alert.id

        // Act: org1 のコンテキストで自分のアラートを既読にする
        switchTenant(ORG1)
        val result = productAlertService.markAsRead(alertId)

        // Assert: 正常に既読化される
        assertNotNull(result)
        assertEquals(true, requireNotNull(result).isRead)
    }

    // --- ProductAlertService: listByOrganization ---

    @Test
    @TestTransaction
    fun `ProductAlert listByOrganization - org1のアラート一覧にorg2のアラートが含まれない`() {
        // Arrange
        em.persist(
            ProductAlertEntity().apply {
                organizationId = ORG1
                productId = PRODUCT_X
                alertType = "TRENDING"
                description = "org1のアラート"
            },
        )
        em.persist(
            ProductAlertEntity().apply {
                organizationId = ORG2
                productId = PRODUCT_Y
                alertType = "ANOMALY"
                description = "org2のアラート"
            },
        )
        em.flush()

        // Act: org1 のアラート一覧取得
        val (org1Alerts, org1Count) = productAlertService.listByOrganization(ORG1, 0, 20)

        // Assert
        assertEquals(1L, org1Count)
        assertEquals(1, org1Alerts.size)
        assertTrue(org1Alerts.all { it.organizationId == ORG1 })

        // Act: org2 のアラート一覧取得
        val (org2Alerts, org2Count) = productAlertService.listByOrganization(ORG2, 0, 20)

        // Assert
        assertEquals(1L, org2Count)
        assertEquals(1, org2Alerts.size)
        assertTrue(org2Alerts.all { it.organizationId == ORG2 })
    }

    // --- フェイルセーフ: テナントフィルタ未設定時 ---

    @Test
    fun `フェイルセーフ - getDailySalesでorganizationId未設定時にIllegalArgumentExceptionがスローされる`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            analyticsService.getDailySales(STORE_A, DATE, DATE)
        }
    }

    @Test
    fun `フェイルセーフ - getSalesSummaryでorganizationId未設定時にIllegalArgumentExceptionがスローされる`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            analyticsService.getSalesSummary(STORE_A, DATE, DATE)
        }
    }

    @Test
    fun `フェイルセーフ - getHourlySalesでorganizationId未設定時にIllegalArgumentExceptionがスローされる`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            analyticsService.getHourlySales(STORE_A, DATE)
        }
    }

    @Test
    fun `フェイルセーフ - getProductSalesでorganizationId未設定時にIllegalArgumentExceptionがスローされる`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            analyticsService.getProductSales(STORE_A, DATE, DATE, null, null, 0, 20)
        }
    }

    @Test
    fun `フェイルセーフ - getAbcAnalysisでorganizationId未設定時にIllegalArgumentExceptionがスローされる`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            analyticsQueryService.getAbcAnalysis(STORE_A, DATE, DATE)
        }
    }

    @Test
    fun `フェイルセーフ - getGrossProfitReportでorganizationId未設定時にIllegalArgumentExceptionがスローされる`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            analyticsQueryService.getGrossProfitReport(STORE_A, DATE, DATE)
        }
    }

    @Test
    fun `フェイルセーフ - getSalesForecastでorganizationId未設定時にIllegalArgumentExceptionがスローされる`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            analyticsQueryService.getSalesForecast(STORE_A, DATE, DATE, 7)
        }
    }

    @Test
    fun `フェイルセーフ - getCategorySalesReportでorganizationId未設定時にIllegalArgumentExceptionがスローされる`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            analyticsQueryService.getCategorySalesReport(STORE_A, DATE, DATE)
        }
    }

    @Test
    fun `フェイルセーフ - SalesTargetService listAllでorganizationId未設定時にIllegalArgumentExceptionがスローされる`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            salesTargetService.listAll()
        }
    }

    @Test
    fun `フェイルセーフ - SalesTargetService findByStoreAndMonthでorganizationId未設定時にIllegalArgumentExceptionがスローされる`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            salesTargetService.findByStoreAndMonth(STORE_A, DATE)
        }
    }

    // --- 複合シナリオ: テナント切り替え時のデータ完全分離 ---

    @Test
    @TestTransaction
    fun `テナント切り替えを繰り返してもデータが漏洩しない`() {
        // Arrange: 両テナントに全エンティティのデータを投入
        em.persist(
            DailySalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                date = DATE
                grossAmount = 111111
                netAmount = 100000
                taxAmount = 11111
                transactionCount = 11
            },
        )
        em.persist(
            DailySalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_A
                date = DATE
                grossAmount = 222222
                netAmount = 200000
                taxAmount = 22222
                transactionCount = 22
            },
        )
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                date = DATE
                productId = PRODUCT_X
                productName = "org1商品"
                quantitySold = 1
                totalAmount = 1111
                costAmount = 500
                transactionCount = 1
            },
        )
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_A
                date = DATE
                productId = PRODUCT_Y
                productName = "org2商品"
                quantitySold = 2
                totalAmount = 2222
                costAmount = 1000
                transactionCount = 2
            },
        )
        em.persist(
            SalesTargetEntity().apply {
                organizationId = ORG1
                storeId = STORE_A
                targetMonth = DATE
                targetAmount = 111
            },
        )
        em.persist(
            SalesTargetEntity().apply {
                organizationId = ORG2
                storeId = STORE_A
                targetMonth = DATE
                targetAmount = 222
            },
        )
        em.flush()

        // Round 1: org1
        switchTenant(ORG1)
        assertEquals(111111L, analyticsService.getDailySales(STORE_A, DATE, DATE)[0].grossAmount)
        assertEquals(1, analyticsQueryService.getAbcAnalysis(STORE_A, DATE, DATE).size)
        assertEquals(111L, salesTargetService.findByStoreAndMonth(STORE_A, DATE)?.targetAmount)

        // Round 2: org2
        switchTenant(ORG2)
        assertEquals(222222L, analyticsService.getDailySales(STORE_A, DATE, DATE)[0].grossAmount)
        assertEquals(1, analyticsQueryService.getAbcAnalysis(STORE_A, DATE, DATE).size)
        assertEquals(222L, salesTargetService.findByStoreAndMonth(STORE_A, DATE)?.targetAmount)

        // Round 3: org1 に戻す（フィルターキャッシュによる漏洩がないこと）
        switchTenant(ORG1)
        val org1DailyAgain = analyticsService.getDailySales(STORE_A, DATE, DATE)
        assertEquals(1, org1DailyAgain.size)
        assertEquals(111111L, org1DailyAgain[0].grossAmount)
        val org1TargetAgain = salesTargetService.findByStoreAndMonth(STORE_A, DATE)
        assertNotNull(org1TargetAgain)
        assertEquals(111L, requireNotNull(org1TargetAgain).targetAmount)
    }
}
