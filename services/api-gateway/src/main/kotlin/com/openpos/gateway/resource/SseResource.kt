package com.openpos.gateway.resource

import jakarta.annotation.security.DenyAll
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.sse.Sse
import jakarta.ws.rs.sse.SseEventSink

/**
 * Server-Sent Events エンドポイント (#175)。
 * リアルタイム通知のプッシュ配信を提供する。
 *
 * WARNING: 認証・テナント分離が未実装のため @DenyAll で封鎖中。
 * EventSource API は Authorization ヘッダーを送信できないため、
 * Cookie ベース認証または ?token= クエリパラメータ方式への再設計が必要。
 * 実装時は #587 を参照。
 */
@Path("/api/events")
@DenyAll
class SseResource {
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    fun subscribe(
        @Context sink: SseEventSink,
        @Context sse: Sse,
    ) {
        // Send an initial connection event
        val event =
            sse
                .newEventBuilder()
                .name("connected")
                .data("""{"status":"connected"}""")
                .build()
        sink.send(event)
    }
}
