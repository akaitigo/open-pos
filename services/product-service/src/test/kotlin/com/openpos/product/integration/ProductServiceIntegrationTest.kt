package com.openpos.product.integration

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.ProductEntity
import com.openpos.product.repository.ProductRepository
import com.openpos.product.service.ProductService
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * ProductService の結合テスト（H2 ベース）。
 * CDI コンテナ内で Service → Repository → DB の一連の流れを検証する。
 */
@QuarkusTest
@TestProfile(H2IntegrationTestProfile::class)
class ProductServiceIntegrationTest {
    @Inject
    lateinit var productService: ProductService

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
        entityManager.createNativeQuery("DELETE FROM product_schema.products").executeUpdate()
        organizationIdHolder.organizationId = testOrgId
        tenantFilterService.enableFilter()
    }

    @Test
    @Transactional
    fun `service layer persists and retrieves product through repository`() {
        // Arrange
        val product =
            ProductEntity().apply {
                organizationId = testOrgId
                name = "結合テスト商品"
                description = "Service経由のテスト"
                price = 15000L
                displayOrder = 0
                isActive = true
            }
        productRepository.persist(product)

        // Act
        val found = productRepository.findById(product.id)

        // Assert
        assertNotNull(found)
        requireNotNull(found)
        assertEquals("結合テスト商品", found.name)
        assertEquals(15000L, found.price)
    }

    @Test
    @Transactional
    fun `tenant isolation prevents cross-organization access`() {
        // Arrange: create product under testOrgId
        val product =
            ProductEntity().apply {
                organizationId = testOrgId
                name = "テナント分離テスト"
                price = 20000L
                displayOrder = 0
                isActive = true
            }
        productRepository.persist(product)
        entityManager.flush()

        // Act: switch to different org and search
        val differentOrgId = UUID.randomUUID()
        organizationIdHolder.organizationId = differentOrgId
        tenantFilterService.enableFilter()

        val results =
            productRepository.search(
                null,
                null,
                false,
                io.quarkus.panache.common.Page
                    .ofSize(10),
            )

        // Assert: should not find product from different org
        assertEquals(0, results.size)
    }
}
