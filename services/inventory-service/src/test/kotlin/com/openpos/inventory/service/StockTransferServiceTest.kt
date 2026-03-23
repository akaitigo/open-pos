package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockTransferEntity
import com.openpos.inventory.repository.StockTransferRepository
import io.quarkus.panache.common.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class StockTransferServiceTest {
    private lateinit var service: StockTransferService
    private val stockTransferRepository = mock<StockTransferRepository>()
    private val tenantFilterService = mock<TenantFilterService>()
    private val organizationIdHolder = mock<OrganizationIdHolder>()

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            StockTransferService().apply {
                this.stockTransferRepository = this@StockTransferServiceTest.stockTransferRepository
                this.tenantFilterService = this@StockTransferServiceTest.tenantFilterService
                this.organizationIdHolder = this@StockTransferServiceTest.organizationIdHolder
            }
        whenever(organizationIdHolder.organizationId).thenReturn(orgId)
    }

    @Nested
    inner class Create {
        @Test
        fun `sets PENDING status`() {
            val fromStore = UUID.randomUUID()
            val toStore = UUID.randomUUID()
            doNothing().whenever(stockTransferRepository).persist(any<StockTransferEntity>())

            val result = service.create(fromStore, toStore, """[{"productId":"abc","quantity":10}]""", "転送テスト")

            assertNotNull(result)
            assertEquals(fromStore, result.fromStoreId)
            assertEquals(toStore, result.toStoreId)
            assertEquals("PENDING", result.status)
            verify(stockTransferRepository).persist(any<StockTransferEntity>())
        }

        @Test
        fun `throws when fromStoreId equals toStoreId`() {
            val sameId = UUID.randomUUID()
            assertThrows(IllegalArgumentException::class.java) {
                service.create(sameId, sameId, "[]", null)
            }
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `returns entity when found`() {
            val id = UUID.randomUUID()
            val entity =
                StockTransferEntity().apply {
                    this.id = id
                    this.organizationId = orgId
                    this.fromStoreId = UUID.randomUUID()
                    this.toStoreId = UUID.randomUUID()
                    this.items = "[]"
                    this.status = "PENDING"
                }
            whenever(stockTransferRepository.findById(id)).thenReturn(entity)

            val result = service.findById(id)

            assertNotNull(result)
            assertEquals(id, result?.id)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns null when not found`() {
            whenever(stockTransferRepository.findById(any<UUID>())).thenReturn(null)

            val result = service.findById(UUID.randomUUID())

            assertNull(result)
        }
    }

    @Nested
    inner class ListTransfers {
        @Test
        fun `returns paginated results`() {
            val items =
                listOf(
                    StockTransferEntity().apply {
                        id = UUID.randomUUID()
                        organizationId = orgId
                        fromStoreId = UUID.randomUUID()
                        toStoreId = UUID.randomUUID()
                        this.items = "[]"
                        status = "PENDING"
                    },
                )
            whenever(stockTransferRepository.listPaginated(any<Page>())).thenReturn(items)
            whenever(stockTransferRepository.count()).thenReturn(1L)

            val (result, total) = service.list(0, 20)

            assertEquals(1, result.size)
            assertEquals(1L, total)
            verify(tenantFilterService).enableFilter()
        }
    }

    @Nested
    inner class UpdateStatus {
        @Test
        fun `changes status`() {
            val transferId = UUID.randomUUID()
            val entity =
                StockTransferEntity().apply {
                    id = transferId
                    organizationId = orgId
                    fromStoreId = UUID.randomUUID()
                    toStoreId = UUID.randomUUID()
                    items = "[]"
                    status = "PENDING"
                }
            whenever(stockTransferRepository.findById(transferId)).thenReturn(entity)
            doNothing().whenever(stockTransferRepository).persist(any<StockTransferEntity>())

            val result = service.updateStatus(transferId, "IN_TRANSIT")

            assertNotNull(result)
            assertEquals("IN_TRANSIT", result?.status)
        }

        @Test
        fun `returns null when not found`() {
            whenever(stockTransferRepository.findById(any<UUID>())).thenReturn(null)

            val result = service.updateStatus(UUID.randomUUID(), "IN_TRANSIT")

            assertNull(result)
        }
    }
}
