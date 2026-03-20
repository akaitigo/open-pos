package com.openpos.product.entity

import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * RLS（Row Level Security）網羅性検証テスト。
 * product-service の全エンティティが organizationFilter を持つことを検証する。
 */
class RlsCoverageTest {
    @ParameterizedTest(name = "{0} には organizationFilter が定義されている")
    @ValueSource(
        classes = [
            ProductEntity::class,
            ProductVariantEntity::class,
            ProductBundleEntity::class,
            ProductBundleItemEntity::class,
            CategoryEntity::class,
            TaxRateEntity::class,
            TaxRateScheduleEntity::class,
            DiscountEntity::class,
            CouponEntity::class,
            TimeSaleEntity::class,
            ReceiptTemplateEntity::class,
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
                ProductEntity::class.java,
                ProductVariantEntity::class.java,
                ProductBundleEntity::class.java,
                ProductBundleItemEntity::class.java,
                CategoryEntity::class.java,
                TaxRateEntity::class.java,
                TaxRateScheduleEntity::class.java,
                DiscountEntity::class.java,
                CouponEntity::class.java,
                TimeSaleEntity::class.java,
                ReceiptTemplateEntity::class.java,
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
