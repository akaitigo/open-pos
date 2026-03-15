package com.openpos.analytics.event

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ProcessedEventRepository : PanacheRepositoryBase<ProcessedEventEntity, UUID> {
    fun isProcessed(eventId: UUID): Boolean = findById(eventId) != null
}
