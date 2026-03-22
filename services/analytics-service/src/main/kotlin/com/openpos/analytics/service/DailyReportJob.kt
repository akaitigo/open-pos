package com.openpos.analytics.service

import com.openpos.analytics.repository.DailySalesRepository
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.LocalDate
import org.jboss.logging.Logger

/**
 * 日次レポートジョブ。
 * 毎日定時に売上速報を生成する。
 * メール送信は外部サービス連携のプレースホルダー実装。
 */
@ApplicationScoped
class DailyReportJob {
    @Inject
    lateinit var dailySalesRepository: DailySalesRepository

    companion object {
        private val logger: Logger = Logger.getLogger(DailyReportJob::class::class.java)
    }

    /**
     * 毎日 23:59 に前日の売上サマリーをテナントごとに生成する。
     * cron 式: 秒 分 時 日 月 曜日
     */
    @Scheduled(cron = "0 59 23 * * ?", identity = "daily-report-job")
    fun generateDailyReport() {
        val yesterday = LocalDate.now().minusDays(1)
        logger.info("Generating daily sales report for $yesterday")

        val organizationIds = dailySalesRepository.findDistinctOrganizationIdsBySaleDate(yesterday)
        if (organizationIds.isEmpty()) {
            logger.info("No sales data found for $yesterday")
            return
        }

        for (orgId in organizationIds) {
            generateReportForOrganization(yesterday, orgId)
        }
    }

    private fun generateReportForOrganization(
        date: LocalDate,
        organizationId: java.util.UUID,
    ) {
        val sales = dailySalesRepository.findBySaleDate(date, organizationId)
        if (sales.isEmpty()) return

        val totalSales = sales.sumOf { it.grossAmount }
        val totalTransactions = sales.sumOf { it.transactionCount }
        val storeCount = sales.size

        val summary =
            buildString {
                appendLine("=== 売上速報 ($date) [org=$organizationId] ===")
                appendLine("総売上: $totalSales (銭単位)")
                appendLine("取引数: $totalTransactions")
                appendLine("店舗数: $storeCount")
                appendLine()
                for (sale in sales) {
                    appendLine("  店舗: ${sale.storeId} / 売上: ${sale.grossAmount} / 取引数: ${sale.transactionCount}")
                }
            }

        logger.info(summary)
        // プレースホルダー: メール送信
        // emailService.send(to = "admin@example.com", subject = "売上速報 $date", body = summary)
    }
}
