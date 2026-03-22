package com.openpos.gateway.config

import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.MDC
import java.util.UUID

/**
 * Correlation ID フィルター。
 * 全リクエストに x-request-id を付与し、MDC に設定してログ出力に含める。
 * レスポンスヘッダーにも含めてクライアント側でのトレースを可能にする。
 */
@Provider
@Priority(Priorities.USER - 100)
class CorrelationIdFilter :
    ContainerRequestFilter,
    ContainerResponseFilter {
    companion object {
        const val HEADER_NAME = "X-Request-Id"
        const val MDC_KEY = "requestId"
    }

    override fun filter(requestContext: ContainerRequestContext) {
        val requestId =
            requestContext.getHeaderString(HEADER_NAME)
                ?: UUID.randomUUID().toString()
        requestContext.headers.putSingle(HEADER_NAME, requestId)
        MDC.put(MDC_KEY, requestId)
    }

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        val requestId = requestContext.getHeaderString(HEADER_NAME)
        if (requestId != null) {
            responseContext.headers.putSingle(HEADER_NAME, requestId)
        }
        MDC.remove(MDC_KEY)
    }
}
