package com.openpos.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.openpos.product.cache.ProductCacheService
import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.DiscountEntity
import com.openpos.product.repository.DiscountRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

/**
 * 割引のビジネスロジック層。
 * CRUD と有効期間チェックを提供する。
 * cache-aside パターンで Redis キャッシュを利用する。
 * キー形式: openpos:product-service:{orgId}:discount:{id}
 * TTL: 1800 秒（30 分）
 */
@ApplicationScoped
class DiscountService {
    @Inject
    lateinit var discountRepository: DiscountRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Inject
    lateinit var cacheService: ProductCacheService

    @Inject
    lateinit var objectMapper: ObjectMapper

    private val log = Logger.getLogger(DiscountService::class.java)

    companion object {
        const val DISCOUNT_TTL_SECONDS = 1800L
        private const val PREFIX = "openpos:product-service"
    }

    private fun discountKey(
        orgId: String,
        id: String,
    ): String = "$PREFIX:$orgId:discount:$id"

    private fun discountListKey(
        orgId: String,
        activeOnly: Boolean,
    ): String = "$PREFIX:$orgId:discount:list:$activeOnly"

    /**
     * 割引を作成する。作成後にリストキャッシュを無効化する。
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
        invalidateDiscountListCaches()
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
     * cache-aside: Redis にキャッシュがあればそこから返す。
     */
    fun findById(id: UUID): DiscountEntity? {
        tenantFilterService.enableFilter()
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }
        val cacheKey = discountKey(orgId.toString(), id.toString())

        // cache-aside: キャッシュ確認
        val cached = cacheService.get(cacheKey)
        if (cached != null) {
            return try {
                val dto = objectMapper.readValue(cached, DiscountCacheDto::class.java)
                dto.toEntity()
            } catch (e: Exception) {
                log.warnf("Failed to deserialize discount cache: %s", e.message)
                discountRepository.findById(id)
            }
        }

        // cache miss: DB から取得
        val entity = discountRepository.findById(id) ?: return null

        // キャッシュに書き込み
        try {
            cacheService.set(cacheKey, objectMapper.writeValueAsString(DiscountCacheDto.from(entity)), DISCOUNT_TTL_SECONDS)
        } catch (e: Exception) {
            log.warnf("Failed to cache discount: %s", e.message)
        }

        return entity
    }

    /**
     * 割引を削除する（論理削除: isActive = false）。キャッシュを無効化する。
     */
    @Transactional
    fun delete(id: UUID): Boolean {
        tenantFilterService.enableFilter()
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }
        val entity = discountRepository.findById(id) ?: return false
        entity.isActive = false
        discountRepository.persist(entity)
        cacheService.invalidate(discountKey(orgId.toString(), id.toString()))
        invalidateDiscountListCaches()
        return true
    }

    /**
     * 割引を更新する。更新後にキャッシュを無効化する。
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
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }
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
        cacheService.invalidate(discountKey(orgId.toString(), id.toString()))
        invalidateDiscountListCaches()
        return entity
    }

    private fun invalidateDiscountListCaches() {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }
        cacheService.invalidatePattern("$PREFIX:$orgId:discount:list:*")
    }
}

/**
 * Redis シリアライズ用の DTO。
 * JPA エンティティを直接シリアライズしない。
 */
data class DiscountCacheDto(
    val id: UUID,
    val organizationId: UUID,
    val name: String,
    val discountType: String,
    val value: Long,
    val appliesTo: String,
    val validFrom: String?,
    val validUntil: String?,
    val isActive: Boolean,
) {
    fun toEntity(): DiscountEntity =
        DiscountEntity().apply {
            this.id = this@DiscountCacheDto.id
            this.organizationId = this@DiscountCacheDto.organizationId
            this.name = this@DiscountCacheDto.name
            this.discountType = this@DiscountCacheDto.discountType
            this.value = this@DiscountCacheDto.value
            this.appliesTo = this@DiscountCacheDto.appliesTo
            this.validFrom = this@DiscountCacheDto.validFrom?.let { Instant.parse(it) }
            this.validUntil = this@DiscountCacheDto.validUntil?.let { Instant.parse(it) }
            this.isActive = this@DiscountCacheDto.isActive
        }

    companion object {
        fun from(entity: DiscountEntity): DiscountCacheDto =
            DiscountCacheDto(
                id = entity.id,
                organizationId = entity.organizationId,
                name = entity.name,
                discountType = entity.discountType,
                value = entity.value,
                appliesTo = entity.appliesTo,
                validFrom = entity.validFrom?.toString(),
                validUntil = entity.validUntil?.toString(),
                isActive = entity.isActive,
            )
    }
}
