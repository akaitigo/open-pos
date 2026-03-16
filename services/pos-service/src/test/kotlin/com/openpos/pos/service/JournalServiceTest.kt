package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.JournalEntryEntity
import com.openpos.pos.repository.JournalEntryRepository
import io.quarkus.panache.common.Page
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@QuarkusTest
class JournalServiceTest {
    @Inject
    lateinit var journalService: JournalService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var journalEntryRepository: JournalEntryRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()
    private val staffId = UUID.randomUUID()
    private val terminalId = UUID.randomUUID()
    private val transactionId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === recordEntry ===

    @Nested
    inner class RecordEntry {
        @Test
        fun `ジャーナルエントリを正常に記録する`() {
            // Arrange
            doNothing().whenever(journalEntryRepository).persist(any<JournalEntryEntity>())

            // Act
            val result =
                journalService.recordEntry(
                    type = "SALE",
                    transactionId = transactionId,
                    staffId = staffId,
                    terminalId = terminalId,
                    details = """{"amount":10000}""",
                )

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals("SALE", result.type)
            assertEquals(transactionId, result.transactionId)
            assertEquals(staffId, result.staffId)
            assertEquals(terminalId, result.terminalId)
            assertEquals("""{"amount":10000}""", result.details)
            verify(journalEntryRepository).persist(any<JournalEntryEntity>())
        }

        @Test
        fun `transactionIdがnullでも記録できる`() {
            // Arrange
            doNothing().whenever(journalEntryRepository).persist(any<JournalEntryEntity>())

            // Act
            val result =
                journalService.recordEntry(
                    type = "SETTLEMENT",
                    transactionId = null,
                    staffId = staffId,
                    terminalId = terminalId,
                    details = """{"action":"close"}""",
                )

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals("SETTLEMENT", result.type)
            assertEquals(null, result.transactionId)
            assertEquals(staffId, result.staffId)
            assertEquals(terminalId, result.terminalId)
        }

        @Test
        fun `VOIDタイプのエントリを記録する`() {
            // Arrange
            doNothing().whenever(journalEntryRepository).persist(any<JournalEntryEntity>())

            // Act
            val result =
                journalService.recordEntry(
                    type = "VOID",
                    transactionId = transactionId,
                    staffId = staffId,
                    terminalId = terminalId,
                    details = """{"reason":"customer request"}""",
                )

            // Assert
            assertEquals("VOID", result.type)
            assertEquals(transactionId, result.transactionId)
        }

        @Test
        fun `organizationIdが未設定の場合はエラー`() {
            // Arrange
            organizationIdHolder.organizationId = null

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                journalService.recordEntry(
                    type = "SALE",
                    transactionId = transactionId,
                    staffId = staffId,
                    terminalId = terminalId,
                    details = "{}",
                )
            }
        }
    }

    // === listEntries ===

    @Nested
    inner class ListEntries {
        @Test
        fun `フィルタなしで全エントリを取得する`() {
            // Arrange
            val entry1 = createJournalEntry("SALE")
            val entry2 = createJournalEntry("VOID")
            whenever(journalEntryRepository.listByFilters(eq(null), eq(null), eq(null), any()))
                .thenReturn(listOf(entry1, entry2))
            whenever(journalEntryRepository.countByFilters(eq(null), eq(null), eq(null)))
                .thenReturn(2L)

            // Act
            val (entries, totalCount) =
                journalService.listEntries(
                    type = null,
                    startDate = null,
                    endDate = null,
                    page = 0,
                    pageSize = 20,
                )

            // Assert
            assertEquals(2, entries.size)
            assertEquals(2L, totalCount)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `typeフィルタでエントリを取得する`() {
            // Arrange
            val entry = createJournalEntry("SALE")
            whenever(journalEntryRepository.listByFilters(eq("SALE"), eq(null), eq(null), any()))
                .thenReturn(listOf(entry))
            whenever(journalEntryRepository.countByFilters(eq("SALE"), eq(null), eq(null)))
                .thenReturn(1L)

            // Act
            val (entries, totalCount) =
                journalService.listEntries(
                    type = "SALE",
                    startDate = null,
                    endDate = null,
                    page = 0,
                    pageSize = 20,
                )

            // Assert
            assertEquals(1, entries.size)
            assertEquals("SALE", entries[0].type)
            assertEquals(1L, totalCount)
        }

        @Test
        fun `日付範囲フィルタでエントリを取得する`() {
            // Arrange
            val startDate = Instant.parse("2026-03-01T00:00:00Z")
            val endDate = Instant.parse("2026-03-31T23:59:59Z")
            val entry = createJournalEntry("SALE")
            whenever(journalEntryRepository.listByFilters(eq(null), eq(startDate), eq(endDate), any()))
                .thenReturn(listOf(entry))
            whenever(journalEntryRepository.countByFilters(eq(null), eq(startDate), eq(endDate)))
                .thenReturn(1L)

            // Act
            val (entries, totalCount) =
                journalService.listEntries(
                    type = null,
                    startDate = startDate,
                    endDate = endDate,
                    page = 0,
                    pageSize = 20,
                )

            // Assert
            assertEquals(1, entries.size)
            assertEquals(1L, totalCount)
        }

        @Test
        fun `結果が空の場合は空リストと0を返す`() {
            // Arrange
            whenever(journalEntryRepository.listByFilters(any(), any(), any(), any()))
                .thenReturn(emptyList())
            whenever(journalEntryRepository.countByFilters(any(), any(), any()))
                .thenReturn(0L)

            // Act
            val (entries, totalCount) =
                journalService.listEntries(
                    type = "RETURN",
                    startDate = null,
                    endDate = null,
                    page = 0,
                    pageSize = 20,
                )

            // Assert
            assertEquals(0, entries.size)
            assertEquals(0L, totalCount)
        }

        @Test
        fun `typeと日付範囲の複合フィルタで取得する`() {
            // Arrange
            val startDate = Instant.parse("2026-03-01T00:00:00Z")
            val endDate = Instant.parse("2026-03-31T23:59:59Z")
            val entry = createJournalEntry("VOID")
            whenever(journalEntryRepository.listByFilters(eq("VOID"), eq(startDate), eq(endDate), any()))
                .thenReturn(listOf(entry))
            whenever(journalEntryRepository.countByFilters(eq("VOID"), eq(startDate), eq(endDate)))
                .thenReturn(1L)

            // Act
            val (entries, totalCount) =
                journalService.listEntries(
                    type = "VOID",
                    startDate = startDate,
                    endDate = endDate,
                    page = 0,
                    pageSize = 10,
                )

            // Assert
            assertEquals(1, entries.size)
            assertEquals("VOID", entries[0].type)
            assertEquals(1L, totalCount)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === helper ===

    private fun createJournalEntry(type: String): JournalEntryEntity =
        JournalEntryEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.type = type
            this.transactionId = this@JournalServiceTest.transactionId
            this.staffId = this@JournalServiceTest.staffId
            this.terminalId = this@JournalServiceTest.terminalId
            this.details = "{}"
        }
}
