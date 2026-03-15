package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.FavoriteProductEntity
import com.openpos.store.repository.FavoriteProductRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class FavoriteProductServiceTest {
    @Inject
    lateinit var favoriteProductService: FavoriteProductService

    @InjectMock
    lateinit var favoriteProductRepository: FavoriteProductRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @InjectMock
    lateinit var organizationIdHolder: OrganizationIdHolder

    private val testOrgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        doNothing().whenever(favoriteProductRepository).persist(any<FavoriteProductEntity>())
        doNothing().whenever(favoriteProductRepository).delete(any<FavoriteProductEntity>())
        doNothing().whenever(tenantFilterService).enableFilter()
        whenever(organizationIdHolder.organizationId).thenReturn(testOrgId)
    }

    @Test
    fun `toggle should add favorite when not exists`() {
        val staffId = UUID.randomUUID()
        val productId = UUID.randomUUID()

        whenever(favoriteProductRepository.findByStaffAndProduct(staffId, productId)).thenReturn(null)

        val result = favoriteProductService.toggle(staffId, productId)

        assertTrue(result) // true = added
    }

    @Test
    fun `toggle should remove favorite when exists`() {
        val staffId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val existing =
            FavoriteProductEntity().apply {
                id = UUID.randomUUID()
                organizationId = testOrgId
                this.staffId = staffId
                this.productId = productId
            }

        whenever(favoriteProductRepository.findByStaffAndProduct(staffId, productId)).thenReturn(existing)

        val result = favoriteProductService.toggle(staffId, productId)

        assertFalse(result) // false = removed
    }

    @Test
    fun `listByStaffId should return favorites`() {
        val staffId = UUID.randomUUID()
        val fav =
            FavoriteProductEntity().apply {
                id = UUID.randomUUID()
                organizationId = testOrgId
                this.staffId = staffId
                productId = UUID.randomUUID()
                sortOrder = 0
            }
        whenever(favoriteProductRepository.findByStaffId(staffId)).thenReturn(listOf(fav))

        val result = favoriteProductService.listByStaffId(staffId)

        assertEquals(1, result.size)
    }
}
