package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.paginatedResponse
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import openpos.common.v1.DateRange
import openpos.common.v1.PaginationRequest
import openpos.pos.v1.ListJournalEntriesRequest
import openpos.pos.v1.PosServiceGrpc
import org.eclipse.microprofile.faulttolerance.Timeout

@Path("/api/journal")
@Blocking
@Timeout(30000)
class JournalResource {
    @Inject
    @GrpcClient("pos-service")
    lateinit var stub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
        @QueryParam("type") type: String?,
        @QueryParam("startDate") startDate: String?,
        @QueryParam("endDate") endDate: String?,
    ): Map<String, Any> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            ListJournalEntriesRequest
                .newBuilder()
                .setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).apply {
                    type?.let { setType(it) }
                    if (startDate != null || endDate != null) {
                        setDateRange(
                            DateRange
                                .newBuilder()
                                .apply {
                                    startDate?.let { setStart(it) }
                                    endDate?.let { setEnd(it) }
                                }.build(),
                        )
                    }
                }.build()
        val response = grpc.withTenant(stub).listJournalEntries(request)
        return paginatedResponse(
            data = response.entriesList.map { it.toMap() },
            pagination = response.pagination,
        )
    }
}
