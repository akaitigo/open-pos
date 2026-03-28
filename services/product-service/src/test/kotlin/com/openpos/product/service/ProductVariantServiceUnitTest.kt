package com.openpos.product.service

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.ProductVariantEntity
import com.openpos.product.repository.ProductVariantRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class ProductVariantServiceUnitTest {
    private lateinit var service: ProductVariantService
    private lateinit var variantRepository: ProductVariantRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        variantRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = ProductVariantService()
        service.variantRepository = variantRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(variantRepository).persist(any<ProductVariantEntity>())
    }

    @Nested
    inner class Create {
        @Test
        fun `creates variant with all fields`() {
            // Arrange
            val name = "Sサイズ"
            val sku = "ITEM-S"
            val barcode = "4901234567890"
            val price = 10000L
            val displayOrder = 1

            // Act
            val result = service.create(productId, name, sku, barcode, price, displayOrder)

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals(productId, result.productId)
            assertEquals(name, result.name)
            assertEquals(sku, result.sku)
            assertEquals(barcode, result.barcode)
            assertEquals(price, result.price)
            assertEquals(displayOrder, result.displayOrder)
            verify(variantRepository).persist(any<ProductVariantEntity>())
        }

        @Test
        fun `creates variant with null sku and barcode`() {
            // Arrange & Act
            val result = service.create(productId, "Mサイズ", null, null, 0L, 0)

            // Assert
            assertNotNull(result)
            assertEquals("Mサイズ", result.name)
            assertEquals(null, result.sku)
            assertEquals(null, result.barcode)
            assertEquals(0L, result.price)
        }

        @Test
        fun `creates variant with zero price (uses parent price)`() {
            // Arrange & Act
            val result = service.create(productId, "レッド", "ITEM-RED", null, 0L, 2)

            // Assert
            assertEquals(0L, result.price)
        }

        @Test
        fun `throws when organizationId is not set`() {
            // Arrange
            organizationIdHolder.organizationId = null

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                service.create(productId, "Lサイズ", null, null, 10000L, 0)
            }
        }
    }

    @Nested
    inner class ListByProductId {
        @Test
        fun `returns variants for product`() {
            // Arrange
            val variant1 =
                ProductVariantEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.productId = this@ProductVariantServiceUnitTest.productId
                    name = "Sサイズ"
                    price = 10000L
                    displayOrder = 0
                }
            val variant2 =
                ProductVariantEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.productId = this@ProductVariantServiceUnitTest.productId
                    name = "Mサイズ"
                    price = 12000L
                    displayOrder = 1
                }
            whenever(variantRepository.findByProductId(productId)).thenReturn(listOf(variant1, variant2))

            // Act
            val result = service.listByProductId(productId)

            // Assert
            assertEquals(2, result.size)
            assertEquals("Sサイズ", result[0].name)
            assertEquals("Mサイズ", result[1].name)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns empty list when no variants exist`() {
            // Arrange
            whenever(variantRepository.findByProductId(productId)).thenReturn(emptyList())

            // Act
            val result = service.listByProductId(productId)

            // Assert
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class ListByProductIds {
        @Test
        fun `returns variants grouped by product ID`() {
            // Arrange
            val productId2 = UUID.randomUUID()
            val variant1 =
                ProductVariantEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.productId = this@ProductVariantServiceUnitTest.productId
                    name = "S"
                    price = 10000L
                    displayOrder = 0
                }
            val variant2 =
                ProductVariantEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.productId = productId2
                    name = "レッド"
                    price = 15000L
                    displayOrder = 0
                }
            whenever(variantRepository.findByProductIds(listOf(productId, productId2)))
                .thenReturn(listOf(variant1, variant2))

            // Act
            val result = service.listByProductIds(listOf(productId, productId2))

            // Assert
            assertEquals(2, result.size)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns empty list for empty product IDs`() {
            // Arrange
            whenever(variantRepository.findByProductIds(emptyList())).thenReturn(emptyList())

            // Act
            val result = service.listByProductIds(emptyList())

            // Assert
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `updates variant name`() {
            // Arrange
            val variantId = UUID.randomUUID()
            val entity =
                ProductVariantEntity().apply {
                    id = variantId
                    organizationId = orgId
                    this.productId = this@ProductVariantServiceUnitTest.productId
                    name = "Old Name"
                    price = 10000L
                    isActive = true
                    displayOrder = 0
                }
            whenever(variantRepository.findById(variantId)).thenReturn(entity)

            // Act
            val result = service.update(variantId, "New Name", null, null, null, null, null)

            // Assert
            assertEquals("New Name", result.name)
            assertEquals(10000L, result.price)
            verify(variantRepository).persist(any<ProductVariantEntity>())
        }

        @Test
        fun `updates variant price`() {
            // Arrange
            val variantId = UUID.randomUUID()
            val entity =
                ProductVariantEntity().apply {
                    id = variantId
                    organizationId = orgId
                    this.productId = this@ProductVariantServiceUnitTest.productId
                    name = "Sサイズ"
                    price = 10000L
                    isActive = true
                    displayOrder = 0
                }
            whenever(variantRepository.findById(variantId)).thenReturn(entity)

            // Act
            val result = service.update(variantId, null, null, null, 20000L, null, null)

            // Assert
            assertEquals("Sサイズ", result.name)
            assertEquals(20000L, result.price)
        }

        @Test
        fun `updates variant isActive`() {
            // Arrange
            val variantId = UUID.randomUUID()
            val entity =
                ProductVariantEntity().apply {
                    id = variantId
                    organizationId = orgId
                    this.productId = this@ProductVariantServiceUnitTest.productId
                    name = "Mサイズ"
                    price = 15000L
                    isActive = true
                    displayOrder = 0
                }
            whenever(variantRepository.findById(variantId)).thenReturn(entity)

            // Act
            val result = service.update(variantId, null, null, null, null, false, null)

            // Assert
            assertFalse(result.isActive)
        }

        @Test
        fun `updates all fields simultaneously`() {
            // Arrange
            val variantId = UUID.randomUUID()
            val entity =
                ProductVariantEntity().apply {
                    id = variantId
                    organizationId = orgId
                    this.productId = this@ProductVariantServiceUnitTest.productId
                    name = "Original"
                    sku = "OLD-SKU"
                    barcode = "0000000000000"
                    price = 10000L
                    isActive = true
                    displayOrder = 0
                }
            whenever(variantRepository.findById(variantId)).thenReturn(entity)

            // Act
            val result =
                service.update(
                    variantId,
                    "Updated",
                    "NEW-SKU",
                    "9999999999999",
                    30000L,
                    false,
                    5,
                )

            // Assert
            assertEquals("Updated", result.name)
            assertEquals("NEW-SKU", result.sku)
            assertEquals("9999999999999", result.barcode)
            assertEquals(30000L, result.price)
            assertFalse(result.isActive)
            assertEquals(5, result.displayOrder)
        }

        @Test
        fun `throws when variant not found`() {
            // Arrange
            val variantId = UUID.randomUUID()
            whenever(variantRepository.findById(variantId)).thenReturn(null)

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                service.update(variantId, "Name", null, null, null, null, null)
            }
        }

        @Test
        fun `updates price to zero (revert to parent price)`() {
            // Arrange
            val variantId = UUID.randomUUID()
            val entity =
                ProductVariantEntity().apply {
                    id = variantId
                    organizationId = orgId
                    this.productId = this@ProductVariantServiceUnitTest.productId
                    name = "Sサイズ"
                    price = 10000L
                    isActive = true
                    displayOrder = 0
                }
            whenever(variantRepository.findById(variantId)).thenReturn(entity)

            // Act
            val result = service.update(variantId, null, null, null, 0L, null, null)

            // Assert
            assertEquals(0L, result.price)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `deletes variant and returns true`() {
            // Arrange
            val variantId = UUID.randomUUID()
            whenever(variantRepository.deleteById(variantId)).thenReturn(true)

            // Act
            val result = service.delete(variantId)

            // Assert
            assertTrue(result)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns false when variant not found`() {
            // Arrange
            val variantId = UUID.randomUUID()
            whenever(variantRepository.deleteById(variantId)).thenReturn(false)

            // Act
            val result = service.delete(variantId)

            // Assert
            assertFalse(result)
        }
    }

    @Nested
    inner class PriceBoundary {
        @Test
        fun `creates variant with minimum price (1 sen)`() {
            // Arrange & Act
            val result = service.create(productId, "最安バリアント", null, null, 1L, 0)

            // Assert
            assertEquals(1L, result.price)
        }

        @Test
        fun `creates variant with large price`() {
            // Arrange & Act
            val result = service.create(productId, "高額バリアント", null, null, 999_999_999_00L, 0)

            // Assert
            assertEquals(999_999_999_00L, result.price)
        }
    }
}
