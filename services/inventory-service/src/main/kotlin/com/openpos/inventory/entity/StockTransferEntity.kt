package com.openpos.inventory.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

/**
 * 在庫移動エンティティ。
 * 多店舗間在庫移動 (#145)。
 * items は JSON 配列で [{productId, quantity}] を保持。
 */
@Entity
@Table(name = "stock_transfers", schema = "inventory_schema")
class StockTransferEntity : BaseEntity() {
    @Column(name = "from_store_id", nullable = false)
    lateinit var fromStoreId: UUID

    @Column(name = "to_store_id", nullable = false)
    lateinit var toStoreId: UUID

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", nullable = false, columnDefinition = "jsonb")
    var items: String = "[]"

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "PENDING"

    @Column(name = "note")
    var note: String? = null
}
