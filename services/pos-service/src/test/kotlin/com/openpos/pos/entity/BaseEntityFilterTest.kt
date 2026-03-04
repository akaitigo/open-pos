package com.openpos.pos.entity

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.Table
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * テスト用サンプルエンティティ。
 * BaseEntity を継承し、organizationFilter の動作を検証する。
 */
@Entity
@Table(name = "test_pos_entity")
class TestPosEntity : BaseEntity() {
    var name: String = ""
}

@QuarkusTest
class BaseEntityFilterTest {
    @Inject
    lateinit var entityManager: EntityManager

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Test
    @Transactional
    fun `BaseEntity has organizationId field`() {
        val entity = TestPosEntity()
        entity.organizationId = UUID.randomUUID()
        entity.name = "test-pos"

        entityManager.persist(entity)
        entityManager.flush()

        assertNotNull(entity.id)
        assertNotNull(entity.organizationId)
    }

    @Test
    @Transactional
    fun `prePersist sets createdAt and updatedAt`() {
        val entity = TestPosEntity()
        entity.organizationId = UUID.randomUUID()
        entity.name = "timestamp-test"

        entityManager.persist(entity)
        entityManager.flush()

        assertNotNull(entity.createdAt)
        assertNotNull(entity.updatedAt)
    }

    @Test
    @Transactional
    fun `organizationFilter filters entities by organizationId`() {
        val orgA = UUID.randomUUID()
        val orgB = UUID.randomUUID()

        val entityA =
            TestPosEntity().apply {
                organizationId = orgA
                name = "org-a-pos"
            }
        val entityB =
            TestPosEntity().apply {
                organizationId = orgB
                name = "org-b-pos"
            }

        entityManager.persist(entityA)
        entityManager.persist(entityB)
        entityManager.flush()

        // フィルターを有効化して orgA のエンティティのみ取得
        organizationIdHolder.organizationId = orgA
        tenantFilterService.enableFilter()

        val results =
            entityManager
                .createQuery("SELECT e FROM TestPosEntity e", TestPosEntity::class.java)
                .resultList

        assertTrue(results.all { it.organizationId == orgA })
        assertTrue(results.none { it.organizationId == orgB })
    }

    @Test
    @Transactional
    fun `organizationFilter with different org returns different results`() {
        val orgA = UUID.randomUUID()
        val orgB = UUID.randomUUID()

        repeat(3) {
            entityManager.persist(
                TestPosEntity().apply {
                    organizationId = orgA
                    name = "org-a-item-$it"
                },
            )
        }
        repeat(2) {
            entityManager.persist(
                TestPosEntity().apply {
                    organizationId = orgB
                    name = "org-b-item-$it"
                },
            )
        }
        entityManager.flush()

        // orgA でフィルター
        organizationIdHolder.organizationId = orgA
        tenantFilterService.enableFilter()

        val resultsA =
            entityManager
                .createQuery("SELECT e FROM TestPosEntity e", TestPosEntity::class.java)
                .resultList

        assertEquals(3, resultsA.size)

        // orgB でフィルター（セッションリセット）
        entityManager.unwrap(org.hibernate.Session::class.java).disableFilter("organizationFilter")
        organizationIdHolder.organizationId = orgB
        tenantFilterService.enableFilter()

        val resultsB =
            entityManager
                .createQuery("SELECT e FROM TestPosEntity e", TestPosEntity::class.java)
                .resultList

        assertEquals(2, resultsB.size)
    }

    @Test
    fun `TenantFilterService throws when organizationId is null`() {
        organizationIdHolder.organizationId = null

        assertThrows(IllegalStateException::class.java) {
            tenantFilterService.enableFilter()
        }
    }
}
