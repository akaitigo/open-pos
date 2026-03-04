package com.openpos.product.repository

import com.openpos.product.entity.CouponEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * クーポンリポジトリ。
 */
@ApplicationScoped
class CouponRepository : PanacheRepositoryBase<CouponEntity, UUID> {
    /**
     * クーポンコードでクーポンを検索する（組織フィルター適用後に一意）。
     */
    fun findByCode(code: String): CouponEntity? = find("code = ?1", code).firstResult()
}
