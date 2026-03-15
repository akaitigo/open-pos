package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import io.grpc.Status
import io.grpc.StatusRuntimeException
import openpos.product.v1.Category
import openpos.product.v1.CreateCategoryResponse
import openpos.product.v1.DeleteCategoryResponse
import openpos.product.v1.ListCategoriesResponse
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.UpdateCategoryResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class CategoryResourceTest {
    private val stub: ProductServiceGrpc.ProductServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val resource =
        CategoryResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
        }

    private val orgId = UUID.randomUUID().toString()

    private fun buildCategory(): Category =
        Category
            .newBuilder()
            .setId(UUID.randomUUID().toString())
            .setOrganizationId(orgId)
            .setName("飲み物")
            .setColor("#ff0000")
            .setDisplayOrder(0)
            .setCreatedAt("2026-01-01T00:00:00Z")
            .setUpdatedAt("2026-01-01T00:00:00Z")
            .build()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
    }

    @Nested
    inner class Create {
        @Test
        fun `カテゴリ作成で201を返す`() {
            // Arrange
            whenever(stub.createCategory(any())).thenReturn(
                CreateCategoryResponse.newBuilder().setCategory(buildCategory()).build(),
            )

            // Act
            val response = resource.create(CreateCategoryBody(name = "飲み物"))

            // Assert
            assertEquals(201, response.status)
        }
    }

    @Nested
    inner class List {
        @Test
        fun `カテゴリ一覧を返す`() {
            // Arrange
            whenever(stub.listCategories(any())).thenReturn(
                ListCategoriesResponse.newBuilder().addCategories(buildCategory()).build(),
            )

            // Act
            val result = resource.list(parentId = null)

            // Assert
            assertEquals(1, result.size)
            assertEquals("飲み物", result[0]["name"])
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `カテゴリ更新でMapを返す`() {
            // Arrange
            whenever(stub.updateCategory(any())).thenReturn(
                UpdateCategoryResponse.newBuilder().setCategory(buildCategory()).build(),
            )

            // Act
            val result = resource.update("cat-id", UpdateCategoryBody(name = "食べ物"))

            // Assert
            assertEquals("飲み物", result["name"])
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `カテゴリ削除で204を返す`() {
            // Arrange
            whenever(stub.deleteCategory(any())).thenReturn(DeleteCategoryResponse.getDefaultInstance())

            // Act
            val response = resource.delete("cat-id")

            // Assert
            assertEquals(204, response.status)
        }

        @Test
        fun `存在しないカテゴリでNOT_FOUND`() {
            // Arrange
            whenever(stub.deleteCategory(any())).thenThrow(
                StatusRuntimeException(Status.NOT_FOUND.withDescription("Category not found")),
            )

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                resource.delete("nonexistent")
            }
        }
    }
}
