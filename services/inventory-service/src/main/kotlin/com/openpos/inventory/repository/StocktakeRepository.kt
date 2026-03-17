package com.openpos.inventory.repository

import com.openpos.inventory.entity.StocktakeEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import java.util.UUID

@ApplicationScoped
class StocktakeRepository : PanacheRepositoryBase<StocktakeEntity, UUID> {
    @Inject
    lateinit var entityManager: EntityManager

    /**
     * EntityGraph を使用して StocktakeEntity を items と一括で取得する（N+1 防止）。
     */
    fun findByIdWithItems(id: UUID): StocktakeEntity? {
        val graph = entityManager.getEntityGraph("StocktakeEntity.withItems")
        return entityManager.find(
            StocktakeEntity::class.java,
            id,
            mapOf("jakarta.persistence.fetchgraph" to graph),
        )
    }
}
