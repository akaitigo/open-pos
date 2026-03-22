package com.openpos.product.service

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.TaxRateEntity
import com.openpos.product.repository.TaxRateRepository
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
import java.math.BigDecimal
import java.util.UUID

@QuarkusTest
class TaxRateServiceTest {
    @Inject
    lateinit var taxRateService: TaxRateService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var taxRateRepository: TaxRateRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === create ===

    @Nested
    inner class Create {
        @Test
        fun `標準税率を作成する`() {
            // Arrange
            doNothing().whenever(taxRateRepository).persist(any<TaxRateEntity>())

            // Act
            val result =
                taxRateService.create(
                    name = "標準税率10%",
                    rate = BigDecimal("0.1000"),
                    taxType = "STANDARD",
                    isDefault = true,
                )

            // Assert
            assertEquals("標準税率10%", result.name)
            assertEquals(BigDecimal("0.1000"), result.rate)
            assertEquals("STANDARD", result.taxType)
            assertEquals(true, result.isDefault)
            assertTrue(result.isActive)
            assertEquals(orgId, result.organizationId)
            verify(taxRateRepository).persist(any<TaxRateEntity>())
        }

        @Test
        fun `デフォルト税率作成時は既存デフォルトを解除する`() {
            // Arrange
            val existingDefaultId = UUID.randomUUID()
            val existingDefault =
                TaxRateEntity().apply {
                    this.id = existingDefaultId
                    this.organizationId = orgId
                    this.name = "旧デフォルト税率"
                    this.rate = BigDecimal("0.1000")
                    this.taxType = "STANDARD"
                    this.isDefault = true
                    this.isActive = true
                }
            whenever(taxRateRepository.findDefaultsByOrganizationId(orgId)).thenReturn(listOf(existingDefault))
            doNothing().whenever(taxRateRepository).persist(any<TaxRateEntity>())

            // Act
            val result =
                taxRateService.create(
                    name = "新デフォルト税率12%",
                    rate = BigDecimal("0.1200"),
                    taxType = "STANDARD",
                    isDefault = true,
                )

            // Assert
            assertEquals("新デフォルト税率12%", result.name)
            assertEquals(true, result.isDefault)
            assertEquals(false, existingDefault.isDefault)
            verify(taxRateRepository).findDefaultsByOrganizationId(orgId)
            verify(taxRateRepository).persist(any<TaxRateEntity>())
        }

        @Test
        fun `軽減税率を作成する`() {
            // Arrange
            doNothing().whenever(taxRateRepository).persist(any<TaxRateEntity>())

            // Act
            val result =
                taxRateService.create(
                    name = "軽減税率8%",
                    rate = BigDecimal("0.0800"),
                    taxType = "REDUCED",
                    isDefault = false,
                )

            // Assert
            assertEquals("軽減税率8%", result.name)
            assertEquals(BigDecimal("0.0800"), result.rate)
            assertEquals("REDUCED", result.taxType)
            assertEquals(false, result.isDefault)
            assertTrue(result.isActive)
            verify(taxRateRepository).persist(any<TaxRateEntity>())
        }
    }

    // === listAll ===

    @Nested
    inner class ListAll {
        @Test
        fun `有効な税率一覧を取得する`() {
            // Arrange
            val taxRate1 =
                TaxRateEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "標準税率10%"
                    this.rate = BigDecimal("0.1000")
                    this.taxType = "STANDARD"
                    this.isActive = true
                }
            val taxRate2 =
                TaxRateEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "軽減税率8%"
                    this.rate = BigDecimal("0.0800")
                    this.taxType = "REDUCED"
                    this.isActive = true
                }
            whenever(taxRateRepository.findActive()).thenReturn(listOf(taxRate1, taxRate2))

            // Act
            val result = taxRateService.listAll()

