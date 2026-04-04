package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.CustomerEntity
import com.openpos.store.entity.PointTransactionEntity
import com.openpos.store.repository.CustomerRepository
import com.openpos.store.repository.PointTransactionRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

/**
 * 顧客管理のビジネスロジック層。
 * 顧客の CRUD 操作およびポイントの付与・利用を提供する。
 */
@ApplicationScoped
class CustomerService {
    @Inject lateinit var customerRepository: CustomerRepository

    @Inject lateinit var pointTransactionRepository: PointTransactionRepository

    @Inject lateinit var tenantFilterService: TenantFilterService

    @Inject lateinit var organizationIdHolder: OrganizationIdHolder

    /**
     * 新規顧客を作成する。
     *
     * @param name 顧客名
     * @param email メールアドレス（省略可）
     * @param phone 電話番号（省略可）
     * @return 作成された顧客エンティティ
     */
    @Transactional
    fun create(
        name: String,
        email: String?,
        phone: String?,
        notes: String?,
    ): CustomerEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val entity =
            CustomerEntity().apply {
                this.organizationId = orgId
                this.name = name
                this.email = email
                this.phone = phone
                this.notes = notes
            }
        customerRepository.persist(entity)
        return entity
    }

    /**
     * 顧客をIDで取得する。
     *
     * @param id 顧客ID
     * @return 顧客エンティティ（存在しない場合は null）
     */
    fun findById(id: UUID): CustomerEntity? {
        tenantFilterService.enableFilter()
        // findById() は em.find() ベースのため Hibernate Filter をバイパスする。
        // HQL クエリで organizationFilter を適用してテナント隔離を保証する。
        return customerRepository.find("id = ?1", id).firstResult()
    }

    /**
     * 顧客一覧を取得する（ページネーション対応）。
     *
     * @param page ページ番号（0始まり）
     * @param pageSize ページサイズ
     * @return Pair<顧客リスト, 総件数>
     */
    fun list(
        page: Int,
        pageSize: Int,
        search: String? = null,
    ): Pair<List<CustomerEntity>, Long> {
        tenantFilterService.enableFilter()
        if (search.isNullOrBlank()) {
            val customers = customerRepository.listPaginated(Page.of(page, pageSize))
            val total = customerRepository.count()
            return Pair(customers, total)
        }
        val customers = customerRepository.searchPaginated(search, Page.of(page, pageSize))
        val total = customerRepository.countBySearch(search)
        return Pair(customers, total)
    }

    /**
     * 顧客情報を更新する。
     *
     * null でないパラメータのみ更新される（部分更新）。
     *
     * @param id 顧客ID
     * @param name 新しい顧客名（省略可）
     * @param email 新しいメールアドレス（省略可）
     * @param phone 新しい電話番号（省略可）
     * @return 更新後の顧客エンティティ（顧客が存在しない場合は null）
     */
    @Transactional
    fun update(
        id: UUID,
        name: String?,
        email: String?,
        phone: String?,
        notes: String?,
    ): CustomerEntity? {
        tenantFilterService.enableFilter()
        val entity = customerRepository.find("id = ?1", id).firstResult() ?: return null
        name?.let { entity.name = it }
        email?.let { entity.email = it }
        phone?.let { entity.phone = it }
        notes?.let { entity.notes = it }
        customerRepository.persist(entity)
        return entity
    }

    /**
     * 取引金額に応じてポイントを付与する。
     *
     * 100円（10000銭）につき1ポイントを計算し、顧客のポイント残高に加算する。
     * ポイント取引履歴も記録する。
     *
     * @param customerId 顧客ID
     * @param transactionTotal 取引合計額（銭単位）
     * @param transactionId 関連する取引ID（省略可）
     * @return 付与されたポイント数（0の場合もあり）
     * @throws IllegalArgumentException 顧客が存在しない場合
     */
    @Transactional
    fun earnPoints(
        customerId: UUID,
        transactionTotal: Long,
        transactionId: UUID?,
    ): Long {
        tenantFilterService.enableFilter()
        val customer =
            customerRepository.find("id = ?1", customerId).firstResult()
                ?: throw IllegalArgumentException("Customer not found: $customerId")
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val points = transactionTotal / 10000
        if (points <= 0) return 0
        customer.points += points
        customerRepository.persist(customer)

        val pt =
            PointTransactionEntity().apply {
                this.organizationId = orgId
                this.customerId = customerId
                this.points = points
                this.type = "EARN"
                this.transactionId = transactionId
                this.description = "購入ポイント付与"
            }
        pointTransactionRepository.persist(pt)
        return points
    }

    /**
     * 顧客のポイントを利用（消費）する。
     *
     * 悲観的ロック（SELECT FOR UPDATE）で残高を確認し、十分なポイントがある場合に減算する。
     * ポイント取引履歴も記録する。
     *
     * @param customerId 顧客ID
     * @param points 利用するポイント数
     * @param transactionId 関連する取引ID（省略可）
     * @return true: ポイント利用成功、false: ポイント残高不足
     * @throws IllegalArgumentException 顧客が存在しない場合
     */
    @Transactional
    fun redeemPoints(
        customerId: UUID,
        points: Long,
        transactionId: UUID?,
    ): Boolean {
        tenantFilterService.enableFilter()
        val customer =
            customerRepository.findByIdForUpdate(customerId)
                ?: throw IllegalArgumentException("Customer not found: $customerId")
        if (customer.points < points) return false
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        customer.points -= points
        customerRepository.persist(customer)

        val pt =
            PointTransactionEntity().apply {
                this.organizationId = orgId
                this.customerId = customerId
                this.points = -points
                this.type = "REDEEM"
                this.transactionId = transactionId
                this.description = "ポイント利用"
            }
        pointTransactionRepository.persist(pt)
        return true
    }
}
