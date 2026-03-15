package com.openpos.pos.repository

import com.openpos.pos.entity.ReservationEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ReservationRepository : PanacheRepositoryBase<ReservationEntity, UUID> {
    fun listByStoreId(
        storeId: UUID,
        page: Page,
    ): List<ReservationEntity> =
        find("storeId = ?1", Sort.descending("createdAt"), storeId)
            .page(page)
            .list()

    fun countByStoreId(storeId: UUID): Long = count("storeId = ?1", storeId)

    fun listByStoreIdAndStatus(
        storeId: UUID,
        status: String,
        page: Page,
    ): List<ReservationEntity> =
        find("storeId = ?1 AND status = ?2", Sort.descending("createdAt"), storeId, status)
            .page(page)
            .list()

    fun countByStoreIdAndStatus(
        storeId: UUID,
        status: String,
    ): Long = count("storeId = ?1 AND status = ?2", storeId, status)
}
