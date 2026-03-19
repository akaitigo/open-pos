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
     * 特定日の指定テナントの日次売上を取得する。
     */
    fun findBySaleDate(
        date: LocalDate,
        organizationId: UUID,
    ): List<DailySalesEntity> = list("date = ?1 AND organizationId = ?2", date, organizationId)

    /**
     * 日付範囲で指定テナントの日次売上を取得する（日付昇順）。
     */
    fun listByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        organizationId: UUID,
    ): List<DailySalesEntity> =
        find(
            "date >= ?1 AND date <= ?2 AND organizationId = ?3",
            Sort.ascending("date"),
            startDate,
            endDate,
            organizationId,
        ).list()

    /**
     * 指定日に売上データが存在するテナントの organization_id 一覧を返す。
     */
    fun findDistinctOrganizationIdsBySaleDate(date: LocalDate): List<UUID> {
        @Suppress("UNCHECKED_CAST")
        return getEntityManager()
            .createQuery(
                "SELECT DISTINCT d.organizationId FROM DailySalesEntity d WHERE d.date = :date",
            ).setParameter("date", date)
            .resultList as List<UUID>
    }
}
