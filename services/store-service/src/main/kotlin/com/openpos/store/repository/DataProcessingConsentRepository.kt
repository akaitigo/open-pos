package com.openpos.store.repository

import com.openpos.store.entity.DataProcessingConsentEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class DataProcessingConsentRepository : PanacheRepositoryBase<DataProcessingConsentEntity, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<DataProcessingConsentEntity> = find("organizationId = ?1", organizationId).list()

    fun findByOrganizationAndType(
        organizationId: UUID,
        consentType: String,
    ): DataProcessingConsentEntity? = find("organizationId = ?1 AND consentType = ?2", organizationId, consentType).firstResult()

    fun deleteByOrganizationId(organizationId: UUID): Long = delete("organizationId = ?1", organizationId)
}
