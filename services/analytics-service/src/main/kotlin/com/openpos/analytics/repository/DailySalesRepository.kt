package com.openpos.analytics.repository

import com.openpos.analytics.entity.DailySalesEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

/**
 * 日次売上リポジトリ。
 * 店舗×日の売上集計データを検索・更新する。
 */
@ApplicationScoped
class DailySalesRepository : PanacheRepositoryBase<DailySalesEntity, UUID> {
    /**
     * 店舗×日で売上レコードを検索する。
     */
    fun findByStoreAndDate(
        storeId: UUID,
        date: LocalDate,
    ): DailySalesEntity? = find("storeId = ?1 AND date = ?2", storeId, date).firstResult()

    /**
     * 店舗の日付範囲で日次売上を取得する（日付昇順）。
     */
    fun listByStoreAndDateRange(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DailySalesEntity> =
        find(
            "storeId = ?1 AND date >= ?2 AND date <= ?3",
            Sort.ascending("date"),
            storeId,
            startDate,
            endDate,
        ).list()

    /**
     * 特定日の全店舗の日次売上を取得する。
     */
    fun findBySaleDate(date: LocalDate): List<DailySalesEntity> = list("date = ?1", date)

    /**
     * 日付範囲で全店舗の日次売上を取得する（日付昇順）。
     */
    fun listByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DailySalesEntity> =
        find(
            "date >= ?1 AND date <= ?2",
            Sort.ascending("date"),
            startDate,
            endDate,
        ).list()
}
