package com.openpos.pos.entity

import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * RLS（Row Level Security）網羅性検証テスト。
 * pos-service の全エンティティが organizationFilter を持つことを検証する。
 */
class RlsCoverageTest {
    @ParameterizedTest(name = "{0} には organizationFilter が定義されている")
    @ValueSource(
        classes = [
            TransactionEntity::class,
            TransactionItemEntity::class,
            TransactionDiscountEntity::class,
            PaymentEntity::class,
            TaxSummaryEntity::class,
            DrawerEntity::class,
            SettlementEntity::class,
            JournalEntryEntity::class,
            DiscountReasonEntity::class,
            ReservationEntity::class,
            OutboxEventEntity::class,
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
                TransactionEntity::class.java,
                TransactionItemEntity::class.java,
                TransactionDiscountEntity::class.java,
                PaymentEntity::class.java,
                TaxSummaryEntity::class.java,
                DrawerEntity::class.java,
                SettlementEntity::class.java,
                JournalEntryEntity::class.java,
                DiscountReasonEntity::class.java,
                ReservationEntity::class.java,
                OutboxEventEntity::class.java,
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
}
