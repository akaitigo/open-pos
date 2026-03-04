package com.openpos.product.service

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.DiscountEntity
import com.openpos.product.repository.DiscountRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 割引のビジネスロジック層。
 * CRUD と有効期間チェックを提供する。
 */
@ApplicationScoped
class DiscountService {
    @Inject
    lateinit var discountRepository: DiscountRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    /**
     * 割引を作成する。
     */
    @Transactional
    fun create(
        name: String,
        discountType: String,
        value: Long,
        validFrom: Instant?,
        validUntil: Instant?,
    ): DiscountEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        val entity =
            DiscountEntity().apply {
                this.organizationId = orgId
                this.name = name
                this.discountType = discountType
                this.value = value
                this.validFrom = validFrom
                this.validUntil = validUntil
                this.isActive = true
            }
        discountRepository.persist(entity)
        return entity
    }

    /**
     * 割引一覧を取得する。
     * activeOnly が true の場合は有効かつ期間内のもののみ返す。
     */
    fun list(activeOnly: Boolean): List<DiscountEntity> {
        tenantFilterService.enableFilter()
        return if (activeOnly) {
            discountRepository.findActiveAndValid(Instant.now())
        } else {
            discountRepository.findAllOrdered()
        }
    }

    /**
     * IDで割引を取得する。
     */
    fun findById(id: UUID): DiscountEntity? {
        tenantFilterService.enableFilter()
        return discountRepository.findById(id)
    }

    /**
     * 割引を更新する。
     */
    @Transactional
    fun update(
        id: UUID,
        name: String?,
        discountType: String?,
        value: Long?,
        validFrom: Instant?,
        validUntil: Instant?,
        isActive: Boolean?,
    ): DiscountEntity? {
        tenantFilterService.enableFilter()
        val entity = discountRepository.findById(id) ?: return null

        name?.let { entity.name = it }
        discountType?.let { entity.discountType = it }
        value?.let { entity.value = it }
        // validFrom/validUntil は null にリセットする用途があるため、引数の存在自体で更新判断は行わない
        // gRPC 層で明示的にセットされた場合のみ更新される
        entity.validFrom = validFrom ?: entity.validFrom
        entity.validUntil = validUntil ?: entity.validUntil
        isActive?.let { entity.isActive = it }

        discountRepository.persist(entity)
        return entity
    }
}
