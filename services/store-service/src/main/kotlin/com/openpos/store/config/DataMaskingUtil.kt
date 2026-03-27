package com.openpos.store.config

/**
 * 個人情報（PII）のマスキングユーティリティ。
 * 監査ログ出力時に使用し、平文での PII 記録を防止する。
 */
object DataMaskingUtil {
    /**
     * 名前をマスクする。先頭 N 文字だけ表示してマスク。
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
}
