package com.openpos.store.service

import com.openpos.store.cache.StoreCacheService
import com.openpos.store.entity.OrganizationEntity
import com.openpos.store.repository.OrganizationRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class OrganizationServiceTest {
    @Inject
    lateinit var organizationService: OrganizationService

    @InjectMock
    lateinit var organizationRepository: OrganizationRepository

    @InjectMock
    lateinit var cacheService: StoreCacheService

    @BeforeEach
    fun setUp() {
        doNothing().whenever(cacheService).invalidateOrganization(any(), any())
    }

    // === create ===

    @Nested
    inner class Create {
        @Test
        fun `組織を正常に作成する`() {
            // Arrange
            doNothing().whenever(organizationRepository).persist(any<OrganizationEntity>())

            // Act
            val result = organizationService.create("テスト組織", "RETAIL", "T1234567890123")

            // Assert
            assertNotNull(result)
            assertEquals("テスト組織", result.name)
            assertEquals("RETAIL", result.businessType)
            assertEquals("T1234567890123", result.invoiceNumber)
        }

        @Test
        fun `invoiceNumberがnullでも組織を作成できる`() {
            // Arrange
            doNothing().whenever(organizationRepository).persist(any<OrganizationEntity>())

            // Act
            val result = organizationService.create("テスト組織", "FOOD_SERVICE", null)

            // Assert
            assertNotNull(result)
            assertEquals("テスト組織", result.name)
            assertEquals("FOOD_SERVICE", result.businessType)
            assertNull(result.invoiceNumber)
        }
    }

    // === findById ===

    @Nested
    inner class FindById {
        @Test
        fun `存在するIDで組織を取得する`() {
            // Arrange
            val orgId = UUID.randomUUID()
            val entity =
                OrganizationEntity().apply {
                    this.id = orgId
                    this.name = "テスト組織"
                    this.businessType = "RETAIL"
                    this.invoiceNumber = "T1234567890123"
                }
            whenever(organizationRepository.findByIdNotDeleted(orgId)).thenReturn(entity)

            // Act
            val result = organizationService.findById(orgId)

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result?.id)
            assertEquals("テスト組織", result?.name)
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val orgId = UUID.randomUUID()
            whenever(organizationRepository.findByIdNotDeleted(orgId)).thenReturn(null)

            // Act
            val result = organizationService.findById(orgId)

            // Assert
            assertNull(result)
        }
    }

    // === update ===

    @Nested
    inner class Update {
        @Test
        fun `組織名のみを更新する`() {
            // Arrange
            val orgId = UUID.randomUUID()
            val entity =
                OrganizationEntity().apply {
                    this.id = orgId
                    this.name = "旧名称"
                    this.businessType = "RETAIL"
                    this.invoiceNumber = "T1234567890123"
                }
            whenever(organizationRepository.findByIdNotDeleted(orgId)).thenReturn(entity)
            doNothing().whenever(organizationRepository).persist(any<OrganizationEntity>())

            // Act
            val result = organizationService.update(orgId, "新名称", null, null)

            // Assert
            assertNotNull(result)
            assertEquals("新名称", result?.name)
            assertEquals("RETAIL", result?.businessType)
            assertEquals("T1234567890123", result?.invoiceNumber)
        }

        @Test
        fun `businessTypeのみを更新する`() {
            // Arrange
            val orgId = UUID.randomUUID()
            val entity =
                OrganizationEntity().apply {
                    this.id = orgId
                    this.name = "テスト組織"
                    this.businessType = "RETAIL"
                    this.invoiceNumber = null
                }
            whenever(organizationRepository.findByIdNotDeleted(orgId)).thenReturn(entity)
            doNothing().whenever(organizationRepository).persist(any<OrganizationEntity>())

            // Act
            val result = organizationService.update(orgId, null, "FOOD_SERVICE", null)

            // Assert
            assertNotNull(result)
            assertEquals("テスト組織", result?.name)
            assertEquals("FOOD_SERVICE", result?.businessType)
        }

        @Test
        fun `invoiceNumberを更新する`() {
            // Arrange
            val orgId = UUID.randomUUID()
            val entity =
                OrganizationEntity().apply {
                    this.id = orgId
                    this.name = "テスト組織"
                    this.businessType = "RETAIL"
                    this.invoiceNumber = null
                }
            whenever(organizationRepository.findByIdNotDeleted(orgId)).thenReturn(entity)
            doNothing().whenever(organizationRepository).persist(any<OrganizationEntity>())

            // Act
            val result = organizationService.update(orgId, null, null, "T9999999999999")

            // Assert
            assertNotNull(result)
            assertEquals("T9999999999999", result?.invoiceNumber)
        }

        @Test
        fun `全フィールドを一括更新する`() {
            // Arrange
            val orgId = UUID.randomUUID()
            val entity =
                OrganizationEntity().apply {
                    this.id = orgId
                    this.name = "旧名称"
                    this.businessType = "RETAIL"
                    this.invoiceNumber = "T0000000000000"
                }
            whenever(organizationRepository.findByIdNotDeleted(orgId)).thenReturn(entity)
            doNothing().whenever(organizationRepository).persist(any<OrganizationEntity>())

            // Act
            val result = organizationService.update(orgId, "新名称", "FOOD_SERVICE", "T9999999999999")

            // Assert
            assertNotNull(result)
            assertEquals("新名称", result?.name)
            assertEquals("FOOD_SERVICE", result?.businessType)
            assertEquals("T9999999999999", result?.invoiceNumber)
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val orgId = UUID.randomUUID()
            whenever(organizationRepository.findByIdNotDeleted(orgId)).thenReturn(null)

            // Act
            val result = organizationService.update(orgId, "新名称", null, null)

            // Assert
            assertNull(result)
        }
    }
}
