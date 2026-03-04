package com.openpos.store.service

import com.openpos.store.entity.OrganizationEntity
import com.openpos.store.repository.OrganizationRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

@ApplicationScoped
class OrganizationService {
    @Inject
    lateinit var organizationRepository: OrganizationRepository

    @Transactional
    fun create(
        name: String,
        businessType: String,
        invoiceNumber: String?,
    ): OrganizationEntity {
        val entity =
            OrganizationEntity().apply {
                this.name = name
                this.businessType = businessType
                this.invoiceNumber = invoiceNumber
            }
        organizationRepository.persist(entity)
        return entity
    }

    fun findById(id: UUID): OrganizationEntity? = organizationRepository.findByIdNotDeleted(id)

    @Transactional
    fun update(
        id: UUID,
        name: String?,
        businessType: String?,
        invoiceNumber: String?,
    ): OrganizationEntity? {
        val entity = organizationRepository.findByIdNotDeleted(id) ?: return null
        name?.let { entity.name = it }
        businessType?.let { entity.businessType = it }
        invoiceNumber?.let { entity.invoiceNumber = it }
        organizationRepository.persist(entity)
        return entity
    }
}
