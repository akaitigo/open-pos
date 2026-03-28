package com.openpos.gateway.resource

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.sse.Sse
import jakarta.ws.rs.sse.SseEventSink
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.jwt.JsonWebToken
import org.jboss.logging.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Server-Sent Events エンドポイント (#175)。
 * リアルタイム通知のプッシュ配信を提供する。
 *
 * EventSource API は Authorization ヘッダーを送信できないため、
 * ?token= クエリパラメータ方式で JWT 認証を行う。
 */
@Path("/api/events")
class SseResource {
    @Inject
    lateinit var broadcaster: SseBroadcaster

    @Inject
    lateinit var jwtParser: org.eclipse.microprofile.jwt.JsonWebToken

    @ConfigProperty(name = "openpos.auth.enabled", defaultValue = "true")
    var authEnabled: Boolean = true

    @Inject
    lateinit var sessionJwtParser: io.smallrye.jwt.auth.principal.JWTParser

    companion object {
        private val LOG = Logger.getLogger(SseResource::class.java)
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    fun subscribe(
        @QueryParam("token") token: String?,
        @Context sink: SseEventSink,
        @Context sse: Sse,
    ) {
        val organizationId =
            if (authEnabled) {
                if (token.isNullOrBlank()) {
                    val errorEvent =
                        sse
                            .newEventBuilder()
                            .name("error")
                            .data("""{"error":"UNAUTHORIZED","message":"Missing token parameter"}""")
                            .build()
                    sink.send(errorEvent)
                    sink.close()
                    return
                }
                try {
                    val jwt = sessionJwtParser.parse(token)
                    val orgId =
                        jwt.getClaim<String>("organization_id")
                            ?: throw IllegalArgumentException("Missing organization_id claim")
                    UUID.fromString(orgId)
                } catch (e: Exception) {
                    LOG.warnf("SSE auth failed: %s", e.message)
                    val errorEvent =
                        sse
                            .newEventBuilder()
                            .name("error")
                            .data("""{"error":"UNAUTHORIZED","message":"Invalid token"}""")
                            .build()
                    sink.send(errorEvent)
                    sink.close()
                    return
                }
            } else {
                null
            }

        broadcaster.addConnection(organizationId, sink, sse)

        val connectedEvent =
            sse
                .newEventBuilder()
                .name("connected")
                .data("""{"status":"connected"}""")
                .build()
        sink.send(connectedEvent)
    }
}

/**
 * SSE ブロードキャスト管理。
 * テナントごとの接続管理と 30 秒間隔のハートビートを提供する。
 */
@ApplicationScoped
class SseBroadcaster {
    data class SseConnection(
        val organizationId: UUID?,
        val sink: SseEventSink,
        val sse: Sse,
    )

    private val connections = ConcurrentHashMap<SseEventSink, SseConnection>()

    private val heartbeatExecutor =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "sse-heartbeat").apply { isDaemon = true }
        }

    companion object {
        private val LOG = Logger.getLogger(SseBroadcaster::class.java)
        private const val HEARTBEAT_INTERVAL_SECONDS = 30L
    }

    init {
        heartbeatExecutor.scheduleAtFixedRate(
            ::sendHeartbeats,
            HEARTBEAT_INTERVAL_SECONDS,
            HEARTBEAT_INTERVAL_SECONDS,
            TimeUnit.SECONDS,
        )
    }

    fun addConnection(
        organizationId: UUID?,
        sink: SseEventSink,
        sse: Sse,
    ) {
        connections[sink] = SseConnection(organizationId, sink, sse)
    }

    /**
     * 指定テナントの全接続にイベントをブロードキャストする。
     *
     * @param organizationId テナントID（null の場合は全接続に送信）
     * @param eventName イベント名
     * @param data JSON データ
     */
    fun broadcast(
        organizationId: UUID?,
        eventName: String,
        data: String,
    ) {
        val targets =
            if (organizationId != null) {
                connections.values.filter { it.organizationId == organizationId }
            } else {
                connections.values.toList()
            }

        for (conn in targets) {
            try {
                if (conn.sink.isClosed) {
                    connections.remove(conn.sink)
                    continue
                }
                val event =
                    conn.sse
                        .newEventBuilder()
                        .name(eventName)
                        .data(data)
                        .build()
                conn.sink.send(event)
            } catch (e: Exception) {
                LOG.warnf("Failed to send SSE event to connection: %s", e.message)
                connections.remove(conn.sink)
            }
        }
    }

    private fun sendHeartbeats() {
        val closed = mutableListOf<SseEventSink>()
        for ((sink, conn) in connections) {
            try {
                if (sink.isClosed) {
                    closed.add(sink)
                    continue
                }
                val heartbeat =
                    conn.sse
                        .newEventBuilder()
                        .name("heartbeat")
                        .data("""{"timestamp":${System.currentTimeMillis()}}""")
                        .build()
                sink.send(heartbeat)
            } catch (e: Exception) {
                closed.add(sink)
            }
        }
        closed.forEach { connections.remove(it) }
    }
}
