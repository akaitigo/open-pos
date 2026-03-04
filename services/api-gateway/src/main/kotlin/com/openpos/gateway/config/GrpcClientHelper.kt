package com.openpos.gateway.config

import io.grpc.Metadata
import io.grpc.stub.AbstractBlockingStub
import io.grpc.stub.MetadataUtils
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
        val metadata = Metadata()
        tenantContext.organizationId?.let { orgId ->
            metadata.put(ORG_ID_KEY, orgId.toString())
        }
        return MetadataUtils.attachHeaders(stub, metadata)
    }
}
