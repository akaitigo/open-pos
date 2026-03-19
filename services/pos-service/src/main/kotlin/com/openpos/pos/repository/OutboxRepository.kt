package com.openpos.pos.repository

import com.openpos.pos.entity.OutboxEventEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * イベントアウトボックスリポジトリ。
 * 未送信イベントの検索とステータス更新を担当する。
 */
@ApplicationScoped
class OutboxRepository : PanacheRepositoryBase<OutboxEventEntity, UUID> {
    /**
     * PENDING ステータスのイベントを作成日時順に取得する。
     * @param limit 最大取得件数
     * @return PENDING イベントのリスト
     */
    fun findPendingEvents(limit: Int): List<OutboxEventEntity> =
        find("status = ?1 order by createdAt asc", "PENDING")
            .range(0, limit - 1)
            .list()
}
