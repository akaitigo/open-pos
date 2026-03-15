package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.SystemSettingEntity
import com.openpos.store.repository.SystemSettingRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional

/**
 * システム設定のビジネスロジック層。
 * テナントレベルのキーバリュー設定の CRUD を提供する。
 */
@ApplicationScoped
class SystemSettingService {
    @Inject
    lateinit var settingRepository: SystemSettingRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    fun getByKey(key: String): SystemSettingEntity? {
        tenantFilterService.enableFilter()
        return settingRepository.findByKey(key)
    }

    fun listAll(): List<SystemSettingEntity> {
        tenantFilterService.enableFilter()
        return settingRepository.listAllSorted()
    }

    @Transactional
    fun upsert(
        key: String,
        value: String,
        description: String?,
    ): SystemSettingEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        tenantFilterService.enableFilter()

        val existing = settingRepository.findByKey(key)
        if (existing != null) {
            existing.value = value
            description?.let { existing.description = it }
            settingRepository.persist(existing)
            return existing
        }

        val entity =
            SystemSettingEntity().apply {
                this.organizationId = orgId
                this.key = key
                this.value = value
                this.description = description
            }
        settingRepository.persist(entity)
        return entity
    }

    @Transactional
    fun delete(key: String): Boolean {
        tenantFilterService.enableFilter()
        val entity = settingRepository.findByKey(key) ?: return false
        settingRepository.delete(entity)
        return true
    }
}
