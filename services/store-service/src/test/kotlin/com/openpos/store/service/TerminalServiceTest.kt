package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.TerminalEntity
import com.openpos.store.repository.TerminalRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class TerminalServiceTest {
    @Inject
    lateinit var terminalService: TerminalService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var terminalRepository: TerminalRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === register ===

    @Nested
    inner class Register {
        @Test
        fun `ターミナルを正常に登録する`() {
            // Arrange
            doNothing().whenever(terminalRepository).persist(any<TerminalEntity>())

            // Act
            val result = terminalService.register(storeId, "POS-01", "レジ1号機")

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals(storeId, result.storeId)
            assertEquals("POS-01", result.terminalCode)
            assertEquals("レジ1号機", result.name)
            assertTrue(result.isActive)
        }
    }

    // === listByStoreId ===

    @Nested
    inner class ListByStoreId {
        @Test
        fun `店舗IDでターミナル一覧を取得する`() {
            // Arrange
            val terminal1 =
                TerminalEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.storeId = this@TerminalServiceTest.storeId
                    this.terminalCode = "POS-01"
                    this.name = "レジ1号機"
                    this.isActive = true
                }
            val terminal2 =
                TerminalEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.storeId = this@TerminalServiceTest.storeId
                    this.terminalCode = "POS-02"
                    this.name = "レジ2号機"
                    this.isActive = true
                }
            whenever(terminalRepository.findByStoreId(storeId)).thenReturn(listOf(terminal1, terminal2))

            // Act
            val result = terminalService.listByStoreId(storeId)

            // Assert
            assertEquals(2, result.size)
            assertEquals("POS-01", result[0].terminalCode)
            assertEquals("POS-02", result[1].terminalCode)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `ターミナルが存在しない場合は空リストを返す`() {
            // Arrange
            whenever(terminalRepository.findByStoreId(storeId)).thenReturn(emptyList())

            // Act
            val result = terminalService.listByStoreId(storeId)

            // Assert
            assertTrue(result.isEmpty())
            verify(tenantFilterService).enableFilter()
        }
    }

    // === updateSync ===

    @Nested
    inner class UpdateSync {
        @Test
        fun `ターミナルの最終同期日時を更新する`() {
            // Arrange
            val terminalId = UUID.randomUUID()
            val entity =
                TerminalEntity().apply {
                    this.id = terminalId
                    this.organizationId = orgId
                    this.storeId = this@TerminalServiceTest.storeId
                    this.terminalCode = "POS-01"
                    this.name = "レジ1号機"
                    this.lastSyncAt = null
                    this.isActive = true
                }
            whenever(terminalRepository.findById(terminalId)).thenReturn(entity)
            doNothing().whenever(terminalRepository).persist(any<TerminalEntity>())

            // Act
            val result = terminalService.updateSync(terminalId)

            // Assert
            assertNotNull(result)
            assertNotNull(result?.lastSyncAt)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val terminalId = UUID.randomUUID()
            whenever(terminalRepository.findById(terminalId)).thenReturn(null)

            // Act
            val result = terminalService.updateSync(terminalId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }
}
