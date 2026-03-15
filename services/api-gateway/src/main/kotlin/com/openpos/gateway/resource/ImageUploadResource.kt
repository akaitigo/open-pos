package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * 画像アップロード REST リソース (#173)。
 * Phase 9: 商品画像管理の REST API placeholder。
 * 本番では GCS / S3 にアップロードし、URL を返す。
 */
@Path("/api/images")
@Blocking
class ImageUploadResource {
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun upload(): Response {
        // Placeholder: generate a fake URL
        val imageId = UUID.randomUUID()
        val fakeUrl = "/api/images/$imageId"
        return Response.status(Response.Status.CREATED)
            .entity(mapOf("imageUrl" to fakeUrl, "id" to imageId.toString()))
            .build()
    }
}
