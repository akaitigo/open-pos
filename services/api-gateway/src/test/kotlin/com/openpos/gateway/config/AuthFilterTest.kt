package com.openpos.gateway.config

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.UriInfo
import org.eclipse.microprofile.jwt.JsonWebToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AuthFilterTest {
    private val jwt: JsonWebToken = mock()
    private val requestContext: ContainerRequestContext = mock()
    private val uriInfo: UriInfo = mock()
    private val headers = MultivaluedHashMap<String, String>()

    private val tenantContext = TenantContext()

    private val filter =
        AuthFilter().also {
            val jwtField = AuthFilter::class.java.getDeclaredField("jwt")
            jwtField.isAccessible = true
            jwtField.set(it, jwt)

            val authEnabledField = AuthFilter::class.java.getDeclaredField("authEnabled")
            authEnabledField.isAccessible = true
            authEnabledField.set(it, java.lang.Boolean.TRUE)

            val skipPathsField = AuthFilter::class.java.getDeclaredField("skipPaths")
            skipPathsField.isAccessible = true
            skipPathsField.set(it, "/api/health,/api/staff/{id}/authenticate,/q/")

            val tcField = AuthFilter::class.java.getDeclaredField("tenantContext")
            tcField.isAccessible = true
            tcField.set(it, tenantContext)
        }

    @BeforeEach
    fun setUp() {
        whenever(requestContext.uriInfo).thenReturn(uriInfo)
        whenever(requestContext.headers).thenReturn(headers)
        whenever(requestContext.method).thenReturn("GET")
    }

    @Nested
    inner class SkipAuth {
        @Test
        fun `OPTIONSリクエストは認証をスキップする`() {
            // Arrange
            whenever(requestContext.method).thenReturn("OPTIONS")

            // Act
            filter.filter(requestContext)

            // Assert
            assertNull(headers.getFirst("X-Staff-Id"))
            verify(requestContext, never()).abortWith(any())
        }

        @Test
        fun `ヘルスチェックパスは認証をスキップする`() {
            // Arrange
            whenever(uriInfo.path).thenReturn("api/health")

            // Act
            filter.filter(requestContext)

            // Assert
            assertNull(headers.getFirst("X-Staff-Id"))
            verify(requestContext, never()).abortWith(any())
        }

        @Test
        fun `PIN認証パスは認証をスキップする`() {
            // Arrange
            whenever(uriInfo.path).thenReturn("api/staff/550e8400-e29b-41d4-a716-446655440000/authenticate")

            // Act
            filter.filter(requestContext)

            // Assert
            assertNull(headers.getFirst("X-Staff-Id"))
            verify(requestContext, never()).abortWith(any())
        }
    }

    @Nested
    inner class AuthDisabled {
        @Test
        fun `認証無効時はリクエストを通過させる`() {
            // Arrange
            val disabledFilter =
                AuthFilter().also {
                    val jwtField = AuthFilter::class.java.getDeclaredField("jwt")
                    jwtField.isAccessible = true
                    jwtField.set(it, jwt)

                    val authEnabledField = AuthFilter::class.java.getDeclaredField("authEnabled")
                    authEnabledField.isAccessible = true
                    authEnabledField.set(it, java.lang.Boolean.FALSE)

                    val skipPathsField = AuthFilter::class.java.getDeclaredField("skipPaths")
                    skipPathsField.isAccessible = true
                    skipPathsField.set(it, "/api/health")
                }
            whenever(uriInfo.path).thenReturn("api/products")

            // Act
            disabledFilter.filter(requestContext)

            // Assert
            assertNull(headers.getFirst("X-Staff-Id"))
            verify(requestContext, never()).abortWith(any())
        }
    }

    @Nested
    inner class MissingToken {
        @Test
        fun `Authorizationヘッダーがない場合は401を返す`() {
            // Arrange
            whenever(uriInfo.path).thenReturn("api/products")
            whenever(requestContext.getHeaderString("Authorization")).thenReturn(null)

            // Act
            filter.filter(requestContext)

            // Assert
            verify(requestContext).abortWith(any())
        }

        @Test
        fun `Bearer以外のAuthorizationヘッダーは401を返す`() {
            // Arrange
            whenever(uriInfo.path).thenReturn("api/products")
            whenever(requestContext.getHeaderString("Authorization")).thenReturn("Basic abc123")

            // Act
            filter.filter(requestContext)

            // Assert
            verify(requestContext).abortWith(any())
        }
    }

    @Nested
    inner class ValidToken {
        @Test
        fun `有効なトークンでX-Staff-Idヘッダーが設定される`() {
            // Arrange
            val staffId = "550e8400-e29b-41d4-a716-446655440010"
            whenever(uriInfo.path).thenReturn("api/products")
            whenever(requestContext.getHeaderString("Authorization")).thenReturn("Bearer valid-token")
            whenever(jwt.subject).thenReturn(staffId)
            whenever(jwt.getClaim<String>("role")).thenReturn("MANAGER")

            // Act
            filter.filter(requestContext)

            // Assert
            verify(requestContext, never()).abortWith(any())
            assertEquals(staffId, headers.getFirst("X-Staff-Id"))
            assertEquals("MANAGER", headers.getFirst("X-Staff-Role"))
        }
    }

    @Nested
    inner class InvalidToken {
        @Test
        fun `subjectがnullのトークンは401を返す`() {
            // Arrange
            whenever(uriInfo.path).thenReturn("api/products")
            whenever(requestContext.getHeaderString("Authorization")).thenReturn("Bearer invalid-token")
            whenever(jwt.subject).thenReturn(null)

            // Act
            filter.filter(requestContext)

            // Assert
            verify(requestContext).abortWith(any())
        }
    }
}
