package com.openpos.gateway.config

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.stub.AbstractBlockingStub
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class GrpcClientHelper {
    @Inject
    lateinit var tenantContext: TenantContext

    companion object {
        private val ORG_ID_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-organization-id", Metadata.ASCII_STRING_MARSHALLER)
    }

    fun <T : AbstractBlockingStub<T>> withTenant(stub: T): T {
        val extraHeaders = Metadata()
        tenantContext.organizationId?.let { orgId ->
            extraHeaders.put(ORG_ID_KEY, orgId.toString())
        }
        val interceptor =
            object : ClientInterceptor {
                override fun <ReqT, RespT> interceptCall(
                    method: MethodDescriptor<ReqT, RespT>,
                    callOptions: CallOptions,
                    next: Channel,
                ): ClientCall<ReqT, RespT> =
                    object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                        override fun start(
                            responseListener: Listener<RespT>,
                            headers: Metadata,
                        ) {
                            headers.merge(extraHeaders)
                            super.start(responseListener, headers)
                        }
                    }
            }
        return stub.withInterceptors(interceptor)
    }
}
