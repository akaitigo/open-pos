package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.SystemSettingEntity
import com.openpos.store.repository.SystemSettingRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
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
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class SystemSettingServiceTest {
    @Inject
    lateinit var systemSettingService: SystemSettingService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var settingRepository: SystemSettingRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === getByKey ===

    @Nested
    inner class GetByKey {
        @Test
        fun `キーで設定を取得する`() {
            // Arrange
            val entity = createSettingEntity("store.name", "テスト店舗", "店舗名設定")
            whenever(settingRepository.findByKey("store.name")).thenReturn(entity)

            // Act
            val result = systemSettingService.getByKey("store.name")

            // Assert
            assertNotNull(result)
            assertEquals("store.name", result?.key)
            assertEquals("テスト店舗", result?.value)
            assertEquals("店舗名設定", result?.description)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `存在しないキーの場合はnullを返す`() {
            // Arrange
            whenever(settingRepository.findByKey("nonexistent.key")).thenReturn(null)

            // Act
            val result = systemSettingService.getByKey("nonexistent.key")

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === listAll ===

    @Nested
    inner class ListAll {
        @Test
        fun `全設定をソート済みで取得する`() {
            // Arrange
            val setting1 = createSettingEntity("app.currency", "JPY", "通貨設定")
            val setting2 = createSettingEntity("store.name", "テスト店舗", "店舗名")
            whenever(settingRepository.listAllSorted()).thenReturn(listOf(setting1, setting2))

            // Act
            val result = systemSettingService.listAll()

            // Assert
            assertEquals(2, result.size)
            assertEquals("app.currency", result[0].key)
            assertEquals("store.name", result[1].key)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `設定が存在しない場合は空リストを返す`() {
            // Arrange
            whenever(settingRepository.listAllSorted()).thenReturn(emptyList())

            // Act
            val result = systemSettingService.listAll()

            // Assert
            assertEquals(0, result.size)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === upsert ===

    @Nested
    inner class Upsert {
        @Test
        fun `新規設定を作成する`() {
            // Arrange
            whenever(settingRepository.findByKey("new.setting")).thenReturn(null)
            doNothing().whenever(settingRepository).persist(any<SystemSettingEntity>())

            // Act
            val result = systemSettingService.upsert("new.setting", "value1", "新しい設定")

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals("new.setting", result.key)
            assertEquals("value1", result.value)
            assertEquals("新しい設定", result.description)
            verify(settingRepository).persist(any<SystemSettingEntity>())
        }

        @Test
        fun `既存設定の値を更新する`() {
            // Arrange
            val existing = createSettingEntity("existing.key", "old_value", "既存設定")
            whenever(settingRepository.findByKey("existing.key")).thenReturn(existing)
            doNothing().whenever(settingRepository).persist(any<SystemSettingEntity>())

            // Act
            val result = systemSettingService.upsert("existing.key", "new_value", "更新後の説明")

            // Assert
            assertEquals("new_value", result.value)
            assertEquals("更新後の説明", result.description)
            verify(settingRepository).persist(any<SystemSettingEntity>())
        }

        @Test
        fun `既存設定の更新時にdescriptionがnullの場合は既存descriptionを維持する`() {
            // Arrange
            val existing = createSettingEntity("existing.key", "old_value", "既存の説明")
            whenever(settingRepository.findByKey("existing.key")).thenReturn(existing)
            doNothing().whenever(settingRepository).persist(any<SystemSettingEntity>())

            // Act
            val result = systemSettingService.upsert("existing.key", "new_value", null)

            // Assert
            assertEquals("new_value", result.value)
            assertEquals("既存の説明", result.description)
        }

        @Test
        fun `新規作成時にdescriptionがnullでも作成できる`() {
            // Arrange
            whenever(settingRepository.findByKey("no.desc")).thenReturn(null)
            doNothing().whenever(settingRepository).persist(any<SystemSettingEntity>())

            // Act
            val result = systemSettingService.upsert("no.desc", "value", null)

            // Assert
            assertNotNull(result)
            assertEquals("no.desc", result.key)
            assertEquals("value", result.value)
            assertNull(result.description)
        }

        @Test
        fun `organizationIdが未設定の場合はエラー`() {
            // Arrange
            organizationIdHolder.organizationId = null

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                systemSettingService.upsert("test.key", "value", null)
            }
        }
    }

    // === delete ===

    @Nested
    inner class Delete {
        @Test
        fun `設定を正常に削除する`() {
            // Arrange
            val entity = createSettingEntity("delete.me", "value", "削除対象")
            whenever(settingRepository.findByKey("delete.me")).thenReturn(entity)
            doNothing().whenever(settingRepository).delete(any<SystemSettingEntity>())

            // Act
            val result = systemSettingService.delete("delete.me")

            // Assert
            assertTrue(result)
            verify(tenantFilterService).enableFilter()
            verify(settingRepository).delete(entity)
        }

        @Test
        fun `存在しないキーの場合はfalseを返す`() {
            // Arrange
            whenever(settingRepository.findByKey("nonexistent")).thenReturn(null)

            // Act
            val result = systemSettingService.delete("nonexistent")

            // Assert
            assertFalse(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === helper ===

    private fun createSettingEntity(
        key: String,
        value: String,
        description: String?,
    ): SystemSettingEntity =
        SystemSettingEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.key = key
            this.value = value
            this.description = description
        }
}