            // Assert
            assertEquals(2, result.size)
            assertEquals("標準税率10%", result[0].name)
            assertEquals("軽減税率8%", result[1].name)
            verify(tenantFilterService).enableFilter()
            verify(taxRateRepository).findActive()
        }

        @Test
        fun `税率が存在しない場合は空リストを返す`() {
            // Arrange
            whenever(taxRateRepository.findActive()).thenReturn(emptyList())

            // Act
            val result = taxRateService.listAll()

            // Assert
            assertEquals(0, result.size)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === findById ===

    @Nested
    inner class FindById {
        @Test
        fun `IDで税率を取得する`() {
            // Arrange
            val taxRateId = UUID.randomUUID()
            val entity =
                TaxRateEntity().apply {
                    this.id = taxRateId
                    this.organizationId = orgId
                    this.name = "標準税率10%"
                    this.rate = BigDecimal("0.1000")
                    this.taxType = "STANDARD"
                    this.isActive = true
                }
            whenever(taxRateRepository.findById(taxRateId)).thenReturn(entity)

            // Act
            val result = taxRateService.findById(taxRateId)

            // Assert
            assertNotNull(result)
            assertEquals(taxRateId, result!!.id)
            assertEquals("標準税率10%", result.name)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val taxRateId = UUID.randomUUID()
            whenever(taxRateRepository.findById(taxRateId)).thenReturn(null)

            // Act
            val result = taxRateService.findById(taxRateId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === update ===

    @Nested
    inner class Update {
        @Test
        fun `税率の値を更新する`() {
            // Arrange
            val taxRateId = UUID.randomUUID()
            val entity =
                TaxRateEntity().apply {
                    this.id = taxRateId
                    this.organizationId = orgId
                    this.name = "標準税率10%"
                    this.rate = BigDecimal("0.1000")
                    this.taxType = "STANDARD"
                    this.isActive = true
                }
            whenever(taxRateRepository.findById(taxRateId)).thenReturn(entity)
            doNothing().whenever(taxRateRepository).persist(any<TaxRateEntity>())

            // Act
            val result =
                taxRateService.update(
                    id = taxRateId,
                    name = null,
                    rate = BigDecimal("0.0800"),
                    taxType = null,
                    isActive = null,
                    isDefault = null,
                )

            // Assert
            assertNotNull(result)
            assertEquals("標準税率10%", result!!.name) // name は更新されない
            assertEquals(BigDecimal("0.0800"), result.rate)
            assertEquals("STANDARD", result.taxType) // taxType は更新されない
            assertTrue(result.isActive)
            verify(tenantFilterService).enableFilter()
            verify(taxRateRepository).persist(any<TaxRateEntity>())
        }

        @Test
        fun `税率を無効化する`() {
            // Arrange
            val taxRateId = UUID.randomUUID()
            val entity =
                TaxRateEntity().apply {
                    this.id = taxRateId
                    this.organizationId = orgId
                    this.name = "旧税率"
                    this.rate = BigDecimal("0.0500")
                    this.taxType = "STANDARD"
                    this.isActive = true
                }
            whenever(taxRateRepository.findById(taxRateId)).thenReturn(entity)
            doNothing().whenever(taxRateRepository).persist(any<TaxRateEntity>())

            // Act
            val result =
                taxRateService.update(
                    id = taxRateId,
                    name = null,
                    rate = null,
                    taxType = null,
                    isActive = false,
                    isDefault = null,
                )

            // Assert
            assertNotNull(result)
            assertEquals(false, result!!.isActive)
            verify(taxRateRepository).persist(any<TaxRateEntity>())
        }

        @Test
        fun `存在しない税率の更新はnullを返す`() {
            // Arrange
            val taxRateId = UUID.randomUUID()
            whenever(taxRateRepository.findById(taxRateId)).thenReturn(null)

            // Act
            val result =
                taxRateService.update(
                    id = taxRateId,
                    name = "新名前",
                    rate = null,
                    taxType = null,
                    isActive = null,
                    isDefault = null,
                )

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `デフォルト税率更新時は既存デフォルトを解除する`() {
            // Arrange
            val taxRateId = UUID.randomUUID()
            val existingDefaultId = UUID.randomUUID()
            val entity =
                TaxRateEntity().apply {
                    this.id = taxRateId
                    this.organizationId = orgId
                    this.name = "軽減税率8%"
                    this.rate = BigDecimal("0.0800")
                    this.taxType = "REDUCED"
                    this.isDefault = false
                    this.isActive = true
                }
            val existingDefault =
                TaxRateEntity().apply {
                    this.id = existingDefaultId
                    this.organizationId = orgId
                    this.name = "標準税率10%"
                    this.rate = BigDecimal("0.1000")
                    this.taxType = "STANDARD"
                    this.isDefault = true
                    this.isActive = true
                }
            whenever(taxRateRepository.findById(taxRateId)).thenReturn(entity)
            whenever(taxRateRepository.findDefaultsByOrganizationIdExcludingId(orgId, taxRateId)).thenReturn(listOf(existingDefault))
            doNothing().whenever(taxRateRepository).persist(any<TaxRateEntity>())

            // Act
            val result =
                taxRateService.update(
                    id = taxRateId,
                    name = null,
                    rate = null,
                    taxType = null,
                    isActive = null,
                    isDefault = true,
                )

            // Assert
            assertNotNull(result)
            assertEquals(true, result!!.isDefault)
            assertEquals(false, existingDefault.isDefault)
            verify(taxRateRepository).persist(any<TaxRateEntity>())
        }
    }
}
