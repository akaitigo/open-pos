package com.openpos.store.grpc

import com.openpos.store.entity.SystemSettingEntity
import com.openpos.store.service.SystemSettingService
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import openpos.store.v1.DeleteSystemSettingRequest
import openpos.store.v1.DeleteSystemSettingResponse
import openpos.store.v1.GetSystemSettingRequest
import openpos.store.v1.GetSystemSettingResponse
import openpos.store.v1.ListSystemSettingsRequest
import openpos.store.v1.ListSystemSettingsResponse
import openpos.store.v1.SystemSetting
import openpos.store.v1.SystemSettingServiceGrpc
import openpos.store.v1.UpsertSystemSettingRequest
import openpos.store.v1.UpsertSystemSettingResponse

@GrpcService
@Blocking
class SystemSettingGrpcService : SystemSettingServiceGrpc.SystemSettingServiceImplBase() {
    @Inject
    lateinit var systemSettingService: SystemSettingService

    @Inject
    lateinit var tenantHelper: GrpcTenantHelper

    override fun getSystemSetting(
        request: GetSystemSettingRequest,
        responseObserver: StreamObserver<GetSystemSettingResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            systemSettingService.getByKey(request.key)
                ?: throw Status.NOT_FOUND.withDescription("Setting not found: ${request.key}").asRuntimeException()
        responseObserver.onNext(
            GetSystemSettingResponse.newBuilder().setSetting(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun listSystemSettings(
        request: ListSystemSettingsRequest,
        responseObserver: StreamObserver<ListSystemSettingsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val settings = systemSettingService.listAll()
        responseObserver.onNext(
            ListSystemSettingsResponse
                .newBuilder()
                .addAllSettings(settings.map { it.toProto() })
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun upsertSystemSetting(
        request: UpsertSystemSettingRequest,
        responseObserver: StreamObserver<UpsertSystemSettingResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val entity =
            systemSettingService.upsert(
                key = request.key,
                value = request.value,
                description = request.description.ifBlank { null },
            )
        responseObserver.onNext(
            UpsertSystemSettingResponse.newBuilder().setSetting(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun deleteSystemSetting(
        request: DeleteSystemSettingRequest,
        responseObserver: StreamObserver<DeleteSystemSettingResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val deleted = systemSettingService.delete(request.key)
        if (!deleted) {
            throw Status.NOT_FOUND.withDescription("Setting not found: ${request.key}").asRuntimeException()
        }
        responseObserver.onNext(
            DeleteSystemSettingResponse.newBuilder().setSuccess(true).build(),
        )
        responseObserver.onCompleted()
    }

    private fun SystemSettingEntity.toProto(): SystemSetting =
        SystemSetting
            .newBuilder()
            .setKey(key)
            .setValue(value)
            .setDescription(description.orEmpty())
            .build()
}
