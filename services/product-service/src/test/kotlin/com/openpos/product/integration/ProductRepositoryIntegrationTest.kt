package com.openpos.product.integration

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.ProductEntity
import com.openpos.product.repository.ProductRepository
import io.quarkus.panache.common.Page
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * ProductRepository の結合テスト。
 * Testcontainers で PostgreSQL 17 を起動し、Flyway マイグレーションで実スキーマを構築する。
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile::class)
class ProductRepositoryIntegrationTest {
    @Inject
    lateinit var productRepository: ProductRepository

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
        // フィルターなしで全データ削除（テスト間のデータ干渉を防止）
        entityManager.createNativeQuery("DELETE FROM product_schema.products").executeUpdate()
        organizationIdHolder.organizationId = testOrgId
        tenantFilterService.enableFilter()
    }

    @Test
    @Transactional
    fun `persist and find product by id`() {
        // Arrange
        val product =
            ProductEntity().apply {
                organizationId = testOrgId
                name = "テスト商品A"
                description = "商品説明"
                barcode = "4901234567890"
                sku = "SKU-001"
                price = 10000L
                displayOrder = 1
                isActive = true
            }

        // Act
        productRepository.persist(product)
        val found = productRepository.findById(product.id)

        // Assert
        assertNotNull(found)
        requireNotNull(found)
        assertEquals("テスト商品A", found.name)
        assertEquals("商品説明", found.description)
        assertEquals("4901234567890", found.barcode)
        assertEquals(10000L, found.price)
        assertEquals(testOrgId, found.organizationId)
    }

    @Test
    @Transactional
    fun `findByBarcode returns matching product`() {
        // Arrange
        val product =
            ProductEntity().apply {
                organizationId = testOrgId
                name = "バーコード検索商品"
                barcode = "4901234567891"
                price = 20000L
                displayOrder = 0
                isActive = true
            }
        productRepository.persist(product)

        // Act
        val found = productRepository.findByBarcode("4901234567891")

        // Assert
        assertNotNull(found)
        requireNotNull(found)
        assertEquals("バーコード検索商品", found.name)
    }

    @Test
    @Transactional
    fun `findByBarcode returns null for non-existent barcode`() {
        // Arrange - no products

        // Act
        val found = productRepository.findByBarcode("9999999999999")

        // Assert
        assertNull(found)
    }

    @Test
    @Transactional
    fun `search with query filters by name`() {
        // Arrange
        listOf("りんご", "みかん", "りんごジュース").forEach { name ->
            productRepository.persist(
                ProductEntity().apply {
                    organizationId = testOrgId
                    this.name = name
                    price = 10000L
                    displayOrder = 0
                    isActive = true
                },
            )
        }

        // Act
        val results = productRepository.search("りんご", null, false, Page.ofSize(10))

        // Assert
        assertEquals(2, results.size)
        assertTrue(results.all { it.name.contains("りんご") })
    }

    @Test
    @Transactional
    fun `search with activeOnly filters inactive products`() {
        // Arrange
        productRepository.persist(
            ProductEntity().apply {
                organizationId = testOrgId
                name = "アクティブ商品"
                price = 10000L
                displayOrder = 0
                isActive = true
            },
        )
        productRepository.persist(
            ProductEntity().apply {
                organizationId = testOrgId
                name = "非アクティブ商品"
                price = 20000L
                displayOrder = 1
                isActive = false
            },
        )

        // Act
        val results = productRepository.search(null, null, true, Page.ofSize(10))

        // Assert
        assertEquals(1, results.size)
        assertEquals("アクティブ商品", results.first().name)
    }

    @Test
    @Transactional
    fun `searchCount returns correct count`() {
        // Arrange
        repeat(5) { i ->
            productRepository.persist(
                ProductEntity().apply {
                    organizationId = testOrgId
                    name = "商品$i"
                    price = (i + 1) * 10000L
                    displayOrder = i
                    isActive = true
                },
            )
        }

        // Act
        val count = productRepository.searchCount(null, null, false)

        // Assert
        assertEquals(5L, count)
    }
}
