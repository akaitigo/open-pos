package com.openpos.gateway.config

import openpos.common.v1.PaginationResponse
import openpos.product.v1.Category
import openpos.product.v1.Coupon
import openpos.product.v1.Discount
import openpos.product.v1.Product
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
                openpos.product.v1.DiscountType.DISCOUNT_TYPE_PERCENTAGE -> "PERCENTAGE"
                openpos.product.v1.DiscountType.DISCOUNT_TYPE_FIXED_AMOUNT -> "FIXED_AMOUNT"
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
