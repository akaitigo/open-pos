package com.openpos.store.entity

import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * RLS（Row Level Security）網羅性検証テスト。
 * store-service の全エンティティが organizationFilter を持つことを検証する。
 *
 * 対象外:
 * - OrganizationEntity: テナントルート（自身が organization_id を持たない）
 * - PlanEntity: テナント横断マスタデータ
 */
class RlsCoverageTest {
    @ParameterizedTest(name = "{0} には organizationFilter が定義されている")
    @ValueSource(
        classes = [
            StoreEntity::class,
            StaffEntity::class,
            TerminalEntity::class,
            CustomerEntity::class,
            ShiftEntity::class,
            AttendanceEntity::class,
            GiftCardEntity::class,
            FavoriteProductEntity::class,
            NotificationEntity::class,
            PointTransactionEntity::class,
            WebhookEntity::class,
            SystemSettingEntity::class,
            AuditLogEntity::class,
            SubscriptionEntity::class,
        ],
    )
    fun `エンティティに organizationFilter アノテーションが存在する`(entityClass: Class<*>) {
        val filterDef =
            entityClass.getAnnotation(FilterDef::class.java)
                ?: entityClass.superclass?.getAnnotation(FilterDef::class.java)
        val filter =
            entityClass.getAnnotation(Filter::class.java)
                ?: entityClass.superclass?.getAnnotation(Filter::class.java)

        assertNotNull(filterDef, "${entityClass.simpleName} に @FilterDef が未定義")
        assertNotNull(filter, "${entityClass.simpleName} に @Filter が未定義")
        assertTrue(
            filter!!.name == "organizationFilter",
            "${entityClass.simpleName} の @Filter 名が organizationFilter ではない: ${filter.name}",
        )
        assertTrue(
            filter.condition.contains("organization_id"),
            "${entityClass.simpleName} の @Filter 条件に organization_id が含まれていない",
        )
    }

    @Test
    fun `organizationId フィールドが全テナントエンティティに存在する`() {
        val tenantEntities =
            listOf(
                StoreEntity::class.java,
                StaffEntity::class.java,
                TerminalEntity::class.java,
                CustomerEntity::class.java,
                ShiftEntity::class.java,
                AttendanceEntity::class.java,
                GiftCardEntity::class.java,
                FavoriteProductEntity::class.java,
                NotificationEntity::class.java,
                PointTransactionEntity::class.java,
                WebhookEntity::class.java,
                SystemSettingEntity::class.java,
                AuditLogEntity::class.java,
                SubscriptionEntity::class.java,
            )

        for (entityClass in tenantEntities) {
            val hasOrgId =
                generateSequence<Class<*>>(entityClass) { it.superclass }
                    .any { cls ->
                        cls.declaredFields.any { it.name == "organizationId" }
                    }
            assertTrue(hasOrgId, "${entityClass.simpleName} に organizationId フィールドが未定義")
        }
    }

    @Test
    fun `テナント横断エンティティに organizationFilter が定義されていないことを確認する`() {
        // OrganizationEntity と PlanEntity はテナント横断のため、
        // BaseEntity を継承していないことを確認する。
        val crossTenantEntities =
            listOf(
                OrganizationEntity::class.java,
                PlanEntity::class.java,
            )

        for (entityClass in crossTenantEntities) {
            assertTrue(
                entityClass.superclass != BaseEntity::class.java,
                "${entityClass.simpleName} は BaseEntity を継承してはならない（テナント横断エンティティ）",
            )
        }
    }
}
