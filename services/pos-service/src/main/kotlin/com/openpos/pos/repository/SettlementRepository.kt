package com.openpos.pos.repository

import com.openpos.pos.entity.SettlementEntity
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class SettlementRepository : PanacheRepositoryBase<SettlementEntity, UUID>
