package com.openpos.gateway.resource

import com.google.protobuf.BoolValue
import com.google.protobuf.Int32Value
import com.google.protobuf.Int64Value
import com.openpos.gateway.cache.RedisCacheService
import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.paginatedResponse
import com.openpos.gateway.config.requireValidPage
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import openpos.common.v1.PaginationRequest
import openpos.product.v1.CreateProductRequest
import openpos.product.v1.DeleteProductRequest
import openpos.product.v1.GetProductByBarcodeRequest
import openpos.product.v1.GetProductRequest
import openpos.product.v1.ListProductsRequest
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.UpdateProductRequest
import org.eclipse.microprofile.faulttolerance.Timeout
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@Path("/api/products")
@Blocking
@Timeout(30000)
@Tag(name = "Products", description = "商品管理API")
class ProductResource {
    @Inject
    @GrpcClient("product-service")
    lateinit var stub: ProductServiceGrpc.ProductServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var cache: RedisCacheService

    @Inject
    lateinit var tenantContext: TenantContext

    @POST
    @Operation(summary = "商品を作成する")
    fun create(body: CreateProductBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            CreateProductRequest
                .newBuilder()
                .setName(body.name)
                .setPrice(body.price)
                .apply {
                    body.description?.let { setDescription(it) }
                    body.barcode?.let { setBarcode(it) }
                    body.sku?.let { setSku(it) }
                    body.categoryId?.let { setCategoryId(it) }
                    body.taxRateId?.let { setTaxRateId(it) }
                    body.imageUrl?.let { setImageUrl(it) }
                    body.displayOrder?.let { setDisplayOrder(it) }
                }.build()
        val response = grpc.withTenant(stub).createProduct(request)
        val orgId = tenantContext.organizationId
        cache.invalidatePattern("openpos:gateway:product:list:${orgId ?: "*"}:*")
        return Response.status(Response.Status.CREATED).entity(response.product.toMap()).build()
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "IDで商品を取得する")
    fun get(
        @PathParam("id") id: String,
    ): Map<String, Any?> {
        val request = GetProductRequest.newBuilder().setId(id).build()
        return grpc
            .withTenant(stub)
            .getProduct(request)
            .product
            .toMap()
    }

    @GET
    @Operation(summary = "商品一覧を取得する")
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
        @QueryParam("categoryId") categoryId: String?,
        @QueryParam("search") search: String?,
        @QueryParam("activeOnly") @DefaultValue("false") activeOnly: Boolean,
    ): Map<String, Any> {
        requireValidPage(page)
        val request =
            ListProductsRequest
                .newBuilder()
                .setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).apply {
                    categoryId?.let { setCategoryId(it) }
                    search?.let { setSearch(it) }
                    setActiveOnly(activeOnly)
                }.build()
        val response = grpc.withTenant(stub).listProducts(request)
        return paginatedResponse(
            data = response.productsList.map { it.toMap() },
            pagination = response.pagination,
        )
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "商品を更新する")
    fun update(
        @PathParam("id") id: String,
        body: UpdateProductBody,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            UpdateProductRequest
                .newBuilder()
                .setId(id)
                .apply {
                    body.name?.let { setName(it) }
                    body.description?.let { setDescription(it) }
                    body.barcode?.let { setBarcode(it) }
                    body.sku?.let { setSku(it) }
                    body.price?.let { setPriceValue(Int64Value.of(it)) }
                    body.categoryId?.let { setCategoryId(it) }
                    body.taxRateId?.let { setTaxRateId(it) }
                    body.imageUrl?.let { setImageUrl(it) }
                    body.displayOrder?.let { setDisplayOrderValue(Int32Value.of(it)) }
                    body.isActive?.let { setIsActiveValue(BoolValue.of(it)) }
                }.build()
        val response = grpc.withTenant(stub).updateProduct(request)
        val orgId = tenantContext.organizationId
        cache.invalidatePattern("openpos:gateway:product:list:${orgId ?: "*"}:*")
        return response.product.toMap()
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "商品を論理削除する")
    fun delete(
        @PathParam("id") id: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        grpc.withTenant(stub).deleteProduct(DeleteProductRequest.newBuilder().setId(id).build())
        val orgId = tenantContext.organizationId
        cache.invalidatePattern("openpos:gateway:product:list:${orgId ?: "*"}:*")
        return Response.noContent().build()
    }

    @GET
    @Path("/barcode/{barcode}")
    @Operation(summary = "バーコードで商品を取得する")
    fun getByBarcode(
        @PathParam("barcode") barcode: String,
    ): Map<String, Any?> {
        val request = GetProductByBarcodeRequest.newBuilder().setBarcode(barcode).build()
        return grpc
            .withTenant(stub)
            .getProductByBarcode(request)
            .product
            .toMap()
    }
}

data class CreateProductBody(
    val name: String,
    val price: Long,
    val description: String? = null,
    val barcode: String? = null,
    val sku: String? = null,
    val categoryId: String? = null,
    val taxRateId: String? = null,
    val imageUrl: String? = null,
    val displayOrder: Int? = null,
)

data class UpdateProductBody(
    val name: String? = null,
    val price: Long? = null,
    val description: String? = null,
    val barcode: String? = null,
    val sku: String? = null,
    val categoryId: String? = null,
    val taxRateId: String? = null,
    val imageUrl: String? = null,
    val displayOrder: Int? = null,
    val isActive: Boolean? = null,
)
