package com.openpos.gateway.config

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.MultivaluedHashMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class InternalHeaderStripFilterTest {
    private val requestContext: ContainerRequestContext = mock()
    private val headers = MultivaluedHashMap<String, String>()
    private val filter = InternalHeaderStripFilter()

    @BeforeEach
    fun setUp() {
        whenever(requestContext.headers).thenReturn(headers)
    }

    @Test
    fun `X-Staff-Id ヘッダーがストリップされる`() {
        headers.putSingle("X-Staff-Id", "spoofed-id")
        headers.putSingle("X-Staff-Role", "OWNER")

        filter.filter(requestContext)

        assertNull(headers.getFirst("X-Staff-Id"))
        assertNull(headers.getFirst("X-Staff-Role"))
    }

    @Test
    fun `X-Organization-Id はストリップされない`() {
        headers.putSingle("X-Organization-Id", "org-123")
        headers.putSingle("X-Staff-Id", "spoofed-id")

        filter.filter(requestContext)

        assertEquals("org-123", headers.getFirst("X-Organization-Id"))
        assertNull(headers.getFirst("X-Staff-Id"))
    }

    @Test
    fun `内部ヘッダーが存在しない場合もエラーにならない`() {
        headers.putSingle("Authorization", "Bearer token")

        filter.filter(requestContext)

        assertEquals("Bearer token", headers.getFirst("Authorization"))
    }
}
