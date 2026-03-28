package com.openpos.gateway.config

import openpos.common.v1.PaginationResponse
import openpos.inventory.v1.MovementType
import openpos.inventory.v1.PurchaseOrder
import openpos.inventory.v1.PurchaseOrderItem
import openpos.inventory.v1.PurchaseOrderStatus
import openpos.inventory.v1.Stock
import openpos.inventory.v1.StockMovement
import openpos.pos.v1.Drawer
import openpos.pos.v1.PaymentMethod
import openpos.pos.v1.Receipt
import openpos.pos.v1.Settlement
import openpos.pos.v1.TaxSummary
import openpos.pos.v1.Transaction
import openpos.pos.v1.TransactionDiscount
import openpos.pos.v1.TransactionItem
import openpos.pos.v1.TransactionStatus
import openpos.pos.v1.TransactionType
import openpos.product.v1.Category
import openpos.product.v1.Coupon
import openpos.product.v1.Discount
import openpos.product.v1.DiscountType
import openpos.product.v1.Product
import openpos.product.v1.ProductVariant
import openpos.product.v1.TaxRate
import openpos.store.v1.Organization
import openpos.store.v1.Staff
import openpos.store.v1.StaffRole
import openpos.store.v1.Store
import openpos.store.v1.Terminal

fun Product.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "name" to name,
        "description" to description.ifEmpty { null },
        "barcode" to barcode.ifEmpty { null },
        "sku" to sku.ifEmpty { null },
        "price" to price,
        "categoryId" to categoryId.ifEmpty { null },
        "taxRateId" to taxRateId.ifEmpty { null },
        "imageUrl" to imageUrl.ifEmpty { null },
        "displayOrder" to displayOrder,
        "isActive" to isActive,
        "variants" to variantsList.map { it.toMap() },
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun ProductVariant.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "productId" to productId,
        "name" to name,
        "sku" to sku.ifEmpty { null },
        "barcode" to barcode.ifEmpty { null },
        "price" to price,
        "isActive" to isActive,
        "displayOrder" to displayOrder,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun Category.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "name" to name,
        "parentId" to parentId.ifEmpty { null },
        "color" to color.ifEmpty { null },
        "icon" to icon.ifEmpty { null },
        "displayOrder" to displayOrder,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun TaxRate.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "name" to name,
        "rate" to rate,
        "isReduced" to isReduced,
        "isDefault" to isDefault,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun Discount.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "name" to name,
        "discountType" to
            when (discountType) {
                DiscountType.DISCOUNT_TYPE_PERCENTAGE -> "PERCENTAGE"
                DiscountType.DISCOUNT_TYPE_FIXED_AMOUNT -> "FIXED_AMOUNT"
                else -> "UNSPECIFIED"
            },
        "value" to value,
        "startDate" to startDate.ifEmpty { null },
        "endDate" to endDate.ifEmpty { null },
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun Coupon.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "code" to code,
        "discountId" to discountId,
        "maxUses" to maxUses,
        "currentUses" to currentUses,
        "startDate" to startDate.ifEmpty { null },
        "endDate" to endDate.ifEmpty { null },
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun Organization.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "name" to name,
        "businessType" to businessType,
        "invoiceNumber" to invoiceNumber.ifEmpty { null },
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun Store.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "name" to name,
        "address" to address.ifEmpty { null },
        "phone" to phone.ifEmpty { null },
        "timezone" to timezone,
        "settings" to settings.ifEmpty { null },
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun Terminal.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "storeId" to storeId,
        "terminalCode" to terminalCode,
        "name" to name.ifEmpty { null },
        "isActive" to isActive,
        "lastSyncAt" to lastSyncAt.ifEmpty { null },
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun Staff.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "storeId" to storeId,
        "name" to name,
        "email" to email.ifEmpty { null },
        "role" to
            when (role) {
                StaffRole.STAFF_ROLE_OWNER -> "OWNER"
                StaffRole.STAFF_ROLE_MANAGER -> "MANAGER"
                StaffRole.STAFF_ROLE_CASHIER -> "CASHIER"
                else -> "UNSPECIFIED"
            },
        "isActive" to isActive,
        "failedPinAttempts" to failedPinAttempts,
        "isLocked" to isLocked,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

// === POS 取引 ===

