package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import openpos.product.v1.CreateProductRequest
import openpos.product.v1.ProductServiceGrpc
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.multipart.FileUpload
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * 商品CSV一括インポート REST リソース (#1025)。
 * CSVファイルから商品マスタを一括登録する。
 *
 * CSV形式（ヘッダー必須）:
 * name,price,barcode,sku,category_id,tax_rate_id,description
 */
@Path("/api/products/import")
@Blocking
@Tag(name = "Products", description = "商品管理API")
class ProductImportResource {
    @Inject
    @GrpcClient("product-service")
    lateinit var stub: ProductServiceGrpc.ProductServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    companion object {
        private val log = Logger.getLogger(ProductImportResource::class.java)
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "CSVファイルから商品を一括インポートする")
    fun importCsv(
        @RestForm("file") file: FileUpload,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")

        val results = mutableListOf<Map<String, Any?>>()
        var successCount = 0
        var errorCount = 0
        var skippedCount = 0

        BufferedReader(
            InputStreamReader(file.uploadedFile().toFile().inputStream(), StandardCharsets.UTF_8),
        ).use { reader ->
            val header =
                reader.readLine()
                    ?: return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(mapOf("error" to "Empty CSV file"))
                        .build()

            val columns = header.split(",").map { it.trim().lowercase() }
            val nameIdx = columns.indexOf("name")
            val priceIdx = columns.indexOf("price")
            if (nameIdx < 0 || priceIdx < 0) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(mapOf("error" to "CSV must have 'name' and 'price' columns"))
                    .build()
            }

            val barcodeIdx = columns.indexOf("barcode")
            val skuIdx = columns.indexOf("sku")
            val categoryIdx = columns.indexOf("category_id")
            val taxRateIdx = columns.indexOf("tax_rate_id")
            val descIdx = columns.indexOf("description")

            var lineNumber = 1
            reader.forEachLine { line ->
                lineNumber++
                if (line.isBlank()) {
                    skippedCount++
                    results.add(mapOf("line" to lineNumber, "status" to "skipped", "message" to "Empty line"))
                    return@forEachLine
                }
                try {
                    val fields = parseCsvLine(line)
                    val name = fields.getOrElse(nameIdx) { "" }
                    if (name.isBlank()) {
                        skippedCount++
                        results.add(
                            mapOf("line" to lineNumber, "status" to "skipped", "message" to "Empty product name"),
                        )
                        return@forEachLine
                    }
                    val price = fields.getOrElse(priceIdx) { "0" }.toLongOrNull() ?: 0L
                    if (price <= 0) {
                        skippedCount++
                        results.add(
                            mapOf(
                                "line" to lineNumber,
                                "status" to "skipped",
                                "message" to "Price must be positive, got: $price",
                            ),
                        )
                        return@forEachLine
                    }
                    val request =
                        CreateProductRequest
                            .newBuilder()
                            .setName(name)
                            .setPrice(price)
                            .apply {
                                if (barcodeIdx >= 0) {
                                    fields.getOrNull(barcodeIdx)?.takeIf { it.isNotBlank() }?.let { setBarcode(it) }
                                }
                                if (skuIdx >= 0) {
                                    fields.getOrNull(skuIdx)?.takeIf { it.isNotBlank() }?.let { setSku(it) }
                                }
                                if (categoryIdx >= 0) {
                                    fields.getOrNull(categoryIdx)?.takeIf { it.isNotBlank() }?.let { setCategoryId(it) }
                                }
                                if (taxRateIdx >= 0) {
                                    fields.getOrNull(taxRateIdx)?.takeIf { it.isNotBlank() }?.let { setTaxRateId(it) }
                                }
                                if (descIdx >= 0) {
                                    fields.getOrNull(descIdx)?.takeIf { it.isNotBlank() }?.let { setDescription(it) }
                                }
                            }.build()
                    val response = grpc.withTenant(stub).createProduct(request)
                    successCount++
                    results.add(mapOf("line" to lineNumber, "status" to "success", "id" to response.product.id))
                } catch (e: Exception) {
                    errorCount++
                    log.warnf("CSV import line %d failed: %s", lineNumber, e.message)
                    results.add(
                        mapOf("line" to lineNumber, "status" to "error", "message" to (e.message ?: "Unknown error")),
                    )
                }
            }
        }

        return Response
            .ok(
                mapOf(
                    "totalProcessed" to (successCount + errorCount + skippedCount),
                    "success" to successCount,
                    "errors" to errorCount,
                    "skipped" to skippedCount,
                    "details" to results,
                ),
            ).build()
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                }

                ch == ',' && !inQuotes -> {
                    fields.add(current.toString().trim())
                    current.clear()
                }

                else -> {
                    current.append(ch)
                }
            }
        }
        fields.add(current.toString().trim())
        return fields
    }
}
