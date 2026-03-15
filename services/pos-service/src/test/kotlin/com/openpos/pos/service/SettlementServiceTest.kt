package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.event.EventPublisher
import com.openpos.pos.grpc.ProductServiceClient
import com.openpos.pos.grpc.ProductSnapshot
import com.openpos.pos.repository.PaymentRepository
import com.openpos.pos.repository.SettlementRepository
import com.openpos.pos.repository.TaxSummaryRepository
import com.openpos.pos.repository.TransactionDiscountRepository
import com.openpos.pos.repository.TransactionItemRepository
import com.openpos.pos.repository.TransactionRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class SettlementServiceTest {
    @Inject
    lateinit var settlementService: SettlementService

    @Inject
    lateinit var transactionService: TransactionService

    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var itemRepository: TransactionItemRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var taxSummaryRepository: TaxSummaryRepository

    @Inject
    lateinit var discountRepository: TransactionDiscountRepository

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var productServiceClient: ProductServiceClient

    @InjectMock
    lateinit var eventPublisher: EventPublisher

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val terminalId = UUID.randomUUID()
    private val staffId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    private val standardProduct =
        ProductSnapshot(
            name = "テスト商品",
            price = 10000,
            taxRateName = "標準税率10%",
            taxRate = "0.10",
            isReduced = false,
        )

    @BeforeEach
    @Transactional
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        settlementRepository.deleteAll()
        taxSummaryRepository.deleteAll()
        paymentRepository.deleteAll()
        discountRepository.deleteAll()
        itemRepository.deleteAll()
        transactionRepository.deleteAll()
    }

    @Nested
    inner class CreateSettlement {
        @Test
        fun `取引なしで精算を作成する場合、cashExpectedは0になる`() {
            // Act
            val settlement = settlementService.createSettlement(storeId, terminalId, staffId, 50000L)

            // Assert
            assertNotNull(settlement.id)
            assertEquals(orgId, settlement.organizationId)
            assertEquals(storeId, settlement.storeId)
            assertEquals(terminalId, settlement.terminalId)
            assertEquals(staffId, settlement.staffId)
            assertEquals(0L, settlement.cashExpected)
            assertEquals(50000L, settlement.cashActual)
            assertEquals(50000L, settlement.difference)
            assertNotNull(settlement.settledAt)
        }

        @Test
        fun `完了済み取引がある場合にcashExpectedが計算される`() {
            // Arrange
            whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
                .thenReturn(standardProduct)
            val tx = transactionService.createTransaction(storeId, terminalId, staffId, "SALE", null)
            transactionService.addItem(tx.id, productId, 2) // 200円 + tax = 220円 (22000)
            transactionService.finalizeTransaction(
                tx.id,
                listOf(PaymentInput(method = "CASH", amount = 22000, received = 30000, reference = null)),
            )
            // cashExpected = 22000 (220円分のCASH支払い)

            // Act
            val settlement = settlementService.createSettlement(storeId, terminalId, staffId, 22000L)

            // Assert
            assertEquals(22000L, settlement.cashExpected)
            assertEquals(22000L, settlement.cashActual)
            assertEquals(0L, settlement.difference) // ぴったり一致
        }

        @Test
        fun `現金不足の場合、differenceがマイナスになる`() {
            // Arrange
            whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
                .thenReturn(standardProduct)
            val tx = transactionService.createTransaction(storeId, terminalId, staffId, "SALE", null)
            transactionService.addItem(tx.id, productId, 1) // 100円 + tax = 110円 (11000)
            transactionService.finalizeTransaction(
                tx.id,
                listOf(PaymentInput(method = "CASH", amount = 11000, received = 11000, reference = null)),
            )

            // Act
            val settlement = settlementService.createSettlement(storeId, terminalId, staffId, 5000L)

            // Assert
            assertEquals(11000L, settlement.cashExpected)
            assertEquals(5000L, settlement.cashActual)
            assertTrue(settlement.difference < 0) // 不足
            assertEquals(-6000L, settlement.difference)
        }
    }

    @Nested
    inner class GetSettlement {
        @Test
        fun `精算を正常に取得する`() {
            // Arrange
            val created = settlementService.createSettlement(storeId, terminalId, staffId, 50000L)

            // Act
            val result = settlementService.getSettlement(created.id)

            // Assert
            assertEquals(created.id, result.id)
            assertEquals(50000L, result.cashActual)
        }

        @Test
        fun `存在しない精算IDの場合はエラー`() {
            // Arrange
            val fakeId = UUID.randomUUID()

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                settlementService.getSettlement(fakeId)
            }
        }
    }
}
