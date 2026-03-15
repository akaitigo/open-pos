package com.openpos.inventory.repository

import com.openpos.inventory.entity.StocktakeEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class StocktakeRepository : PanacheRepositoryBase<StocktakeEntity, UUID>
