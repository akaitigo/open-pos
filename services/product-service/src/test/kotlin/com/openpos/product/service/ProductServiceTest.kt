package com.openpos.product.service

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.ProductEntity
import com.openpos.product.repository.ProductRepository
import io.quarkus.panache.common.Page
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class ProductServiceTest {
    @Inject
    lateinit var productService: ProductService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var productRepository: ProductRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()
    private val taxRateId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === create ===

    @Nested
    inner class Create {
        @Test
        fun `商品を正常に作成する`() {
            // Arrange
            doNothing().whenever(productRepository).persist(any<ProductEntity>())

            // Act
            val result =
                productService.create(
                    name = "テスト商品",
                    barcode = "4901234567890",
                    sku = "SKU-001",
                    price = 10000L,
                    categoryId = categoryId,
                    taxRateId = taxRateId,
                    imageUrl = "https://example.com/image.png",
                    displayOrder = 1,
                )

            // Assert
            assertEquals("テスト商品", result.name)
            assertEquals("4901234567890", result.barcode)
            assertEquals("SKU-001", result.sku)
            assertEquals(10000L, result.price)
            assertEquals(categoryId, result.categoryId)
            assertEquals(taxRateId, result.taxRateId)
            assertEquals("https://example.com/image.png", result.imageUrl)
            assertEquals(1, result.displayOrder)
            assertTrue(result.isActive)
            assertEquals(orgId, result.organizationId)
            verify(productRepository).persist(any<ProductEntity>())
        }

        @Test
        fun `オプションフィールドがnullでも作成できる`() {
            // Arrange
            doNothing().whenever(productRepository).persist(any<ProductEntity>())

            // Act
            val result =
                productService.create(
                    name = "シンプル商品",
                    barcode = null,
                    sku = null,
                    price = 5000L,
                    categoryId = null,
                    taxRateId = null,
                    imageUrl = null,
                    displayOrder = 0,
                )

            // Assert
            assertEquals("シンプル商品", result.name)
            assertNull(result.barcode)
            assertNull(result.sku)
            assertEquals(5000L, result.price)
            assertNull(result.categoryId)
            assertNull(result.taxRateId)
            assertNull(result.imageUrl)
            verify(productRepository).persist(any<ProductEntity>())
        }
    }

    // === findById ===

    @Nested
    inner class FindById {
        @Test
        fun `IDで商品を取得する`() {
            // Arrange
            val productId = UUID.randomUUID()
            val entity =
                ProductEntity().apply {
                    this.id = productId
                    this.organizationId = orgId
                    this.name = "テスト商品"
                    this.price = 10000L
                    this.displayOrder = 1
                    this.isActive = true
                }
            whenever(productRepository.findById(productId)).thenReturn(entity)

            // Act
            val result = productService.findById(productId)

            // Assert
            assertNotNull(result)
            assertEquals(productId, result!!.id)
            assertEquals("テスト商品", result.name)
            verify(tenantFilterService).enableFilter()
            verify(productRepository).findById(productId)
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val productId = UUID.randomUUID()
            whenever(productRepository.findById(productId)).thenReturn(null)

            // Act
            val result = productService.findById(productId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === findByBarcode ===

    @Nested
    inner class FindByBarcode {
        @Test
        fun `バーコードで商品を取得する`() {
            // Arrange
            val entity =
                ProductEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "バーコード商品"
                    this.barcode = "4901234567890"
                    this.price = 15000L
                    this.displayOrder = 0
                    this.isActive = true
                }
            whenever(productRepository.findByBarcode("4901234567890")).thenReturn(entity)

            // Act
            val result = productService.findByBarcode("4901234567890")

            // Assert
            assertNotNull(result)
            assertEquals("バーコード商品", result!!.name)
            assertEquals("4901234567890", result.barcode)
            verify(tenantFilterService).enableFilter()
            verify(productRepository).findByBarcode("4901234567890")
        }

        @Test
        fun `存在しないバーコードの場合はnullを返す`() {
            // Arrange
            whenever(productRepository.findByBarcode("9999999999999")).thenReturn(null)

            // Act
            val result = productService.findByBarcode("9999999999999")

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === search ===

    @Nested
    inner class Search {
        @Test
        fun `クエリとカテゴリで商品を検索する`() {
            // Arrange
            val product1 =
                ProductEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "テスト商品A"
                    this.price = 10000L
                    this.displayOrder = 1
                    this.isActive = true
                }
            val product2 =
                ProductEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "テスト商品B"
                    this.price = 20000L
                    this.displayOrder = 2
                    this.isActive = true
                }
            whenever(productRepository.search(any(), any(), any(), any<Page>()))
                .thenReturn(listOf(product1, product2))
            whenever(productRepository.searchCount(any(), any(), any()))
                .thenReturn(2L)

            // Act
            val (products, totalCount) =
                productService.search(
                    query = "テスト",
                    categoryId = categoryId,
                    activeOnly = true,
                    page = 0,
                    pageSize = 20,
                )

            // Assert
            assertEquals(2, products.size)
            assertEquals(2L, totalCount)
            assertEquals("テスト商品A", products[0].name)
            assertEquals("テスト商品B", products[1].name)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `検索結果が0件の場合は空リストを返す`() {
            // Arrange
            whenever(productRepository.search(any(), any(), any(), any<Page>()))
                .thenReturn(emptyList())
            whenever(productRepository.searchCount(any(), any(), any()))
                .thenReturn(0L)

            // Act
            val (products, totalCount) =
                productService.search(
                    query = "存在しない商品",
                    categoryId = null,
                    activeOnly = false,
                    page = 0,
                    pageSize = 20,
                )

            // Assert
            assertEquals(0, products.size)
            assertEquals(0L, totalCount)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === update ===

    @Nested
    inner class Update {
        @Test
        fun `商品の一部フィールドを更新する`() {
            // Arrange
            val productId = UUID.randomUUID()
            val entity =
                ProductEntity().apply {
                    this.id = productId
                    this.organizationId = orgId
                    this.name = "更新前の商品"
                    this.barcode = "1111111111111"
                    this.sku = "OLD-SKU"
                    this.price = 10000L
                    this.displayOrder = 1
                    this.isActive = true
                }
            whenever(productRepository.findById(productId)).thenReturn(entity)
            doNothing().whenever(productRepository).persist(any<ProductEntity>())

            // Act
            val result =
                productService.update(
                    id = productId,
                    name = "更新後の商品",
                    barcode = null,
                    sku = null,
                    price = 20000L,
                    categoryId = null,
                    taxRateId = null,
                    imageUrl = null,
                    displayOrder = null,
                    isActive = null,
                )

            // Assert
            assertNotNull(result)
            assertEquals("更新後の商品", result!!.name)
            assertEquals(20000L, result.price)
            // null のフィールドは更新されない
            assertEquals("1111111111111", result.barcode)
            assertEquals("OLD-SKU", result.sku)
            verify(tenantFilterService).enableFilter()
            verify(productRepository).persist(any<ProductEntity>())
        }

        @Test
        fun `存在しない商品の更新はnullを返す`() {
            // Arrange
            val productId = UUID.randomUUID()
            whenever(productRepository.findById(productId)).thenReturn(null)

            // Act
            val result =
                productService.update(
                    id = productId,
                    name = "更新後の商品",
                    barcode = null,
                    sku = null,
                    price = null,
                    categoryId = null,
                    taxRateId = null,
                    imageUrl = null,
                    displayOrder = null,
                    isActive = null,
                )

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === delete ===

    @Nested
    inner class Delete {
        @Test
        fun `商品を論理削除する`() {
            // Arrange
            val productId = UUID.randomUUID()
            val entity =
                ProductEntity().apply {
                    this.id = productId
                    this.organizationId = orgId
                    this.name = "削除対象商品"
                    this.price = 10000L
                    this.displayOrder = 0
                    this.isActive = true
                }
            whenever(productRepository.findById(productId)).thenReturn(entity)
            doNothing().whenever(productRepository).persist(any<ProductEntity>())

            // Act
            val result = productService.delete(productId)

            // Assert
            assertTrue(result)
            assertFalse(entity.isActive)
            verify(tenantFilterService).enableFilter()
            verify(productRepository).persist(any<ProductEntity>())
        }

        @Test
        fun `存在しない商品の削除はfalseを返す`() {
            // Arrange
            val productId = UUID.randomUUID()
            whenever(productRepository.findById(productId)).thenReturn(null)

            // Act
            val result = productService.delete(productId)

            // Assert
            assertFalse(result)
            verify(tenantFilterService).enableFilter()
        }
    }
}
