package com.openpos.pos.grpc

import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionDiscountEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import io.grpc.Status
import openpos.pos.v1.Payment
import openpos.pos.v1.PaymentMethod
import openpos.pos.v1.TaxSummary
import openpos.pos.v1.Transaction
import openpos.pos.v1.TransactionDiscount
import openpos.pos.v1.TransactionItem
import openpos.pos.v1.TransactionStatus
import openpos.pos.v1.TransactionType

internal fun TransactionEntity.toProto(
    items: List<TransactionItemEntity>,
    payments: List<PaymentEntity>,
    discounts: List<TransactionDiscountEntity>,
    taxSummaries: List<TaxSummaryEntity>,
): Transaction =
    Transaction
        .newBuilder()
        .setId(id.toString())
        .setOrganizationId(organizationId.toString())
        .setStoreId(storeId.toString())
        .setTerminalId(terminalId.toString())
        .setStaffId(staffId.toString())
        .setTransactionNumber(transactionNumber)
        .setType(type.toProtoTransactionType())
        .setStatus(status.toProtoTransactionStatus())
        .setClientId(clientId.orEmpty())
        .addAllItems(items.map { it.toProto() })
        .addAllDiscounts(discounts.map { it.toProto() })
        .addAllPayments(payments.map { it.toProto() })
        .addAllTaxSummaries(taxSummaries.map { it.toProto() })
        .setSubtotal(subtotal)
        .setTaxTotal(taxTotal)
        .setDiscountTotal(discountTotal)
        .setTotal(total)
        .setChangeAmount(changeAmount)
        .setCreatedAt(createdAt.toString())
        .setUpdatedAt(updatedAt.toString())
        .setCompletedAt(completedAt?.toString().orEmpty())
        .build()

internal fun TransactionItemEntity.toProto(): TransactionItem =
    TransactionItem
        .newBuilder()
        .setId(id.toString())
        .setProductId(productId?.toString().orEmpty())
        .setProductName(productName)
        .setUnitPrice(unitPrice)
        .setQuantity(quantity)
        .setTaxRateName(taxRateName)
        .setTaxRate(taxRate)
        .setIsReducedTax(isReducedTax)
        .setSubtotal(subtotal)
        .setTaxAmount(taxAmount)
        .setTotal(total)
        .build()

internal fun PaymentEntity.toProto(): Payment =
    Payment
        .newBuilder()
        .setId(id.toString())
        .setMethod(method.toProtoPaymentMethod())
        .setAmount(amount)
        .setReceived(received ?: 0)
        .setChange(change ?: 0)
        .setReference(reference.orEmpty())
        .build()

internal fun TransactionDiscountEntity.toProto(): TransactionDiscount =
    TransactionDiscount
        .newBuilder()
        .setId(id.toString())
        .setDiscountId(discountId?.toString().orEmpty())
        .setName(name)
        .setDiscountType(discountType)
        .setValue(value)
        .setAmount(amount)
        .setTransactionItemId(transactionItemId?.toString().orEmpty())
        .build()

internal fun TaxSummaryEntity.toProto(): TaxSummary =
    TaxSummary
        .newBuilder()
        .setTaxRateName(taxRateName)
        .setTaxRate(taxRate)
        .setIsReduced(isReduced)
        .setTaxableAmount(taxableAmount)
        .setTaxAmount(taxAmount)
        .build()

internal fun String.toProtoTransactionType(): TransactionType =
    when (this) {
        "SALE" -> TransactionType.TRANSACTION_TYPE_SALE
        "RETURN" -> TransactionType.TRANSACTION_TYPE_RETURN
        "VOID" -> TransactionType.TRANSACTION_TYPE_VOID
        else -> TransactionType.TRANSACTION_TYPE_UNSPECIFIED
    }

internal fun String.toProtoTransactionStatus(): TransactionStatus =
    when (this) {
        "DRAFT" -> TransactionStatus.TRANSACTION_STATUS_DRAFT
        "COMPLETED" -> TransactionStatus.TRANSACTION_STATUS_COMPLETED
        "VOIDED" -> TransactionStatus.TRANSACTION_STATUS_VOIDED
        else -> TransactionStatus.TRANSACTION_STATUS_UNSPECIFIED
    }

internal fun String.toProtoPaymentMethod(): PaymentMethod =
    when (this) {
        "CASH" -> PaymentMethod.PAYMENT_METHOD_CASH
        "CREDIT_CARD" -> PaymentMethod.PAYMENT_METHOD_CREDIT_CARD
        "QR_CODE" -> PaymentMethod.PAYMENT_METHOD_QR_CODE
        else -> PaymentMethod.PAYMENT_METHOD_UNSPECIFIED
    }

internal fun PaymentMethod.toDbValue(): String =
    when (this) {
        PaymentMethod.PAYMENT_METHOD_CASH -> "CASH"
        PaymentMethod.PAYMENT_METHOD_CREDIT_CARD -> "CREDIT_CARD"
        PaymentMethod.PAYMENT_METHOD_QR_CODE -> "QR_CODE"
        else -> throw Status.INVALID_ARGUMENT.withDescription("payment method is required").asRuntimeException()
    }
