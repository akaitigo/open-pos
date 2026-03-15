package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

/**
 * お気に入り商品エンティティ。
 * スタッフごとにクイックアクセスする商品を登録する。
 */
@Entity
@Table(name = "favorite_products", schema = "store_schema")
class FavoriteProductEntity : BaseEntity() {
    @Column(name = "staff_id", nullable = false)
    lateinit var staffId: UUID

    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0
}
