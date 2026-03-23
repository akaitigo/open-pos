package com.openpos.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.openpos.product.cache.ProductCacheService
import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.CategoryEntity
import com.openpos.product.repository.CategoryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

/**
 * CategoryService の純粋ユニットテスト。
 * キャッシュヒット/ミス/エラーパスをカバーする。
 */
class CategoryServiceUnitTest {
    private lateinit var service: CategoryService
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder
    private lateinit var cacheService: ProductCacheService
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        categoryRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()
        cacheService = mock()

        service = CategoryService()
        service.categoryRepository = categoryRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder
        service.cacheService = cacheService
        service.objectMapper = objectMapper

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(cacheService).invalidateCategory(any())
        doNothing().whenever(cacheService).invalidateAllCategoryLists()
    }

    @Nested
    inner class ListByParentIdCacheTests {
        @Test
        fun `returns from cache when cache hit`() {
            val categoryId = UUID.randomUUID()
            val cacheKey = "openpos:product-service:$orgId:category:list:null"
            whenever(cacheService.categoryListKey(eq(null))).thenReturn(cacheKey)

            val dtos =
                listOf(
                    CategoryCacheDto(
                        id = categoryId,
                        organizationId = orgId,
                        name = "Cached Category",
                        parentId = null,
                        color = "#FF0000",
                        icon = "icon",
                        displayOrder = 1,
                    ),
                )
            whenever(cacheService.get(cacheKey)).thenReturn(objectMapper.writeValueAsString(dtos))

            val result = service.listByParentId(null)

            assertEquals(1, result.size)
            assertEquals("Cached Category", result[0].name)
            assertEquals(categoryId, result[0].id)
        }

        @Test
        fun `falls back to DB when cache deserialization fails`() {
            val cacheKey = "openpos:product-service:$orgId:category:list:null"
            whenever(cacheService.categoryListKey(eq(null))).thenReturn(cacheKey)
            whenever(cacheService.get(cacheKey)).thenReturn("invalid json")

            val dbCategories =
                listOf(
                    CategoryEntity().apply {
                        this.id = UUID.randomUUID()
                        this.organizationId = orgId
                        this.name = "DB Category"
                        this.displayOrder = 1
                    },
                )
            whenever(categoryRepository.findByParentId(null)).thenReturn(dbCategories)

            val result = service.listByParentId(null)

            assertEquals(1, result.size)
            assertEquals("DB Category", result[0].name)
        }

        @Test
        fun `fetches from DB and caches when cache miss`() {
            val cacheKey = "openpos:product-service:$orgId:category:list:null"
            whenever(cacheService.categoryListKey(eq(null))).thenReturn(cacheKey)
            whenever(cacheService.get(cacheKey)).thenReturn(null)
            doNothing().whenever(cacheService).set(any(), any(), any())

            val dbCategories =
                listOf(
                    CategoryEntity().apply {
                        this.id = UUID.randomUUID()
                        this.organizationId = orgId
                        this.name = "From DB"
                        this.displayOrder = 1
                    },
                )
            whenever(categoryRepository.findByParentId(null)).thenReturn(dbCategories)

            val result = service.listByParentId(null)

            assertEquals(1, result.size)
            assertEquals("From DB", result[0].name)
            verify(cacheService).set(eq(cacheKey), any(), eq(3600L))
        }

        @Test
        fun `handles cache write failure gracefully for listByParentId`() {
            val cacheKey = "openpos:product-service:$orgId:category:list:null"
            whenever(cacheService.categoryListKey(eq(null))).thenReturn(cacheKey)
            whenever(cacheService.get(cacheKey)).thenReturn(null)
            doThrow(RuntimeException("Redis down")).whenever(cacheService).set(any(), any(), any())

            val dbCategories =
                listOf(
                    CategoryEntity().apply {
                        this.id = UUID.randomUUID()
                        this.organizationId = orgId
                        this.name = "Fallback"
                        this.displayOrder = 1
                    },
                )
            whenever(categoryRepository.findByParentId(null)).thenReturn(dbCategories)

            val result = service.listByParentId(null)

            assertEquals(1, result.size)
            assertEquals("Fallback", result[0].name)
        }
    }

    @Nested
    inner class FindByIdCacheTests {
        @Test
        fun `returns from cache when cache hit`() {
            val categoryId = UUID.randomUUID()
            val cacheKey = "openpos:product-service:$orgId:category:$categoryId"
            whenever(cacheService.categoryKey(eq(categoryId.toString()))).thenReturn(cacheKey)

            val dto =
                CategoryCacheDto(
                    id = categoryId,
                    organizationId = orgId,
                    name = "Cached",
                    parentId = null,
                    color = null,
                    icon = null,
                    displayOrder = 1,
                )
            whenever(cacheService.get(cacheKey)).thenReturn(objectMapper.writeValueAsString(dto))

            val result = service.findById(categoryId)

            assertNotNull(result)
            assertEquals("Cached", result?.name)
        }

        @Test
        fun `falls back to DB when cache deserialization fails`() {
            val categoryId = UUID.randomUUID()
            val cacheKey = "openpos:product-service:$orgId:category:$categoryId"
            whenever(cacheService.categoryKey(eq(categoryId.toString()))).thenReturn(cacheKey)
            whenever(cacheService.get(cacheKey)).thenReturn("bad json")

            val entity =
                CategoryEntity().apply {
                    this.id = categoryId
                    this.organizationId = orgId
                    this.name = "DB Fallback"
                    this.displayOrder = 1
                }
            whenever(categoryRepository.findById(categoryId)).thenReturn(entity)

            val result = service.findById(categoryId)

            assertNotNull(result)
            assertEquals("DB Fallback", result?.name)
        }

        @Test
        fun `fetches from DB and caches on cache miss`() {
            val categoryId = UUID.randomUUID()
            val cacheKey = "openpos:product-service:$orgId:category:$categoryId"
            whenever(cacheService.categoryKey(eq(categoryId.toString()))).thenReturn(cacheKey)
            whenever(cacheService.get(cacheKey)).thenReturn(null)
            doNothing().whenever(cacheService).set(any(), any(), any())

            val entity =
                CategoryEntity().apply {
                    this.id = categoryId
                    this.organizationId = orgId
                    this.name = "DB Entity"
                    this.displayOrder = 1
                }
            whenever(categoryRepository.findById(categoryId)).thenReturn(entity)

            val result = service.findById(categoryId)

            assertNotNull(result)
            assertEquals("DB Entity", result?.name)
            verify(cacheService).set(eq(cacheKey), any(), eq(3600L))
        }

        @Test
        fun `handles cache write failure gracefully for findById`() {
            val categoryId = UUID.randomUUID()
            val cacheKey = "openpos:product-service:$orgId:category:$categoryId"
            whenever(cacheService.categoryKey(eq(categoryId.toString()))).thenReturn(cacheKey)
            whenever(cacheService.get(cacheKey)).thenReturn(null)
            doThrow(RuntimeException("Redis down")).whenever(cacheService).set(any(), any(), any())

            val entity =
                CategoryEntity().apply {
                    this.id = categoryId
                    this.organizationId = orgId
                    this.name = "Fallback Entity"
                    this.displayOrder = 1
                }
            whenever(categoryRepository.findById(categoryId)).thenReturn(entity)

            val result = service.findById(categoryId)

            assertNotNull(result)
            assertEquals("Fallback Entity", result?.name)
        }

        @Test
        fun `returns null when not found in DB on cache miss`() {
            val categoryId = UUID.randomUUID()
            val cacheKey = "openpos:product-service:$orgId:category:$categoryId"
            whenever(cacheService.categoryKey(eq(categoryId.toString()))).thenReturn(cacheKey)
            whenever(cacheService.get(cacheKey)).thenReturn(null)
            whenever(categoryRepository.findById(categoryId)).thenReturn(null)

            val result = service.findById(categoryId)

            assertNull(result)
        }
    }

    @Nested
    inner class CreateTest {
        @Test
        fun `creates category and invalidates cache`() {
            doNothing().whenever(categoryRepository).persist(any<CategoryEntity>())

            val result = service.create("New Category", null, "#FF0000", "icon", 1)

            assertEquals("New Category", result.name)
            assertEquals(orgId, result.organizationId)
            verify(cacheService).invalidateAllCategoryLists()
        }
    }

    @Nested
    inner class UpdateTest {
        @Test
        fun `updates category with parentId`() {
            val categoryId = UUID.randomUUID()
            val newParentId = UUID.randomUUID()
            val entity =
                CategoryEntity().apply {
                    this.id = categoryId
                    this.organizationId = orgId
                    this.name = "Old"
                    this.displayOrder = 1
                }
            whenever(categoryRepository.findById(categoryId)).thenReturn(entity)
            doNothing().whenever(categoryRepository).persist(any<CategoryEntity>())

            val result = service.update(categoryId, "New Name", newParentId, "#00FF00", "new-icon", 5)

            assertNotNull(result)
            assertEquals("New Name", result?.name)
            assertEquals(newParentId, result?.parentId)
            assertEquals("#00FF00", result?.color)
            assertEquals("new-icon", result?.icon)
            assertEquals(5, result?.displayOrder)
            verify(cacheService).invalidateCategory(categoryId.toString())
        }
    }

    @Nested
    inner class UpdateNotFound {
        @Test
        fun `returns null when category not found`() {
            val categoryId = UUID.randomUUID()
            whenever(categoryRepository.findById(categoryId)).thenReturn(null)

            val result = service.update(categoryId, "New", null, null, null, null)

            assertNull(result)
        }
    }

    @Nested
    inner class DeleteTest {
        @Test
        fun `deletes category and invalidates cache`() {
            val categoryId = UUID.randomUUID()
            val entity =
                CategoryEntity().apply {
                    this.id = categoryId
                    this.organizationId = orgId
                    this.name = "To Delete"
                    this.displayOrder = 0
                }
            whenever(categoryRepository.findById(categoryId)).thenReturn(entity)
            doNothing().whenever(categoryRepository).delete(any<CategoryEntity>())

            val result = service.delete(categoryId)

            assertEquals(true, result)
            verify(cacheService).invalidateCategory(categoryId.toString())
        }

        @Test
        fun `returns false when category not found`() {
            val categoryId = UUID.randomUUID()
            whenever(categoryRepository.findById(categoryId)).thenReturn(null)

            val result = service.delete(categoryId)

            assertEquals(false, result)
        }
    }
}
