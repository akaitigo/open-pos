package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import openpos.store.v1.AnonymizeCustomerDataResponse
import openpos.store.v1.AnonymizeStaffDataResponse
import openpos.store.v1.DataProcessingConsent
import openpos.store.v1.DeleteOrganizationDataResponse
import openpos.store.v1.GetConsentResponse
import openpos.store.v1.RecordConsentResponse
import openpos.store.v1.StoreServiceGrpc
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class GdprResourceTest {
    private val stub: StoreServiceGrpc.StoreServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val resource =
        GdprResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
        }

    private val orgId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
    }

    @Nested
    inner class DeleteOrganizationData {
        @Test
        fun `テナントデータ削除で200を返す`() {
            // Arrange
            whenever(stub.deleteOrganizationData(any())).thenReturn(
                DeleteOrganizationDataResponse
                    .newBuilder()
                    .setSuccess(true)
                    .setMessage("テナントデータの削除が完了しました")
                    .build(),
            )

            // Act
            val response = resource.deleteOrganizationData(orgId)

            // Assert
            assertEquals(200, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any>
            assertEquals(true, entity["success"])
            assertEquals("テナントデータの削除が完了しました", entity["message"])
        }
    }

    @Nested
    inner class AnonymizeStaff {
        @Test
        fun `スタッフ匿名化で件数を返す`() {
            // Arrange
            whenever(stub.anonymizeStaffData(any())).thenReturn(
                AnonymizeStaffDataResponse
                    .newBuilder()
                    .setAnonymizedCount(5)
                    .build(),
            )

            // Act
            val result = resource.anonymizeStaffData(orgId)

            // Assert
            assertEquals(5, result["anonymizedCount"])
        }
    }

    @Nested
    inner class AnonymizeCustomer {
        @Test
        fun `顧客匿名化で件数を返す`() {
            // Arrange
            whenever(stub.anonymizeCustomerData(any())).thenReturn(
                AnonymizeCustomerDataResponse
                    .newBuilder()
                    .setAnonymizedCount(3)
                    .build(),
            )

            // Act
            val result = resource.anonymizeCustomerData(orgId)

            // Assert
            assertEquals(3, result["anonymizedCount"])
        }
    }

    @Nested
    inner class RecordConsent {
        @Test
        fun `同意記録で同意情報を返す`() {
            // Arrange
            val consentId = UUID.randomUUID().toString()
            whenever(stub.recordConsent(any())).thenReturn(
                RecordConsentResponse
                    .newBuilder()
                    .setConsent(
                        DataProcessingConsent
                            .newBuilder()
                            .setId(consentId)
                            .setOrganizationId(orgId)
                            .setConsentType("DATA_PROCESSING")
                            .setGranted(true)
                            .setGrantedAt("2026-03-20T00:00:00Z")
                            .setPolicyVersion("1.0")
                            .setCreatedAt("2026-03-20T00:00:00Z")
                            .setUpdatedAt("2026-03-20T00:00:00Z")
                            .build(),
                    ).build(),
            )

            // Act
            val body =
                RecordConsentBody(
                    consentType = "DATA_PROCESSING",
                    granted = true,
                    policyVersion = "1.0",
                )
            val response = resource.recordConsent(orgId, body)

            // Assert
            assertEquals(200, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals("DATA_PROCESSING", entity["consentType"])
            assertEquals(true, entity["granted"])
            assertEquals("1.0", entity["policyVersion"])
        }
    }

    @Nested
    inner class GetConsents {
        @Test
        fun `同意一覧を取得する`() {
            // Arrange
            whenever(stub.getConsent(any())).thenReturn(
                GetConsentResponse
                    .newBuilder()
                    .addConsents(
                        DataProcessingConsent
                            .newBuilder()
                            .setId(UUID.randomUUID().toString())
                            .setOrganizationId(orgId)
                            .setConsentType("DATA_PROCESSING")
                            .setGranted(true)
                            .setPolicyVersion("1.0")
                            .setCreatedAt("2026-03-20T00:00:00Z")
                            .setUpdatedAt("2026-03-20T00:00:00Z")
                            .build(),
                    ).build(),
            )

            // Act
            val result = resource.getConsents(orgId)

            // Assert
            assertEquals(1, result.size)
            assertEquals("DATA_PROCESSING", result[0]["consentType"])
            assertEquals(true, result[0]["granted"])
        }

        @Test
        fun `同意が存在しない場合は空リストを返す`() {
            // Arrange
            whenever(stub.getConsent(any())).thenReturn(
                GetConsentResponse.newBuilder().build(),
            )

            // Act
            val result = resource.getConsents(orgId)

            // Assert
            assertTrue(result.isEmpty())
        }
    }
}
