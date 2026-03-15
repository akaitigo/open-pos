package com.openpos.gateway.resource

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
 */
@Path("/api/events")
class SseResource {
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    fun subscribe(
        @Context sink: SseEventSink,
        @Context sse: Sse,
    ) {
        // Send an initial connection event
        val event = sse.newEventBuilder()
            .name("connected")
            .data("""{"status":"connected"}""")
            .build()
        sink.send(event)
    }
}
