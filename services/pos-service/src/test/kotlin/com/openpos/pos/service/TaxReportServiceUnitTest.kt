package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.repository.TaxSummaryRepository
import com.openpos.pos.repository.TransactionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class TaxReportServiceUnitTest {
    private lateinit var service: TaxReportService
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var taxSummaryRepository: TaxSummaryRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder
    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val startDate = Instant.parse("2026-03-01T00:00:00Z")
    private val endDate = Instant.parse("2026-03-31T23:59:59Z")

    @BeforeEach
    fun setUp() {
        transactionRepository = mock()
        taxSummaryRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()
        organizationIdHolder.organizationId = orgId
        service = TaxReportService().also {
            val cls = it::class.java
            for ((name, value) in mapOf("transactionRepository" to transactionRepository, "taxSummaryRepository" to taxSummaryRepository, "tenantFilterService" to tenantFilterService, "organizationIdHolder" to organizationIdHolder)) {
                cls.getDeclaredField(name).apply { isAccessible = true }.set(it, value)
            }
        }
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    private fun taxSummary(txId: UUID, name: String, rate: String, reduced: Boolean, taxable: Long, tax: Long) =
        TaxSummaryEntity().also { it.id = UUID.randomUUID(); it.organizationId = orgId; it.transactionId = txId; it.taxRateName = name; it.taxRate = rate; it.isReduced = reduced; it.taxableAmount = taxable; it.taxAmount = tax }

    @Nested inner class GetTaxReport {
        @Test fun `対象取引がない場合は空リストを返す`() {
            whenever(transactionRepository.findCompletedTransactionIds(storeId, startDate, endDate)).thenReturn(emptyList())
            assertTrue(service.getTaxReport(storeId, startDate, endDate).isEmpty())
        }
        @Test fun `単一税率の取引を正しく集計する`() {
            val t1 = UUID.randomUUID(); val t2 = UUID.randomUUID()
            whenever(transactionRepository.findCompletedTransactionIds(storeId, startDate, endDate)).thenReturn(listOf(t1, t2))
            whenever(taxSummaryRepository.findByTransactionIds(listOf(t1, t2))).thenReturn(listOf(
                taxSummary(t1, "標準税率10%", "0.10", false, 100000, 10000),
                taxSummary(t2, "標準税率10%", "0.10", false, 200000, 20000)))
            val r = service.getTaxReport(storeId, startDate, endDate)
            assertEquals(1, r.size); assertEquals(300000, r[0].taxableAmount); assertEquals(30000, r[0].taxAmount); assertEquals(2, r[0].transactionCount)
        }
        @Test fun `標準税率と軽減税率を分けて集計する`() {
            val t1 = UUID.randomUUID(); val t2 = UUID.randomUUID()
            whenever(transactionRepository.findCompletedTransactionIds(storeId, startDate, endDate)).thenReturn(listOf(t1, t2))
            whenever(taxSummaryRepository.findByTransactionIds(listOf(t1, t2))).thenReturn(listOf(
                taxSummary(t1, "標準税率10%", "0.10", false, 100000, 10000),
                taxSummary(t1, "軽減税率8%", "0.08", true, 50000, 4000),
                taxSummary(t2, "標準税率10%", "0.10", false, 200000, 20000)))
            val r = service.getTaxReport(storeId, startDate, endDate)
            assertEquals(2, r.size)
            assertFalse(r[0].isReduced); assertEquals(300000, r[0].taxableAmount)
            assertTrue(r[1].isReduced); assertEquals(50000, r[1].taxableAmount)
        }
        @Test fun `organizationIdが未設定の場合はエラー`() {
            organizationIdHolder.organizationId = null
            assertThrows<IllegalArgumentException> { service.getTaxReport(storeId, startDate, endDate) }
        }
        @Test fun `tenantFilterが呼ばれる`() {
            whenever(transactionRepository.findCompletedTransactionIds(storeId, startDate, endDate)).thenReturn(emptyList())
            service.getTaxReport(storeId, startDate, endDate)
            verify(tenantFilterService).enableFilter()
        }
    }
}
