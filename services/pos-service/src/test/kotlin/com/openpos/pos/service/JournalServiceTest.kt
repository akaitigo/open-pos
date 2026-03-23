package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.JournalEntryEntity
import com.openpos.pos.repository.JournalEntryRepository
import io.quarkus.panache.common.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class JournalServiceTest {
    private lateinit var service: JournalService
    private lateinit var journalEntryRepository: JournalEntryRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val staffId = UUID.randomUUID()
    private val terminalId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        journalEntryRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = JournalService()
        service.journalEntryRepository = journalEntryRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class RecordEntry {
        @Test
        fun `records journal entry with all fields`() {
            doNothing().whenever(journalEntryRepository).persist(any<JournalEntryEntity>())
            val transactionId = UUID.randomUUID()

            val result = service.recordEntry("SALE", transactionId, staffId, terminalId, "{\"amount\": 10000}")

            assertEquals("SALE", result.type)
            assertEquals(transactionId, result.transactionId)
            assertEquals(staffId, result.staffId)
            assertEquals(terminalId, result.terminalId)
            assertEquals("{\"amount\": 10000}", result.details)
            assertEquals(orgId, result.organizationId)
            verify(journalEntryRepository).persist(any<JournalEntryEntity>())
        }

        @Test
        fun `records journal entry with null transactionId`() {
            doNothing().whenever(journalEntryRepository).persist(any<JournalEntryEntity>())

            val result = service.recordEntry("SETTLEMENT", null, staffId, terminalId, "{}")

            assertEquals("SETTLEMENT", result.type)
            assertEquals(null, result.transactionId)
        }

        @Test
        fun `throws when organizationId is not set`() {
            organizationIdHolder.organizationId = null

            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
                service.recordEntry("SALE", null, staffId, terminalId, "{}")
            }
        }
    }

    @Nested
    inner class ListEntries {
        @Test
        fun `returns entries with no filters`() {
            val entries =
                listOf(
                    JournalEntryEntity().apply {
                        this.id = UUID.randomUUID()
                        this.organizationId = orgId
                        this.type = "SALE"
                        this.staffId = this@JournalServiceTest.staffId
                        this.terminalId = this@JournalServiceTest.terminalId
                        this.details = "{}"
                    },
                )
            whenever(journalEntryRepository.listByFilters(eq(null), eq(null), eq(null), any<Page>()))
                .thenReturn(entries)
            whenever(journalEntryRepository.countByFilters(eq(null), eq(null), eq(null)))
                .thenReturn(1L)

            val (result, total) = service.listEntries(null, null, null, 0, 20)

            assertEquals(1, result.size)
            assertEquals(1L, total)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns entries filtered by type and date range`() {
            val start = Instant.parse("2026-03-01T00:00:00Z")
            val end = Instant.parse("2026-03-31T23:59:59Z")
            whenever(journalEntryRepository.listByFilters(eq("VOID"), eq(start), eq(end), any<Page>()))
                .thenReturn(emptyList())
            whenever(journalEntryRepository.countByFilters("VOID", start, end))
                .thenReturn(0L)

            val (result, total) = service.listEntries("VOID", start, end, 0, 20)

            assertEquals(0, result.size)
            assertEquals(0L, total)
        }
    }
}
