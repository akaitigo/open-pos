package com.openpos.inventory.config

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.quarkus.grpc.GlobalInterceptor
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * gRPC ServerInterceptor。
 * metadata の x-organization-id から organizationId を取得し、
 * gRPC Context および OrganizationIdHolder に設定する。
 */
@ApplicationScoped
@GlobalInterceptor
class OrganizationIdInterceptor : ServerInterceptor {
    companion object {
        private val ORGANIZATION_ID_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-organization-id", Metadata.ASCII_STRING_MARSHALLER)

        val ORGANIZATION_ID_CTX_KEY: Context.Key<UUID> =
            Context.key("organizationId")
    }

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val orgIdStr = headers.get(ORGANIZATION_ID_KEY)

        if (orgIdStr.isNullOrBlank()) {
            call.close(
                Status.UNAUTHENTICATED.withDescription("x-organization-id metadata is required"),
                Metadata(),
            )
            return object : ServerCall.Listener<ReqT>() {}
        }

        val orgId =
            try {
                UUID.fromString(orgIdStr)
            } catch (e: IllegalArgumentException) {
                call.close(
                    Status.INVALID_ARGUMENT.withDescription("x-organization-id must be a valid UUID"),
                    Metadata(),
                )
                return object : ServerCall.Listener<ReqT>() {}
            }

        val ctx = Context.current().withValue(ORGANIZATION_ID_CTX_KEY, orgId)
        return Contexts.interceptCall(ctx, call, headers, next)
    }
}
