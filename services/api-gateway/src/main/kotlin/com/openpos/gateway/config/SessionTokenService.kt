package com.openpos.gateway.config

import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration

/**
 * PIN 認証成功後のセッショントークンを生成するサービス。
 * ORY Hydra の OIDC トークンとは別の、内部セッション用 JWT。
 */
@ApplicationScoped
class SessionTokenService {
    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "http://localhost:14444/")
    lateinit var issuer: String

    companion object {
        /** セッショントークンの有効期限: 8時間（1シフト分） */
        private val TOKEN_DURATION: Duration = Duration.ofHours(8)
    }

    /**
     * スタッフ用セッショントークンを生成する。
     *
     * @param staffId スタッフ UUID
     * @param staffRole ロール（OWNER, MANAGER, CASHIER）
     * @param storeId 店舗 UUID
     * @param organizationId テナント UUID
     * @return JWT 文字列
     */
    fun generateToken(
        staffId: String,
        staffRole: String,
        storeId: String,
        organizationId: String,
    ): String =
        Jwt
            .issuer(issuer)
            .subject(staffId)
            .claim("role", staffRole)
            .claim("store_id", storeId)
            .claim("organization_id", organizationId)
            .claim("token_type", "session")
            .expiresIn(TOKEN_DURATION)
            .sign()
}
