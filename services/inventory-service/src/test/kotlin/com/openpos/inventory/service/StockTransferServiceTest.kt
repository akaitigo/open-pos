package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockTransferEntity
import com.openpos.inventory.repository.StockTransferRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class StockTransferServiceTest {
    private lateinit var service: StockTransferService
    private val stockTransferRepository = mock<StockTransferRepository>()
    private val tenantFilterService = mock<TenantFilterService>()
    private val organizationIdHolder = mock<OrganizationIdHolder>()
    private val stockService = mock<StockService>()

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            StockTransferService().apply {
                this.stockTransferRepository = this@StockTransferServiceTest.stockTransferRepository
                this.tenantFilterService = this@StockTransferServiceTest.tenantFilterService
                this.organizationIdHolder = this@StockTransferServiceTest.organizationIdHolder
                this.stockService = this@StockTransferServiceTest.stockService
            }
        whenever(organizationIdHolder.organizationId).thenReturn(orgId)
    }

    @Nested
    inner class Create {
        @Test
        fun `sets PENDING status`() {
            val fromStore = UUID.randomUUID()
            val toStore = UUID.randomUUID()
            val productId = UUID.randomUUID()
            doNothing().whenever(stockTransferRepository).persist(any<StockTransferEntity>())

            val result =
                service.create(
                    fromStore,
                    toStore,
                    """[{"productId":"$productId","quantity":10}]""",
                    "転送テスト",
                )

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

        @Test
        fun `throws when item quantity is zero`() {
            val fromStore = UUID.randomUUID()
            val toStore = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val itemsJson = """[{"productId":"$productId","quantity":0}]"""

            val exception =
                assertThrows(IllegalArgumentException::class.java) {
                    service.create(fromStore, toStore, itemsJson, null)
                }
            assertEquals(true, exception.message?.contains("non-positive"))
        }

        @Test
        fun `throws when item quantity is negative`() {
            val fromStore = UUID.randomUUID()
            val toStore = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val itemsJson = """[{"productId":"$productId","quantity":-5}]"""

            val exception =
                assertThrows(IllegalArgumentException::class.java) {
                    service.create(fromStore, toStore, itemsJson, null)
                }
            assertEquals(true, exception.message?.contains("non-positive"))
        }

        @Test
        fun `throws when one of multiple items has non-positive quantity`() {
            val fromStore = UUID.randomUUID()
            val toStore = UUID.randomUUID()
            val pid1 = UUID.randomUUID()
            val pid2 = UUID.randomUUID()
            val itemsJson =
                """[{"productId":"$pid1","quantity":5},{"productId":"$pid2","quantity":-1}]"""

            assertThrows(IllegalArgumentException::class.java) {
                service.create(fromStore, toStore, itemsJson, null)
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
            val mockQuery1 = mock<PanacheQuery<StockTransferEntity>>()
            whenever(mockQuery1.firstResult()).thenReturn(entity)
            whenever(stockTransferRepository.find(eq("id = ?1"), eq(id))).thenReturn(mockQuery1)

            val result = service.findById(id)

            assertNotNull(result)
            assertEquals(id, result?.id)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns null when not found`() {
            val notFoundQuery = mock<PanacheQuery<StockTransferEntity>>()
            whenever(notFoundQuery.firstResult()).thenReturn(null)
            whenever(stockTransferRepository.find(eq("id = ?1"), any<UUID>())).thenReturn(notFoundQuery)

            val result = service.findById(UUID.randomUUID())

            assertNull(result)
        }
    }

    @Nested
    inner class ListTransfers {
        @Test
        fun `returns paginated results without status filter`() {
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
            whenever(stockTransferRepository.listPaginated(eq(null), any<Page>())).thenReturn(items)
            whenever(stockTransferRepository.countByStatus(eq(null))).thenReturn(1L)

            val (result, total) = service.list(null, 0, 20)

            assertEquals(1, result.size)
            assertEquals(1L, total)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns paginated results with status filter`() {
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
            whenever(stockTransferRepository.listPaginated(eq("PENDING"), any<Page>())).thenReturn(items)
            whenever(stockTransferRepository.countByStatus(eq("PENDING"))).thenReturn(1L)

            val (result, total) = service.list("PENDING", 0, 20)

            assertEquals(1, result.size)
            assertEquals(1L, total)
            verify(tenantFilterService).enableFilter()
            verify(stockTransferRepository).listPaginated(eq("PENDING"), any<Page>())
            verify(stockTransferRepository).countByStatus(eq("PENDING"))
        }
    }

    @Nested
    inner class ListByStoreId {
        @Test
        fun `returns filtered results by store`() {
            val storeId = UUID.randomUUID()
            val items =
                listOf(
                    StockTransferEntity().apply {
                        id = UUID.randomUUID()
                        organizationId = orgId
                        fromStoreId = storeId
                        toStoreId = UUID.randomUUID()
                        this.items = "[]"
                        status = "PENDING"
                    },
                )
            whenever(stockTransferRepository.listByStoreId(eq(storeId), eq(null), any<Page>())).thenReturn(items)
            whenever(stockTransferRepository.countByStoreId(eq(storeId), eq(null))).thenReturn(1L)

            val (result, total) = service.listByStoreId(storeId, null, 0, 20)

            assertEquals(1, result.size)
            assertEquals(1L, total)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns filtered results by store and status`() {
            val storeId = UUID.randomUUID()
            whenever(
                stockTransferRepository.listByStoreId(eq(storeId), eq("PENDING"), any<Page>()),
            ).thenReturn(emptyList())
            whenever(stockTransferRepository.countByStoreId(eq(storeId), eq("PENDING"))).thenReturn(0L)

            val (result, total) = service.listByStoreId(storeId, "PENDING", 0, 20)

            assertEquals(0, result.size)
            assertEquals(0L, total)
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
            val mockQuery2 = mock<PanacheQuery<StockTransferEntity>>()
            whenever(mockQuery2.firstResult()).thenReturn(entity)
            whenever(stockTransferRepository.find(eq("id = ?1"), eq(transferId))).thenReturn(mockQuery2)
            doNothing().whenever(stockTransferRepository).persist(any<StockTransferEntity>())

            val result = service.updateStatus(transferId, "IN_TRANSIT")

            assertNotNull(result)
            assertEquals("IN_TRANSIT", result?.status)
        }

        @Test
        fun `returns null when not found`() {
            val notFoundQuery = mock<PanacheQuery<StockTransferEntity>>()
            whenever(notFoundQuery.firstResult()).thenReturn(null)
            whenever(stockTransferRepository.find(eq("id = ?1"), any<UUID>())).thenReturn(notFoundQuery)

            val result = service.updateStatus(UUID.randomUUID(), "IN_TRANSIT")

            assertNull(result)
        }
    }

    @Nested
    inner class Complete {
        private val fromStoreId = UUID.randomUUID()
        private val toStoreId = UUID.randomUUID()
        private val productId1 = UUID.randomUUID()
        private val productId2 = UUID.randomUUID()

        @Test
        fun `completes transfer and adjusts stock for all items`() {
            // Arrange
            val transferId = UUID.randomUUID()
            val itemsJson =
                """[{"productId":"$productId1","quantity":5},{"productId":"$productId2","quantity":3}]"""
            val entity =
                StockTransferEntity().apply {
                    id = transferId
                    organizationId = orgId
                    this.fromStoreId = this@Complete.fromStoreId
                    this.toStoreId = this@Complete.toStoreId
                    items = itemsJson
                    status = "PENDING"
                }
            val mockQuery3 = mock<PanacheQuery<StockTransferEntity>>()
            whenever(mockQuery3.firstResult()).thenReturn(entity)
            whenever(stockTransferRepository.find(eq("id = ?1"), eq(transferId))).thenReturn(mockQuery3)
            doNothing().whenever(stockTransferRepository).persist(any<StockTransferEntity>())
            whenever(stockService.adjustStock(any(), any(), any(), any(), any(), any())).thenReturn(mock())

            // Act
            val result = service.complete(transferId)

            // Assert
            assertEquals("COMPLETED", result.status)
            // 2 items x 2 adjustments (source decrement + destination increment) = 4 calls
            verify(stockService, times(4)).adjustStock(any(), any(), any(), any(), any(), any())
            verify(stockService).adjustStock(
                eq(fromStoreId),
                eq(productId1),
                eq(-5),
                eq("TRANSFER"),
                eq(transferId.toString()),
                any(),
            )
            verify(stockService).adjustStock(
                eq(toStoreId),
                eq(productId1),
                eq(5),
                eq("TRANSFER"),
                eq(transferId.toString()),
                any(),
            )
        }

        @Test
        fun `throws when transfer not found`() {
            val notFoundQuery = mock<PanacheQuery<StockTransferEntity>>()
            whenever(notFoundQuery.firstResult()).thenReturn(null)
            whenever(stockTransferRepository.find(eq("id = ?1"), any<UUID>())).thenReturn(notFoundQuery)

            assertThrows(IllegalArgumentException::class.java) {
                service.complete(UUID.randomUUID())
            }
        }

        @Test
        fun `throws when status is COMPLETED`() {
            val transferId = UUID.randomUUID()
            val entity =
                StockTransferEntity().apply {
                    id = transferId
                    organizationId = orgId
                    fromStoreId = this@Complete.fromStoreId
                    toStoreId = this@Complete.toStoreId
                    items = "[]"
                    status = "COMPLETED"
                }
            val mockQuery4 = mock<PanacheQuery<StockTransferEntity>>()
            whenever(mockQuery4.firstResult()).thenReturn(entity)
            whenever(stockTransferRepository.find(eq("id = ?1"), eq(transferId))).thenReturn(mockQuery4)

            assertThrows(IllegalArgumentException::class.java) {
                service.complete(transferId)
            }
        }

        @Test
        fun `throws when status is CANCELLED`() {
            val transferId = UUID.randomUUID()
            val entity =
                StockTransferEntity().apply {
                    id = transferId
                    organizationId = orgId
                    fromStoreId = this@Complete.fromStoreId
                    toStoreId = this@Complete.toStoreId
                    items = "[]"
                    status = "CANCELLED"
                }
            val mockQuery5 = mock<PanacheQuery<StockTransferEntity>>()
            whenever(mockQuery5.firstResult()).thenReturn(entity)
            whenever(stockTransferRepository.find(eq("id = ?1"), eq(transferId))).thenReturn(mockQuery5)

            assertThrows(IllegalArgumentException::class.java) {
                service.complete(transferId)
            }
        }

        @Test
        fun `allows completing IN_TRANSIT transfers`() {
            val transferId = UUID.randomUUID()
            val itemsJson = """[{"productId":"$productId1","quantity":2}]"""
            val entity =
                StockTransferEntity().apply {
                    id = transferId
                    organizationId = orgId
                    this.fromStoreId = this@Complete.fromStoreId
                    this.toStoreId = this@Complete.toStoreId
                    items = itemsJson
                    status = "IN_TRANSIT"
                }
            val mockQuery6 = mock<PanacheQuery<StockTransferEntity>>()
            whenever(mockQuery6.firstResult()).thenReturn(entity)
            whenever(stockTransferRepository.find(eq("id = ?1"), eq(transferId))).thenReturn(mockQuery6)
            doNothing().whenever(stockTransferRepository).persist(any<StockTransferEntity>())
            whenever(stockService.adjustStock(any(), any(), any(), any(), any(), any())).thenReturn(mock())

            val result = service.complete(transferId)

            assertEquals("COMPLETED", result.status)
        }
    }

    @Nested
    inner class ParseItems {
        @Test
        fun `parses valid JSON items`() {
            val productId = UUID.randomUUID()
            val json = """[{"productId":"$productId","quantity":10}]"""

            val result = service.parseItems(json)

            assertEquals(1, result.size)
            assertEquals(productId, result[0].productId)
            assertEquals(10, result[0].quantity)
        }

        @Test
        fun `parses multiple items`() {
            val pid1 = UUID.randomUUID()
            val pid2 = UUID.randomUUID()
            val json = """[{"productId":"$pid1","quantity":5},{"productId":"$pid2","quantity":3}]"""

            val result = service.parseItems(json)

            assertEquals(2, result.size)
            assertEquals(pid1, result[0].productId)
            assertEquals(5, result[0].quantity)
            assertEquals(pid2, result[1].productId)
            assertEquals(3, result[1].quantity)
        }

        @Test
        fun `returns empty list for empty array`() {
            val result = service.parseItems("[]")

            assertEquals(0, result.size)
        }

        @Test
        fun `parses negative quantity`() {
            val productId = UUID.randomUUID()
            val json = """[{"productId":"$productId","quantity":-3}]"""

            val result = service.parseItems(json)

            assertEquals(1, result.size)
            assertEquals(productId, result[0].productId)
            assertEquals(-3, result[0].quantity)
        }

        @Test
        fun `parses zero quantity`() {
            val productId = UUID.randomUUID()
            val json = """[{"productId":"$productId","quantity":0}]"""

            val result = service.parseItems(json)

            assertEquals(1, result.size)
            assertEquals(0, result[0].quantity)
        }
    }
}
