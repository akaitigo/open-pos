package com.openpos.pos.entity.annotation

/**
 * PII（個人情報）フィールドを示すアノテーション。
 * GDPR / 個人情報保護法の対象フィールドをコード上で明示する。
 *
 * @param category PII のカテゴリ（例: "NAME", "EMAIL", "PHONE", "ADDRESS"）
 * @param description フィールドの用途説明
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class PersonalData(
    val category: String,
    val description: String = "",
)
