package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.event.EventPublisher
import com.openpos.pos.grpc.ProductServiceClient
import com.openpos.pos.grpc.ProductSnapshot
import com.openpos.pos.repository.PaymentRepository
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
import com.openpos.pos.grpc.BusinessPreconditionException
import com.openpos.pos.grpc.InvalidInputException
import com.openpos.pos.grpc.ResourceNotFoundException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class TransactionServiceTest {
    @Inject
    lateinit var transactionService: TransactionService

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
            name = "テスト商品A",
            price = 10000, // 100円
            taxRateName = "標準税率10%",
            taxRate = "0.10",
            isReduced = false,
        )

    private val reducedProduct =
        ProductSnapshot(
            name = "テスト食品B",
            price = 15000, // 150円
            taxRateName = "軽減税率8%",
            taxRate = "0.08",
            isReduced = true,
        )

    @BeforeEach
    @Transactional
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        // テストデータをクリア（子テーブルから順に削除）
        taxSummaryRepository.deleteAll()
        paymentRepository.deleteAll()
        discountRepository.deleteAll()
        itemRepository.deleteAll()
        transactionRepository.deleteAll()
    }

    // === createTransaction ===

    @Nested
    inner class CreateTransaction {
        @Test
        fun `DRAFT取引を正常に作成する`() {
            // Act
            val tx = transactionService.createTransaction(storeId, terminalId, staffId, "SALE", null)

            // Assert
            assertNotNull(tx.id)
            assertEquals("DRAFT", tx.status)
            assertEquals("SALE", tx.type)
            assertEquals(storeId, tx.storeId)
            assertEquals(terminalId, tx.terminalId)
            assertEquals(staffId, tx.staffId)
            assertEquals(orgId, tx.organizationId)
            assertEquals(0L, tx.subtotal)
            assertEquals(0L, tx.total)
            assertNotNull(tx.transactionNumber)
        }

        @Test
        fun `typeが空文字の場合SALEがデフォルト設定される`() {
            // Act
            val tx = transactionService.createTransaction(storeId, terminalId, staffId, "", null)

            // Assert
            assertEquals("SALE", tx.type)
        }

        @Test
        fun `clientIdが同一の場合は既存取引を返す（冪等性）`() {
            // Arrange
            val clientId = UUID.randomUUID().toString()
            val first = transactionService.createTransaction(storeId, terminalId, staffId, "SALE", clientId)

            // Act
            val second = transactionService.createTransaction(storeId, terminalId, staffId, "SALE", clientId)

            // Assert
            assertEquals(first.id, second.id)
        }

        @Test
        fun `clientIdが空文字の場合はnullとして扱われ新規作成される`() {
            // Act
            val first = transactionService.createTransaction(storeId, terminalId, staffId, "SALE", "")
            val second = transactionService.createTransaction(storeId, terminalId, staffId, "SALE", "")

            // Assert — 空文字は null 扱いなので別々の取引が作成される
            assert(first.id != second.id)
        }
    }

    // === addItem ===

    @Nested
    inner class AddItem {
        @Test
        fun `新規商品を取引に追加する`() {
            // Arrange
            val tx = createDraftTransaction()
            whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
                .thenReturn(standardProduct)

            // Act
            val result = transactionService.addItem(tx.id, productId, 2)

            // Assert
            val items = itemRepository.findByTransactionId(tx.id)
            assertEquals(1, items.size)
            val item = items[0]
            assertEquals("テスト商品A", item.productName)
            assertEquals(10000L, item.unitPrice)
            assertEquals(2, item.quantity)
            assertEquals(20000L, item.subtotal) // 100円 x 2
            assertEquals(2000L, item.taxAmount) // 200円 x 10%
            assertEquals(22000L, item.total) // 220円
            // 取引合計も更新される
            assertEquals(20000L, result.subtotal)
            assertEquals(2000L, result.taxTotal)
            assertEquals(22000L, result.total)
        }

        @Test
        fun `同一商品を追加すると数量がマージされる`() {
            // Arrange
            val tx = createDraftTransaction()
            whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
                .thenReturn(standardProduct)

            // Act — 1回目: 2個、2回目: 3個
            transactionService.addItem(tx.id, productId, 2)
            val result = transactionService.addItem(tx.id, productId, 3)

            // Assert — 合計5個
            val items = itemRepository.findByTransactionId(tx.id)
            assertEquals(1, items.size)
            assertEquals(5, items[0].quantity)
            assertEquals(50000L, items[0].subtotal) // 100円 x 5
            assertEquals(5000L, items[0].taxAmount) // 500円 x 10%
            assertEquals(55000L, items[0].total)
            assertEquals(55000L, result.total)
        }

        @Test
        fun `数量が0以下の場合はエラー`() {
            // Arrange
            val tx = createDraftTransaction()

            // Act & Assert
            assertThrows(InvalidInputException::class.java) {
                transactionService.addItem(tx.id, productId, 0)
            }
        }
    }

    // === updateItem ===

    @Nested
    inner class UpdateItem {
        @Test
        fun `明細の数量を更新する`() {
            // Arrange
            val tx = createDraftTransaction()
            whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
                .thenReturn(standardProduct)
            transactionService.addItem(tx.id, productId, 2)
            val itemId = itemRepository.findByTransactionId(tx.id)[0].id

            // Act
            val result = transactionService.updateItem(tx.id, itemId, 5)

            // Assert — 返却された取引の合計で数量更新を検証
            // updateItem は recalculateTransactionTotals を呼ぶため
            // 取引合計が 5個分 (100円 x 5 + 税10%) = 550円 (55000銭) になる
            assertEquals(50000L, result.subtotal)
            assertEquals(5000L, result.taxTotal)
            assertEquals(55000L, result.total)
        }

        @Test
        fun `存在しないitemIdの場合はエラー`() {
            // Arrange
            val tx = createDraftTransaction()
            val fakeItemId = UUID.randomUUID()

            // Act & Assert
            assertThrows(ResourceNotFoundException::class.java) {
                transactionService.updateItem(tx.id, fakeItemId, 3)
            }
        }

        @Test
        fun `数量が0以下の場合はエラー`() {
            // Arrange
            val tx = createDraftTransaction()
            whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
                .thenReturn(standardProduct)
            transactionService.addItem(tx.id, productId, 1)
            val itemId = itemRepository.findByTransactionId(tx.id)[0].id

            // Act & Assert
            assertThrows(InvalidInputException::class.java) {
                transactionService.updateItem(tx.id, itemId, 0)
            }
        }
    }

    // === removeItem ===

    @Nested
    inner class RemoveItem {
        @Test
        fun `明細を削除し合計を再計算する`() {
            // Arrange
            val tx = createDraftTransaction()
            whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
                .thenReturn(standardProduct)
            transactionService.addItem(tx.id, productId, 2)
            val itemId = itemRepository.findByTransactionId(tx.id)[0].id

            // Act
            val result = transactionService.removeItem(tx.id, itemId)

            // Assert
            val items = itemRepository.findByTransactionId(tx.id)
            assertEquals(0, items.size)
            assertEquals(0L, result.subtotal)
            assertEquals(0L, result.taxTotal)
            assertEquals(0L, result.total)
        }

        @Test
        fun `存在しないitemIdの場合はエラー`() {
            // Arrange
            val tx = createDraftTransaction()
            val fakeItemId = UUID.randomUUID()

            // Act & Assert
            assertThrows(ResourceNotFoundException::class.java) {
                transactionService.removeItem(tx.id, fakeItemId)
            }
        }
    }

    // === finalizeTransaction ===

    @Nested
    inner class FinalizeTransaction {
        @Test
        fun `CASH支払いで取引を完了する`() {
            // Arrange
            val tx = createDraftTransaction()
            whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
                .thenReturn(standardProduct)
            transactionService.addItem(tx.id, productId, 2)
            // total = 22000（220円）
            val payments = listOf(PaymentInput(method = "CASH", amount = 22000, received = 30000, reference = null))

            // Act
            val result = transactionService.finalizeTransaction(tx.id, payments)

            // Assert
            assertEquals("COMPLETED", result.status)
            assertNotNull(result.completedAt)

            // 支払いレコード
            val savedPayments = paymentRepository.findByTransactionId(tx.id)
            assertEquals(1, savedPayments.size)
            assertEquals("CASH", savedPayments[0].method)
            assertEquals(22000L, savedPayments[0].amount)
            assertEquals(30000L, savedPayments[0].received)
            assertEquals(8000L, savedPayments[0].change) // 80円のお釣り

            // 税額集計レコード
            val taxSummaries = taxSummaryRepository.findByTransactionId(tx.id)
            assertEquals(1, taxSummaries.size)
            assertEquals("標準税率10%", taxSummaries[0].taxRateName)
            assertEquals(20000L, taxSummaries[0].taxableAmount)
            assertEquals(2000L, taxSummaries[0].taxAmount)

            // イベント発行
            verify(eventPublisher).publish(eq("sale.completed"), eq(orgId), any())
        }

        @Test
        fun `支払い金額が不足の場合はエラー`() {
            // Arrange
            val tx = createDraftTransaction()
            whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
                .thenReturn(standardProduct)
            transactionService.addItem(tx.id, productId, 2) // total = 22000
            val payments = listOf(PaymentInput(method = "CASH", amount = 10000, received = 10000, reference = null))

            // Act & Assert
            assertThrows(BusinessPreconditionException::class.java) {
                transactionService.finalizeTransaction(tx.id, payments)
            }
        }

        @Test
        fun `DRAFT以外のステータスの場合はエラー`() {
            // Arrange
            val tx = createDraftTransaction()
            whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
                .thenReturn(standardProduct)
            transactionService.addItem(tx.id, productId, 1)
            val payments = listOf(PaymentInput(method = "CASH", amount = 11000, received = 11000, reference = null))
            transactionService.finalizeTransaction(tx.id, payments) // COMPLETED に

            // Act & Assert — 再度 finalize 試行
            assertThrows(BusinessPreconditionException::class.java) {
                transactionService.finalizeTransaction(tx.id, payments)
            }
        }

        @Test
        fun `明細が空の場合はエラー`() {
            // Arrange
            val tx = createDraftTransaction()
            val payments = listOf(PaymentInput(method = "CASH", amount = 0, received = 0, reference = null))

            // Act & Assert
            assertThrows(BusinessPreconditionException::class.java) {
                transactionService.finalizeTransaction(tx.id, payments)
            }
        }

        @Test
        fun `複数税率の商品で税額集計が正しく生成される`() {
            // Arrange
            val tx = createDraftTransaction()
            val productId2 = UUID.randomUUID()
            whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
                .thenReturn(standardProduct)
            whenever(productServiceClient.getProductSnapshot(eq(productId2), eq(orgId)))
                .thenReturn(reducedProduct)

            transactionService.addItem(tx.id, productId, 1) // 100円 + 税10円 = 110円 (11000)
            transactionService.addItem(tx.id, productId2, 1) // 150円 + 税12円 = 162円 (16200)
            // total = 11000 + 16200 = 27200

            val payments = listOf(PaymentInput(method = "CASH", amount = 27200, received = 30000, reference = null))

            // Act
            val result = transactionService.finalizeTransaction(tx.id, payments)

            // Assert
            assertEquals("COMPLETED", result.status)
            val taxSummaries = taxSummaryRepository.findByTransactionId(tx.id)
            assertEquals(2, taxSummaries.size)

            val standard = taxSummaries.find { it.taxRateName == "標準税率10%" }
            assertNotNull(standard)
            assertEquals(10000L, standard!!.taxableAmount)
            assertEquals(1000L, standard.taxAmount)

            val reduced = taxSummaries.find { it.taxRateName == "軽減税率8%" }
            assertNotNull(reduced)
            assertEquals(15000L, reduced!!.taxableAmount)
            assertEquals(1200L, reduced.taxAmount)
        }
    }

    // === voidTransaction ===

    @Nested
    inner class VoidTransaction {
        @Test
        fun `COMPLETED取引をVOIDにする`() {
            // Arrange
            val tx = createCompletedTransaction()

            // Act
            val result = transactionService.voidTransaction(tx.id, "お客様都合による取消")

            // Assert
            assertEquals("VOIDED", result.status)
            verify(eventPublisher).publish(eq("sale.voided"), eq(orgId), any())
        }

        @Test
        fun `COMPLETED以外のステータスの場合はエラー`() {
            // Arrange
            val tx = createDraftTransaction()

            // Act & Assert
            assertThrows(BusinessPreconditionException::class.java) {
                transactionService.voidTransaction(tx.id, "テスト理由")
            }
        }

        @Test
        fun `理由が空文字の場合はエラー`() {
            // Arrange
            val tx = createCompletedTransaction()

            // Act & Assert
            assertThrows(InvalidInputException::class.java) {
                transactionService.voidTransaction(tx.id, "")
            }
        }

        @Test
        fun `理由がブランクの場合はエラー`() {
            // Arrange
            val tx = createCompletedTransaction()

            // Act & Assert
            assertThrows(InvalidInputException::class.java) {
                transactionService.voidTransaction(tx.id, "   ")
            }
        }
    }

    // === getTransaction ===

    @Nested
    inner class GetTransaction {
        @Test
        fun `取引を正常に取得する`() {
            // Arrange
            val tx = createDraftTransaction()

            // Act
            val result = transactionService.getTransaction(tx.id)

            // Assert
            assertEquals(tx.id, result.id)
            assertEquals("DRAFT", result.status)
        }

        @Test
        fun `存在しない取引IDの場合はエラー`() {
            // Arrange
            val fakeId = UUID.randomUUID()

            // Act & Assert
            assertThrows(ResourceNotFoundException::class.java) {
                transactionService.getTransaction(fakeId)
            }
        }
    }

    @Nested
    inner class ListTransactions {
        @Test
        fun `storeId 未指定なら組織配下の全取引を返す`() {
            transactionService.createTransaction(storeId, terminalId, staffId, "SALE", null)
            transactionService.createTransaction(UUID.randomUUID(), terminalId, staffId, "SALE", null)

            val (transactions, totalCount) =
                transactionService.listTransactions(
                    storeId = null,
                    terminalId = null,
                    status = null,
                    startDate = null,
                    endDate = null,
                    page = 0,
                    pageSize = 20,
                )

            assertEquals(2, transactions.size)
            assertEquals(2L, totalCount)
        }

        @Test
        fun `storeId 指定時は対象店舗の取引だけを返す`() {
            transactionService.createTransaction(storeId, terminalId, staffId, "SALE", null)
            transactionService.createTransaction(UUID.randomUUID(), terminalId, staffId, "SALE", null)

            val (transactions, totalCount) =
                transactionService.listTransactions(
                    storeId = storeId,
                    terminalId = null,
                    status = null,
                    startDate = null,
                    endDate = null,
                    page = 0,
                    pageSize = 20,
                )

            assertEquals(1, transactions.size)
            assertEquals(1L, totalCount)
            assertEquals(storeId, transactions[0].storeId)
        }
    }

    // === ヘルパーメソッド ===

    private fun createDraftTransaction(): TransactionEntity =
        transactionService.createTransaction(storeId, terminalId, staffId, "SALE", null)

    private fun createCompletedTransaction(): TransactionEntity {
        val tx = createDraftTransaction()
        whenever(productServiceClient.getProductSnapshot(eq(productId), eq(orgId)))
            .thenReturn(standardProduct)
        transactionService.addItem(tx.id, productId, 1)
        val payments = listOf(PaymentInput(method = "CASH", amount = 11000, received = 11000, reference = null))
        return transactionService.finalizeTransaction(tx.id, payments)
    }
}
