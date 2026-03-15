package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response

/**
 * デジタルスタンプカード REST リソース（#222）。
 * プレースホルダー実装。
 */
@Path("/api/stamp-cards")
@Blocking
class StampCardResource {
    @GET
    @Path("/{customerId}")
    fun get(
        @PathParam("customerId") customerId: String,
    ): Map<String, Any> =
        mapOf(
            "customerId" to customerId,
            "totalStamps" to 0,
            "rewardThreshold" to 10,
            "isRewardAvailable" to false,
        )

    @POST
    @Path("/{customerId}/stamp")
    fun addStamp(
        @PathParam("customerId") customerId: String,
    ): Response =
        Response
            .ok(
                mapOf(
                    "customerId" to customerId,
                    "totalStamps" to 1,
                    "message" to "スタンプを追加しました",
                ),
            ).build()

    @POST
    @Path("/{customerId}/redeem")
    fun redeemReward(
        @PathParam("customerId") customerId: String,
    ): Response =
        Response
            .ok(
                mapOf(
                    "customerId" to customerId,
                    "message" to "特典を利用しました",
                ),
            ).build()
}
