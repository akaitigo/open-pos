package com.openpos.store.integration

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.StoreEntity
import com.openpos.store.repository.StoreRepository
import io.quarkus.panache.common.Page
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * StoreRepository の Testcontainers 結合テスト。
 * PostgreSQL 17 + Flyway マイグレーションで実スキーマを使用する。
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile::class)
class StoreRepositoryIntegrationTest {
    @Inject
    lateinit var storeRepository: StoreRepository

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var entityManager: EntityManager

    private val testOrgId: UUID = UUID.randomUUID()

    @BeforeEach
    @Transactional
    fun setUp() {
        // FK制約を考慮した順序でクリーンアップ（子テーブルから先に削除）
        entityManager.createNativeQuery("DELETE FROM store_schema.staff").executeUpdate()
        entityManager.createNativeQuery("DELETE FROM store_schema.terminals").executeUpdate()
        entityManager.createNativeQuery("DELETE FROM store_schema.stores").executeUpdate()
        entityManager.createNativeQuery("DELETE FROM store_schema.organizations").executeUpdate()
        // テスト用 organization を挿入（stores.organization_id FK のため）
        entityManager
            .createNativeQuery(
                """INSERT INTO store_schema.organizations (id, name, business_type, plan, created_at, updated_at)
                   VALUES (:id, 'Test Organization', 'RETAIL', 'FREE', NOW(), NOW())""",
            ).setParameter("id", testOrgId)
            .executeUpdate()
        organizationIdHolder.organizationId = testOrgId
        tenantFilterService.enableFilter()
    }

    @Test
    @Transactional
    fun `persist and find store by id`() {
        // Arrange
        val store =
            StoreEntity().apply {
                organizationId = testOrgId
                name = "テスト渋谷店"
                code = "SHIBUYA-001"
                address = "東京都渋谷区道玄坂1-1-1"
                phone = "03-1234-5678"
                timezone = "Asia/Tokyo"
                isActive = true
            }

        // Act
        storeRepository.persist(store)
        val found = storeRepository.findById(store.id)

        // Assert
        assertNotNull(found)
        requireNotNull(found)
        assertEquals("テスト渋谷店", found.name)
        assertEquals("SHIBUYA-001", found.code)
        assertEquals("東京都渋谷区道玄坂1-1-1", found.address)
        assertEquals("03-1234-5678", found.phone)
        assertEquals(testOrgId, found.organizationId)
    }

    @Test
    @Transactional
    fun `listPaginated returns stores sorted by name`() {
        // Arrange
        listOf("C店舗", "A店舗", "B店舗").forEach { name ->
            storeRepository.persist(
                StoreEntity().apply {
                    organizationId = testOrgId
                    this.name = name
                    timezone = "Asia/Tokyo"
                    isActive = true
                },
            )
        }

        // Act
        val results = storeRepository.listPaginated(Page.ofSize(10))

        // Assert
        assertEquals(3, results.size)
        assertEquals("A店舗", results[0].name)
        assertEquals("B店舗", results[1].name)
        assertEquals("C店舗", results[2].name)
    }

    @Test
    @Transactional
    fun `tenant isolation prevents cross-organization access`() {
        // Arrange: create store under testOrgId
        storeRepository.persist(
            StoreEntity().apply {
                organizationId = testOrgId
                name = "テナント分離テスト店舗"
                timezone = "Asia/Tokyo"
                isActive = true
            },
        )
        entityManager.flush()

        // Act: switch to different org
        val differentOrgId = UUID.randomUUID()
        organizationIdHolder.organizationId = differentOrgId
        tenantFilterService.enableFilter()

        val results = storeRepository.listPaginated(Page.ofSize(10))

        // Assert: should not find store from different org
        assertEquals(0, results.size)
    }

    @Test
    @Transactional
    fun `findAllByOrganizationId returns correct stores`() {
        // Arrange
        val otherOrgId = UUID.randomUUID()
        storeRepository.persist(
            StoreEntity().apply {
                organizationId = testOrgId
                name = "自組織の店舗"
                timezone = "Asia/Tokyo"
                isActive = true
            },
        )
        // 他組織の organization を先に挿入（FK制約のため）
        entityManager
            .createNativeQuery(
                """INSERT INTO store_schema.organizations (id, name, business_type, plan, created_at, updated_at)
                   VALUES (:id, 'Other Organization', 'RETAIL', 'FREE', NOW(), NOW())""",
            ).setParameter("id", otherOrgId)
            .executeUpdate()
        // Note: we need to bypass the filter for inserting another org's store
        entityManager
            .createNativeQuery(
                """INSERT INTO store_schema.stores
               (id, organization_id, name, timezone, is_active, settings, created_at, updated_at)
               VALUES (:id, :orgId, :name, 'Asia/Tokyo', true, '{}', NOW(), NOW())""",
            ).setParameter("id", UUID.randomUUID())
            .setParameter("orgId", otherOrgId)
            .setParameter("name", "他組織の店舗")
            .executeUpdate()

        entityManager.flush()

        // Act
        val results = storeRepository.findAllByOrganizationId(testOrgId)

        // Assert
        assertEquals(1, results.size)
        assertEquals("自組織の店舗", results.first().name)
    }

    @Test
    fun `Flyway migrations executed successfully`() {
        // Verify that Flyway migrations ran by checking for expected tables
        val tables =
            entityManager
                .createNativeQuery(
                    """SELECT table_name FROM information_schema.tables
               WHERE table_schema = 'store_schema'
               ORDER BY table_name""",
                ).resultList

        // Assert core tables exist
        val tableNames = tables.map { it.toString() }
        assertTrue(tableNames.contains("stores"), "stores table should exist")
        assertTrue(tableNames.contains("staff"), "staff table should exist")
        assertTrue(tableNames.contains("terminals"), "terminals table should exist")
    }
}
