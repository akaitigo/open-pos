package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.FavoriteProductEntity
import com.openpos.store.repository.FavoriteProductRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

/**
 * お気に入り商品サービス。
 * スタッフごとのクイックアクセス商品を管理する。
 */
@ApplicationScoped
class FavoriteProductService {
    @Inject
    lateinit var favoriteProductRepository: FavoriteProductRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    fun listByStaffId(staffId: UUID): List<FavoriteProductEntity> {
        tenantFilterService.enableFilter()
        return favoriteProductRepository.findByStaffId(staffId)
    }

    @Transactional
    fun toggle(
        staffId: UUID,
        productId: UUID,
    ): Boolean {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        tenantFilterService.enableFilter()
        val existing = favoriteProductRepository.findByStaffAndProduct(staffId, productId)
        if (existing != null) {
            favoriteProductRepository.delete(existing)
            return false
        }

        val entity =
            FavoriteProductEntity().apply {
                this.organizationId = orgId
                this.staffId = staffId
                this.productId = productId
                this.sortOrder = 0
            }
        favoriteProductRepository.persist(entity)
        return true
    }
}
