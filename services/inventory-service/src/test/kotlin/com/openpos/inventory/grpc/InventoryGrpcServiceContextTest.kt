package com.openpos.inventory.grpc

import com.openpos.inventory.config.OrganizationIdInterceptor
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockEntity
import com.openpos.inventory.entity.StockMovementEntity
import com.openpos.inventory.event.StockLowEventPublisher
import com.openpos.inventory.repository.StockMovementRepository
import com.openpos.inventory.repository.StockRepository
import io.grpc.Context
import io.grpc.stub.StreamObserver
import io.quarkus.arc.Arc
import io.quarkus.grpc.GrpcService
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import openpos.inventory.v1.AdjustStockRequest
import openpos.inventory.v1.AdjustStockResponse
import openpos.inventory.v1.MovementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@QuarkusTest
class InventoryGrpcServiceContextTest {
    @Inject
    @GrpcService
    lateinit var grpcService: InventoryGrpcService

    @InjectMock
    lateinit var stockRepository: StockRepository

    @InjectMock
    lateinit var movementRepository: StockMovementRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @InjectMock
    lateinit var stockLowEventPublisher: StockLowEventPublisher

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        doNothing().whenever(stockRepository).persist(any<StockEntity>())
        doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(stockLowEventPublisher).publish(any(), any(), any(), any(), any())
    }

    @Test
    fun `adjustStock works even when request context is inactive`() {
        val existingStock =
            StockEntity().apply {
                id = UUID.randomUUID()
                organizationId = orgId
                storeId = this@InventoryGrpcServiceContextTest.storeId
                productId = this@InventoryGrpcServiceContextTest.productId
                quantity = 10
                updatedAt = Instant.now()
            }
        whenever(stockRepository.findByStoreAndProductForUpdate(storeId, productId)).thenReturn(existingStock)

        val request =
            AdjustStockRequest
                .newBuilder()
                .setStoreId(storeId.toString())
                .setProductId(productId.toString())
                .setQuantityChange(5)
                .setMovementType(MovementType.MOVEMENT_TYPE_RECEIPT)
                .setReferenceId("po-001")
                .setNote("restock")
                .build()
        val observer = CapturingObserver<AdjustStockResponse>()

        val requestContext = Arc.container().requestContext()
        if (requestContext.isActive) {
            requestContext.terminate()
        }

        val grpcContext =
            Context
                .current()
                .withValue(OrganizationIdInterceptor.ORGANIZATION_ID_CTX_KEY, orgId)
        grpcContext.run(
            Runnable {
                grpcService.adjustStock(request, observer)
            },
        )

        assertTrue(observer.completed)
        assertEquals(15, observer.value!!.stock.quantity)
        verify(stockRepository).persist(any<StockEntity>())
        verify(movementRepository).persist(any<StockMovementEntity>())
    }

    private class CapturingObserver<T> : StreamObserver<T> {
        var value: T? = null
        var completed = false

        override fun onNext(value: T) {
            this.value = value
        }

        override fun onError(t: Throwable): Unit = throw AssertionError("Unexpected error", t)

        override fun onCompleted() {
            completed = true
        }
    }
}
