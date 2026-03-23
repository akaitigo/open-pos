package com.openpos.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.openpos.product.cache.ProductCacheService
import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.DiscountEntity
import com.openpos.product.repository.DiscountRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class DiscountServiceUnitTest {
    private lateinit var service: DiscountService
    private lateinit var discountRepository: DiscountRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder
    private lateinit var cacheService: ProductCacheService
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        discountRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()
        cacheService = mock()

        service = DiscountService()
        service.discountRepository = discountRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder
        service.cacheService = cacheService
        service.objectMapper = objectMapper

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(discountRepository).persist(any<DiscountEntity>())
    }

    @Nested
    inner class FindByIdCache {
        @Test
        fun `returns from cache when cache hit`() {
            val discountId = UUID.randomUUID()
            // Jackson Kotlin module serializes isActive as "active", so build JSON manually
            val cachedJson =
                """
                {"id":"$discountId","organizationId":"$orgId","name":"Cached Discount",
                 "discountType":"PERCENTAGE","value":10,"appliesTo":"ALL",
                 "validFrom":null,"validUntil":null,"isActive":true}
                """.trimIndent()
            whenever(cacheService.get(any())).thenReturn(cachedJson)

            val result = service.findById(discountId)

            assertNotNull(result)
            assertEquals("Cached Discount", result?.name)
            assertEquals(discountId, result?.id)
        }

        @Test
        fun `falls back to DB when cache deserialization fails`() {
            val discountId = UUID.randomUUID()
            whenever(cacheService.get(any())).thenReturn("invalid json")

            val entity =
                DiscountEntity().apply {
                    this.id = discountId
                    this.organizationId = orgId
                    this.name = "DB Discount"
                    this.discountType = "PERCENTAGE"
                    this.value = 10
                    this.isActive = true
                }
            whenever(discountRepository.findById(discountId)).thenReturn(entity)

            val result = service.findById(discountId)

            assertNotNull(result)
            assertEquals("DB Discount", result?.name)
        }

        @Test
        fun `caches entity after DB fetch on cache miss`() {
            val discountId = UUID.randomUUID()
            whenever(cacheService.get(any())).thenReturn(null)
            doNothing().whenever(cacheService).set(any(), any(), any())

            val entity =
                DiscountEntity().apply {
                    this.id = discountId
                    this.organizationId = orgId
                    this.name = "DB Fetched"
                    this.discountType = "FIXED_AMOUNT"
                    this.value = 50000
                    this.isActive = true
                }
            whenever(discountRepository.findById(discountId)).thenReturn(entity)

            val result = service.findById(discountId)

            assertNotNull(result)
            verify(cacheService).set(any(), any(), eq(1800L))
        }

        @Test
        fun `returns null when not in cache or DB`() {
            val discountId = UUID.randomUUID()
            whenever(cacheService.get(any())).thenReturn(null)
            whenever(discountRepository.findById(discountId)).thenReturn(null)

            val result = service.findById(discountId)

            assertNull(result)
        }

        @Test
        fun `handles cache write failure gracefully`() {
            val discountId = UUID.randomUUID()
            whenever(cacheService.get(any())).thenReturn(null)
            doThrow(RuntimeException("Redis down")).whenever(cacheService).set(any(), any(), any())

            val entity =
                DiscountEntity().apply {
                    this.id = discountId
                    this.organizationId = orgId
                    this.name = "Fallback"
                    this.discountType = "PERCENTAGE"
                    this.value = 5
                    this.isActive = true
                }
            whenever(discountRepository.findById(discountId)).thenReturn(entity)

            val result = service.findById(discountId)

            assertNotNull(result)
            assertEquals("Fallback", result?.name)
        }
    }

    @Nested
    inner class DiscountCacheDtoTest {
        @Test
        fun `toEntity converts DTO with validFrom and validUntil`() {
            val now = Instant.now()
            val dto =
                DiscountCacheDto(
                    id = UUID.randomUUID(),
                    organizationId = orgId,
                    name = "Test",
                    discountType = "PERCENTAGE",
                    value = 10,
                    appliesTo = "ALL",
                    validFrom = now.toString(),
                    validUntil = now.plusSeconds(3600).toString(),
                    isActive = true,
                )

            val entity = dto.toEntity()

            assertEquals(dto.id, entity.id)
            assertEquals(dto.name, entity.name)
            assertNotNull(entity.validFrom)
            assertNotNull(entity.validUntil)
        }

        @Test
        fun `toEntity handles null validFrom and validUntil`() {
            val dto =
                DiscountCacheDto(
                    id = UUID.randomUUID(),
                    organizationId = orgId,
                    name = "No Dates",
                    discountType = "FIXED_AMOUNT",
                    value = 1000,
                    appliesTo = "ALL",
                    validFrom = null,
                    validUntil = null,
                    isActive = false,
                )

            val entity = dto.toEntity()

            assertNull(entity.validFrom)
            assertNull(entity.validUntil)
            assertEquals(false, entity.isActive)
        }

        @Test
        fun `from creates DTO from entity with dates`() {
            val now = Instant.now()
            val entity =
                DiscountEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    name = "Entity"
                    discountType = "PERCENTAGE"
                    value = 20
                    appliesTo = "ALL"
                    validFrom = now
                    validUntil = now.plusSeconds(3600)
                    isActive = true
                }

            val dto = DiscountCacheDto.from(entity)

            assertEquals(entity.id, dto.id)
            assertEquals(now.toString(), dto.validFrom)
            assertEquals(now.plusSeconds(3600).toString(), dto.validUntil)
        }

        @Test
        fun `from creates DTO from entity without dates`() {
            val entity =
                DiscountEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    name = "No Dates"
                    discountType = "FIXED_AMOUNT"
                    value = 500
                    appliesTo = "ALL"
                    validFrom = null
                    validUntil = null
                    isActive = true
                }

            val dto = DiscountCacheDto.from(entity)

            assertNull(dto.validFrom)
            assertNull(dto.validUntil)
        }
    }

    @Nested
    inner class UpdateCache {
        @Test
        fun `update invalidates cache after modifying entity`() {
            val discountId = UUID.randomUUID()
            val entity =
                DiscountEntity().apply {
                    id = discountId
                    organizationId = orgId
                    name = "Old"
                    discountType = "PERCENTAGE"
                    value = 10
                    isActive = true
                }
            whenever(discountRepository.findById(discountId)).thenReturn(entity)
            doNothing().whenever(cacheService).invalidate(any())
            doNothing().whenever(cacheService).invalidatePattern(any())

            val result = service.update(discountId, "New", "FIXED_AMOUNT", 5000L, null, null, false)

            assertNotNull(result)
            assertEquals("New", result?.name)
            assertEquals("FIXED_AMOUNT", result?.discountType)
            assertEquals(5000L, result?.value)
            assertEquals(false, result?.isActive)
            verify(cacheService).invalidate(any())
        }
    }

    @Nested
    inner class CreateCache {
        @Test
        fun `create invalidates list caches`() {
            doNothing().whenever(cacheService).invalidatePattern(any())

            val result = service.create("New Discount", "PERCENTAGE", 10, null, null)

            assertNotNull(result)
            verify(cacheService).invalidatePattern(any())
        }
    }
}
