package com.openpos.product.grpc

import com.openpos.product.entity.CategoryEntity
import com.openpos.product.entity.CouponEntity
import com.openpos.product.entity.DiscountEntity
import com.openpos.product.entity.ProductEntity
import com.openpos.product.entity.TaxRateEntity
import com.openpos.product.service.CategoryService
import com.openpos.product.service.CouponService
import com.openpos.product.service.DiscountService
import com.openpos.product.service.ProductService
import com.openpos.product.service.TaxRateService
import io.grpc.Status
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.persistence.OptimisticLockException
import openpos.common.v1.PaginationResponse
import openpos.product.v1.BatchGetProductsRequest
import openpos.product.v1.BatchGetProductsResponse
import openpos.product.v1.Category
import openpos.product.v1.Coupon
import openpos.product.v1.CreateCategoryRequest
import openpos.product.v1.CreateCategoryResponse
import openpos.product.v1.CreateCouponRequest
import openpos.product.v1.CreateCouponResponse
import openpos.product.v1.CreateDiscountRequest
import openpos.product.v1.CreateDiscountResponse
import openpos.product.v1.CreateProductRequest
import openpos.product.v1.CreateProductResponse
import openpos.product.v1.CreateTaxRateRequest
import openpos.product.v1.CreateTaxRateResponse
import openpos.product.v1.DeleteCategoryRequest
import openpos.product.v1.DeleteCategoryResponse
import openpos.product.v1.DeleteDiscountRequest
import openpos.product.v1.DeleteDiscountResponse
import openpos.product.v1.DeleteProductRequest
import openpos.product.v1.DeleteProductResponse
import openpos.product.v1.DeleteTaxRateRequest
import openpos.product.v1.DeleteTaxRateResponse
import openpos.product.v1.Discount
import openpos.product.v1.DiscountType
import openpos.product.v1.GetProductByBarcodeRequest
import openpos.product.v1.GetProductByBarcodeResponse
import openpos.product.v1.GetProductRequest
import openpos.product.v1.GetProductResponse
import openpos.product.v1.ListCategoriesRequest
import openpos.product.v1.ListCategoriesResponse
import openpos.product.v1.ListDiscountsRequest
import openpos.product.v1.ListDiscountsResponse
import openpos.product.v1.ListProductsRequest
import openpos.product.v1.ListProductsResponse
import openpos.product.v1.ListTaxRatesRequest
import openpos.product.v1.ListTaxRatesResponse
import openpos.product.v1.Product
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.TaxRate
import openpos.product.v1.UpdateCategoryRequest
import openpos.product.v1.UpdateCategoryResponse
import openpos.product.v1.UpdateDiscountRequest
import openpos.product.v1.UpdateDiscountResponse
import openpos.product.v1.UpdateProductRequest
import openpos.product.v1.UpdateProductResponse
import openpos.product.v1.UpdateTaxRateRequest
import openpos.product.v1.UpdateTaxRateResponse
import openpos.product.v1.ValidateCouponRequest
import openpos.product.v1.ValidateCouponResponse
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@GrpcService
@Blocking
class ProductGrpcService : ProductServiceGrpc.ProductServiceImplBase() {
    @Inject
    lateinit var productService: ProductService

    @Inject
    lateinit var categoryService: CategoryService

    @Inject
    lateinit var taxRateService: TaxRateService

    @Inject
    lateinit var discountService: DiscountService

    @Inject
    lateinit var couponService: CouponService

    @Inject
    lateinit var tenantHelper: GrpcTenantHelper

    // === Product ===

