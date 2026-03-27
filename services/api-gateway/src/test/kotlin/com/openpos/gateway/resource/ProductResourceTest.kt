package com.openpos.gateway.resource

import com.openpos.gateway.cache.RedisCacheService
import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import io.grpc.Status
import io.grpc.StatusRuntimeException
import openpos.common.v1.PaginationResponse
import openpos.product.v1.CreateProductResponse
import openpos.product.v1.DeleteProductResponse
import openpos.product.v1.GetProductByBarcodeResponse
import openpos.product.v1.GetProductResponse
import openpos.product.v1.ListProductsResponse
import openpos.product.v1.Product
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.UpdateProductResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class ProductResourceTest {
    private val stub: ProductServiceGrpc.ProductServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val cache: RedisCacheService = mock()
    private val tenantContext: TenantContext = TenantContext()
    private val resource =
        ProductResource().also { r ->
            setField(r, "stub", stub)
            setField(r, "grpc", grpc)
            setField(r, "cache", cache)
            setField(r, "tenantContext", tenantContext)
        }

    private val orgId = UUID.randomUUID().toString()
    private val productId = UUID.randomUUID().toString()

    private fun buildProduct(id: String = productId): Product =
        Product
            .newBuilder()
            .setId(id)
            .setOrganizationId(orgId)
            .setName("コーヒー")
            .setPrice(30000)
            .setBarcode("4901234567890")
            .setDisplayOrder(0)
            .setIsActive(true)
            .setCreatedAt("2026-01-01T00:00:00Z")
            .setUpdatedAt("2026-01-01T00:00:00Z")
            .build()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
        tenantContext.organizationId = UUID.fromString(orgId)
    }

    @Nested
    inner class Create {
        @Test
        fun `商品作成で201を返す`() {
            // Arrange
            val product = buildProduct()
            whenever(stub.createProduct(any())).thenReturn(
                CreateProductResponse.newBuilder().setProduct(product).build(),
            )
            val body = CreateProductBody(name = "コーヒー", price = 30000)

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(201, response.status)
            verify(cache).invalidatePattern("openpos:gateway:product:list:$orgId:*")
        }

        @Test
        fun `オプションフィールド付きで商品作成`() {
            // Arrange
            val product = buildProduct()
            whenever(stub.createProduct(any())).thenReturn(
                CreateProductResponse.newBuilder().setProduct(product).build(),
            )
            val body =
                CreateProductBody(
                    name = "コーヒー",
                    price = 30000,
                    barcode = "4901234567890",
                    sku = "SKU-001",
                    categoryId = UUID.randomUUID().toString(),
                    taxRateId = UUID.randomUUID().toString(),
                    description = "おいしいコーヒー",
                    imageUrl = "https://example.com/img.jpg",
                    displayOrder = 1,
                )

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(201, response.status)
        }
    }

    @Nested
    inner class Get {
        @Test
        fun `商品取得でMapを返す`() {
            // Arrange
            val product = buildProduct()
            whenever(stub.getProduct(any())).thenReturn(
                GetProductResponse.newBuilder().setProduct(product).build(),
            )

            // Act
            val result = resource.get(productId)

            // Assert
            assertEquals("コーヒー", result["name"])
            assertEquals(30000L, result["price"])
        }

        @Test
        fun `存在しない商品でNOT_FOUND例外`() {
            // Arrange
            whenever(stub.getProduct(any())).thenThrow(
                StatusRuntimeException(Status.NOT_FOUND.withDescription("Product not found")),
            )

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                resource.get("nonexistent-id")
            }
        }
    }

    @Nested
    inner class List {
        @Test
        fun `商品一覧でページネーション付きMapを返す`() {
            // Arrange
            val product = buildProduct()
            whenever(stub.listProducts(any())).thenReturn(
                ListProductsResponse
                    .newBuilder()
                    .addProducts(product)
                    .setPagination(
                        PaginationResponse
                            .newBuilder()
                            .setPage(1)
                            .setPageSize(20)
                            .setTotalCount(1)
                            .setTotalPages(1)
                            .build(),
                    ).build(),
            )

            // Act
            val result = resource.list(page = 1, pageSize = 20, categoryId = null, search = null, activeOnly = false)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as kotlin.collections.List<*>
            assertEquals(1, data.size)
        }

        @Test
        fun `検索フィルターとカテゴリフィルターを渡す`() {
            // Arrange
            whenever(stub.listProducts(any())).thenReturn(
                ListProductsResponse
                    .newBuilder()
                    .setPagination(
                        PaginationResponse
                            .newBuilder()
                            .setPage(1)
                            .setPageSize(20)
                            .setTotalCount(0)
                            .setTotalPages(0)
                            .build(),
                    ).build(),
            )

            // Act
            val result = resource.list(page = 1, pageSize = 20, categoryId = "cat-1", search = "コーヒー", activeOnly = true)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as kotlin.collections.List<*>
            assertEquals(0, data.size)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `商品更新でMapを返す`() {
            // Arrange
            val product = buildProduct()
            whenever(stub.updateProduct(any())).thenReturn(
                UpdateProductResponse.newBuilder().setProduct(product).build(),
            )
            val body = UpdateProductBody(name = "ホットコーヒー")

            // Act
            val result = resource.update(productId, body)

            // Assert
            assertEquals("コーヒー", result["name"])
            verify(cache).invalidatePattern("openpos:gateway:product:list:$orgId:*")
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `商品削除で204を返す`() {
            // Arrange
            whenever(stub.deleteProduct(any())).thenReturn(DeleteProductResponse.getDefaultInstance())

            // Act
            val response = resource.delete(productId)

            // Assert
            assertEquals(204, response.status)
            verify(cache).invalidatePattern("openpos:gateway:product:list:$orgId:*")
        }
    }

    @Nested
    inner class GetByBarcode {
        @Test
        fun `バーコードで商品取得`() {
            // Arrange
            val product = buildProduct()
            whenever(stub.getProductByBarcode(any())).thenReturn(
                GetProductByBarcodeResponse.newBuilder().setProduct(product).build(),
            )

            // Act
            val result = resource.getByBarcode("4901234567890")

            // Assert
            assertEquals("コーヒー", result["name"])
        }
    }

    companion object {
        fun setField(
            target: Any,
            fieldName: String,
            value: Any,
        ) {
            val field = target::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(target, value)
        }
    }
}
