package com.openpos.store.repository

import com.openpos.store.entity.FavoriteProductEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class FavoriteProductRepository : PanacheRepositoryBase<FavoriteProductEntity, UUID> {
    fun findByStaffId(staffId: UUID): List<FavoriteProductEntity> = list("staffId = ?1", Sort.ascending("sortOrder"), staffId)

    fun findByStaffAndProduct(
        staffId: UUID,
        productId: UUID,
    ): FavoriteProductEntity? = find("staffId = ?1 AND productId = ?2", staffId, productId).firstResult()

    fun deleteByStaffAndProduct(
        staffId: UUID,
        productId: UUID,
    ): Long = delete("staffId = ?1 AND productId = ?2", staffId, productId)
}
