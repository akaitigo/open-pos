package com.openpos.gateway.config

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ClientInterceptors
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.stub.AbstractBlockingStub
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class GrpcClientHelper {
    companion object {
        private val IDEMPOTENCY_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
    }

    // Keep the call sites stable while tenant propagation is handled centrally by TenantClientInterceptor.
    fun <T : AbstractBlockingStub<T>> withTenant(stub: T): T = stub

    /**
     * gRPC metadata に Idempotency-Key を付与したスタブを返す。
     * 決済確定（finalize）リクエストの重複排除に使用する。
     */
    fun <T : AbstractBlockingStub<T>> withIdempotencyKey(
        stub: T,
        idempotencyKey: String,
    ): T {
        val interceptor =
            object : ClientInterceptor {
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
                            headers.put(IDEMPOTENCY_KEY, idempotencyKey)
                            super.start(responseListener, headers)
                        }
                    }
            }
        val channel = ClientInterceptors.intercept(stub.channel, interceptor)
        return stub.withChannel(channel)
    }
}
