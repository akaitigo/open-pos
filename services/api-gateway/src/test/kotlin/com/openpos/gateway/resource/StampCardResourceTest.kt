package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import openpos.store.v1.IssueStampCardResponse
import openpos.store.v1.StampCard
import openpos.store.v1.StoreServiceGrpc
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class StampCardResourceTest {
    private val stub: StoreServiceGrpc.StoreServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val tenantContext = TenantContext()
    private val resource =
        StampCardResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    private val orgId = UUID.randomUUID().toString()
    private val customerId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        tenantContext.staffRole = null
        tenantContext.organizationId = UUID.fromString(orgId)
        whenever(grpc.withTenant(stub)).thenReturn(stub)
    }

    private fun buildStampCard(): StampCard =
        StampCard
            .newBuilder()
            .setId(UUID.randomUUID().toString())
            .setOrganizationId(orgId)
            .setCustomerId(customerId)
            .setStampCount(0)
            .setMaxStamps(10)
            .setStatus("ACTIVE")
            .setIssuedAt("2026-01-01T00:00:00Z")
            .setCreatedAt("2026-01-01T00:00:00Z")
            .setUpdatedAt("2026-01-01T00:00:00Z")
            .build()

    @Nested
    inner class IssueStampCard {
        @Test
        fun `スタンプカード発行で201を返す`() {
            val card = buildStampCard()
            whenever(stub.issueStampCard(any())).thenReturn(
                IssueStampCardResponse.newBuilder().setStampCard(card).build(),
            )
            val body = IssueStampCardBody(customerId = customerId, maxStamps = 10)

            val response = resource.issue(body)

            assertEquals(201, response.status)
        }

        @Test
        fun `認証なし（dev）で発行可能`() {
            val card = buildStampCard()
            whenever(stub.issueStampCard(any())).thenReturn(
                IssueStampCardResponse.newBuilder().setStampCard(card).build(),
            )
            tenantContext.staffRole = null
            val body = IssueStampCardBody(customerId = customerId)

            val response = resource.issue(body)

            assertEquals(201, response.status)
        }
    }

    @Nested
    inner class RoleCheck {
        @Test
        fun `VIEWER権限で発行するとForbiddenException`() {
            tenantContext.staffRole = "VIEWER"
            val body = IssueStampCardBody(customerId = customerId)

            assertThrows<ForbiddenException> {
                resource.issue(body)
            }
        }
    }
}
