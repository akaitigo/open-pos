package com.openpos.store.grpc

import com.google.protobuf.BoolValue
import com.openpos.store.entity.StaffEntity
import com.openpos.store.entity.StoreEntity
import com.openpos.store.service.StaffService
import com.openpos.store.service.StoreService
import io.grpc.stub.StreamObserver
import openpos.store.v1.UpdateStaffRequest
import openpos.store.v1.UpdateStoreRequest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class StoreGrpcServiceTest {
    private lateinit var grpcService: StoreGrpcService
    private val storeService = mock<StoreService>()
    private val staffService = mock<StaffService>()
    private val tenantHelper = mock<GrpcTenantHelper>()

    @BeforeEach
    fun setUp() {
        grpcService =
            StoreGrpcService().apply {
                this.storeService = this@StoreGrpcServiceTest.storeService
                this.staffService = this@StoreGrpcServiceTest.staffService
                this.tenantHelper = this@StoreGrpcServiceTest.tenantHelper
            }
    }

    @Test
    fun `updateStore forwards explicit false isActive`() {
        val storeId = UUID.randomUUID()
        whenever(
            storeService.update(
                eq(storeId),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(false),
            ),
        ).thenReturn(storeEntity(storeId))

        val observer = CapturingObserver<openpos.store.v1.UpdateStoreResponse>()

        grpcService.updateStore(
            UpdateStoreRequest.newBuilder().setId(storeId.toString()).setIsActiveValue(BoolValue.of(false)).build(),
            observer,
        )

        verify(tenantHelper).setupTenantContext()
        verify(storeService).update(
            eq(storeId),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq(false),
        )
        assertNotNull(observer.value)
        assertTrue(observer.completed)
    }

    @Test
    fun `updateStore keeps isActive null when omitted`() {
        val storeId = UUID.randomUUID()
        whenever(
            storeService.update(
                eq(storeId),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                isNull(),
            ),
        ).thenReturn(storeEntity(storeId))

        val observer = CapturingObserver<openpos.store.v1.UpdateStoreResponse>()

        grpcService.updateStore(
            UpdateStoreRequest.newBuilder().setId(storeId.toString()).build(),
            observer,
        )

        verify(storeService).update(
            eq(storeId),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            isNull(),
        )
        assertNotNull(observer.value)
        assertTrue(observer.completed)
    }

    @Test
    fun `updateStaff forwards explicit false isActive`() {
        val staffId = UUID.randomUUID()
        whenever(
            staffService.update(
                eq(staffId),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(false),
            ),
        ).thenReturn(staffEntity(staffId))

        val observer = CapturingObserver<openpos.store.v1.UpdateStaffResponse>()

        grpcService.updateStaff(
            UpdateStaffRequest.newBuilder().setId(staffId.toString()).setIsActiveValue(BoolValue.of(false)).build(),
            observer,
        )

        verify(tenantHelper).setupTenantContext()
        verify(staffService).update(
            eq(staffId),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq(false),
        )
        assertNotNull(observer.value)
        assertTrue(observer.completed)
    }

    @Test
    fun `updateStaff keeps isActive null when omitted`() {
        val staffId = UUID.randomUUID()
        whenever(
            staffService.update(
                eq(staffId),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                isNull(),
            ),
        ).thenReturn(staffEntity(staffId))

        val observer = CapturingObserver<openpos.store.v1.UpdateStaffResponse>()

        grpcService.updateStaff(
            UpdateStaffRequest.newBuilder().setId(staffId.toString()).build(),
            observer,
        )

        verify(staffService).update(
            eq(staffId),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            isNull(),
        )
        assertNotNull(observer.value)
        assertTrue(observer.completed)
    }

    private fun storeEntity(storeId: UUID): StoreEntity =
        StoreEntity().apply {
            id = storeId
            organizationId = UUID.randomUUID()
            name = "Store"
            timezone = "Asia/Tokyo"
            settings = "{}"
            isActive = true
        }

    private fun staffEntity(staffId: UUID): StaffEntity =
        StaffEntity().apply {
            id = staffId
            organizationId = UUID.randomUUID()
            storeId = UUID.randomUUID()
            name = "Staff"
            role = "CASHIER"
            isActive = true
        }

    private class CapturingObserver<T> : StreamObserver<T> {
        var value: T? = null
        var completed = false

        override fun onNext(value: T) {
            this.value = value
        }

        override fun onError(t: Throwable) {
            throw AssertionError("Unexpected error", t)
        }

        override fun onCompleted() {
            completed = true
        }
    }
}
