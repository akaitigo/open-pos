package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.SystemSettingEntity
import com.openpos.store.repository.SystemSettingRepository
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class SystemSettingServiceTest {
    private lateinit var service: SystemSettingService
    private lateinit var settingRepository: SystemSettingRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        settingRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = SystemSettingService()
        service.settingRepository = settingRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class GetByKey {
        @Test
        fun `returns setting when found`() {
            val entity =
                SystemSettingEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.key = "tax.rounding"
                    this.value = "FLOOR"
                }
            whenever(settingRepository.findByKey("tax.rounding")).thenReturn(entity)

            val result = service.getByKey("tax.rounding")

            assertNotNull(result)
            assertEquals("FLOOR", result?.value)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns null when not found`() {
            whenever(settingRepository.findByKey("nonexistent")).thenReturn(null)

            val result = service.getByKey("nonexistent")

            assertNull(result)
        }
    }

    @Nested
    inner class ListAll {
        @Test
        fun `returns all settings sorted`() {
            val settings =
                listOf(
                    SystemSettingEntity().apply {
                        this.id = UUID.randomUUID()
                        this.organizationId = orgId
                        this.key = "a.setting"
                        this.value = "val1"
                    },
                    SystemSettingEntity().apply {
                        this.id = UUID.randomUUID()
                        this.organizationId = orgId
                        this.key = "b.setting"
                        this.value = "val2"
                    },
                )
            whenever(settingRepository.listAllSorted()).thenReturn(settings)

            val result = service.listAll()

            assertEquals(2, result.size)
            verify(tenantFilterService).enableFilter()
        }
    }

    @Nested
    inner class Upsert {
        @Test
        fun `creates new setting when key does not exist`() {
            whenever(settingRepository.findByKey("new.key")).thenReturn(null)
            doNothing().whenever(settingRepository).persist(any<SystemSettingEntity>())

            val result = service.upsert("new.key", "new-value", "A new setting")

            assertEquals("new.key", result.key)
            assertEquals("new-value", result.value)
            assertEquals("A new setting", result.description)
            assertEquals(orgId, result.organizationId)
            verify(settingRepository).persist(any<SystemSettingEntity>())
        }

        @Test
        fun `updates existing setting`() {
            val existing =
                SystemSettingEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.key = "existing.key"
                    this.value = "old-value"
                    this.description = "Old description"
                }
            whenever(settingRepository.findByKey("existing.key")).thenReturn(existing)
            doNothing().whenever(settingRepository).persist(any<SystemSettingEntity>())

            val result = service.upsert("existing.key", "new-value", "New description")

            assertEquals("new-value", result.value)
            assertEquals("New description", result.description)
            verify(settingRepository).persist(any<SystemSettingEntity>())
        }

        @Test
        fun `updates value but keeps existing description when description is null`() {
            val existing =
                SystemSettingEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.key = "existing.key"
                    this.value = "old-value"
                    this.description = "Original description"
                }
            whenever(settingRepository.findByKey("existing.key")).thenReturn(existing)
            doNothing().whenever(settingRepository).persist(any<SystemSettingEntity>())

            val result = service.upsert("existing.key", "new-value", null)

            assertEquals("new-value", result.value)
            assertEquals("Original description", result.description)
        }

        @Test
        fun `throws when organizationId is not set`() {
            organizationIdHolder.organizationId = null

            assertThrows(IllegalArgumentException::class.java) {
                service.upsert("key", "value", null)
            }
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `deletes existing setting and returns true`() {
            val entity =
                SystemSettingEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.key = "delete.key"
                    this.value = "val"
                }
            whenever(settingRepository.findByKey("delete.key")).thenReturn(entity)
            doNothing().whenever(settingRepository).delete(any<SystemSettingEntity>())

            val result = service.delete("delete.key")

            assertTrue(result)
            verify(settingRepository).delete(any<SystemSettingEntity>())
        }

        @Test
        fun `returns false when key does not exist`() {
            whenever(settingRepository.findByKey("nonexistent")).thenReturn(null)

            val result = service.delete("nonexistent")

            assertFalse(result)
        }
    }
}
