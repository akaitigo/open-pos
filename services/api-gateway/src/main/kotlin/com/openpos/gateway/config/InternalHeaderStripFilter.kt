package com.openpos.gateway.config

import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.ext.Provider

/**
 * 受信リクエストから内部ヘッダーをストリップするフィルター。
 * 外部クライアントが X-Staff-Id / X-Staff-Role を偽装するのを防ぐため、
 * 他のフィルター（AuthFilter 等）より先に実行する。
 */
@Provider
@Priority(0)
@ApplicationScoped
class InternalHeaderStripFilter : ContainerRequestFilter {
    companion object {
        private val INTERNAL_HEADERS = listOf("X-Staff-Id", "X-Staff-Role")
    }

    override fun filter(requestContext: ContainerRequestContext) {
        INTERNAL_HEADERS.forEach { header ->
            requestContext.headers.remove(header)
        }
    }
}
