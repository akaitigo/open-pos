package com.openpos.inventory.repository

import com.openpos.inventory.entity.StocktakeEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import java.util.UUID

@ApplicationScoped
class StocktakeRepository : PanacheRepositoryBase<StocktakeEntity, UUID> {
    /**
     * EntityGraph を使用して StocktakeEntity を items と一括で取得する（N+1 防止）。
     */
    fun findByIdWithItems(id: UUID): StocktakeEntity? {
        val em: EntityManager = getEntityManager()
        val graph = em.getEntityGraph("StocktakeEntity.withItems")
        return em.find(
            StocktakeEntity::class.java,
            id,
            mapOf("jakarta.persistence.fetchgraph" to graph),
        )
    }
}
