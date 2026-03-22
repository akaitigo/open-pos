package com.openpos.gateway.config

import jakarta.enterprise.context.RequestScoped
import java.util.UUID

@RequestScoped
class TenantContext {
    var organizationId: UUID? = null
    var staffRole: String? = null

    fun requireRole(vararg allowedRoles: String) {
        // 認証無効（dev/test）時は staffRole が null — ロールチェックをスキップ
        val role = staffRole ?: return
        if (role !in allowedRoles) {
            throw ForbiddenException("Role '$role' is not allowed. Required: ${allowedRoles.joinToString()}")
        }
    }
}

class ForbiddenException(
    message: String,
) : RuntimeException(message)
