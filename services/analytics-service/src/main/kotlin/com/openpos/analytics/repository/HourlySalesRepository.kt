package com.openpos.analytics.repository

import com.openpos.analytics.entity.HourlySalesEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

/**
 * 時間帯別売上リポジトリ。
 * 店舗×日×時間帯の売上集計データを検索・更新する。
 */
@ApplicationScoped
class HourlySalesRepository : PanacheRepositoryBase<HourlySalesEntity, UUID> {
    /**
     * 店舗×日×時間帯で売上レコードを検索する。
     */
    fun findByStoreAndDateAndHour(
        storeId: UUID,
        saleDate: LocalDate,
        hour: Int,
    ): HourlySalesEntity? = find("storeId = ?1 AND saleDate = ?2 AND hour = ?3", storeId, saleDate, hour).firstResult()

    /**
     * 店舗×日の時間帯別売上を取得する（時間帯昇順）。
     */
    fun listByStoreAndDate(
        storeId: UUID,
        saleDate: LocalDate,
    ): List<HourlySalesEntity> =
        find(
            "storeId = ?1 AND saleDate = ?2",
            Sort.ascending("hour"),
            storeId,
            saleDate,
        ).list()

    /**
     * 店舗×日付範囲の時間帯別売上を集計する（時間帯でグルーピング）。
     * 複数日にまたがる場合は同じ時間帯の売上を合算する。
     */
    fun listByStoreAndDateRange(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<HourlySalesEntity> =
        find(
            "storeId = ?1 AND saleDate >= ?2 AND saleDate <= ?3",
            Sort.ascending("hour"),
            storeId,
            startDate,
            endDate,
        ).list()
}
