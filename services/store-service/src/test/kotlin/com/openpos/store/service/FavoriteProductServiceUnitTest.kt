package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.FavoriteProductEntity
import com.openpos.store.repository.FavoriteProductRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

class FavoriteProductServiceUnitTest {
    private lateinit var service: FavoriteProductService
    private lateinit var favoriteProductRepository: FavoriteProductRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        favoriteProductRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = FavoriteProductService()
        service.favoriteProductRepository = favoriteProductRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(favoriteProductRepository).persist(any<FavoriteProductEntity>())
        doNothing().whenever(favoriteProductRepository).delete(any<FavoriteProductEntity>())
    }

    @Nested
    inner class Toggle {
        @Test
        fun `adds favorite when not exists`() {
            val staffId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            whenever(favoriteProductRepository.findByStaffAndProduct(staffId, productId)).thenReturn(null)

            val result = service.toggle(staffId, productId)

            assertTrue(result)
            verify(favoriteProductRepository).persist(any<FavoriteProductEntity>())
        }

        @Test
        fun `removes favorite when exists`() {
            val staffId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val existing =
                FavoriteProductEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.staffId = staffId
                    this.productId = productId
                }
            whenever(favoriteProductRepository.findByStaffAndProduct(staffId, productId)).thenReturn(existing)

            val result = service.toggle(staffId, productId)

            assertFalse(result)
            verify(favoriteProductRepository).delete(existing)
        }
    }

    @Nested
    inner class ListByStaffId {
        @Test
        fun `returns favorites for staff`() {
            val staffId = UUID.randomUUID()
            val fav =
                FavoriteProductEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.staffId = staffId
                    productId = UUID.randomUUID()
                    sortOrder = 0
                }
            whenever(favoriteProductRepository.findByStaffId(staffId)).thenReturn(listOf(fav))

            val result = service.listByStaffId(staffId)

            assertEquals(1, result.size)
            verify(tenantFilterService).enableFilter()
        }
    }
}
