package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import openpos.common.v1.DateRange
import openpos.pos.v1.GetTaxReportRequest
import openpos.pos.v1.PosServiceGrpc
import org.eclipse.microprofile.faulttolerance.Timeout

@Path("/api/reports/tax")
@Blocking
@Timeout(30000)
class TaxReportResource {
    @Inject
    @GrpcClient("pos-service")
    lateinit var stub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    fun getTaxReport(
        @QueryParam("storeId") storeId: String,
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
    ): Map<String, Any> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            GetTaxReportRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(startDate)
                        .setEnd(endDate)
                        .build(),
                ).build()
        val response = grpc.withTenant(stub).getTaxReport(request)
        return mapOf(
            "data" to
                response.itemsList.map { item ->
                    mapOf(
                        "taxRateName" to item.taxRateName,
                        "taxRatePercentage" to item.taxRatePercentage,
                        "isReduced" to item.isReduced,
                        "taxableAmount" to item.taxableAmount,
                        "taxAmount" to item.taxAmount,
                        "transactionCount" to item.transactionCount,
                    )
                },
        )
    }
}
