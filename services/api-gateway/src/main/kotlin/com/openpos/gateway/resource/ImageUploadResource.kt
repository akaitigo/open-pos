package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.annotation.security.DenyAll
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

/**
 * 画像アップロード REST リソース (#173)。
 * 未実装: オブジェクトストレージ連携が未整備のため全エンドポイントをブロック。
 */
@Path("/api/images")
@Blocking
@DenyAll
class ImageUploadResource {
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun upload(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Image upload API is not yet implemented"))
            .build()
}
