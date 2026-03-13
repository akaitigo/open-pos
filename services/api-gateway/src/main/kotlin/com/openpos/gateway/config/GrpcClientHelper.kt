package com.openpos.gateway.config

import io.grpc.stub.AbstractBlockingStub
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class GrpcClientHelper {
    // Keep the call sites stable while tenant propagation is handled centrally by TenantClientInterceptor.
    fun <T : AbstractBlockingStub<T>> withTenant(stub: T): T = stub
}
