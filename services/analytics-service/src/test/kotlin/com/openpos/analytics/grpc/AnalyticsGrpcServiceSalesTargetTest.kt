package com.openpos.analytics.grpc

import com.openpos.analytics.entity.SalesTargetEntity
import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.HourlySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import com.openpos.analytics.service.AnalyticsQueryService
import com.openpos.analytics.service.AnalyticsService
import com.openpos.analytics.service.SalesTargetService
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import openpos.analytics.v1.DeleteSalesTargetRequest
import openpos.analytics.v1.DeleteSalesTargetResponse
import openpos.analytics.v1.GetSalesTargetRequest
import openpos.analytics.v1.GetSalesTargetResponse
import openpos.analytics.v1.ListSalesTargetsRequest
import openpos.analytics.v1.ListSalesTargetsResponse
import openpos.analytics.v1.UpsertSalesTargetRequest
import openpos.analytics.v1.UpsertSalesTargetResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AnalyticsGrpcServiceSalesTargetTest {
    private lateinit var grpcService: AnalyticsGrpcService
    private val tenantHelper = mock<GrpcTenantHelper>()
    private val salesTargetService = mock<SalesTargetService>()
    private val dailySalesRepository = mock<DailySalesRepository>()
    private val productSalesRepository = mock<ProductSalesRepository>()
    private val analyticsQueryService = mock<AnalyticsQueryService>()
    private val analyticsService = mock<AnalyticsService>()
    private val hourlySalesRepository = mock<HourlySalesRepository>()

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        doNothing().whenever(tenantHelper).setupTenantContext()

        grpcService =
            AnalyticsGrpcService().apply {
                this.tenantHelper = this@AnalyticsGrpcServiceSalesTargetTest.tenantHelper
                this.salesTargetService = this@AnalyticsGrpcServiceSalesTargetTest.salesTargetService
                this.dailySalesRepository = this@AnalyticsGrpcServiceSalesTargetTest.dailySalesRepository
                this.productSalesRepository = this@AnalyticsGrpcServiceSalesTargetTest.productSalesRepository
                this.analyticsQueryService = this@AnalyticsGrpcServiceSalesTargetTest.analyticsQueryService
                this.analyticsService = this@AnalyticsGrpcServiceSalesTargetTest.analyticsService
                this.hourlySalesRepository = this@AnalyticsGrpcServiceSalesTargetTest.hourlySalesRepository
            }
    }

    private fun buildEntity(
        id: UUID = UUID.randomUUID(),
        entityStoreId: UUID? = storeId,
        month: LocalDate = LocalDate.of(2026, 4, 1),
        amount: Long = 10000000,
    ): SalesTargetEntity =
        SalesTargetEntity().apply {
            this.id = id
            this.organizationId = orgId
            this.storeId = entityStoreId
            this.targetMonth = month
            this.targetAmount = amount
            this.createdAt = Instant.parse("2026-01-01T00:00:00Z")
            this.updatedAt = Instant.parse("2026-01-01T00:00:00Z")
        }

    @Nested
    inner class ListSalesTargets {
        @Test
        fun `returns all targets`() {
            // Arrange
            val entities = listOf(buildEntity(), buildEntity(month = LocalDate.of(2026, 5, 1)))
            whenever(salesTargetService.listAll()).thenReturn(entities)

            val observer = CapturingObserver<ListSalesTargetsResponse>()

            // Act
            grpcService.listSalesTargets(
                ListSalesTargetsRequest.getDefaultInstance(),
                observer,
            )

            // Assert
            assertTrue(observer.completed)
            val response = requireNotNull(observer.value)
            assertEquals(2, response.salesTargetsCount)
        }

        @Test
        fun `filters by store_id`() {
            // Arrange
            val otherStoreId = UUID.randomUUID()
            val entities =
                listOf(
                    buildEntity(entityStoreId = storeId),
                    buildEntity(entityStoreId = otherStoreId),
                )
            whenever(salesTargetService.listAll()).thenReturn(entities)

            val observer = CapturingObserver<ListSalesTargetsResponse>()

            // Act
            grpcService.listSalesTargets(
                ListSalesTargetsRequest.newBuilder().setStoreId(storeId.toString()).build(),
                observer,
            )

            // Assert
            val response = requireNotNull(observer.value)
            assertEquals(1, response.salesTargetsCount)
            assertEquals(storeId.toString(), response.getSalesTargets(0).storeId)
        }

        @Test
        fun `filters by month`() {
            // Arrange
            val entities =
                listOf(
                    buildEntity(month = LocalDate.of(2026, 4, 1)),
                    buildEntity(month = LocalDate.of(2026, 5, 1)),
                )
            whenever(salesTargetService.listAll()).thenReturn(entities)

            val observer = CapturingObserver<ListSalesTargetsResponse>()

            // Act
            grpcService.listSalesTargets(
                ListSalesTargetsRequest.newBuilder().setMonth("2026-04-01").build(),
                observer,
            )

            // Assert
            val response = requireNotNull(observer.value)
            assertEquals(1, response.salesTargetsCount)
            assertEquals("2026-04-01", response.getSalesTargets(0).targetMonth)
        }

        @Test
        fun `invalid month throws INVALID_ARGUMENT`() {
            // Arrange
            whenever(salesTargetService.listAll()).thenReturn(emptyList())
            val observer = CapturingObserver<ListSalesTargetsResponse>()

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                grpcService.listSalesTargets(
                    ListSalesTargetsRequest.newBuilder().setMonth("invalid").build(),
                    observer,
                )
            }
        }
    }

    @Nested
    inner class GetSalesTarget {
        @Test
        fun `returns target by id`() {
            // Arrange
            val id = UUID.randomUUID()
            val entity = buildEntity(id = id)
            whenever(salesTargetService.findById(id)).thenReturn(entity)

            val observer = CapturingObserver<GetSalesTargetResponse>()

            // Act
            grpcService.getSalesTarget(
                GetSalesTargetRequest.newBuilder().setId(id.toString()).build(),
                observer,
            )

            // Assert
            assertTrue(observer.completed)
            val response = requireNotNull(observer.value)
            assertEquals(id.toString(), response.salesTarget.id)
        }

        @Test
        fun `throws NOT_FOUND when target does not exist`() {
            // Arrange
            val id = UUID.randomUUID()
            whenever(salesTargetService.findById(id)).thenReturn(null)

            val observer = CapturingObserver<GetSalesTargetResponse>()

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                grpcService.getSalesTarget(
                    GetSalesTargetRequest.newBuilder().setId(id.toString()).build(),
                    observer,
                )
            }
        }

        @Test
        fun `invalid UUID throws INVALID_ARGUMENT`() {
            // Arrange
            val observer = CapturingObserver<GetSalesTargetResponse>()

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                grpcService.getSalesTarget(
                    GetSalesTargetRequest.newBuilder().setId("invalid-uuid").build(),
                    observer,
                )
            }
        }
    }

    @Nested
    inner class UpsertSalesTarget {
        @Test
        fun `creates or updates sales target`() {
            // Arrange
            val entity = buildEntity()
            whenever(salesTargetService.upsert(any(), any(), any())).thenReturn(entity)

            val observer = CapturingObserver<UpsertSalesTargetResponse>()

            // Act
            grpcService.upsertSalesTarget(
                UpsertSalesTargetRequest
                    .newBuilder()
                    .setStoreId(storeId.toString())
                    .setTargetMonth("2026-04-01")
                    .setTargetAmount(10000000)
                    .build(),
                observer,
            )

            // Assert
            assertTrue(observer.completed)
            val response = requireNotNull(observer.value)
            assertEquals(10000000, response.salesTarget.targetAmount)
        }

        @Test
        fun `empty store_id passes null to service`() {
            // Arrange
            val entity = buildEntity(entityStoreId = null)
            whenever(salesTargetService.upsert(any(), any(), any())).thenReturn(entity)

            val observer = CapturingObserver<UpsertSalesTargetResponse>()

            // Act
            grpcService.upsertSalesTarget(
                UpsertSalesTargetRequest
                    .newBuilder()
                    .setTargetMonth("2026-04-01")
                    .setTargetAmount(10000000)
                    .build(),
                observer,
            )

            // Assert
            assertTrue(observer.completed)
        }

        @Test
        fun `invalid target_month throws INVALID_ARGUMENT`() {
            // Arrange
            val observer = CapturingObserver<UpsertSalesTargetResponse>()

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                grpcService.upsertSalesTarget(
                    UpsertSalesTargetRequest
                        .newBuilder()
                        .setTargetMonth("invalid")
                        .setTargetAmount(10000000)
                        .build(),
                    observer,
                )
            }
        }

        @Test
        fun `zero target_amount throws INVALID_ARGUMENT`() {
            // Arrange
            val observer = CapturingObserver<UpsertSalesTargetResponse>()

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                grpcService.upsertSalesTarget(
                    UpsertSalesTargetRequest
                        .newBuilder()
                        .setStoreId(storeId.toString())
                        .setTargetMonth("2026-04-01")
                        .setTargetAmount(0)
                        .build(),
                    observer,
                )
            }
        }

        @Test
        fun `negative target_amount throws INVALID_ARGUMENT`() {
            // Arrange
            val observer = CapturingObserver<UpsertSalesTargetResponse>()

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                grpcService.upsertSalesTarget(
                    UpsertSalesTargetRequest
                        .newBuilder()
                        .setStoreId(storeId.toString())
                        .setTargetMonth("2026-04-01")
                        .setTargetAmount(-100)
                        .build(),
                    observer,
                )
            }
        }
    }

    @Nested
    inner class DeleteSalesTarget {
        @Test
        fun `deletes existing target`() {
            // Arrange
            val id = UUID.randomUUID()
            whenever(salesTargetService.delete(id)).thenReturn(true)

            val observer = CapturingObserver<DeleteSalesTargetResponse>()

            // Act
            grpcService.deleteSalesTarget(
                DeleteSalesTargetRequest.newBuilder().setId(id.toString()).build(),
                observer,
            )

            // Assert
            assertTrue(observer.completed)
        }

        @Test
        fun `throws NOT_FOUND when target does not exist`() {
            // Arrange
            val id = UUID.randomUUID()
            whenever(salesTargetService.delete(id)).thenReturn(false)

            val observer = CapturingObserver<DeleteSalesTargetResponse>()

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                grpcService.deleteSalesTarget(
                    DeleteSalesTargetRequest.newBuilder().setId(id.toString()).build(),
                    observer,
                )
            }
        }

        @Test
        fun `invalid UUID throws INVALID_ARGUMENT`() {
            // Arrange
            val observer = CapturingObserver<DeleteSalesTargetResponse>()

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                grpcService.deleteSalesTarget(
                    DeleteSalesTargetRequest.newBuilder().setId("invalid-uuid").build(),
                    observer,
                )
            }
        }
    }

    private class CapturingObserver<T> : StreamObserver<T> {
        var value: T? = null
        var completed = false

        override fun onNext(value: T) {
            this.value = value
        }

        override fun onError(t: Throwable): Unit = throw t

        override fun onCompleted() {
            completed = true
        }
    }
}
