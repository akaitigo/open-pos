package com.openpos.store.grpc

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fasterxml.jackson.databind.ObjectMapper
import com.openpos.store.config.DataMaskingUtil
import com.openpos.store.entity.CustomerEntity
import com.openpos.store.entity.DataProcessingConsentEntity
import com.openpos.store.entity.OrganizationEntity
import com.openpos.store.entity.StaffEntity
import com.openpos.store.entity.StampCardEntity
import com.openpos.store.entity.StoreEntity
import com.openpos.store.entity.TerminalEntity
import com.openpos.store.service.AuditLogService
import com.openpos.store.service.CustomerService
import com.openpos.store.service.GdprService
import com.openpos.store.service.OrganizationService
import com.openpos.store.service.StaffService
import com.openpos.store.service.StampCardService
import com.openpos.store.service.StoreService
import com.openpos.store.service.TerminalService
import io.grpc.Status
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import openpos.common.v1.PaginationResponse
import openpos.store.v1.AnonymizeCustomerDataRequest
import openpos.store.v1.AnonymizeCustomerDataResponse
import openpos.store.v1.AnonymizeStaffDataRequest
import openpos.store.v1.AnonymizeStaffDataResponse
import openpos.store.v1.AuthenticateByPinRequest
import openpos.store.v1.AuthenticateByPinResponse
import openpos.store.v1.CreateCustomerRequest
import openpos.store.v1.CreateCustomerResponse
import openpos.store.v1.CreateOrganizationRequest
import openpos.store.v1.CreateOrganizationResponse
import openpos.store.v1.CreateStaffRequest
import openpos.store.v1.CreateStaffResponse
import openpos.store.v1.CreateStoreRequest
import openpos.store.v1.CreateStoreResponse
import openpos.store.v1.Customer
import openpos.store.v1.CustomerTier
import openpos.store.v1.DataProcessingConsent
import openpos.store.v1.DeleteOrganizationDataRequest
import openpos.store.v1.DeleteOrganizationDataResponse
import openpos.store.v1.EarnPointsRequest
import openpos.store.v1.EarnPointsResponse
import openpos.store.v1.GetConsentRequest
import openpos.store.v1.GetConsentResponse
import openpos.store.v1.GetCustomerRequest
import openpos.store.v1.GetCustomerResponse
import openpos.store.v1.GetOrganizationRequest
import openpos.store.v1.GetOrganizationResponse
import openpos.store.v1.GetStaffRequest
import openpos.store.v1.GetStaffResponse
import openpos.store.v1.GetStoreRequest
import openpos.store.v1.GetStoreResponse
import openpos.store.v1.ListCustomersRequest
import openpos.store.v1.ListCustomersResponse
import openpos.store.v1.ListStaffRequest
import openpos.store.v1.ListStaffResponse
import openpos.store.v1.ListStoresRequest
import openpos.store.v1.ListStoresResponse
import openpos.store.v1.ListTerminalsRequest
import openpos.store.v1.ListTerminalsResponse
import openpos.store.v1.Organization
import openpos.store.v1.RecordConsentRequest
import openpos.store.v1.RecordConsentResponse
import openpos.store.v1.RedeemPointsRequest
import openpos.store.v1.RedeemPointsResponse
import openpos.store.v1.RegisterTerminalRequest
import openpos.store.v1.RegisterTerminalResponse
import openpos.store.v1.Staff
import openpos.store.v1.StaffRole
import openpos.store.v1.Store
import openpos.store.v1.StoreServiceGrpc
import openpos.store.v1.Terminal
import openpos.store.v1.UpdateCustomerRequest
import openpos.store.v1.UpdateCustomerResponse
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
    private val logger =
        org.jboss.logging.Logger
            .getLogger(StoreGrpcService::class.java)

    @Inject
    lateinit var organizationService: OrganizationService

    @Inject
    lateinit var storeService: StoreService

    @Inject
    lateinit var terminalService: TerminalService

    @Inject
    lateinit var staffService: StaffService

    @Inject
    lateinit var auditLogService: AuditLogService

    @Inject
    lateinit var gdprService: GdprService

    @Inject
    lateinit var customerService: CustomerService

    @Inject
    lateinit var stampCardService: StampCardService

    @Inject
    lateinit var tenantHelper: GrpcTenantHelper

    @Inject
    lateinit var objectMapper: ObjectMapper

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
        tenantHelper.setupTenantContextWithoutFilter()
        val requestedId = request.id.toUUID()
        val callerId = requireNotNull(tenantHelper.currentOrganizationId()) { "organizationId is not set" }
        if (requestedId != callerId) {
            throw Status.PERMISSION_DENIED
                .withDescription("Cannot access organization belonging to another tenant")
                .asRuntimeException()
        }
        val entity =
            organizationService.findById(requestedId)
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
        tenantHelper.setupTenantContextWithoutFilter()
        val requestedId = request.id.toUUID()
        val callerId = requireNotNull(tenantHelper.currentOrganizationId()) { "organizationId is not set" }
        if (requestedId != callerId) {
            throw Status.PERMISSION_DENIED
                .withDescription("Cannot update organization belonging to another tenant")
                .asRuntimeException()
        }
        val entity =
            organizationService.update(
                id = requestedId,
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
        val pageSize = if (request.hasPagination() && request.pagination.pageSize > 0) request.pagination.pageSize.coerceAtMost(100) else 20
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
                isActive =
                    when {
                        request.hasIsActiveValue() -> request.isActiveValue.value
                        request.isActive -> true
                        else -> null
                    },
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
        auditLogService.log(
            organizationId = entity.organizationId,
            action = "CREATE",
            entityType = "STAFF",
            entityId = entity.id.toString(),
            details =
                objectMapper.writeValueAsString(
                    mapOf("name" to DataMaskingUtil.maskName(entity.name), "role" to entity.role, "storeId" to entity.storeId.toString()),
                ),
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
        try {
            tenantHelper.setupTenantContext()
            val page = if (request.hasPagination()) request.pagination.page - 1 else 0
            val pageSize =
                if (request.hasPagination() &&
                    request.pagination.pageSize > 0
                ) {
                    request.pagination.pageSize.coerceAtMost(100)
                } else {
                    20
                }
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
        } catch (e: Exception) {
            throw mapToGrpcException(e)
        }
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
                isActive =
                    when {
                        request.hasIsActiveValue() -> request.isActiveValue.value
                        request.isActive -> true
                        else -> null
                    },
            ) ?: throw Status.NOT_FOUND.withDescription("Staff not found: ${request.id}").asRuntimeException()
        auditLogService.log(
            organizationId = entity.organizationId,
            action = "UPDATE",
            entityType = "STAFF",
            entityId = entity.id.toString(),
            details =
                objectMapper.writeValueAsString(
                    mapOf("name" to DataMaskingUtil.maskName(entity.name), "role" to entity.role),
                ),
        )
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

        // 監査ログ: ログイン試行を記録（PII なし）
        result.staff?.let { staff ->
            val action = if (result.success) "LOGIN_SUCCESS" else "LOGIN_FAILURE"
            auditLogService.log(
                organizationId = staff.organizationId,
                staffId = staff.id,
                action = action,
                entityType = "STAFF",
                entityId = staff.id.toString(),
                details =
                    objectMapper.writeValueAsString(
                        mapOf("reason" to (result.reason ?: "OK")),
                    ),
            )
        }

        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()
    }

    // === Customer ===

    override fun createCustomer(
        request: CreateCustomerRequest,
        responseObserver: io.grpc.stub.StreamObserver<CreateCustomerResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val entity =
            customerService.create(
                name = request.name,
                email = request.email.ifBlank { null },
                phone = request.phone.ifBlank { null },
                notes = request.notes.ifBlank { null },
            )
        responseObserver.onNext(
            CreateCustomerResponse.newBuilder().setCustomer(entity.toCustomerProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun getCustomer(
        request: GetCustomerRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetCustomerResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            customerService.findById(request.id.toUUID())
                ?: throw Status.NOT_FOUND.withDescription("Customer not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            GetCustomerResponse.newBuilder().setCustomer(entity.toCustomerProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun listCustomers(
        request: ListCustomersRequest,
        responseObserver: io.grpc.stub.StreamObserver<ListCustomersResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val page = if (request.hasPagination()) request.pagination.page - 1 else 0
        val pageSize =
            if (request.hasPagination() && request.pagination.pageSize > 0) {
                request.pagination.pageSize.coerceAtMost(100)
            } else {
                20
            }
        val search = request.search.ifBlank { null }
        val (customers, totalCount) = customerService.list(page, pageSize, search)
        val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0
        responseObserver.onNext(
            ListCustomersResponse
                .newBuilder()
                .addAllCustomers(customers.map { it.toCustomerProto() })
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

    override fun updateCustomer(
        request: UpdateCustomerRequest,
        responseObserver: io.grpc.stub.StreamObserver<UpdateCustomerResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            customerService.update(
                id = request.id.toUUID(),
                name = request.name.ifBlank { null },
                email = request.email.ifBlank { null },
                phone = request.phone.ifBlank { null },
                notes = request.notes.ifBlank { null },
            ) ?: throw Status.NOT_FOUND.withDescription("Customer not found: ${request.id}").asRuntimeException()
        responseObserver.onNext(
            UpdateCustomerResponse.newBuilder().setCustomer(entity.toCustomerProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun earnPoints(
        request: EarnPointsRequest,
        responseObserver: io.grpc.stub.StreamObserver<EarnPointsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val customerId = request.customerId.toUUID()
        val transactionId = request.transactionId.ifBlank { null }?.let { it.toUUID() }
        try {
            val earnedPoints = customerService.earnPoints(customerId, request.transactionTotal, transactionId)
            val customer =
                customerService.findById(customerId)
                    ?: throw Status.NOT_FOUND.withDescription("Customer not found: ${request.customerId}").asRuntimeException()
            responseObserver.onNext(
                EarnPointsResponse
                    .newBuilder()
                    .setEarnedPoints(earnedPoints)
                    .setCustomer(customer.toCustomerProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: IllegalArgumentException) {
            throw Status.NOT_FOUND.withDescription(e.message).asRuntimeException()
        }
    }

    override fun redeemPoints(
        request: RedeemPointsRequest,
        responseObserver: io.grpc.stub.StreamObserver<RedeemPointsResponse>,
    ) {
        if (request.points <= 0) {
            throw Status.INVALID_ARGUMENT
                .withDescription("points must be positive, got: ${request.points}")
                .asRuntimeException()
        }
        tenantHelper.setupTenantContext()
        val customerId = request.customerId.toUUID()
        val transactionId = request.transactionId.ifBlank { null }?.let { it.toUUID() }
        try {
            val success = customerService.redeemPoints(customerId, request.points, transactionId)
            val customer =
                customerService.findById(customerId)
                    ?: throw Status.NOT_FOUND.withDescription("Customer not found: ${request.customerId}").asRuntimeException()
            responseObserver.onNext(
                RedeemPointsResponse
                    .newBuilder()
                    .setSuccess(success)
                    .setCustomer(customer.toCustomerProto())
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: IllegalArgumentException) {
            throw Status.NOT_FOUND.withDescription(e.message).asRuntimeException()
        }
    }

    // === GDPR / 個人情報保護 ===

    override fun deleteOrganizationData(
        request: DeleteOrganizationDataRequest,
        responseObserver: io.grpc.stub.StreamObserver<DeleteOrganizationDataResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val orgId = request.organizationId.toUUID()
        val callerId = requireNotNull(tenantHelper.currentOrganizationId()) { "organizationId is not set" }
        if (orgId != callerId) {
            throw Status.PERMISSION_DENIED
                .withDescription("Cannot delete data belonging to another tenant")
                .asRuntimeException()
        }
        try {
            gdprService.deleteOrganizationData(orgId)
            auditLogService.log(
                organizationId = orgId,
                action = "GDPR_DELETE",
                entityType = "ORGANIZATION",
                entityId = orgId.toString(),
                details = """{"action":"organization_data_deleted"}""",
            )
            responseObserver.onNext(
                DeleteOrganizationDataResponse
                    .newBuilder()
                    .setSuccess(true)
                    .setMessage("テナントデータの削除が完了しました")
                    .build(),
            )
        } catch (e: IllegalArgumentException) {
            throw Status.NOT_FOUND.withDescription(e.message).asRuntimeException()
        }
        responseObserver.onCompleted()
    }

    override fun anonymizeStaffData(
        request: AnonymizeStaffDataRequest,
        responseObserver: io.grpc.stub.StreamObserver<AnonymizeStaffDataResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val orgId = request.organizationId.toUUID()
        val callerId = requireNotNull(tenantHelper.currentOrganizationId()) { "organizationId is not set" }
        if (orgId != callerId) {
            throw Status.PERMISSION_DENIED
                .withDescription("Cannot anonymize data belonging to another tenant")
                .asRuntimeException()
        }
        val count = gdprService.anonymizeStaffData(orgId)
        auditLogService.log(
            organizationId = orgId,
            action = "GDPR_ANONYMIZE",
            entityType = "STAFF",
            details = """{"anonymized_count":$count}""",
        )
        responseObserver.onNext(
            AnonymizeStaffDataResponse.newBuilder().setAnonymizedCount(count).build(),
        )
        responseObserver.onCompleted()
    }

    override fun anonymizeCustomerData(
        request: AnonymizeCustomerDataRequest,
        responseObserver: io.grpc.stub.StreamObserver<AnonymizeCustomerDataResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val orgId = request.organizationId.toUUID()
        val callerId = requireNotNull(tenantHelper.currentOrganizationId()) { "organizationId is not set" }
        if (orgId != callerId) {
            throw Status.PERMISSION_DENIED
                .withDescription("Cannot anonymize data belonging to another tenant")
                .asRuntimeException()
        }
        val count = gdprService.anonymizeCustomerData(orgId)
        auditLogService.log(
            organizationId = orgId,
            action = "GDPR_ANONYMIZE",
            entityType = "CUSTOMER",
            details = """{"anonymized_count":$count}""",
        )
        responseObserver.onNext(
            AnonymizeCustomerDataResponse.newBuilder().setAnonymizedCount(count).build(),
        )
        responseObserver.onCompleted()
    }

    override fun recordConsent(
        request: RecordConsentRequest,
        responseObserver: io.grpc.stub.StreamObserver<RecordConsentResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val orgId = request.organizationId.toUUID()
        val callerId = requireNotNull(tenantHelper.currentOrganizationId()) { "organizationId is not set" }
        if (orgId != callerId) {
            throw Status.PERMISSION_DENIED
                .withDescription("Cannot manage consent for another tenant")
                .asRuntimeException()
        }
        val entity =
            gdprService.recordConsent(
                organizationId = orgId,
                consentType = request.consentType,
                granted = request.granted,
                grantedBy = request.grantedBy.ifBlank { null }?.let { UUID.fromString(it) },
                policyVersion = request.policyVersion,
                ipAddress = request.ipAddress.ifBlank { null },
            )
        responseObserver.onNext(
            RecordConsentResponse.newBuilder().setConsent(entity.toConsentProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun getConsent(
        request: GetConsentRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetConsentResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val orgId = request.organizationId.toUUID()
        val callerId = requireNotNull(tenantHelper.currentOrganizationId()) { "organizationId is not set" }
        if (orgId != callerId) {
            throw Status.PERMISSION_DENIED
                .withDescription("Cannot view consent for another tenant")
                .asRuntimeException()
        }
        val consents =
            if (request.consentType.isBlank()) {
                gdprService.getConsents(orgId)
            } else {
                val consent = gdprService.getConsent(orgId, request.consentType)
                if (consent != null) listOf(consent) else emptyList()
            }
        responseObserver.onNext(
            GetConsentResponse
                .newBuilder()
                .addAllConsents(consents.map { it.toConsentProto() })
                .build(),
        )
        responseObserver.onCompleted()
    }

    // === Mapper Extensions ===

    private fun CustomerEntity.toCustomerProto(): Customer =
        Customer
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setName(name)
            .setEmail(email.orEmpty())
            .setPhone(phone.orEmpty())
            .setPoints(points)
            .setTier(tier.toProtoTier())
            .setIsActive(isActive)
            .setNotes(notes.orEmpty())
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    private fun String.toProtoTier(): CustomerTier =
        when (this) {
            "REGULAR" -> CustomerTier.CUSTOMER_TIER_REGULAR
            "SILVER" -> CustomerTier.CUSTOMER_TIER_SILVER
            "GOLD" -> CustomerTier.CUSTOMER_TIER_GOLD
            "VIP" -> CustomerTier.CUSTOMER_TIER_VIP
            else -> CustomerTier.CUSTOMER_TIER_UNSPECIFIED
        }

    private fun DataProcessingConsentEntity.toConsentProto(): DataProcessingConsent =
        DataProcessingConsent
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setConsentType(consentType)
            .setGranted(granted)
            .setGrantedAt(grantedAt?.toString().orEmpty())
            .setRevokedAt(revokedAt?.toString().orEmpty())
            .setPolicyVersion(policyVersion)
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

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

    // === StampCard ===

    override fun getStampCard(
        request: openpos.store.v1.GetStampCardRequest,
        responseObserver: io.grpc.stub.StreamObserver<openpos.store.v1.GetStampCardResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val card = stampCardService.getByCustomerId(java.util.UUID.fromString(request.customerId))
        if (card == null) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription("Stamp card not found for customer: ${request.customerId}").asRuntimeException(),
            )
            return
        }
        responseObserver.onNext(
            openpos.store.v1.GetStampCardResponse
                .newBuilder()
                .setStampCard(card.toStampCardProto())
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun issueStampCard(
        request: openpos.store.v1.IssueStampCardRequest,
        responseObserver: io.grpc.stub.StreamObserver<openpos.store.v1.IssueStampCardResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val card =
            stampCardService.issue(
                customerId = java.util.UUID.fromString(request.customerId),
                maxStamps = if (request.maxStamps > 0) request.maxStamps else 10,
                rewardDescription = request.rewardDescription.ifBlank { null },
            )
        responseObserver.onNext(
            openpos.store.v1.IssueStampCardResponse
                .newBuilder()
                .setStampCard(card.toStampCardProto())
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun addStamp(
        request: openpos.store.v1.AddStampRequest,
        responseObserver: io.grpc.stub.StreamObserver<openpos.store.v1.AddStampResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val card = stampCardService.addStamp(java.util.UUID.fromString(request.customerId))
        responseObserver.onNext(
            openpos.store.v1.AddStampResponse
                .newBuilder()
                .setStampCard(card.toStampCardProto())
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun redeemStampReward(
        request: openpos.store.v1.RedeemStampRewardRequest,
        responseObserver: io.grpc.stub.StreamObserver<openpos.store.v1.RedeemStampRewardResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val card = stampCardService.redeemReward(java.util.UUID.fromString(request.customerId))
        responseObserver.onNext(
            openpos.store.v1.RedeemStampRewardResponse
                .newBuilder()
                .setStampCard(card.toStampCardProto())
                .build(),
        )
        responseObserver.onCompleted()
    }

    private fun StampCardEntity.toStampCardProto(): openpos.store.v1.StampCard =
        openpos.store.v1.StampCard
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setCustomerId(customerId.toString())
            .setStampCount(stampCount)
            .setMaxStamps(maxStamps)
            .apply { rewardDescription?.let { setRewardDescription(it) } }
            .setStatus(status)
            .setIssuedAt(issuedAt.toString())
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()
}
