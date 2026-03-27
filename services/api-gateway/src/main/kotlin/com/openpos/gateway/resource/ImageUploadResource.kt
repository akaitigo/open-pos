package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.multipart.FileUpload
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * 画像アップロード REST リソース (#173)。
 * ローカルファイルシステムにJPEG/PNG/WebP画像を保存する。
 */
@Path("/api/images")
@Blocking
class ImageUploadResource {
    @Inject
    lateinit var tenantContext: TenantContext

    @ConfigProperty(name = "openpos.image.upload-dir", defaultValue = "/tmp/openpos/images")
    lateinit var uploadDir: String

    @ConfigProperty(name = "openpos.image.base-url", defaultValue = "http://localhost:8080/api/images")
    lateinit var baseUrl: String

    companion object {
        private val LOG = Logger.getLogger(ImageUploadResource::class.java)
        private const val MAX_FILE_SIZE = 5L * 1024 * 1024 // 5MB
        private val ALLOWED_CONTENT_TYPES =
            setOf("image/jpeg", "image/png", "image/webp")
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun upload(
        @RestForm("file") file: FileUpload,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")

        val contentType = file.contentType()
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(
                    mapOf(
                        "error" to "INVALID_CONTENT_TYPE",
                        "message" to "Allowed types: JPEG, PNG, WebP",
                    ),
                ).build()
        }

        val fileSize = file.size()
        if (fileSize > MAX_FILE_SIZE) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(
                    mapOf(
                        "error" to "FILE_TOO_LARGE",
                        "message" to "Maximum file size is 5MB",
                    ),
                ).build()
        }

        val extension =
            when (contentType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "bin"
            }
        val filename = "${UUID.randomUUID()}.$extension"

        try {
            val targetDir =
                java.nio.file.Path
                    .of(uploadDir)
            Files.createDirectories(targetDir)
            val targetPath = targetDir.resolve(filename)
            Files.copy(file.filePath(), targetPath, StandardCopyOption.REPLACE_EXISTING)

            LOG.infof("Image uploaded: %s (%d bytes)", filename, fileSize)

            return Response
                .status(Response.Status.CREATED)
                .entity(
                    mapOf(
                        "url" to "$baseUrl/$filename",
                        "filename" to filename,
                        "size" to fileSize,
                        "contentType" to contentType,
                    ),
                ).build()
        } catch (e: Exception) {
            LOG.errorf("Failed to upload image: %s", e.message)
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    mapOf(
                        "error" to "UPLOAD_FAILED",
                        "message" to "Failed to save image",
                    ),
                ).build()
        }
    }
}