fun Transaction.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "storeId" to storeId,
        "terminalId" to terminalId,
        "staffId" to staffId,
        "transactionNumber" to transactionNumber,
        "type" to
            when (type) {
                TransactionType.TRANSACTION_TYPE_SALE -> "SALE"
                TransactionType.TRANSACTION_TYPE_RETURN -> "RETURN"
                TransactionType.TRANSACTION_TYPE_VOID -> "VOID"
                else -> "UNSPECIFIED"
            },
        "status" to
            when (status) {
                TransactionStatus.TRANSACTION_STATUS_DRAFT -> "DRAFT"
                TransactionStatus.TRANSACTION_STATUS_COMPLETED -> "COMPLETED"
                TransactionStatus.TRANSACTION_STATUS_VOIDED -> "VOIDED"
                else -> "UNSPECIFIED"
            },
        "clientId" to clientId.ifEmpty { null },
        "items" to itemsList.map { it.toMap() },
        "discounts" to discountsList.map { it.toMap() },
        "payments" to paymentsList.map { it.toMap() },
        "taxSummaries" to taxSummariesList.map { it.toMap() },
        "subtotal" to subtotal,
        "taxTotal" to taxTotal,
        "discountTotal" to discountTotal,
        "total" to total,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "completedAt" to completedAt.ifEmpty { null },
    )

fun TransactionItem.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "productId" to productId,
        "productName" to productName,
        "unitPrice" to unitPrice,
        "quantity" to quantity,
        "taxRateName" to taxRateName,
        "taxRate" to taxRate,
        "isReducedTax" to isReducedTax,
        "subtotal" to subtotal,
        "taxAmount" to taxAmount,
        "total" to total,
    )

fun TransactionDiscount.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "discountId" to discountId,
        "name" to name,
        "discountType" to discountType,
        "value" to value,
        "amount" to amount,
        "transactionItemId" to transactionItemId.ifEmpty { null },
    )

fun openpos.pos.v1.Payment.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "method" to
            when (method) {
                PaymentMethod.PAYMENT_METHOD_CASH -> "CASH"
                PaymentMethod.PAYMENT_METHOD_CREDIT_CARD -> "CREDIT_CARD"
                PaymentMethod.PAYMENT_METHOD_QR_CODE -> "QR_CODE"
                else -> "UNSPECIFIED"
            },
        "amount" to amount,
        "received" to received,
        "change" to change,
        "reference" to reference.ifEmpty { null },
    )

fun TaxSummary.toMap(): Map<String, Any?> =
    mapOf(
        "taxRateName" to taxRateName,
        "taxRate" to taxRate,
        "isReduced" to isReduced,
        "taxableAmount" to taxableAmount,
        "taxAmount" to taxAmount,
    )

fun Receipt.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "transactionId" to transactionId,
        "receiptData" to receiptData,
        "pdfUrl" to pdfUrl.ifEmpty { null },
        "createdAt" to createdAt,
    )

// === ドロワー・精算 ===

fun Drawer.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "storeId" to storeId,
        "terminalId" to terminalId,
        "openingAmount" to openingAmount,
        "currentAmount" to currentAmount,
        "isOpen" to isOpen,
        "openedAt" to openedAt.ifEmpty { null },
        "closedAt" to closedAt.ifEmpty { null },
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun Settlement.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "storeId" to storeId,
        "terminalId" to terminalId,
        "staffId" to staffId,
        "cashExpected" to cashExpected,
        "cashActual" to cashActual,
        "difference" to difference,
        "settledAt" to settledAt,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

// === 在庫管理 ===

fun Stock.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "storeId" to storeId,
        "productId" to productId,
        "quantity" to quantity,
        "lowStockThreshold" to lowStockThreshold,
        "updatedAt" to updatedAt,
    )

fun StockMovement.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "storeId" to storeId,
        "productId" to productId,
        "movementType" to
            when (movementType) {
                MovementType.MOVEMENT_TYPE_SALE -> "SALE"
                MovementType.MOVEMENT_TYPE_RETURN -> "RETURN"
                MovementType.MOVEMENT_TYPE_RECEIPT -> "RECEIPT"
                MovementType.MOVEMENT_TYPE_ADJUSTMENT -> "ADJUSTMENT"
                MovementType.MOVEMENT_TYPE_TRANSFER -> "TRANSFER"
                else -> "UNSPECIFIED"
            },
        "quantity" to quantity,
        "referenceId" to referenceId.ifEmpty { null },
        "note" to note.ifEmpty { null },
        "createdAt" to createdAt,
    )

