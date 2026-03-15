package com.openpos.store.service

import com.openpos.store.cache.StoreCacheService
import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.StoreEntity
import com.openpos.store.repository.StoreRepository
import io.quarkus.panache.common.Page
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
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
class StoreServiceTest {
    @Inject
    lateinit var storeService: StoreService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var storeRepository: StoreRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @InjectMock
    lateinit var cacheService: StoreCacheService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(cacheService).invalidateStore(any())
    }

    // === create ===

    @Nested
    inner class Create {
        @Test
        fun `店舗を正常に作成する`() {
            // Arrange
            doNothing().whenever(storeRepository).persist(any<StoreEntity>())

            // Act
            val result = storeService.create("渋谷店", "東京都渋谷区1-1-1", "03-1234-5678", "Asia/Tokyo", "{}")

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals("渋谷店", result.name)
            assertEquals("東京都渋谷区1-1-1", result.address)
            assertEquals("03-1234-5678", result.phone)
            assertEquals("Asia/Tokyo", result.timezone)
            assertEquals("{}", result.settings)
            assertTrue(result.isActive)
        }

        @Test
        fun `address・phoneがnullでも店舗を作成できる`() {
            // Arrange
            doNothing().whenever(storeRepository).persist(any<StoreEntity>())

            // Act
            val result = storeService.create("新宿店", null, null, "Asia/Tokyo", "{}")

            // Assert
            assertNotNull(result)
            assertEquals("新宿店", result.name)
            assertNull(result.address)
            assertNull(result.phone)
        }
    }

    // === findById ===

    @Nested
    inner class FindById {
        @Test
        fun `存在するIDで店舗を取得する`() {
            // Arrange
            val storeId = UUID.randomUUID()
            val entity =
                StoreEntity().apply {
                    this.id = storeId
                    this.organizationId = orgId
                    this.name = "渋谷店"
                    this.address = "東京都渋谷区1-1-1"
                    this.timezone = "Asia/Tokyo"
                    this.settings = "{}"
                    this.isActive = true
                }
            whenever(storeRepository.findById(storeId)).thenReturn(entity)

            // Act
            val result = storeService.findById(storeId)

            // Assert
            assertNotNull(result)
            assertEquals(storeId, result?.id)
            assertEquals("渋谷店", result?.name)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val storeId = UUID.randomUUID()
            whenever(storeRepository.findById(storeId)).thenReturn(null)

            // Act
            val result = storeService.findById(storeId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === list ===

    @Nested
    inner class List {
        @Test
        fun `ページネーション付きで店舗一覧を取得する`() {
            // Arrange
            val store1 =
                StoreEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "渋谷店"
                    this.timezone = "Asia/Tokyo"
                    this.settings = "{}"
                }
            val store2 =
                StoreEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "新宿店"
                    this.timezone = "Asia/Tokyo"
                    this.settings = "{}"
                }
            whenever(storeRepository.listPaginated(any())).thenReturn(listOf(store1, store2))
            whenever(storeRepository.count()).thenReturn(2L)

            // Act
            val (stores, totalCount) = storeService.list(0, 10)

            // Assert
            assertEquals(2, stores.size)
            assertEquals(2L, totalCount)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `店舗が存在しない場合は空リストを返す`() {
            // Arrange
            whenever(storeRepository.listPaginated(any())).thenReturn(emptyList())
            whenever(storeRepository.count()).thenReturn(0L)

            // Act
            val (stores, totalCount) = storeService.list(0, 10)

            // Assert
            assertTrue(stores.isEmpty())
            assertEquals(0L, totalCount)
        }
    }

    // === update ===

    @Nested
    inner class Update {
        @Test
        fun `店舗名のみを更新する`() {
            // Arrange
            val storeId = UUID.randomUUID()
            val entity =
                StoreEntity().apply {
                    this.id = storeId
                    this.organizationId = orgId
                    this.name = "旧店舗名"
                    this.address = "東京都渋谷区1-1-1"
                    this.phone = "03-1234-5678"
                    this.timezone = "Asia/Tokyo"
                    this.settings = "{}"
                    this.isActive = true
                }
            whenever(storeRepository.findById(storeId)).thenReturn(entity)
            doNothing().whenever(storeRepository).persist(any<StoreEntity>())

            // Act
            val result = storeService.update(storeId, "新店舗名", null, null, null, null, null)

            // Assert
            assertNotNull(result)
            assertEquals("新店舗名", result?.name)
            assertEquals("東京都渋谷区1-1-1", result?.address)
            assertEquals("03-1234-5678", result?.phone)
            assertEquals("Asia/Tokyo", result?.timezone)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `全フィールドを一括更新する`() {
            // Arrange
            val storeId = UUID.randomUUID()
            val entity =
                StoreEntity().apply {
                    this.id = storeId
                    this.organizationId = orgId
                    this.name = "旧店舗名"
                    this.address = "旧住所"
                    this.phone = "000-0000-0000"
                    this.timezone = "Asia/Tokyo"
                    this.settings = "{}"
                    this.isActive = true
                }
            whenever(storeRepository.findById(storeId)).thenReturn(entity)
            doNothing().whenever(storeRepository).persist(any<StoreEntity>())

            // Act
            val result =
                storeService.update(
                    storeId,
                    "新店舗名",
                    "新住所",
                    "03-9999-9999",
                    "America/New_York",
                    "{\"key\":\"value\"}",
                    false,
                )

            // Assert
            assertNotNull(result)
            assertEquals("新店舗名", result?.name)
            assertEquals("新住所", result?.address)
            assertEquals("03-9999-9999", result?.phone)
            assertEquals("America/New_York", result?.timezone)
            assertEquals("{\"key\":\"value\"}", result?.settings)
            assertEquals(false, result?.isActive)
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val storeId = UUID.randomUUID()
            whenever(storeRepository.findById(storeId)).thenReturn(null)

            // Act
            val result = storeService.update(storeId, "新店舗名", null, null, null, null, null)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }
}
