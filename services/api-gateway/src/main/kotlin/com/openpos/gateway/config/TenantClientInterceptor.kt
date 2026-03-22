package com.openpos.gateway.config

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.quarkus.grpc.GlobalInterceptor
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
@GlobalInterceptor
class TenantClientInterceptor : ClientInterceptor {
    @Inject
    lateinit var tenantContext: TenantContext

    companion object {
        private val ORG_ID_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-organization-id", Metadata.ASCII_STRING_MARSHALLER)
        private val REQUEST_ID_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER)
    }

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> =
        object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions),
        ) {
            override fun start(
                responseListener: Listener<RespT>,
                headers: Metadata,
            ) {
                tenantContext.organizationId?.let { orgId ->
                    headers.put(ORG_ID_KEY, orgId.toString())
                }
                // Correlation ID を gRPC metadata に伝播
                val requestId =
                    org.jboss.logging.MDC
                        .get(CorrelationIdFilter.MDC_KEY)
                if (requestId != null) {
                    headers.put(REQUEST_ID_KEY, requestId.toString())
                }
                super.start(responseListener, headers)
            }
        }
}
