package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID

/**
 * カテゴリエンティティ。
 * 商品を階層構造（親子）で分類する。parent_id による自己参照で階層を表現する。
 */
@Entity
@Table(name = "categories", schema = "product_schema")
class CategoryEntity : BaseEntity() {
    @Column(name = "parent_id")
    var parentId: UUID? = null

    @Column(name = "name", nullable = false, length = 100)
    lateinit var name: String

    @Column(name = "color", length = 7)
    var color: String? = null

    @Column(name = "icon", length = 50)
    var icon: String? = null

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    /** 楽観ロック用バージョン */
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
}
