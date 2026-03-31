package com.openpos.gateway.config

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.stub.AbstractBlockingStub
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.concurrent.TimeUnit

@ApplicationScoped
class GrpcClientHelper {
    @ConfigProperty(name = "openpos.grpc.deadline-seconds", defaultValue = "5")
    var grpcDeadlineSeconds: Long = 5

    companion object {
        private val IDEMPOTENCY_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-idempotency-key", Metadata.ASCII_STRING_MARSHALLER)
    }

    // Keep the call sites stable while tenant propagation is handled centrally by TenantClientInterceptor.
    // Deadline を設定して、下流サービスの遅延によるカスケード障害を防止する。
    fun <T : AbstractBlockingStub<T>> withTenant(stub: T): T = stub.withDeadlineAfter(grpcDeadlineSeconds, TimeUnit.SECONDS)

    /** 冪等性キーを gRPC メタデータとして付与する */
    fun <T : AbstractBlockingStub<T>> withIdempotencyKey(
        stub: T,
        key: String,
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
                            headers.put(IDEMPOTENCY_KEY, key)
                            super.start(responseListener, headers)
                        }
                    }
            }
        return stub.withInterceptors(interceptor)
    }
}
