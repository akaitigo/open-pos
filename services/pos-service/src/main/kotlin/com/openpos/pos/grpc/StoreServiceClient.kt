package com.openpos.pos.grpc

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.quarkus.grpc.GrpcClient
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import openpos.common.v1.PaginationRequest
import openpos.store.v1.GetOrganizationRequest
import openpos.store.v1.ListStaffRequest
import openpos.store.v1.StoreServiceGrpc
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * store-service の gRPC クライアント。
 * 組織情報（インボイス登録番号等）を取得する。
 */
@ApplicationScoped
class StoreServiceClient {
    @Inject
    @GrpcClient("store-service")
    lateinit var channel: Channel

    companion object {
        /** gRPC 呼び出しのデフォルトタイムアウト（秒） */
        private const val GRPC_DEADLINE_SECONDS = 5L
    }

    /**
     * 組織のインボイス登録番号を取得する。
     * 未設定の場合は null を返す。
     */
    fun getInvoiceNumber(organizationId: UUID): String? {
        val stub =
            StoreServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .withInterceptors(TenantHeaderInterceptor(organizationId))

        val response =
            stub.getOrganization(
                GetOrganizationRequest.newBuilder().setId(organizationId.toString()).build(),
            )
        val invoiceNumber = response.organization.invoiceNumber
        return invoiceNumber.ifBlank { null }
    }

    fun getStaffNameMap(
        organizationId: UUID,
        storeId: UUID,
    ): Map<UUID, String> {
        val stub =
            StoreServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .withInterceptors(TenantHeaderInterceptor(organizationId))

        val response =
            stub.listStaff(
                ListStaffRequest
                    .newBuilder()
                    .setStoreId(storeId.toString())
                    .setPagination(
                        PaginationRequest
                            .newBuilder()
                            .setPage(1)
                            .setPageSize(100)
                            .build(),
                    ).build(),
            )
        return response.staffList.associate { staff ->
            UUID.fromString(staff.id) to staff.name
        }
    }

    private class TenantHeaderInterceptor(
        private val organizationId: UUID,
    ) : ClientInterceptor {
        override fun <ReqT, RespT> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel,
        ): ClientCall<ReqT, RespT> =
            object :
                ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions),
                ) {
                override fun start(
                    responseListener: Listener<RespT>,
                    headers: Metadata,
                ) {
                    headers.put(
                        Metadata.Key.of("x-organization-id", Metadata.ASCII_STRING_MARSHALLER),
                        organizationId.toString(),
                    )
                    super.start(responseListener, headers)
                }
            }
    }
}
