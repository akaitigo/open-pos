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
        saleDate: LocalDate,
    ): DailySalesEntity? = find("storeId = ?1 AND saleDate = ?2", storeId, saleDate).firstResult()

    /**
     * 店舗の日付範囲で日次売上を取得する（日付昇順）。
     */
    fun listByStoreAndDateRange(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DailySalesEntity> =
        find(
            "storeId = ?1 AND saleDate >= ?2 AND saleDate <= ?3",
            Sort.ascending("saleDate"),
            storeId,
            startDate,
            endDate,
        ).list()

    /**
     * 特定日の全店舗の日次売上を取得する。
     */
    fun findBySaleDate(saleDate: LocalDate): List<DailySalesEntity> = list("saleDate = ?1", saleDate)

    /**
     * 日付範囲で全店舗の日次売上を取得する（日付昇順）。
     */
    fun listByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DailySalesEntity> =
        find(
            "saleDate >= ?1 AND saleDate <= ?2",
            Sort.ascending("saleDate"),
            startDate,
            endDate,
        ).list()
}
