package com.openpos.product.service

import com.openpos.product.cache.ProductCacheService
import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.CategoryEntity
import com.openpos.product.entity.ProductEntity
import com.openpos.product.entity.TaxRateEntity
import com.openpos.product.repository.CategoryRepository
import com.openpos.product.repository.ProductRepository
import com.openpos.product.repository.TaxRateRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import io.quarkus.panache.common.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class ProductServiceTest {
    private lateinit var productService: ProductService

    private lateinit var organizationIdHolder: OrganizationIdHolder

    private lateinit var productRepository: ProductRepository

    private lateinit var tenantFilterService: TenantFilterService

    private lateinit var categoryRepository: CategoryRepository

    private lateinit var taxRateRepository: TaxRateRepository

    private lateinit var cacheService: ProductCacheService

    private val orgId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()
    private val taxRateId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        productRepository = mock()
        categoryRepository = mock()
        taxRateRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()
        cacheService = mock()

        productService = ProductService()
        productService.productRepository = productRepository
        productService.categoryRepository = categoryRepository
        productService.taxRateRepository = taxRateRepository
        productService.tenantFilterService = tenantFilterService
        productService.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()

        // FK 検証: デフォルトで同一テナントのレコードとして返す（HQL パターン）
        val mockCatQuery = mock<PanacheQuery<CategoryEntity>>()
        whenever(mockCatQuery.firstResult()).thenReturn(
            CategoryEntity().apply {
                this.id = categoryId
                this.organizationId = orgId
                this.name = "テストカテゴリ"
            },
        )
        whenever(categoryRepository.find(eq("id = ?1"), eq(categoryId))).thenReturn(mockCatQuery)

        val mockTaxQuery = mock<PanacheQuery<TaxRateEntity>>()
        whenever(mockTaxQuery.firstResult()).thenReturn(
            TaxRateEntity().apply {
                this.id = taxRateId
                this.organizationId = orgId
                this.name = "標準税率"
                this.rate = java.math.BigDecimal("10.00")
            },
        )
        whenever(taxRateRepository.find(eq("id = ?1"), eq(taxRateId))).thenReturn(mockTaxQuery)
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
                    description = "商品説明",
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
            assertEquals("商品説明", result.description)
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
                    description = null,
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
            assertNull(result.description)
            assertNull(result.barcode)
            assertNull(result.sku)
            assertEquals(5000L, result.price)
            assertNull(result.categoryId)
            assertNull(result.taxRateId)
            assertNull(result.imageUrl)
            verify(productRepository).persist(any<ProductEntity>())
        }

        @Test
        fun `価格が0の場合は作成できる`() {
            // Arrange
            doNothing().whenever(productRepository).persist(any<ProductEntity>())

            // Act
            val result =
                productService.create(
                    name = "無料商品",
                    description = null,
                    barcode = null,
                    sku = null,
                    price = 0L,
                    categoryId = null,
                    taxRateId = null,
                    imageUrl = null,
                    displayOrder = 0,
                )

            // Assert
            assertEquals(0L, result.price)
        }

        @Test
        fun `価格が負数の場合はIllegalArgumentExceptionを投げる`() {
            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                productService.create(
                    name = "不正商品",
                    description = null,
                    barcode = null,
                    sku = null,
                    price = -1L,
                    categoryId = null,
                    taxRateId = null,
                    imageUrl = null,
                    displayOrder = 0,
                )
            }
        }
    }

    // === テナント分離 FK 検証 ===

    @Nested
    inner class TenantForeignKeyValidation {
        @Test
        fun `他テナントのcategoryIdで作成するとIllegalArgumentException`() {
            // Arrange: categoryId が他テナント（テナントフィルタで null 返却）
            val otherTenantCategoryId = UUID.randomUUID()
            val mockQuery1 = mock<PanacheQuery<CategoryEntity>>()
            whenever(mockQuery1.firstResult()).thenReturn(null)
            whenever(categoryRepository.find(eq("id = ?1"), eq(otherTenantCategoryId))).thenReturn(mockQuery1)
            doNothing().whenever(productRepository).persist(any<ProductEntity>())

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                productService.create(
                    name = "テスト商品",
                    description = null,
                    barcode = null,
                    sku = null,
                    price = 10000L,
                    categoryId = otherTenantCategoryId,
                    taxRateId = null,
                    imageUrl = null,
                    displayOrder = 0,
                )
            }
        }

        @Test
        fun `他テナントのtaxRateIdで作成するとIllegalArgumentException`() {
            // Arrange: taxRateId が他テナント
            val otherTenantTaxRateId = UUID.randomUUID()
            val mockQuery2 = mock<PanacheQuery<TaxRateEntity>>()
            whenever(mockQuery2.firstResult()).thenReturn(null)
            whenever(taxRateRepository.find(eq("id = ?1"), eq(otherTenantTaxRateId))).thenReturn(mockQuery2)
            doNothing().whenever(productRepository).persist(any<ProductEntity>())

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                productService.create(
                    name = "テスト商品",
                    description = null,
                    barcode = null,
                    sku = null,
                    price = 10000L,
                    categoryId = null,
                    taxRateId = otherTenantTaxRateId,
                    imageUrl = null,
                    displayOrder = 0,
                )
            }
        }

        @Test
        fun `他テナントのcategoryIdで更新するとIllegalArgumentException`() {
            // Arrange
            val productId = UUID.randomUUID()
            val entity =
                ProductEntity().apply {
                    this.id = productId
                    this.organizationId = orgId
                    this.name = "既存商品"
                    this.price = 10000L
                    this.displayOrder = 0
                    this.isActive = true
                }
            val mockQuery3 = mock<PanacheQuery<ProductEntity>>()
            whenever(mockQuery3.firstResult()).thenReturn(entity)
            whenever(productRepository.find(eq("id = ?1"), eq(productId))).thenReturn(mockQuery3)

            val otherTenantCategoryId = UUID.randomUUID()
            val mockQuery4 = mock<PanacheQuery<CategoryEntity>>()
            whenever(mockQuery4.firstResult()).thenReturn(null)
            whenever(categoryRepository.find(eq("id = ?1"), eq(otherTenantCategoryId))).thenReturn(mockQuery4)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                productService.update(
                    id = productId,
                    name = null,
                    description = null,
                    barcode = null,
                    sku = null,
                    price = null,
                    categoryId = otherTenantCategoryId,
                    taxRateId = null,
                    imageUrl = null,
                    displayOrder = null,
                    isActive = null,
                )
            }
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
            val mockQuery5 = mock<PanacheQuery<ProductEntity>>()
            whenever(mockQuery5.firstResult()).thenReturn(entity)
            whenever(productRepository.find(eq("id = ?1"), eq(productId))).thenReturn(mockQuery5)

            // Act
            val result = productService.findById(productId)

            // Assert
            assertNotNull(result)
            assertEquals(productId, result!!.id)
            assertEquals("テスト商品", result.name)
            verify(tenantFilterService).enableFilter()
            verify(productRepository).find(eq("id = ?1"), eq(productId))
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val productId = UUID.randomUUID()
            val mockQuery6 = mock<PanacheQuery<ProductEntity>>()
            whenever(mockQuery6.firstResult()).thenReturn(null)
            whenever(productRepository.find(eq("id = ?1"), eq(productId))).thenReturn(mockQuery6)

            // Act
            val result = productService.findById(productId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === findByIds ===

    @Nested
    inner class FindByIds {
        @Test
        fun `複数IDで商品を一括取得する`() {
            // Arrange
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            val entity1 =
                ProductEntity().apply {
                    this.id = id1
                    this.organizationId = orgId
                    this.name = "商品A"
                    this.price = 10000L
                    this.displayOrder = 1
                    this.isActive = true
                }
            val entity2 =
                ProductEntity().apply {
                    this.id = id2
                    this.organizationId = orgId
                    this.name = "商品B"
                    this.price = 20000L
                    this.displayOrder = 2
                    this.isActive = true
                }
            whenever(productRepository.findByIds(listOf(id1, id2))).thenReturn(listOf(entity1, entity2))

            // Act
            val result = productService.findByIds(listOf(id1, id2))

            // Assert
            assertEquals(2, result.size)
            assertEquals("商品A", result[0].name)
            assertEquals("商品B", result[1].name)
            verify(tenantFilterService).enableFilter()
            verify(productRepository).findByIds(listOf(id1, id2))
        }

        @Test
        fun `空のIDリストの場合は空リストを返す`() {
            // Arrange
            whenever(productRepository.findByIds(emptyList())).thenReturn(emptyList())

            // Act
            val result = productService.findByIds(emptyList())

            // Assert
            assertEquals(0, result.size)
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
            val mockQuery7 = mock<PanacheQuery<ProductEntity>>()
            whenever(mockQuery7.firstResult()).thenReturn(entity)
            whenever(productRepository.find(eq("id = ?1"), eq(productId))).thenReturn(mockQuery7)
            doNothing().whenever(productRepository).persist(any<ProductEntity>())

            // Act
            val result =
                productService.update(
                    id = productId,
                    name = "更新後の商品",
                    description = "更新後の説明",
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
            assertEquals("更新後の説明", result.description)
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
            val mockQuery8 = mock<PanacheQuery<ProductEntity>>()
            whenever(mockQuery8.firstResult()).thenReturn(null)
            whenever(productRepository.find(eq("id = ?1"), eq(productId))).thenReturn(mockQuery8)

            // Act
            val result =
                productService.update(
                    id = productId,
                    name = "更新後の商品",
                    description = null,
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
            val mockQuery9 = mock<PanacheQuery<ProductEntity>>()
            whenever(mockQuery9.firstResult()).thenReturn(entity)
            whenever(productRepository.find(eq("id = ?1"), eq(productId))).thenReturn(mockQuery9)
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
            val mockQuery10 = mock<PanacheQuery<ProductEntity>>()
            whenever(mockQuery10.firstResult()).thenReturn(null)
            whenever(productRepository.find(eq("id = ?1"), eq(productId))).thenReturn(mockQuery10)

            // Act
            val result = productService.delete(productId)

            // Assert
            assertFalse(result)
            verify(tenantFilterService).enableFilter()
        }
    }
}
