package com.openpos.pos.repository

import com.openpos.pos.entity.OutboxEventEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.LockModeType
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
            .withLock(LockModeType.PESSIMISTIC_WRITE)
            .range(0, limit - 1)
            .list()

    /**
     * PENDING ステータスのイベントを悲観ロック付きで取得し、
     * 即座に IN_PROGRESS に更新する。
     *
     * 呼び出し元が @Transactional であること。SELECT FOR UPDATE と
     * ステータス更新が同一トランザクション内で実行されることで、
     * 複数インスタンス間での二重取得を防止する。
     *
     * @param limit 最大取得件数
     * @return IN_PROGRESS に遷移したイベントのリスト
     */
    fun findPendingAndMarkInProgress(limit: Int): List<OutboxEventEntity> {
        val events = findPendingEvents(limit)

        for (event in events) {
            event.status = "IN_PROGRESS"
            persist(event)
        }

        return events
    }
}
