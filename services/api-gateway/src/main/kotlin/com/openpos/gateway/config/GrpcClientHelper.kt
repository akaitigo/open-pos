package com.openpos.gateway.config

import io.grpc.stub.AbstractBlockingStub
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.TimeUnit

@ApplicationScoped
class GrpcClientHelper {
    companion object {
        /** gRPC 呼び出しのデフォルトタイムアウト（秒） */
        private const val GRPC_DEADLINE_SECONDS = 5L
    }

    // Keep the call sites stable while tenant propagation is handled centrally by TenantClientInterceptor.
    // Deadline を設定して、下流サービスの遅延によるカスケード障害を防止する。
    fun <T : AbstractBlockingStub<T>> withTenant(stub: T): T = stub.withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
}
