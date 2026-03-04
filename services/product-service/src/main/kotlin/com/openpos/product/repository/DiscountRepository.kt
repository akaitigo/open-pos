package com.openpos.product.repository

import com.openpos.product.entity.DiscountEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.UUID

/**
 * 割引リポジトリ。
 */
@ApplicationScoped
class DiscountRepository : PanacheRepositoryBase<DiscountEntity, UUID> {
    /**
     * 有効かつ期間内の割引のみ取得する。
     */
    fun findActiveAndValid(now: Instant): List<DiscountEntity> =
        list(
            "isActive = true AND (validFrom IS NULL OR validFrom <= ?1) AND (validUntil IS NULL OR validUntil >= ?1) ORDER BY name ASC",
            now,
        )

    /**
     * 全割引を取得する。
     */
    fun findAllOrdered(): List<DiscountEntity> = list("ORDER BY name ASC")
}
