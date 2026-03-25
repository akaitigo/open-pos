package com.openpos.gateway.config

import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * PIN 認証成功後のセッショントークンを生成するサービス。
 * ORY Hydra の OIDC トークンとは別の、内部セッション用 JWT。
 *
 * 署名鍵は `openpos.session.jwt.secret` で設定し、
 * ローテーション時は環境変数を更新してローリングリスタートする。
 */
@ApplicationScoped
class SessionTokenService {
    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "http://localhost:14444/")
    lateinit var issuer: String

    /**
     * セッション JWT 署名用の HMAC 秘密鍵（Base64 エンコード）。
     * SmallRye JWT 共有鍵から分離した専用鍵。
     * 本番では GCP Secret Manager 経由で注入すること。
     */
    @ConfigProperty(name = "openpos.session.jwt.secret")
    lateinit var jwtSecret: String

    companion object {
        /** セッショントークンの有効期限: 8時間（1シフト分） */
        private val TOKEN_DURATION: Duration = Duration.ofHours(8)
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    private fun signingKey(): SecretKey =
        SecretKeySpec(
            java.util.Base64
                .getDecoder()
                .decode(jwtSecret),
            HMAC_ALGORITHM,
        )

    /**
     * スタッフ用セッショントークンを生成する。
     *
     * @param staffId スタッフ UUID
     * @param staffRole ロール（OWNER, MANAGER, CASHIER）
     * @param storeId 店舗 UUID
     * @param organizationId テナント UUID
     * @return JWT 文字列（HS256 署名）
     */
    fun generateToken(
        staffId: String,
        staffRole: String,
        storeId: String,
        organizationId: String,
    ): String =
        Jwt
            .issuer(issuer)
            .audience("openpos-session")
            .subject(staffId)
            .claim("role", staffRole)
            .claim("store_id", storeId)
            .claim("organization_id", organizationId)
            .claim("token_type", "session")
            .expiresIn(TOKEN_DURATION)
            .sign(signingKey())
}