    override fun createProduct(
        request: CreateProductRequest,
        responseObserver: io.grpc.stub.StreamObserver<CreateProductResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val entity =
            productService.create(
                name = request.name,
                description = request.description.ifBlank { null },
                barcode = request.barcode.ifBlank { null },
                sku = request.sku.ifBlank { null },
                price = request.price,
                categoryId = request.categoryId.uuidOrNull(),
                taxRateId = request.taxRateId.uuidOrNull(),
                imageUrl = request.imageUrl.ifBlank { null },
                displayOrder = request.displayOrder,
            )
        responseObserver.onNext(
            CreateProductResponse.newBuilder().setProduct(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun getProduct(
        request: GetProductRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetProductResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            productService.findById(request.id.toUUID())
                ?: throw Status.NOT_FOUND.withDescription("Product not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            GetProductResponse.newBuilder().setProduct(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun listProducts(
        request: ListProductsRequest,
        responseObserver: io.grpc.stub.StreamObserver<ListProductsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val page = if (request.hasPagination()) request.pagination.page - 1 else 0
        val pageSize = if (request.hasPagination() && request.pagination.pageSize > 0) request.pagination.pageSize.coerceAtMost(100) else 20
        val (products, totalCount) =
            productService.search(
                query = request.search.ifBlank { null },
                categoryId = request.categoryId.uuidOrNull(),
                activeOnly = request.activeOnly,
                page = page,
                pageSize = pageSize,
            )
        val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0
        responseObserver.onNext(
            ListProductsResponse
                .newBuilder()
                .addAllProducts(products.map { it.toProto() })
                .setPagination(
                    PaginationResponse
                        .newBuilder()
                        .setPage(page + 1)
                        .setPageSize(pageSize)
                        .setTotalCount(totalCount)
                        .setTotalPages(totalPages)
                        .build(),
                ).build(),
        )
        responseObserver.onCompleted()
    }

    override fun updateProduct(
        request: UpdateProductRequest,
        responseObserver: io.grpc.stub.StreamObserver<UpdateProductResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val entity =
                productService.update(
                    id = request.id.toUUID(),
                    name = request.name.ifBlank { null },
                    description = request.description.ifBlank { null },
                    barcode = request.barcode.ifBlank { null },
                    sku = request.sku.ifBlank { null },
                    price = request.priceOrNull(),
                    categoryId = request.categoryId.uuidOrNull(),
                    taxRateId = request.taxRateId.uuidOrNull(),
                    imageUrl = request.imageUrl.ifBlank { null },
                    displayOrder = request.displayOrderOrNull(),
                    isActive = request.isActiveOrNull(),
                ) ?: throw Status.NOT_FOUND.withDescription("Product not found: ${request.id}").asRuntimeException()
            responseObserver.onNext(
                UpdateProductResponse.newBuilder().setProduct(entity.toProto()).build(),
            )
            responseObserver.onCompleted()
        } catch (e: OptimisticLockException) {
            throw Status.ABORTED
                .withDescription("Concurrent modification detected for product: ${request.id}")
                .asRuntimeException()
        }
    }

    override fun deleteProduct(
        request: DeleteProductRequest,
        responseObserver: io.grpc.stub.StreamObserver<DeleteProductResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val deleted = productService.delete(request.id.toUUID())
        if (!deleted) {
            throw Status.NOT_FOUND.withDescription("Product not found: ${request.id}").asRuntimeException()
        }
        responseObserver.onNext(DeleteProductResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun getProductByBarcode(
        request: GetProductByBarcodeRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetProductByBarcodeResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            productService.findByBarcode(request.barcode)
                ?: throw Status.NOT_FOUND.withDescription("Product not found for barcode: ${request.barcode}").asRuntimeException()
        responseObserver.onNext(
            GetProductByBarcodeResponse.newBuilder().setProduct(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun batchGetProducts(
        request: BatchGetProductsRequest,
        responseObserver: io.grpc.stub.StreamObserver<BatchGetProductsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val ids = request.idsList.map { it.toUUID() }
        val entities = productService.findByIds(ids)
        responseObserver.onNext(
            BatchGetProductsResponse
                .newBuilder()
                .addAllProducts(entities.map { it.toProto() })
                .build(),
        )
        responseObserver.onCompleted()
    }

    // === Category ===

    override fun createCategory(
        request: CreateCategoryRequest,
        responseObserver: io.grpc.stub.StreamObserver<CreateCategoryResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val entity =
            categoryService.create(
                name = request.name,
                parentId = request.parentId.uuidOrNull(),
                color = request.color.ifBlank { null },
                icon = request.icon.ifBlank { null },
                displayOrder = request.displayOrder,
            )
        responseObserver.onNext(
            CreateCategoryResponse.newBuilder().setCategory(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun listCategories(
        request: ListCategoriesRequest,
        responseObserver: io.grpc.stub.StreamObserver<ListCategoriesResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val categories = categoryService.listByParentId(request.parentId.uuidOrNull())
        responseObserver.onNext(
            ListCategoriesResponse.newBuilder().addAllCategories(categories.map { it.toProto() }).build(),
        )
        responseObserver.onCompleted()
    }

    override fun updateCategory(
        request: UpdateCategoryRequest,
        responseObserver: io.grpc.stub.StreamObserver<UpdateCategoryResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            categoryService.update(
                id = request.id.toUUID(),
                name = request.name.ifBlank { null },
                parentId = request.parentId.uuidOrNull(),
                color = request.color.ifBlank { null },
                icon = request.icon.ifBlank { null },
                displayOrder = if (request.displayOrder > 0) request.displayOrder else null,
            ) ?: throw Status.NOT_FOUND.withDescription("Category not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            UpdateCategoryResponse.newBuilder().setCategory(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun deleteCategory(
        request: DeleteCategoryRequest,
        responseObserver: io.grpc.stub.StreamObserver<DeleteCategoryResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val deleted = categoryService.delete(request.id.toUUID())
        if (!deleted) {
            throw Status.NOT_FOUND.withDescription("Category not found: ${request.id}").asRuntimeException()
        }
        responseObserver.onNext(DeleteCategoryResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    // === TaxRate ===

    override fun createTaxRate(
        request: CreateTaxRateRequest,
        responseObserver: io.grpc.stub.StreamObserver<CreateTaxRateResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val entity =
            taxRateService.create(
                name = request.name,
                rate = BigDecimal(request.rate),
                taxType = if (request.isReduced) "REDUCED" else "STANDARD",
                isDefault = request.isDefault,
            )
        responseObserver.onNext(
            CreateTaxRateResponse.newBuilder().setTaxRate(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun listTaxRates(
        request: ListTaxRatesRequest,
        responseObserver: io.grpc.stub.StreamObserver<ListTaxRatesResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val taxRates = taxRateService.listAll()
        responseObserver.onNext(
            ListTaxRatesResponse.newBuilder().addAllTaxRates(taxRates.map { it.toProto() }).build(),
        )
        responseObserver.onCompleted()
    }

    override fun updateTaxRate(
        request: UpdateTaxRateRequest,
        responseObserver: io.grpc.stub.StreamObserver<UpdateTaxRateResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            taxRateService.update(
                id = request.id.toUUID(),
                name = request.name.ifBlank { null },
                rate = if (request.rate.isNotBlank()) BigDecimal(request.rate) else null,
                taxType = request.taxTypeOrNull(),
                isActive = null,
                isDefault = request.isDefaultOrNull(),
            ) ?: throw Status.NOT_FOUND.withDescription("TaxRate not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            UpdateTaxRateResponse.newBuilder().setTaxRate(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun deleteTaxRate(
        request: DeleteTaxRateRequest,
        responseObserver: io.grpc.stub.StreamObserver<DeleteTaxRateResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val deleted = taxRateService.delete(request.id.toUUID())
        if (!deleted) {
            throw Status.NOT_FOUND.withDescription("TaxRate not found: ${request.id}").asRuntimeException()
        }
        responseObserver.onNext(DeleteTaxRateResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    // === Discount ===

    override fun createDiscount(
        request: CreateDiscountRequest,
        responseObserver: io.grpc.stub.StreamObserver<CreateDiscountResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val entity =
            discountService.create(
                name = request.name,
                discountType =
                    when (request.discountType) {
                        DiscountType.DISCOUNT_TYPE_PERCENTAGE -> "PERCENTAGE"
                        DiscountType.DISCOUNT_TYPE_FIXED_AMOUNT -> "FIXED_AMOUNT"
                        else -> throw Status.INVALID_ARGUMENT.withDescription("discount_type is required").asRuntimeException()
                    },
                value = request.value.toLong(),
                validFrom = request.startDate.instantOrNull(),
                validUntil = request.endDate.instantOrNull(),
            )
        responseObserver.onNext(
            CreateDiscountResponse.newBuilder().setDiscount(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun listDiscounts(
        request: ListDiscountsRequest,
        responseObserver: io.grpc.stub.StreamObserver<ListDiscountsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val discounts = discountService.list(request.activeOnly)
        responseObserver.onNext(
            ListDiscountsResponse.newBuilder().addAllDiscounts(discounts.map { it.toProto() }).build(),
        )
        responseObserver.onCompleted()
    }

    override fun updateDiscount(
        request: UpdateDiscountRequest,
        responseObserver: io.grpc.stub.StreamObserver<UpdateDiscountResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            discountService.update(
                id = request.id.toUUID(),
                name = request.name.ifBlank { null },
                discountType =
                    when (request.discountType) {
                        DiscountType.DISCOUNT_TYPE_PERCENTAGE -> "PERCENTAGE"
                        DiscountType.DISCOUNT_TYPE_FIXED_AMOUNT -> "FIXED_AMOUNT"
                        else -> null
                    },
                value = if (request.value.isNotBlank()) request.value.toLong() else null,
                validFrom = request.startDate.instantOrNull(),
                validUntil = request.endDate.instantOrNull(),
                isActive = request.isActiveOrNull(),
            ) ?: throw Status.NOT_FOUND.withDescription("Discount not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            UpdateDiscountResponse.newBuilder().setDiscount(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun deleteDiscount(
        request: DeleteDiscountRequest,
        responseObserver: io.grpc.stub.StreamObserver<DeleteDiscountResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val deleted = discountService.delete(request.id.toUUID())
        if (!deleted) {
            throw Status.NOT_FOUND.withDescription("Discount not found: ${request.id}").asRuntimeException()
        }
        responseObserver.onNext(DeleteDiscountResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    // === Coupon ===

    override fun createCoupon(
        request: CreateCouponRequest,
        responseObserver: io.grpc.stub.StreamObserver<CreateCouponResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val entity =
            couponService.create(
                code = request.code,
                discountId = request.discountId.toUUID(),
                maxUses = if (request.maxUses > 0) request.maxUses else null,
                validFrom = request.startDate.instantOrNull(),
                validUntil = request.endDate.instantOrNull(),
            )
        responseObserver.onNext(
            CreateCouponResponse.newBuilder().setCoupon(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun validateCoupon(
        request: ValidateCouponRequest,
        responseObserver: io.grpc.stub.StreamObserver<ValidateCouponResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val result = couponService.validate(request.code)
        val builder = ValidateCouponResponse.newBuilder().setIsValid(result.isValid)
        result.coupon?.let { builder.setCoupon(it.toProto()) }
        result.discount?.let { builder.setDiscount(it.toProto()) }
        result.reason?.let { builder.setReason(it) }
        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()
    }

    // === Mapper Extensions ===

    private fun ProductEntity.toProto(): Product =
        Product
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setName(name)
            .setDescription(description.orEmpty())
            .setBarcode(barcode.orEmpty())
            .setSku(sku.orEmpty())
            .setPrice(price)
            .setCategoryId(categoryId?.toString().orEmpty())
            .setTaxRateId(taxRateId?.toString().orEmpty())
            .setImageUrl(imageUrl.orEmpty())
            .setDisplayOrder(displayOrder)
            .setIsActive(isActive)
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    private fun CategoryEntity.toProto(): Category =
        Category
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setName(name)
            .setParentId(parentId?.toString().orEmpty())
            .setColor(color.orEmpty())
            .setIcon(icon.orEmpty())
            .setDisplayOrder(displayOrder)
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    private fun TaxRateEntity.toProto(): TaxRate =
        TaxRate
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setName(name)
            .setRate(rate.toPlainString())
            .setIsReduced(taxType == "REDUCED")
            .setIsDefault(isDefault)
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    private fun DiscountEntity.toProto(): Discount =
        Discount
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setName(name)
            .setDiscountType(
                when (discountType) {
                    "PERCENTAGE" -> DiscountType.DISCOUNT_TYPE_PERCENTAGE
                    "FIXED_AMOUNT" -> DiscountType.DISCOUNT_TYPE_FIXED_AMOUNT
                    else -> DiscountType.DISCOUNT_TYPE_UNSPECIFIED
                },
            ).setValue(value.toString())
            .setStartDate(validFrom?.toString().orEmpty())
            .setEndDate(validUntil?.toString().orEmpty())
            .setIsActive(isActive)
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    private fun CouponEntity.toProto(): Coupon =
        Coupon
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setCode(code)
            .setDiscountId(discountId.toString())
            .setMaxUses(maxUses ?: 0)
            .setCurrentUses(usedCount)
            .setStartDate(validFrom?.toString().orEmpty())
            .setEndDate(validUntil?.toString().orEmpty())
            .setIsActive(isActive)
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    // === Utility Extensions ===

    private fun String.toUUID(): UUID =
        try {
            UUID.fromString(this)
        } catch (e: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT.withDescription("Invalid UUID: $this").asRuntimeException()
        }

    private fun String.uuidOrNull(): UUID? = if (isBlank()) null else toUUID()

    private fun String.instantOrNull(): Instant? = if (isBlank()) null else Instant.parse(this)

    private fun UpdateProductRequest.priceOrNull(): Long? =
        when {
            hasPriceValue() -> priceValue.value
            price > 0 -> price
            else -> null
        }

    private fun UpdateProductRequest.displayOrderOrNull(): Int? =
        when {
            hasDisplayOrderValue() -> displayOrderValue.value
            displayOrder > 0 -> displayOrder
            else -> null
        }

    private fun UpdateProductRequest.isActiveOrNull(): Boolean? =
        when {
            hasIsActiveValue() -> isActiveValue.value
            isActive -> true
            else -> null
        }

    private fun UpdateTaxRateRequest.taxTypeOrNull(): String? =
        when {
            hasIsReducedValue() -> if (isReducedValue.value) "REDUCED" else "STANDARD"
            isReduced -> "REDUCED"
            else -> null
        }

    private fun UpdateTaxRateRequest.isDefaultOrNull(): Boolean? =
        when {
            hasIsDefaultValue() -> isDefaultValue.value
            isDefault -> true
            else -> null
        }

    private fun UpdateDiscountRequest.isActiveOrNull(): Boolean? =
        when {
            hasIsActiveValue() -> isActiveValue.value
            isActive -> true
            else -> null
        }
}
