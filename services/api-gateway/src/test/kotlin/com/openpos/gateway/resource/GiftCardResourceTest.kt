package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import openpos.pos.v1.CreateGiftCardResponse
import openpos.pos.v1.GiftCard
import openpos.pos.v1.ListGiftCardsResponse
import openpos.pos.v1.PosServiceGrpc
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class GiftCardResourceTest {
    private val stub: PosServiceGrpc.PosServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val tenantContext = TenantContext()
    private val resource =
        GiftCardResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    private val orgId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        tenantContext.staffRole = null
        tenantContext.organizationId = UUID.fromString(orgId)
        whenever(grpc.withTenant(stub)).thenReturn(stub)
    }

    private fun buildGiftCard(): GiftCard =
        GiftCard
            .newBuilder()
            .setId(UUID.randomUUID().toString())
            .setOrganizationId(orgId)
            .setCode("ABCD-1234-EFGH-5678")
            .setInitialAmount(500000)
            .setBalance(500000)
            .setStatus("ACTIVE")
            .setIssuedAt("2026-01-01T00:00:00Z")
            .setCreatedAt("2026-01-01T00:00:00Z")
            .setUpdatedAt("2026-01-01T00:00:00Z")
            .build()

    @Nested
    inner class ListGiftCards {
        @Test
        fun `一覧取得でデータを返す`() {
            val card = buildGiftCard()
            whenever(stub.listGiftCards(any())).thenReturn(
                ListGiftCardsResponse.newBuilder().addGiftCards(card).build(),
            )

            val result = resource.list(page = 1, pageSize = 20)

            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<*>
            assertEquals(1, data.size)
        }
    }

    @Nested
    inner class CreateGiftCard {
        @Test
        fun `ギフトカード発行で201を返す`() {
            val card = buildGiftCard()
            whenever(stub.createGiftCard(any())).thenReturn(
                CreateGiftCardResponse.newBuilder().setGiftCard(card).build(),
            )
            val body = CreateGiftCardBody(initialAmount = 500000)

            val response = resource.create(body)

            assertEquals(201, response.status)
        }

        @Test
        fun `STAFF権限で発行するとForbiddenException`() {
            tenantContext.staffRole = "STAFF"
            val body = CreateGiftCardBody(initialAmount = 500000)

            assertThrows<ForbiddenException> {
                resource.create(body)
            }
        }

        @Test
        fun `MANAGER権限で発行可能`() {
            tenantContext.staffRole = "MANAGER"
            val card = buildGiftCard()
            whenever(stub.createGiftCard(any())).thenReturn(
                CreateGiftCardResponse.newBuilder().setGiftCard(card).build(),
            )
            val body = CreateGiftCardBody(initialAmount = 500000)

            val response = resource.create(body)

            assertEquals(201, response.status)
        }
    }

    @Nested
    inner class RedeemGiftCard {
        @Test
        fun `0円利用でIllegalArgumentException`() {
            val body = RedeemGiftCardBody(amount = 0)
            assertThrows<IllegalArgumentException> {
                resource.redeem("ABCD-1234-EFGH-5678", body)
            }
        }

        @Test
        fun `負の金額利用でIllegalArgumentException`() {
            val body = RedeemGiftCardBody(amount = -100)
            assertThrows<IllegalArgumentException> {
                resource.redeem("ABCD-1234-EFGH-5678", body)
            }
        }
    }
}
