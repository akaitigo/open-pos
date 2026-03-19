package com.openpos.inventory.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 棚卸しエンティティ。
 * 在庫実査のヘッダー情報を保持する。
 * N+1 クエリ防止のため、@NamedEntityGraph で items を一括取得可能にする。
 */
@Entity
@Table(name = "stocktakes", schema = "inventory_schema")
@NamedEntityGraph(
    name = "StocktakeEntity.withItems",
    attributeNodes = [NamedAttributeNode("items")],
)
class StocktakeEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "IN_PROGRESS"

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant = Instant.now()

    @Column(name = "completed_at")
    var completedAt: Instant? = null

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "stocktake_id", insertable = false, updatable = false)
    var items: MutableList<StocktakeItemEntity> = mutableListOf()
}
