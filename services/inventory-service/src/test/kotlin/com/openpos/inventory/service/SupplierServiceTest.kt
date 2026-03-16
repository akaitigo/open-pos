package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.SupplierEntity
import com.openpos.inventory.repository.SupplierRepository
import io.quarkus.panache.common.Page
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class SupplierServiceTest {
    @Inject
    lateinit var supplierService: SupplierService

    @InjectMock
    lateinit var supplierRepository: SupplierRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === create ===

    @Test
    fun `create persists supplier with all fields`() {
        // Arrange
        doNothing().whenever(supplierRepository).persist(any<SupplierEntity>())

        // Act
        val result =
            supplierService.create(
                name = "Supplier A",
                contactPerson = "Taro Yamada",
                email = "taro@example.com",
                phone = "03-1234-5678",
                address = "Tokyo, Japan",
            )

        // Assert
        assertEquals(orgId, result.organizationId)
        assertEquals("Supplier A", result.name)
        assertEquals("Taro Yamada", result.contactPerson)
        assertEquals("taro@example.com", result.email)
        assertEquals("03-1234-5678", result.phone)
        assertEquals("Tokyo, Japan", result.address)
        verify(supplierRepository).persist(any<SupplierEntity>())
    }

    @Test
    fun `create allows null optional fields`() {
        // Arrange
        doNothing().whenever(supplierRepository).persist(any<SupplierEntity>())

        // Act
        val result =
            supplierService.create(
                name = "Supplier B",
                contactPerson = null,
                email = null,
                phone = null,
                address = null,
            )

        // Assert
        assertEquals("Supplier B", result.name)
        assertNull(result.contactPerson)
        assertNull(result.email)
        assertNull(result.phone)
        assertNull(result.address)
    }

    @Test
    fun `create throws when organizationId is not set`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            supplierService.create(
                name = "Supplier",
                contactPerson = null,
                email = null,
                phone = null,
                address = null,
            )
        }
    }

    @Test
    fun `create sets organizationId from holder`() {
        // Arrange
        doNothing().whenever(supplierRepository).persist(any<SupplierEntity>())

        // Act
        val result =
            supplierService.create(
                name = "Test Supplier",
                contactPerson = null,
                email = null,
                phone = null,
                address = null,
            )

        // Assert
        assertEquals(orgId, result.organizationId)
    }

    // === findById ===

    @Test
    fun `findById returns supplier when found`() {
        // Arrange
        val supplierId = UUID.randomUUID()
        val supplier = createSupplierEntity(supplierId, name = "Found Supplier")
        whenever(supplierRepository.findById(supplierId)).thenReturn(supplier)

        // Act
        val result = supplierService.findById(supplierId)

        // Assert
        assertNotNull(result)
        assertEquals(supplierId, result?.id)
        assertEquals("Found Supplier", result?.name)
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `findById returns null when not found`() {
        // Arrange
        val supplierId = UUID.randomUUID()
        whenever(supplierRepository.findById(supplierId)).thenReturn(null)

        // Act
        val result = supplierService.findById(supplierId)

        // Assert
        assertNull(result)
    }

    @Test
    fun `findById enables tenant filter`() {
        // Arrange
        val supplierId = UUID.randomUUID()
        whenever(supplierRepository.findById(supplierId)).thenReturn(null)

        // Act
        supplierService.findById(supplierId)

        // Assert
        verify(tenantFilterService).enableFilter()
    }

    // === list ===

    @Test
    fun `list returns suppliers with pagination`() {
        // Arrange
        val suppliers =
            listOf(
                createSupplierEntity(name = "Supplier A"),
                createSupplierEntity(name = "Supplier B"),
            )
        whenever(supplierRepository.listPaginated(any<Page>())).thenReturn(suppliers)
        whenever(supplierRepository.count()).thenReturn(2L)

        // Act
        val (result, totalCount) = supplierService.list(page = 0, pageSize = 20)

        // Assert
        assertEquals(2, result.size)
        assertEquals(2L, totalCount)
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `list returns empty list when no suppliers exist`() {
        // Arrange
        whenever(supplierRepository.listPaginated(any<Page>())).thenReturn(emptyList())
        whenever(supplierRepository.count()).thenReturn(0L)

        // Act
        val (result, totalCount) = supplierService.list(page = 0, pageSize = 20)

        // Assert
        assertEquals(0, result.size)
        assertEquals(0L, totalCount)
    }

    @Test
    fun `list enables tenant filter`() {
        // Arrange
        whenever(supplierRepository.listPaginated(any<Page>())).thenReturn(emptyList())
        whenever(supplierRepository.count()).thenReturn(0L)

        // Act
        supplierService.list(page = 0, pageSize = 10)

        // Assert
        verify(tenantFilterService).enableFilter()
    }

    // === update ===

    @Test
    fun `update modifies all provided fields`() {
        // Arrange
        val supplierId = UUID.randomUUID()
        val existing = createSupplierEntity(supplierId, name = "Old Name")
        whenever(supplierRepository.findById(supplierId)).thenReturn(existing)
        doNothing().whenever(supplierRepository).persist(any<SupplierEntity>())

        // Act
        val result =
            supplierService.update(
                id = supplierId,
                name = "New Name",
                contactPerson = "New Person",
                email = "new@example.com",
                phone = "090-0000-0000",
                address = "Osaka, Japan",
            )

        // Assert
        assertNotNull(result)
        assertEquals("New Name", result?.name)
        assertEquals("New Person", result?.contactPerson)
        assertEquals("new@example.com", result?.email)
        assertEquals("090-0000-0000", result?.phone)
        assertEquals("Osaka, Japan", result?.address)
        verify(supplierRepository).persist(any<SupplierEntity>())
    }

    @Test
    fun `update keeps existing fields when null is provided`() {
        // Arrange
        val supplierId = UUID.randomUUID()
        val existing =
            createSupplierEntity(
                supplierId,
                name = "Original Name",
                contactPerson = "Original Person",
                email = "original@example.com",
            )
        whenever(supplierRepository.findById(supplierId)).thenReturn(existing)
        doNothing().whenever(supplierRepository).persist(any<SupplierEntity>())

        // Act
        val result =
            supplierService.update(
                id = supplierId,
                name = null,
                contactPerson = null,
                email = null,
                phone = null,
                address = null,
            )

        // Assert
        assertNotNull(result)
        assertEquals("Original Name", result?.name)
        assertEquals("Original Person", result?.contactPerson)
        assertEquals("original@example.com", result?.email)
    }

    @Test
    fun `update returns null when supplier not found`() {
        // Arrange
        val supplierId = UUID.randomUUID()
        whenever(supplierRepository.findById(supplierId)).thenReturn(null)

        // Act
        val result =
            supplierService.update(
                id = supplierId,
                name = "New Name",
                contactPerson = null,
                email = null,
                phone = null,
                address = null,
            )

        // Assert
        assertNull(result)
    }

    @Test
    fun `update enables tenant filter`() {
        // Arrange
        val supplierId = UUID.randomUUID()
        whenever(supplierRepository.findById(supplierId)).thenReturn(null)

        // Act
        supplierService.update(
            id = supplierId,
            name = null,
            contactPerson = null,
            email = null,
            phone = null,
            address = null,
        )

        // Assert
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `update partially updates only provided fields`() {
        // Arrange
        val supplierId = UUID.randomUUID()
        val existing =
            createSupplierEntity(
                supplierId,
                name = "Unchanged",
                contactPerson = "Old Person",
                email = "old@example.com",
            )
        whenever(supplierRepository.findById(supplierId)).thenReturn(existing)
        doNothing().whenever(supplierRepository).persist(any<SupplierEntity>())

        // Act
        val result =
            supplierService.update(
                id = supplierId,
                name = "Changed",
                contactPerson = null,
                email = "changed@example.com",
                phone = null,
                address = null,
            )

        // Assert
        assertNotNull(result)
        assertEquals("Changed", result?.name)
        assertEquals("Old Person", result?.contactPerson)
        assertEquals("changed@example.com", result?.email)
    }

    // === Helpers ===

    private fun createSupplierEntity(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Supplier",
        contactPerson: String? = null,
        email: String? = null,
    ): SupplierEntity =
        SupplierEntity().apply {
            this.id = id
            this.organizationId = orgId
            this.name = name
            this.contactPerson = contactPerson
            this.email = email
            this.phone = null
            this.address = null
            this.isActive = true
        }
}
