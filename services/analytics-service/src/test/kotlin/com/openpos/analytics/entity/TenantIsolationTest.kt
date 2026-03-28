package com.openpos.analytics.entity

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import io.quarkus.test.TestTransaction
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

/**
 * テナント隔離 E2E テスト。
 *
 * 2つの異なる組織（ORG1, ORG2）でデータを投入し、
 * Hibernate Filter による organizationId ベースのデータ分離が
 * 全エンティティで正しく機能することを検証する。
 *
 * 対象:
 * - DailySalesEntity（BaseEntity 継承）
 * - HourlySalesEntity（BaseEntity 継承）
 * - ProductSalesEntity（BaseEntity 継承）
 * - SalesTargetEntity（BaseEntity 継承）
 * - ProductAlertEntity（独自 FilterDef）
 *
 * 対象外:
 * - ProcessedEventEntity（テナント横断インフラテーブル）
 */
@QuarkusTest
class TenantIsolationTest {
    @Inject
    lateinit var em: EntityManager

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    companion object {
        private val ORG1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
        private val ORG2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
        private val ORG_EMPTY = UUID.fromString("99999999-9999-9999-9999-999999999999")
        private val STORE_SHARED = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        private val PRODUCT_A = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        private val PRODUCT_B = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        private val DATE = LocalDate.of(2026, 3, 1)
    }

    private fun enableFilter(orgId: UUID) {
        val session = em.unwrap(org.hibernate.Session::class.java)
        session.disableFilter("organizationFilter")
        organizationIdHolder.organizationId = orgId
        tenantFilterService.enableFilter()
    }

    // --- DailySalesEntity ---

    @Test
    @TestTransaction
    fun `DailySales - org1のデータはorg2から見えない`() {
        em.persist(
            DailySalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_SHARED
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
                storeId = STORE_SHARED
                date = DATE
                grossAmount = 200000
                netAmount = 180000
                taxAmount = 20000
                transactionCount = 10
            },
        )
        em.flush()

        enableFilter(ORG1)
        val org1Results = em.createQuery("SELECT e FROM DailySalesEntity e", DailySalesEntity::class.java).resultList
        assertTrue(org1Results.all { it.organizationId == ORG1 })
        assertEquals(1, org1Results.size)
        assertEquals(100000L, org1Results[0].grossAmount)

