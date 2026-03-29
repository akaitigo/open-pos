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
import openpos.pos.v1.GetStaffSalesReportRequest
import openpos.pos.v1.PosServiceGrpc
import org.eclipse.microprofile.faulttolerance.Timeout

@Path("/api/reports")
@Blocking
@Timeout(30000)
class ReportResource {
    @Inject
    @GrpcClient("pos-service")
    lateinit var posStub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    @Path("/staff-sales")
    fun getStaffSalesReport(
        @QueryParam("storeId") storeId: String,
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
    ): Map<String, Any> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            GetStaffSalesReportRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(startDate)
                        .setEnd(endDate)
                        .build(),
                ).build()
        val response = grpc.withTenant(posStub).getStaffSalesReport(request)
        return mapOf(
            "data" to
                response.itemsList.map { item ->
                    mapOf(
                        "staffId" to item.staffId,
                        "staffName" to item.staffName,
                        "totalAmount" to item.totalAmount,
                        "transactionCount" to item.transactionCount,
                        "averageTransaction" to item.averageTransaction,
                    )
                },
        )
    }
}
