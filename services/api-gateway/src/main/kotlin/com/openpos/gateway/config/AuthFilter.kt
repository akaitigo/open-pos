package com.openpos.gateway.config

import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.jwt.JsonWebToken
import org.jboss.logging.Logger

/**
 * JWT 認証フィルター。
 * Authorization ヘッダーの Bearer トークンを検証し、
 * X-Staff-Id / X-Staff-Role ヘッダーを下流サービスに伝播する。
 *
 * openpos.auth.enabled=false で認証をバイパスできる（dev プロファイル用）。
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
class AuthFilter : ContainerRequestFilter {
    @Inject
    lateinit var jwt: JsonWebToken

    @ConfigProperty(name = "openpos.auth.enabled", defaultValue = "true")
    var authEnabled: Boolean = true

    @ConfigProperty(name = "openpos.auth.skip-paths", defaultValue = "/api/health,/q/")
    lateinit var skipPaths: String

    companion object {
        private val logger: Logger = Logger.getLogger(AuthFilter::class::class.java)
    }

    override fun filter(requestContext: ContainerRequestContext) {
        // OPTIONS は CORS プリフライト — 常にスキップ
        if (requestContext.method.equals("OPTIONS", ignoreCase = true)) {
            return
        }

        // 認証無効（dev プロファイル）
        if (!authEnabled) {
            return
        }

        // スキップパスのチェック
        val path = requestContext.uriInfo.path
        if (shouldSkipAuth(path)) {
            return
        }

        // Authorization ヘッダーの取得
        val authHeader = requestContext.getHeaderString("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ", ignoreCase = true)) {
            logger.debugf("Missing or invalid Authorization header for path: %s", path)
            requestContext.abortWith(
                Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity(mapOf("error" to "Unauthorized", "message" to "Missing or invalid Authorization header"))
                    .build(),
            )
            return
        }

        // SmallRye JWT が自動でトークンを検証・パースする。
        // トークンが無効なら jwt.name は null になる。
        val subject = jwt.subject
        if (subject.isNullOrBlank()) {
            logger.debug("JWT validation failed: no subject claim")
            requestContext.abortWith(
                Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity(mapOf("error" to "Unauthorized", "message" to "Invalid or expired token"))
                    .build(),
            )
            return
        }

        // スタッフ情報をヘッダーに設定（下流サービス向け）
        requestContext.headers.putSingle("X-Staff-Id", subject)

        // role クレーム必須: 欠落時は認可不可
        val role = jwt.getClaim<String>("role")
        if (role.isNullOrBlank()) {
            logger.debug("JWT missing required 'role' claim")
            requestContext.abortWith(
                Response
                    .status(Response.Status.FORBIDDEN)
                    .entity(mapOf("error" to "Forbidden", "message" to "Token missing required 'role' claim"))
                    .build(),
            )
            return
        }
        requestContext.headers.putSingle("X-Staff-Role", role)
        tenantContext.staffRole = role

        // organization_id クレーム必須: 欠落時はテナント境界検証不可
        val jwtOrgId = jwt.getClaim<String>("organization_id")
        if (jwtOrgId.isNullOrBlank()) {
            logger.debug("JWT missing required 'organization_id' claim")
            requestContext.abortWith(
                Response
                    .status(Response.Status.FORBIDDEN)
                    .entity(mapOf("error" to "Forbidden", "message" to "Token missing required 'organization_id' claim"))
                    .build(),
            )
            return
        }

        // テナント境界検証: JWT の organization_id クレームと X-Organization-Id ヘッダーを照合
        if (tenantContext.organizationId != null) {
            try {
                val jwtOrgUuid = java.util.UUID.fromString(jwtOrgId)
                if (jwtOrgUuid != tenantContext.organizationId) {
                    logger.warnf(
                        "Tenant boundary violation: JWT org=%s, request org=%s, staff=%s",
                        jwtOrgId,
                        tenantContext.organizationId,
                        subject,
                    )
                    requestContext.abortWith(
                        Response
                            .status(Response.Status.FORBIDDEN)
                            .entity(
                                mapOf(
                                    "error" to "Forbidden",
                                    "message" to "Organization ID mismatch between token and request",
                                ),
                            ).build(),
                    )
                    return
                }
            } catch (_: IllegalArgumentException) {
                logger.warnf("Invalid organization_id claim in JWT: %s", jwtOrgId)
                requestContext.abortWith(
                    Response
                        .status(Response.Status.FORBIDDEN)
                        .entity(mapOf("error" to "Forbidden", "message" to "Invalid organization_id in token"))
                        .build(),
                )
                return
            }
        }
    }

    @Inject
    lateinit var tenantContext: TenantContext

    private fun shouldSkipAuth(path: String): Boolean {
        val normalizedPath = path.removePrefix("/")
        return skipPaths.split(",").any { pattern ->
            val trimmed = pattern.trim()
            if (trimmed.endsWith("{id}/authenticate")) {
                // パターンマッチ: /api/staff/{id}/authenticate
                normalizedPath.matches(Regex("api/staff/[^/]+/authenticate"))
            } else {
                normalizedPath.startsWith(trimmed.removePrefix("/"))
            }
        }
    }
}
