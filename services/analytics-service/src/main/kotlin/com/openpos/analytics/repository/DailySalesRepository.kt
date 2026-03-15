package com.openpos.analytics.repository

import com.openpos.analytics.entity.DailySalesEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

/**
 * 日次売上リポジトリ。
 */
@ApplicationScoped
class DailySalesRepository : PanacheRepositoryBase<DailySalesEntity, UUID> {
    fun findByStoreAndDateRange(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DailySalesEntity> =
        list(
            "storeId = ?1 AND date >= ?2 AND date <= ?3 ORDER BY date ASC",
            storeId,
            startDate,
            endDate,
        )
}
