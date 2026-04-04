package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.SupplierEntity
import com.openpos.inventory.repository.SupplierRepository
import io.quarkus.panache.common.Page
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import org.mockito.kotlin.mock
import org.mockito.kotlin.eq
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class SupplierServiceTest {
    private lateinit var supplierService: SupplierService

    private lateinit var organizationIdHolder: OrganizationIdHolder

    private lateinit var supplierRepository: SupplierRepository

    private lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        supplierRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        supplierService = SupplierService()
        supplierService.supplierRepository = supplierRepository
        supplierService.tenantFilterService = tenantFilterService
        supplierService.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class Create {
        @Test
        fun `仕入先を全フィールド指定で作成する`() {
            // Arrange
            doNothing().whenever(supplierRepository).persist(any<SupplierEntity>())

            // Act
            val result =
                supplierService.create(
                    name = "テスト仕入先",
                    contactPerson = "田中太郎",
                    email = "tanaka@example.com",
                    phone = "03-1234-5678",
                    address = "東京都千代田区",
                )

            // Assert
            assertEquals("テスト仕入先", result.name)
            assertEquals("田中太郎", result.contactPerson)
            assertEquals("tanaka@example.com", result.email)
            assertEquals("03-1234-5678", result.phone)
            assertEquals("東京都千代田区", result.address)
            assertEquals(orgId, result.organizationId)
            verify(supplierRepository).persist(any<SupplierEntity>())
        }

        @Test
        fun `オプションフィールドがnullでも作成できる`() {
            // Arrange
            doNothing().whenever(supplierRepository).persist(any<SupplierEntity>())

            // Act
            val result =
                supplierService.create(
                    name = "シンプル仕入先",
                    contactPerson = null,
                    email = null,
                    phone = null,
                    address = null,
                )

            // Assert
            assertEquals("シンプル仕入先", result.name)
            assertNull(result.contactPerson)
            assertNull(result.email)
            assertNull(result.phone)
            assertNull(result.address)
            verify(supplierRepository).persist(any<SupplierEntity>())
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `IDで仕入先を取得する`() {
            // Arrange
            val supplierId = UUID.randomUUID()
            val entity =
                SupplierEntity().apply {
                    this.id = supplierId
                    this.organizationId = orgId
                    this.name = "テスト仕入先"
                }
            val mockQuery1 = mock<PanacheQuery<SupplierEntity>>()
            whenever(mockQuery1.firstResult()).thenReturn(entity)
            whenever(supplierRepository.find(eq("id = ?1"), eq(supplierId))).thenReturn(mockQuery1)

            // Act
            val result = supplierService.findById(supplierId)

            // Assert
            assertNotNull(result)
            assertEquals(supplierId, result!!.id)
            assertEquals("テスト仕入先", result.name)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val supplierId = UUID.randomUUID()
            val mockQuery2 = mock<PanacheQuery<SupplierEntity>>()
            whenever(mockQuery2.firstResult()).thenReturn(null)
            whenever(supplierRepository.find(eq("id = ?1"), eq(supplierId))).thenReturn(mockQuery2)

            // Act
            val result = supplierService.findById(supplierId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    @Nested
    inner class ListTest {
        @Test
        fun `仕入先一覧をページネーション付きで取得する`() {
            // Arrange
            val supplier1 =
                SupplierEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "仕入先A"
                }
            val supplier2 =
                SupplierEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "仕入先B"
                }
            whenever(supplierRepository.listPaginated(any<Page>())).thenReturn(listOf(supplier1, supplier2))
            whenever(supplierRepository.count()).thenReturn(2L)

            // Act
            val (items, total) = supplierService.list(page = 0, pageSize = 20)

            // Assert
            assertEquals(2, items.size)
            assertEquals(2L, total)
            assertEquals("仕入先A", items[0].name)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `仕入先が存在しない場合は空リストを返す`() {
            // Arrange
            whenever(supplierRepository.listPaginated(any<Page>())).thenReturn(emptyList())
            whenever(supplierRepository.count()).thenReturn(0L)

            // Act
            val (items, total) = supplierService.list(page = 0, pageSize = 20)

            // Assert
            assertEquals(0, items.size)
            assertEquals(0L, total)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `仕入先の名前と連絡先を更新する`() {
            // Arrange
            val supplierId = UUID.randomUUID()
            val entity =
                SupplierEntity().apply {
                    this.id = supplierId
                    this.organizationId = orgId
                    this.name = "旧仕入先"
                    this.contactPerson = "旧担当者"
                    this.email = "old@example.com"
                }
            val mockQuery3 = mock<PanacheQuery<SupplierEntity>>()
            whenever(mockQuery3.firstResult()).thenReturn(entity)
            whenever(supplierRepository.find(eq("id = ?1"), eq(supplierId))).thenReturn(mockQuery3)
            doNothing().whenever(supplierRepository).persist(any<SupplierEntity>())

            // Act
            val result =
                supplierService.update(
                    id = supplierId,
                    name = "新仕入先",
                    contactPerson = "新担当者",
                    email = null,
                    phone = null,
                    address = null,
                )

            // Assert
            assertNotNull(result)
            assertEquals("新仕入先", result!!.name)
            assertEquals("新担当者", result.contactPerson)
            assertEquals("old@example.com", result.email)
            verify(supplierRepository).persist(any<SupplierEntity>())
        }

        @Test
        fun `存在しない仕入先の更新はnullを返す`() {
            // Arrange
            val supplierId = UUID.randomUUID()
            val mockQuery4 = mock<PanacheQuery<SupplierEntity>>()
            whenever(mockQuery4.firstResult()).thenReturn(null)
            whenever(supplierRepository.find(eq("id = ?1"), eq(supplierId))).thenReturn(mockQuery4)

            // Act
            val result =
                supplierService.update(
                    id = supplierId,
                    name = "新名前",
                    contactPerson = null,
                    email = null,
                    phone = null,
                    address = null,
                )

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }
}
