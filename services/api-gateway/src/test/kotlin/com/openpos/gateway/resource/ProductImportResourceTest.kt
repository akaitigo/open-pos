package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import openpos.product.v1.CreateProductResponse
import openpos.product.v1.Product
import openpos.product.v1.ProductServiceGrpc
import org.jboss.resteasy.reactive.multipart.FileUpload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.util.UUID

class ProductImportResourceTest {
    private val stub: ProductServiceGrpc.ProductServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val tenantContext: TenantContext = TenantContext()
    private val resource =
        ProductImportResource().also { r ->
            setField(r, "stub", stub)
            setField(r, "grpc", grpc)
            setField(r, "tenantContext", tenantContext)
        }

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
        tenantContext.organizationId = orgId
    }

    private fun createCsvFileUpload(content: String): FileUpload {
        val tempFile = Files.createTempFile("test-import", ".csv")
        Files.writeString(tempFile, content)
        val fileUpload = mock<FileUpload>()
        whenever(fileUpload.uploadedFile()).thenReturn(tempFile)
        return fileUpload
    }

    private fun mockCreateProduct(id: String = UUID.randomUUID().toString()) {
        val product =
            Product
                .newBuilder()
                .setId(id)
                .setOrganizationId(orgId.toString())
                .setName("Test")
                .setPrice(10000)
                .build()
        whenever(stub.createProduct(any())).thenReturn(
            CreateProductResponse.newBuilder().setProduct(product).build(),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun responseBody(response: jakarta.ws.rs.core.Response): Map<String, Any> = response.entity as Map<String, Any>

    @Nested
    inner class SkipEmptyLines {
        @Test
        fun `skips blank lines and counts them`() {
            // Arrange
            val csv = "name,price\nCoffee,30000\n\nTea,20000\n   \n"
            val fileUpload = createCsvFileUpload(csv)
            mockCreateProduct()

            // Act
            val response = resource.importCsv(fileUpload)

            // Assert
            val body = responseBody(response)
            assertEquals(200, response.status)
            assertEquals(2, body["success"])
            assertEquals(2, body["skipped"])
            assertEquals(0, body["errors"])
            verify(stub, times(2)).createProduct(any())
        }
    }

    @Nested
    inner class SkipEmptyName {
        @Test
        fun `skips rows with empty product name`() {
            // Arrange
            val csv = "name,price\n,30000\nCoffee,20000"
            val fileUpload = createCsvFileUpload(csv)
            mockCreateProduct()

            // Act
            val response = resource.importCsv(fileUpload)

            // Assert
            val body = responseBody(response)
            assertEquals(1, body["success"])
            assertEquals(1, body["skipped"])
            assertEquals(0, body["errors"])
        }

        @Test
        fun `skips rows with whitespace-only name`() {
            // Arrange
            val csv = "name,price\n   ,30000\nCoffee,20000"
            val fileUpload = createCsvFileUpload(csv)
            mockCreateProduct()

            // Act
            val response = resource.importCsv(fileUpload)

            // Assert
            val body = responseBody(response)
            assertEquals(1, body["success"])
            assertEquals(1, body["skipped"])
        }
    }

    @Nested
    inner class SkipInvalidPrice {
        @Test
        fun `skips rows with zero price`() {
            // Arrange
            val csv = "name,price\nCoffee,0\nTea,20000"
            val fileUpload = createCsvFileUpload(csv)
            mockCreateProduct()

            // Act
            val response = resource.importCsv(fileUpload)

            // Assert
            val body = responseBody(response)
            assertEquals(1, body["success"])
            assertEquals(1, body["skipped"])
        }

        @Test
        fun `skips rows with negative price`() {
            // Arrange
            val csv = "name,price\nCoffee,-100\nTea,20000"
            val fileUpload = createCsvFileUpload(csv)
            mockCreateProduct()

            // Act
            val response = resource.importCsv(fileUpload)

            // Assert
            val body = responseBody(response)
            assertEquals(1, body["success"])
            assertEquals(1, body["skipped"])
        }

        @Test
        fun `skips rows with non-numeric price`() {
            // Arrange
            val csv = "name,price\nCoffee,abc\nTea,20000"
            val fileUpload = createCsvFileUpload(csv)
            mockCreateProduct()

            // Act
            val response = resource.importCsv(fileUpload)

            // Assert
            val body = responseBody(response)
            assertEquals(1, body["success"])
            assertEquals(1, body["skipped"])
        }
    }

    @Nested
    inner class ResponseFormat {
        @Test
        fun `includes skipped count in response`() {
            // Arrange
            val csv = "name,price\n,30000\nCoffee,-100\n\nTea,20000"
            val fileUpload = createCsvFileUpload(csv)
            mockCreateProduct()

            // Act
            val response = resource.importCsv(fileUpload)

            // Assert
            val body = responseBody(response)
            assertEquals(4, body["totalProcessed"])
            assertEquals(1, body["success"])
            assertEquals(0, body["errors"])
            assertEquals(3, body["skipped"])
        }

        @Test
        fun `includes skipped details with reason`() {
            // Arrange
            val csv = "name,price\n,30000"
            val fileUpload = createCsvFileUpload(csv)

            // Act
            val response = resource.importCsv(fileUpload)

            // Assert
            val body = responseBody(response)

            @Suppress("UNCHECKED_CAST")
            val details = body["details"] as List<Map<String, Any?>>
            assertEquals(1, details.size)
            assertEquals("skipped", details[0]["status"])
            assertEquals("Empty product name", details[0]["message"])
        }
    }

    @Nested
    inner class ValidCsvProcessing {
        @Test
        fun `processes valid CSV rows successfully`() {
            // Arrange
            val csv = "name,price\nCoffee,30000\nTea,20000"
            val fileUpload = createCsvFileUpload(csv)
            mockCreateProduct()

            // Act
            val response = resource.importCsv(fileUpload)

            // Assert
            val body = responseBody(response)
            assertEquals(200, response.status)
            assertEquals(2, body["success"])
            assertEquals(0, body["errors"])
            assertEquals(0, body["skipped"])
            verify(stub, times(2)).createProduct(any())
        }

        @Test
        fun `does not call gRPC for skipped rows`() {
            // Arrange -- all rows should be skipped
            val csv = "name,price\n,30000\n,0"
            val fileUpload = createCsvFileUpload(csv)

            // Act
            resource.importCsv(fileUpload)

            // Assert
            verify(stub, never()).createProduct(any())
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
