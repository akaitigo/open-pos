package com.openpos.store.grpc

import at.favre.lib.crypto.bcrypt.BCrypt
import com.openpos.store.entity.OrganizationEntity
import com.openpos.store.entity.StaffEntity
import com.openpos.store.entity.StoreEntity
import com.openpos.store.entity.TerminalEntity
import com.openpos.store.service.OrganizationService
import com.openpos.store.service.StaffService
import com.openpos.store.service.StoreService
import com.openpos.store.service.TerminalService
import io.grpc.Status
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import openpos.common.v1.PaginationResponse
import openpos.store.v1.AuthenticateByPinRequest
import openpos.store.v1.AuthenticateByPinResponse
import openpos.store.v1.CreateOrganizationRequest
import openpos.store.v1.CreateOrganizationResponse
import openpos.store.v1.CreateStaffRequest
import openpos.store.v1.CreateStaffResponse
import openpos.store.v1.CreateStoreRequest
import openpos.store.v1.CreateStoreResponse
import openpos.store.v1.GetOrganizationRequest
import openpos.store.v1.GetOrganizationResponse
import openpos.store.v1.GetStaffRequest
import openpos.store.v1.GetStaffResponse
import openpos.store.v1.GetStoreRequest
import openpos.store.v1.GetStoreResponse
import openpos.store.v1.ListStaffRequest
import openpos.store.v1.ListStaffResponse
import openpos.store.v1.ListStoresRequest
import openpos.store.v1.ListStoresResponse
import openpos.store.v1.ListTerminalsRequest
import openpos.store.v1.ListTerminalsResponse
import openpos.store.v1.Organization
import openpos.store.v1.RegisterTerminalRequest
import openpos.store.v1.RegisterTerminalResponse
import openpos.store.v1.Staff
import openpos.store.v1.StaffRole
import openpos.store.v1.Store
import openpos.store.v1.StoreServiceGrpc
import openpos.store.v1.Terminal
import openpos.store.v1.UpdateOrganizationRequest
import openpos.store.v1.UpdateOrganizationResponse
import openpos.store.v1.UpdateStaffRequest
import openpos.store.v1.UpdateStaffResponse
import openpos.store.v1.UpdateStoreRequest
import openpos.store.v1.UpdateStoreResponse
import openpos.store.v1.UpdateTerminalSyncRequest
import openpos.store.v1.UpdateTerminalSyncResponse
import java.util.UUID

@GrpcService
@Blocking
class StoreGrpcService : StoreServiceGrpc.StoreServiceImplBase() {
    @Inject
    lateinit var organizationService: OrganizationService

    @Inject
    lateinit var storeService: StoreService

    @Inject
    lateinit var terminalService: TerminalService

    @Inject
    lateinit var staffService: StaffService

    @Inject
    lateinit var tenantHelper: GrpcTenantHelper

    // === Organization ===

