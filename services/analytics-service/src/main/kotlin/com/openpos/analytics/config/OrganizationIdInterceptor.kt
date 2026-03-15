package com.openpos.analytics.config

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.quarkus.grpc.GlobalInterceptor
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger
import java.util.UUID

/**
 * gRPC ServerInterceptor。
 * metadata の x-organization-id から organizationId を取得し、
 * gRPC Context に設定する。
 * x-request-id の抽出・生成と MDC への設定も行う。
 * メタデータのサニタイゼーション（trim、インジェクション検出）を実施する。
 */
@ApplicationScoped
@GlobalInterceptor
class OrganizationIdInterceptor : ServerInterceptor {
    companion object {
        private val LOG: Logger = Logger.getLogger(OrganizationIdInterceptor::class.java)

        private val ORGANIZATION_ID_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-organization-id", Metadata.ASCII_STRING_MARSHALLER)

        private val REQUEST_ID_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER)

        val ORGANIZATION_ID_CTX_KEY: Context.Key<UUID> =
            Context.key("organizationId")

        val REQUEST_ID_CTX_KEY: Context.Key<String> =
            Context.key("requestId")

        private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        private val INJECTION_PATTERN = Regex("[\\r\\n\\x00]")
    }

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        // x-request-id の抽出または生成
        val requestId = extractOrGenerateRequestId(headers)
        org.jboss.logging.MDC
            .put("requestId", requestId)

        val methodName = call.methodDescriptor.bareMethodName
        val orgIdStr = headers.get(ORGANIZATION_ID_KEY)

        if (orgIdStr.isNullOrBlank()) {
            call.close(
                Status.UNAUTHENTICATED.withDescription("x-organization-id metadata is required"),
                Metadata(),
            )
            return object : ServerCall.Listener<ReqT>() {}
        }

        // サニタイゼーション: trim + インジェクション検出
        val sanitizedOrgId = orgIdStr.trim()
        if (INJECTION_PATTERN.containsMatchIn(sanitizedOrgId)) {
            LOG.warnf("Suspicious x-organization-id metadata detected: method=%s", methodName)
            call.close(
                Status.INVALID_ARGUMENT.withDescription("x-organization-id contains invalid characters"),
                Metadata(),
            )
            return object : ServerCall.Listener<ReqT>() {}
        }

        // UUID 形式バリデーション
        if (!UUID_REGEX.matches(sanitizedOrgId)) {
            call.close(
                Status.INVALID_ARGUMENT.withDescription("x-organization-id must be a valid UUID"),
                Metadata(),
            )
            return object : ServerCall.Listener<ReqT>() {}
        }

        val orgId =
            try {
                UUID.fromString(sanitizedOrgId)
            } catch (e: IllegalArgumentException) {
                call.close(
                    Status.INVALID_ARGUMENT.withDescription("x-organization-id must be a valid UUID"),
                    Metadata(),
                )
                return object : ServerCall.Listener<ReqT>() {}
            }

        val ctx =
            Context
                .current()
                .withValue(ORGANIZATION_ID_CTX_KEY, orgId)
                .withValue(REQUEST_ID_CTX_KEY, requestId)
        return Contexts.interceptCall(ctx, call, headers, next)
    }

    private fun extractOrGenerateRequestId(headers: Metadata): String {
        val rawRequestId = headers.get(REQUEST_ID_KEY)
        if (rawRequestId.isNullOrBlank()) {
            return UUID.randomUUID().toString()
        }
        val sanitized = rawRequestId.trim()
        // x-request-id のインジェクション検出
        if (INJECTION_PATTERN.containsMatchIn(sanitized)) {
            LOG.warnf("Suspicious x-request-id metadata detected: length=%d", rawRequestId.length)
            return UUID.randomUUID().toString()
        }
        // x-request-id の長さ制限（過度に長い値を拒否）
        if (sanitized.length > 128) {
            LOG.warnf("x-request-id too long: length=%d", sanitized.length)
            return UUID.randomUUID().toString()
        }
        return sanitized
    }
}
