package com.openpos.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.openpos.product.cache.ProductCacheService
import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.CategoryEntity
import com.openpos.product.repository.CategoryRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class CategoryServiceTest {
    private lateinit var categoryService: CategoryService

    private lateinit var organizationIdHolder: OrganizationIdHolder

    private lateinit var categoryRepository: CategoryRepository

    private lateinit var tenantFilterService: TenantFilterService

    private lateinit var cacheService: ProductCacheService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        categoryRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()
        cacheService = mock()

        categoryService = CategoryService()
        categoryService.categoryRepository = categoryRepository
        categoryService.tenantFilterService = tenantFilterService
        categoryService.organizationIdHolder = organizationIdHolder
        categoryService.cacheService = cacheService
        categoryService.objectMapper = ObjectMapper()

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(cacheService).invalidateCategory(any(), any())
        doNothing().whenever(cacheService).invalidateAllCategoryLists(any())
    }

    // === create ===

    @Nested
    inner class Create {
        @Test
        fun `ルートカテゴリを作成する`() {
            // Arrange
            doNothing().whenever(categoryRepository).persist(any<CategoryEntity>())

            // Act
            val result =
                categoryService.create(
                    name = "フード",
                    parentId = null,
                    color = "#FF5733",
                    icon = "food",
                    displayOrder = 1,
                )

            // Assert
            assertEquals("フード", result.name)
            assertNull(result.parentId)
            assertEquals("#FF5733", result.color)
            assertEquals("food", result.icon)
            assertEquals(1, result.displayOrder)
            assertEquals(orgId, result.organizationId)
            verify(categoryRepository).persist(any<CategoryEntity>())
        }

        @Test
        fun `子カテゴリを作成する`() {
            // Arrange
            val parentId = UUID.randomUUID()
            doNothing().whenever(categoryRepository).persist(any<CategoryEntity>())

            // Act
            val result =
                categoryService.create(
                    name = "ドリンク",
                    parentId = parentId,
                    color = "#3366FF",
                    icon = "drink",
                    displayOrder = 2,
                )

            // Assert
            assertEquals("ドリンク", result.name)
            assertEquals(parentId, result.parentId)
            assertEquals(orgId, result.organizationId)
            verify(categoryRepository).persist(any<CategoryEntity>())
        }
    }

    // === listByParentId ===

    @Nested
    inner class ListByParentId {
        @Test
        fun `ルートカテゴリ一覧を取得する`() {
            // Arrange
            val category1 =
                CategoryEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "フード"
                    this.displayOrder = 1
                }
            val category2 =
                CategoryEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "ドリンク"
                    this.displayOrder = 2
                }
            whenever(categoryRepository.findByParentId(null)).thenReturn(listOf(category1, category2))

            // Act
            val result = categoryService.listByParentId(null)

            // Assert
            assertEquals(2, result.size)
            assertEquals("フード", result[0].name)
            assertEquals("ドリンク", result[1].name)
            verify(tenantFilterService).enableFilter()
            verify(categoryRepository).findByParentId(null)
        }

        @Test
        fun `指定した親カテゴリの子カテゴリ一覧を取得する`() {
            // Arrange
            val parentId = UUID.randomUUID()
            val child =
                CategoryEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "サブカテゴリ"
                    this.parentId = parentId
                    this.displayOrder = 1
                }
            whenever(categoryRepository.findByParentId(parentId)).thenReturn(listOf(child))

            // Act
            val result = categoryService.listByParentId(parentId)

            // Assert
            assertEquals(1, result.size)
            assertEquals("サブカテゴリ", result[0].name)
            assertEquals(parentId, result[0].parentId)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === findById ===

    @Nested
    inner class FindById {
        @Test
        fun `IDでカテゴリを取得する`() {
            // Arrange
            val categoryId = UUID.randomUUID()
            val entity =
                CategoryEntity().apply {
                    this.id = categoryId
                    this.organizationId = orgId
                    this.name = "フード"
                    this.displayOrder = 1
                }
            val mockQuery1 = mock<PanacheQuery<CategoryEntity>>()
            whenever(mockQuery1.firstResult()).thenReturn(entity)
            whenever(categoryRepository.find(eq("id = ?1"), eq(categoryId))).thenReturn(mockQuery1)

            // Act
            val result = categoryService.findById(categoryId)

            // Assert
            assertNotNull(result)
            assertEquals(categoryId, result!!.id)
            assertEquals("フード", result.name)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val categoryId = UUID.randomUUID()
            val mockQuery2 = mock<PanacheQuery<CategoryEntity>>()
            whenever(mockQuery2.firstResult()).thenReturn(null)
            whenever(categoryRepository.find(eq("id = ?1"), eq(categoryId))).thenReturn(mockQuery2)

            // Act
            val result = categoryService.findById(categoryId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === update ===

    @Nested
    inner class Update {
        @Test
        fun `カテゴリ名のみを更新する`() {
            // Arrange
            val categoryId = UUID.randomUUID()
            val entity =
                CategoryEntity().apply {
                    this.id = categoryId
                    this.organizationId = orgId
                    this.name = "旧カテゴリ名"
                    this.color = "#FF0000"
                    this.icon = "old-icon"
                    this.displayOrder = 1
                }
            val mockQuery3 = mock<PanacheQuery<CategoryEntity>>()
            whenever(mockQuery3.firstResult()).thenReturn(entity)
            whenever(categoryRepository.find(eq("id = ?1"), eq(categoryId))).thenReturn(mockQuery3)
            doNothing().whenever(categoryRepository).persist(any<CategoryEntity>())

            // Act
            val result =
                categoryService.update(
                    id = categoryId,
                    name = "新カテゴリ名",
                    parentId = null,
                    color = null,
                    icon = null,
                    displayOrder = null,
                )

            // Assert
            assertNotNull(result)
            assertEquals("新カテゴリ名", result!!.name)
            // null のフィールドは更新されない
            assertEquals("#FF0000", result.color)
            assertEquals("old-icon", result.icon)
            assertEquals(1, result.displayOrder)
            verify(tenantFilterService).enableFilter()
            verify(categoryRepository).persist(any<CategoryEntity>())
        }

        @Test
        fun `色のみを更新する`() {
            // Arrange
            val categoryId = UUID.randomUUID()
            val entity =
                CategoryEntity().apply {
                    this.id = categoryId
                    this.organizationId = orgId
                    this.name = "フード"
                    this.color = "#FF0000"
                    this.displayOrder = 1
                }
            val mockQuery4 = mock<PanacheQuery<CategoryEntity>>()
            whenever(mockQuery4.firstResult()).thenReturn(entity)
            whenever(categoryRepository.find(eq("id = ?1"), eq(categoryId))).thenReturn(mockQuery4)
            doNothing().whenever(categoryRepository).persist(any<CategoryEntity>())

            // Act
            val result =
                categoryService.update(
                    id = categoryId,
                    name = null,
                    parentId = null,
                    color = "#00FF00",
                    icon = null,
                    displayOrder = null,
                )

            // Assert
            assertNotNull(result)
            assertEquals("フード", result!!.name)
            assertEquals("#00FF00", result.color)
            verify(categoryRepository).persist(any<CategoryEntity>())
        }

        @Test
        fun `存在しないカテゴリの更新はnullを返す`() {
            // Arrange
            val categoryId = UUID.randomUUID()
            val mockQuery5 = mock<PanacheQuery<CategoryEntity>>()
            whenever(mockQuery5.firstResult()).thenReturn(null)
            whenever(categoryRepository.find(eq("id = ?1"), eq(categoryId))).thenReturn(mockQuery5)

            // Act
            val result =
                categoryService.update(
                    id = categoryId,
                    name = "新名前",
                    parentId = null,
                    color = null,
                    icon = null,
                    displayOrder = null,
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
        fun `カテゴリを削除する`() {
            // Arrange
            val categoryId = UUID.randomUUID()
            val entity =
                CategoryEntity().apply {
                    this.id = categoryId
                    this.organizationId = orgId
                    this.name = "削除対象カテゴリ"
                    this.displayOrder = 0
                }
            val mockQuery6 = mock<PanacheQuery<CategoryEntity>>()
            whenever(mockQuery6.firstResult()).thenReturn(entity)
            whenever(categoryRepository.find(eq("id = ?1"), eq(categoryId))).thenReturn(mockQuery6)
            doNothing().whenever(categoryRepository).delete(any<CategoryEntity>())

            // Act
            val result = categoryService.delete(categoryId)

            // Assert
            assertTrue(result)
            verify(tenantFilterService).enableFilter()
            verify(categoryRepository).delete(any<CategoryEntity>())
        }

        @Test
        fun `存在しないカテゴリの削除はfalseを返す`() {
            // Arrange
            val categoryId = UUID.randomUUID()
            val mockQuery7 = mock<PanacheQuery<CategoryEntity>>()
            whenever(mockQuery7.firstResult()).thenReturn(null)
            whenever(categoryRepository.find(eq("id = ?1"), eq(categoryId))).thenReturn(mockQuery7)

            // Act
            val result = categoryService.delete(categoryId)

            // Assert
            assertFalse(result)
            verify(tenantFilterService).enableFilter()
        }
    }
}