        enableFilter(ORG2)
        val org2Results = em.createQuery("SELECT e FROM DailySalesEntity e", DailySalesEntity::class.java).resultList
        assertTrue(org2Results.all { it.organizationId == ORG2 })
        assertEquals(1, org2Results.size)
        assertEquals(200000L, org2Results[0].grossAmount)
    }

    // --- HourlySalesEntity ---

    @Test
    @TestTransaction
    fun `HourlySales - org1のデータはorg2から見えない`() {
        em.persist(
            HourlySalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_SHARED
                saleDate = DATE
                hour = 10
                totalSales = 50000
                transactionCount = 3
            },
        )
        em.persist(
            HourlySalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_SHARED
                saleDate = DATE
                hour = 10
                totalSales = 80000
                transactionCount = 7
            },
        )
        em.flush()

        enableFilter(ORG1)
        val org1Results =
            em.createQuery("SELECT e FROM HourlySalesEntity e", HourlySalesEntity::class.java).resultList
        assertEquals(1, org1Results.size)
        assertTrue(org1Results.all { it.organizationId == ORG1 })
        assertEquals(50000L, org1Results[0].totalSales)

        enableFilter(ORG2)
        val org2Results =
            em.createQuery("SELECT e FROM HourlySalesEntity e", HourlySalesEntity::class.java).resultList
        assertEquals(1, org2Results.size)
        assertTrue(org2Results.all { it.organizationId == ORG2 })
        assertEquals(80000L, org2Results[0].totalSales)
    }

    // --- ProductSalesEntity ---

    @Test
    @TestTransaction
    fun `ProductSales - 同一storeId productIdでもorgごとに分離される`() {
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_SHARED
                date = DATE
                productId = PRODUCT_A
                productName = "コーヒー"
                quantitySold = 10
                totalAmount = 30000
                costAmount = 15000
                transactionCount = 8
            },
        )
        em.persist(
            ProductSalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_SHARED
                date = DATE
                productId = PRODUCT_A
                productName = "コーヒー"
                quantitySold = 20
                totalAmount = 60000
                costAmount = 30000
                transactionCount = 15
            },
        )
        em.flush()

        enableFilter(ORG1)
        val org1Results =
            em.createQuery("SELECT e FROM ProductSalesEntity e", ProductSalesEntity::class.java).resultList
        assertEquals(1, org1Results.size)
        assertTrue(org1Results.all { it.organizationId == ORG1 })
        assertEquals(10, org1Results[0].quantitySold)

        enableFilter(ORG2)
        val org2Results =
            em.createQuery("SELECT e FROM ProductSalesEntity e", ProductSalesEntity::class.java).resultList
        assertEquals(1, org2Results.size)
        assertTrue(org2Results.all { it.organizationId == ORG2 })
        assertEquals(20, org2Results[0].quantitySold)
    }

    // --- SalesTargetEntity ---

    @Test
    @TestTransaction
    fun `SalesTarget - org1のデータはorg2から見えない`() {
        em.persist(
            SalesTargetEntity().apply {
                organizationId = ORG1
                storeId = STORE_SHARED
                targetMonth = DATE
                targetAmount = 5000000
            },
        )
        em.persist(
            SalesTargetEntity().apply {
                organizationId = ORG2
                storeId = STORE_SHARED
                targetMonth = DATE
                targetAmount = 10000000
            },
        )
        em.flush()

        enableFilter(ORG1)
        val org1Results =
            em.createQuery("SELECT e FROM SalesTargetEntity e", SalesTargetEntity::class.java).resultList
        assertEquals(1, org1Results.size)
        assertTrue(org1Results.all { it.organizationId == ORG1 })
        assertEquals(5000000L, org1Results[0].targetAmount)

        enableFilter(ORG2)
        val org2Results =
            em.createQuery("SELECT e FROM SalesTargetEntity e", SalesTargetEntity::class.java).resultList
        assertEquals(1, org2Results.size)
        assertTrue(org2Results.all { it.organizationId == ORG2 })
        assertEquals(10000000L, org2Results[0].targetAmount)
    }

    // --- ProductAlertEntity (独自 FilterDef) ---

    @Test
    @TestTransaction
    fun `ProductAlert - 独自FilterDefでもorgごとに分離される`() {
        em.persist(
            ProductAlertEntity().apply {
                organizationId = ORG1
                productId = PRODUCT_A
                alertType = "TRENDING"
                description = "org1: コーヒーの売上が急上昇"
            },
        )
        em.persist(
            ProductAlertEntity().apply {
                organizationId = ORG2
                productId = PRODUCT_B
                alertType = "DECLINING"
                description = "org2: 紅茶の売上が急降下"
            },
        )
        em.flush()

        enableFilter(ORG1)
        val org1Results =
            em.createQuery("SELECT e FROM ProductAlertEntity e", ProductAlertEntity::class.java).resultList
        assertEquals(1, org1Results.size)
        assertTrue(org1Results.all { it.organizationId == ORG1 })
        assertEquals("TRENDING", org1Results[0].alertType)

        enableFilter(ORG2)
        val org2Results =
            em.createQuery("SELECT e FROM ProductAlertEntity e", ProductAlertEntity::class.java).resultList
        assertEquals(1, org2Results.size)
        assertTrue(org2Results.all { it.organizationId == ORG2 })
        assertEquals("DECLINING", org2Results[0].alertType)
    }

    // --- 境界値テスト ---

    @Test
    @TestTransaction
    fun `データが存在しないテナントでフィルターすると空リストを返す`() {
        em.persist(
            DailySalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_SHARED
                date = DATE.plusDays(10)
                grossAmount = 100000
                netAmount = 90000
                taxAmount = 10000
                transactionCount = 5
            },
        )
        em.flush()

        enableFilter(ORG_EMPTY)
        val results = em.createQuery("SELECT e FROM DailySalesEntity e", DailySalesEntity::class.java).resultList
        assertTrue(results.isEmpty())
    }

    @Test
    @TestTransaction
    fun `複数エンティティにまたがるorg間の完全分離`() {
        // org1 に DailySales + ProductAlert
        em.persist(
            DailySalesEntity().apply {
                organizationId = ORG1
                storeId = STORE_SHARED
                date = DATE.plusDays(20)
                grossAmount = 100000
                netAmount = 90000
                taxAmount = 10000
                transactionCount = 5
            },
        )
        em.persist(
            ProductAlertEntity().apply {
                organizationId = ORG1
                productId = PRODUCT_A
                alertType = "ANOMALY"
                description = "org1 異常検知"
            },
        )
        // org2 に HourlySales + SalesTarget
        em.persist(
            HourlySalesEntity().apply {
                organizationId = ORG2
                storeId = STORE_SHARED
                saleDate = DATE.plusDays(20)
                hour = 14
                totalSales = 70000
                transactionCount = 4
            },
        )
        em.persist(
            SalesTargetEntity().apply {
                organizationId = ORG2
                storeId = STORE_SHARED
                targetMonth = DATE.plusMonths(1)
                targetAmount = 8000000
            },
        )
        em.flush()

        // org1: DailySales/ProductAlert が見える、HourlySales/SalesTarget は見えない
        enableFilter(ORG1)
        val org1Daily =
            em
                .createQuery("SELECT e FROM DailySalesEntity e WHERE e.date = :d", DailySalesEntity::class.java)
                .setParameter("d", DATE.plusDays(20))
                .resultList
        val org1Alerts =
            em
                .createQuery(
                    "SELECT e FROM ProductAlertEntity e WHERE e.alertType = 'ANOMALY'",
                    ProductAlertEntity::class.java,
                ).resultList
        val org1Hourly =
            em
                .createQuery(
                    "SELECT e FROM HourlySalesEntity e WHERE e.saleDate = :d",
                    HourlySalesEntity::class.java,
                ).setParameter("d", DATE.plusDays(20))
                .resultList
        val org1Targets =
            em
                .createQuery(
                    "SELECT e FROM SalesTargetEntity e WHERE e.targetMonth = :m",
                    SalesTargetEntity::class.java,
                ).setParameter("m", DATE.plusMonths(1))
                .resultList

        assertEquals(1, org1Daily.size)
        assertEquals(1, org1Alerts.size)
        assertTrue(org1Hourly.isEmpty(), "org2のHourlySalesがorg1から見えてはならない")
        assertTrue(org1Targets.isEmpty(), "org2のSalesTargetがorg1から見えてはならない")

        // org2: 逆のパターン
        enableFilter(ORG2)
        val org2Daily =
            em
                .createQuery("SELECT e FROM DailySalesEntity e WHERE e.date = :d", DailySalesEntity::class.java)
                .setParameter("d", DATE.plusDays(20))
                .resultList
        val org2Alerts =
            em
                .createQuery(
                    "SELECT e FROM ProductAlertEntity e WHERE e.alertType = 'ANOMALY'",
                    ProductAlertEntity::class.java,
                ).resultList
        val org2Hourly =
            em
                .createQuery(
                    "SELECT e FROM HourlySalesEntity e WHERE e.saleDate = :d",
                    HourlySalesEntity::class.java,
                ).setParameter("d", DATE.plusDays(20))
                .resultList
        val org2Targets =
            em
                .createQuery(
                    "SELECT e FROM SalesTargetEntity e WHERE e.targetMonth = :m",
                    SalesTargetEntity::class.java,
                ).setParameter("m", DATE.plusMonths(1))
                .resultList

        assertTrue(org2Daily.isEmpty(), "org1のDailySalesがorg2から見えてはならない")
        assertTrue(org2Alerts.isEmpty(), "org1のProductAlertがorg2から見えてはならない")
        assertEquals(1, org2Hourly.size)
        assertEquals(1, org2Targets.size)
    }
}
