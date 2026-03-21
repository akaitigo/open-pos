package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import openpos.store.v1.AnonymizeCustomerDataRequest
import openpos.store.v1.AnonymizeStaffDataRequest
import openpos.store.v1.DeleteOrganizationDataRequest
import openpos.store.v1.GetConsentRequest
import openpos.store.v1.RecordConsentRequest
import openpos.store.v1.StoreServiceGrpc

/**
 * GDPR / 個人情報保護 REST エンドポイント。
 * テナントデータの削除・匿名化・同意管理を提供する。
 */
@Path("/api/organizations/{id}")
@Blocking
class GdprResource {
    @Inject
    @GrpcClient("store-service")
    lateinit var stub: StoreServiceGrpc.StoreServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    /**
     * テナントの全データを削除する（GDPR 忘れられる権利）。
     * PII を匿名化し、組織を論理削除する。
     */
    @DELETE
    @Path("/data")
    fun deleteOrganizationData(
        @PathParam("id") id: String,
    ): Response {
        val request =
            DeleteOrganizationDataRequest
                .newBuilder()
                .setOrganizationId(id)
                .build()
        val response = grpc.withTenant(stub).deleteOrganizationData(request)
        return Response
            .ok(
                mapOf(
                    "success" to response.success,
                    "message" to response.message,
                ),
            ).build()
    }

    /**
     * スタッフデータを匿名化する。
     */
    @POST
    @Path("/anonymize/staff")
    fun anonymizeStaffData(
        @PathParam("id") id: String,
    ): Map<String, Any> {
        val request =
            AnonymizeStaffDataRequest
                .newBuilder()
                .setOrganizationId(id)
                .build()
        val response = grpc.withTenant(stub).anonymizeStaffData(request)
        return mapOf("anonymizedCount" to response.anonymizedCount)
    }

    /**
     * 顧客データを匿名化する。
     */
    @POST
    @Path("/anonymize/customers")
    fun anonymizeCustomerData(
        @PathParam("id") id: String,
    ): Map<String, Any> {
        val request =
            AnonymizeCustomerDataRequest
                .newBuilder()
                .setOrganizationId(id)
                .build()
        val response = grpc.withTenant(stub).anonymizeCustomerData(request)
        return mapOf("anonymizedCount" to response.anonymizedCount)
    }

    /**
     * データ処理同意を記録する。
     */
    @POST
    @Path("/consent")
    fun recordConsent(
        @PathParam("id") id: String,
        body: RecordConsentBody,
    ): Response {
        val request =
            RecordConsentRequest
                .newBuilder()
                .setOrganizationId(id)
                .setConsentType(body.consentType)
                .setGranted(body.granted)
                .setPolicyVersion(body.policyVersion)
                .apply {
                    body.grantedBy?.let { setGrantedBy(it) }
                    body.ipAddress?.let { setIpAddress(it) }
                }.build()
        val response = grpc.withTenant(stub).recordConsent(request)
        val consent = response.consent
        return Response
            .ok(
                mapOf(
                    "id" to consent.id,
                    "organizationId" to consent.organizationId,
                    "consentType" to consent.consentType,
                    "granted" to consent.granted,
                    "grantedAt" to consent.grantedAt.ifEmpty { null },
                    "revokedAt" to consent.revokedAt.ifEmpty { null },
                    "policyVersion" to consent.policyVersion,
                    "createdAt" to consent.createdAt,
                    "updatedAt" to consent.updatedAt,
                ),
            ).build()
    }

    /**
     * データ処理同意を取得する。
     */
    @GET
    @Path("/consent")
    fun getConsents(
        @PathParam("id") id: String,
    ): List<Map<String, Any?>> {
        val request =
            GetConsentRequest
                .newBuilder()
                .setOrganizationId(id)
                .build()
        val response = grpc.withTenant(stub).getConsent(request)
        return response.consentsList.map { consent ->
            mapOf(
                "id" to consent.id,
                "organizationId" to consent.organizationId,
                "consentType" to consent.consentType,
                "granted" to consent.granted,
                "grantedAt" to consent.grantedAt.ifEmpty { null },
                "revokedAt" to consent.revokedAt.ifEmpty { null },
                "policyVersion" to consent.policyVersion,
                "createdAt" to consent.createdAt,
                "updatedAt" to consent.updatedAt,
            )
        }
    }
}

data class RecordConsentBody(
    val consentType: String,
    val granted: Boolean,
    val policyVersion: String,
    val grantedBy: String? = null,
    val ipAddress: String? = null,
)