fun PurchaseOrder.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "storeId" to storeId,
        "status" to
            when (status) {
                PurchaseOrderStatus.PURCHASE_ORDER_STATUS_DRAFT -> "DRAFT"
                PurchaseOrderStatus.PURCHASE_ORDER_STATUS_ORDERED -> "ORDERED"
                PurchaseOrderStatus.PURCHASE_ORDER_STATUS_RECEIVED -> "RECEIVED"
                PurchaseOrderStatus.PURCHASE_ORDER_STATUS_CANCELLED -> "CANCELLED"
                else -> "UNSPECIFIED"
            },
        "items" to itemsList.map { it.toMap() },
        "supplierName" to supplierName,
        "note" to note.ifEmpty { null },
        "orderedAt" to orderedAt.ifEmpty { null },
        "receivedAt" to receivedAt.ifEmpty { null },
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun PurchaseOrderItem.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "productId" to productId,
        "orderedQuantity" to orderedQuantity,
        "receivedQuantity" to receivedQuantity,
        "unitCost" to unitCost,
    )

// === 電子ジャーナル ===

fun openpos.pos.v1.JournalEntry.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "type" to type,
        "transactionId" to transactionId.ifEmpty { null },
        "staffId" to staffId,
        "terminalId" to terminalId,
        "details" to details,
        "createdAt" to createdAt,
    )

// === 分析 ===

fun openpos.analytics.v1.DailySales.toMap(): Map<String, Any?> =
    mapOf(
        "date" to date,
        "storeId" to storeId,
        "grossAmount" to grossAmount,
        "netAmount" to netAmount,
        "taxAmount" to taxAmount,
        "discountAmount" to discountAmount,
        "transactionCount" to transactionCount,
        "cashAmount" to cashAmount,
        "cardAmount" to cardAmount,
        "qrAmount" to qrAmount,
    )

fun openpos.analytics.v1.HourlySales.toMap(): Map<String, Any?> =
    mapOf(
        "hour" to hour,
        "amount" to amount,
        "transactionCount" to transactionCount,
    )

fun openpos.analytics.v1.AbcAnalysisItem.toMap(): Map<String, Any?> =
    mapOf(
        "productId" to productId,
        "productName" to productName,
        "revenue" to revenue,
        "revenueRatio" to revenueRatio,
        "cumulativeRatio" to cumulativeRatio,
        "rank" to rank,
    )

fun openpos.analytics.v1.GrossProfitItem.toMap(): Map<String, Any?> =
    mapOf(
        "productId" to productId,
        "productName" to productName,
        "revenue" to revenue,
        "cost" to cost,
        "grossProfit" to grossProfit,
        "marginRate" to marginRate,
        "quantitySold" to quantitySold,
    )

fun openpos.analytics.v1.SalesForecastPoint.toMap(): Map<String, Any?> =
    mapOf(
        "date" to date,
        "actualAmount" to actualAmount,
        "movingAverage" to movingAverage,
    )

fun openpos.analytics.v1.SalesTarget.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "storeId" to storeId.ifEmpty { null },
        "targetMonth" to targetMonth,
        "targetAmount" to targetAmount,
        "organizationId" to organizationId,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

// === システム設定 ===

fun openpos.store.v1.SystemSetting.toMap(): Map<String, Any?> =
    mapOf(
        "key" to key,
        "value" to value,
        "description" to description.ifEmpty { null },
    )

// === ギフトカード ===

fun openpos.pos.v1.GiftCard.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "code" to code,
        "initialAmount" to initialAmount,
        "balance" to balance,
        "status" to status,
        "issuedAt" to issuedAt,
        "expiresAt" to expiresAt.ifBlank { null },
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

// === スタンプカード ===

fun openpos.store.v1.StampCard.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "customerId" to customerId,
        "stampCount" to stampCount,
        "maxStamps" to maxStamps,
        "rewardDescription" to rewardDescription.ifEmpty { null },
        "status" to status,
        "issuedAt" to issuedAt,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

// === ページネーション ===

fun PaginationResponse.toMap(): Map<String, Any> =
    mapOf(
        "page" to page,
        "pageSize" to pageSize,
        "totalCount" to totalCount,
        "totalPages" to totalPages,
    )

fun <T> paginatedResponse(
    data: List<T>,
    pagination: PaginationResponse,
): Map<String, Any> =
    mapOf(
        "data" to data,
        "pagination" to pagination.toMap(),
    )

/**
 * ページ番号のバリデーション。1未満の場合は 400 Bad Request を返す。
 */
fun requireValidPage(page: Int) {
    if (page < 1) {
        throw jakarta.ws.rs.BadRequestException("page must be >= 1, got $page")
    }
}
