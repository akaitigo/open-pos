package com.openpos.gateway.config

/**
 * 個人情報（PII）のマスキングユーティリティ。
 * ログ出力時に使用し、平文での PII 記録を防止する。
 */
object DataMaskingUtil {
    /**
     * メールアドレスをマスクする。
     * 例: tanaka@example.com → ta***@example.com
     */
    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank()) return "***"
        val atIndex = email.indexOf('@')
        if (atIndex <= 0) return "***"
        val localPart = email.substring(0, atIndex)
        val domain = email.substring(atIndex)
        val visible = localPart.take(2.coerceAtMost(localPart.length))
        return "$visible***$domain"
    }

    /**
     * 電話番号をマスクする。
     * 例: 090-1234-5678 → 090-****-5678
     */
    fun maskPhone(phone: String?): String {
        if (phone.isNullOrBlank()) return "***"
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 4) return "***"
        return "${digits.take(3)}-****-${digits.takeLast(4)}"
    }

    /**
     * PIN をマスクする（常に固定文字列）。
     */
    fun maskPin(
        @Suppress("UNUSED_PARAMETER") pin: String?,
    ): String = "****"

    /**
     * 一般的な文字列を先頭 N 文字だけ表示してマスクする。
     * 例: "田中太郎" → "田中**"
     */
    fun maskName(
        name: String?,
        visibleChars: Int = 2,
    ): String {
        if (name.isNullOrBlank()) return "***"
        if (name.length <= visibleChars) return name
        return name.take(visibleChars) + "**"
    }

    /**
     * Map から PII フィールドをマスクした新しい Map を返す。
     * ログ出力用。
     */
    fun maskMapForLogging(data: Map<String, Any?>): Map<String, Any?> =
        data.mapValues { (key, value) ->
            when {
                value == null -> null
                key.equals("email", ignoreCase = true) -> maskEmail(value.toString())
                key.equals("phone", ignoreCase = true) -> maskPhone(value.toString())
                key.equals("pin", ignoreCase = true) -> maskPin(value.toString())
                key.equals("pinHash", ignoreCase = true) -> "***"
                key.equals("pin_hash", ignoreCase = true) -> "***"
                else -> value
            }
        }
}