    override fun createOrganization(
        request: CreateOrganizationRequest,
        responseObserver: io.grpc.stub.StreamObserver<CreateOrganizationResponse>,
    ) {
        // CreateOrganization は新規テナント作成のため、x-organization-id 不要
        val entity =
            organizationService.create(
                name = request.name,
                businessType = request.businessType,
                invoiceNumber = request.invoiceNumber.ifBlank { null },
            )
        responseObserver.onNext(
            CreateOrganizationResponse.newBuilder().setOrganization(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun getOrganization(
        request: GetOrganizationRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetOrganizationResponse>,
    ) {
        val entity =
            organizationService.findById(request.id.toUUID())
                ?: throw Status.NOT_FOUND.withDescription("Organization not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            GetOrganizationResponse.newBuilder().setOrganization(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun updateOrganization(
        request: UpdateOrganizationRequest,
        responseObserver: io.grpc.stub.StreamObserver<UpdateOrganizationResponse>,
    ) {
        val entity =
            organizationService.update(
                id = request.id.toUUID(),
                name = request.name.ifBlank { null },
                businessType = request.businessType.ifBlank { null },
                invoiceNumber = request.invoiceNumber.ifBlank { null },
            ) ?: throw Status.NOT_FOUND.withDescription("Organization not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            UpdateOrganizationResponse.newBuilder().setOrganization(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    // === Store ===

    override fun createStore(
        request: CreateStoreRequest,
        responseObserver: io.grpc.stub.StreamObserver<CreateStoreResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val entity =
            storeService.create(
                name = request.name,
                address = request.address.ifBlank { null },
                phone = request.phone.ifBlank { null },
                timezone = request.timezone.ifBlank { "Asia/Tokyo" },
                settings = request.settings.ifBlank { "{}" },
            )
        responseObserver.onNext(
            CreateStoreResponse.newBuilder().setStore(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun getStore(
        request: GetStoreRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetStoreResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            storeService.findById(request.id.toUUID())
                ?: throw Status.NOT_FOUND.withDescription("Store not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            GetStoreResponse.newBuilder().setStore(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun listStores(
        request: ListStoresRequest,
        responseObserver: io.grpc.stub.StreamObserver<ListStoresResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val page = if (request.hasPagination()) request.pagination.page - 1 else 0
        val pageSize = if (request.hasPagination() && request.pagination.pageSize > 0) request.pagination.pageSize else 20
        val (stores, totalCount) = storeService.list(page, pageSize)
        val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0
        responseObserver.onNext(
            ListStoresResponse
                .newBuilder()
                .addAllStores(stores.map { it.toProto() })
                .setPagination(
                    PaginationResponse
                        .newBuilder()
                        .setPage(page + 1)
                        .setPageSize(pageSize)
                        .setTotalCount(totalCount)
                        .setTotalPages(totalPages)
                        .build(),
                ).build(),
        )
        responseObserver.onCompleted()
    }

    override fun updateStore(
        request: UpdateStoreRequest,
        responseObserver: io.grpc.stub.StreamObserver<UpdateStoreResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            storeService.update(
                id = request.id.toUUID(),
                name = request.name.ifBlank { null },
                address = request.address.ifBlank { null },
                phone = request.phone.ifBlank { null },
                timezone = request.timezone.ifBlank { null },
                settings = request.settings.ifBlank { null },
                isActive = if (request.isActive) true else null,
            ) ?: throw Status.NOT_FOUND.withDescription("Store not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            UpdateStoreResponse.newBuilder().setStore(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    // === Terminal ===

    override fun registerTerminal(
        request: RegisterTerminalRequest,
        responseObserver: io.grpc.stub.StreamObserver<RegisterTerminalResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val entity =
            terminalService.register(
                storeId = request.storeId.toUUID(),
                terminalCode = request.terminalCode,
                name = request.name,
            )
        responseObserver.onNext(
            RegisterTerminalResponse.newBuilder().setTerminal(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun listTerminals(
        request: ListTerminalsRequest,
        responseObserver: io.grpc.stub.StreamObserver<ListTerminalsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val terminals = terminalService.listByStoreId(request.storeId.toUUID())
        responseObserver.onNext(
            ListTerminalsResponse.newBuilder().addAllTerminals(terminals.map { it.toProto() }).build(),
        )
        responseObserver.onCompleted()
    }

    override fun updateTerminalSync(
        request: UpdateTerminalSyncRequest,
        responseObserver: io.grpc.stub.StreamObserver<UpdateTerminalSyncResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            terminalService.updateSync(request.terminalId.toUUID())
                ?: throw Status.NOT_FOUND.withDescription("Terminal not found: ${request.terminalId}").asRuntimeException()
        responseObserver.onNext(
            UpdateTerminalSyncResponse.newBuilder().setTerminal(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    // === Staff ===

    override fun createStaff(
        request: CreateStaffRequest,
        responseObserver: io.grpc.stub.StreamObserver<CreateStaffResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val pinHash = BCrypt.withDefaults().hashToString(12, request.pin.toCharArray())
        val entity =
            staffService.create(
                storeId = request.storeId.toUUID(),
                name = request.name,
                email = request.email,
                role = request.role.toDbRole(),
                pinHash = pinHash,
            )
        responseObserver.onNext(
            CreateStaffResponse.newBuilder().setStaff(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun getStaff(
        request: GetStaffRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetStaffResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            staffService.findById(request.id.toUUID())
                ?: throw Status.NOT_FOUND.withDescription("Staff not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            GetStaffResponse.newBuilder().setStaff(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun listStaff(
        request: ListStaffRequest,
        responseObserver: io.grpc.stub.StreamObserver<ListStaffResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val page = if (request.hasPagination()) request.pagination.page - 1 else 0
        val pageSize = if (request.hasPagination() && request.pagination.pageSize > 0) request.pagination.pageSize else 20
        val (staff, totalCount) = staffService.listByStoreId(request.storeId.toUUID(), page, pageSize)
        val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0
        responseObserver.onNext(
            ListStaffResponse
                .newBuilder()
                .addAllStaff(staff.map { it.toProto() })
                .setPagination(
                    PaginationResponse
                        .newBuilder()
                        .setPage(page + 1)
                        .setPageSize(pageSize)
                        .setTotalCount(totalCount)
                        .setTotalPages(totalPages)
                        .build(),
                ).build(),
        )
        responseObserver.onCompleted()
    }

    override fun updateStaff(
        request: UpdateStaffRequest,
        responseObserver: io.grpc.stub.StreamObserver<UpdateStaffResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val pinHash = if (request.pin.isNotBlank()) BCrypt.withDefaults().hashToString(12, request.pin.toCharArray()) else null
        val entity =
            staffService.update(
                id = request.id.toUUID(),
                name = request.name.ifBlank { null },
                email = request.email.ifBlank { null },
                role = if (request.role != StaffRole.STAFF_ROLE_UNSPECIFIED) request.role.toDbRole() else null,
                pinHash = pinHash,
                isActive = if (request.isActive) true else null,
            ) ?: throw Status.NOT_FOUND.withDescription("Staff not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            UpdateStaffResponse.newBuilder().setStaff(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun authenticateByPin(
        request: AuthenticateByPinRequest,
        responseObserver: io.grpc.stub.StreamObserver<AuthenticateByPinResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val result =
            staffService.authenticateByPin(
                staffId = request.staffId.toUUID(),
                storeId = request.storeId.toUUID(),
                pin = request.pin,
                pinVerifier = { pin, hash ->
                    BCrypt.verifyer().verify(pin.toCharArray(), hash).verified
                },
            )
        val builder = AuthenticateByPinResponse.newBuilder().setSuccess(result.success)
        result.staff?.let { builder.setStaff(it.toProto()) }
        result.reason?.let { builder.setReason(it) }
        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()
    }

    // === Mapper Extensions ===

    private fun OrganizationEntity.toProto(): Organization =
        Organization
            .newBuilder()
            .setId(id.toString())
            .setName(name)
            .setBusinessType(businessType)
            .setInvoiceNumber(invoiceNumber.orEmpty())
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    private fun StoreEntity.toProto(): Store =
        Store
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setName(name)
            .setAddress(address.orEmpty())
            .setPhone(phone.orEmpty())
            .setTimezone(timezone)
            .setSettings(settings)
            .setIsActive(isActive)
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    private fun TerminalEntity.toProto(): Terminal =
        Terminal
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setStoreId(storeId.toString())
            .setTerminalCode(terminalCode)
            .setName(name.orEmpty())
            .setIsActive(isActive)
            .setLastSyncAt(lastSyncAt?.toString().orEmpty())
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    private fun StaffEntity.toProto(): Staff =
        Staff
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setStoreId(storeId.toString())
            .setName(name)
            .setEmail(email.orEmpty())
            .setRole(role.toProtoRole())
            .setIsActive(isActive)
            .setFailedPinAttempts(pinFailedCount)
            .setIsLocked(
                pinLockedUntil?.let {
                    java.time.Instant
                        .now()
                        .isBefore(it)
                } ?: false,
            ).setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    // === Utility Extensions ===

    private fun String.toUUID(): UUID =
        try {
            UUID.fromString(this)
        } catch (e: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT.withDescription("Invalid UUID: $this").asRuntimeException()
        }

    private fun StaffRole.toDbRole(): String =
        when (this) {
            StaffRole.STAFF_ROLE_OWNER -> "OWNER"
            StaffRole.STAFF_ROLE_MANAGER -> "MANAGER"
            StaffRole.STAFF_ROLE_CASHIER -> "CASHIER"
            else -> "CASHIER"
        }

    private fun String.toProtoRole(): StaffRole =
        when (this) {
            "OWNER" -> StaffRole.STAFF_ROLE_OWNER
            "MANAGER" -> StaffRole.STAFF_ROLE_MANAGER
            "CASHIER" -> StaffRole.STAFF_ROLE_CASHIER
            else -> StaffRole.STAFF_ROLE_UNSPECIFIED
        }
}
