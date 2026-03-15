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

@ApplicationScoped
class CustomerService {
    @Inject lateinit var customerRepository: CustomerRepository
    @Inject lateinit var pointTransactionRepository: PointTransactionRepository
    @Inject lateinit var tenantFilterService: TenantFilterService
    @Inject lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun create(name: String, email: String?, phone: String?): CustomerEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val entity = CustomerEntity().apply {
            this.organizationId = orgId
            this.name = name
            this.email = email
            this.phone = phone
        }
        customerRepository.persist(entity)
        return entity
    }

    fun findById(id: UUID): CustomerEntity? {
        tenantFilterService.enableFilter()
        return customerRepository.findById(id)
    }

    fun list(page: Int, pageSize: Int): Pair<List<CustomerEntity>, Long> {
        tenantFilterService.enableFilter()
        val customers = customerRepository.listPaginated(Page.of(page, pageSize))
        val total = customerRepository.count()
        return Pair(customers, total)
    }

    @Transactional
    fun update(id: UUID, name: String?, email: String?, phone: String?): CustomerEntity? {
        tenantFilterService.enableFilter()
        val entity = customerRepository.findById(id) ?: return null
        name?.let { entity.name = it }
        email?.let { entity.email = it }
        phone?.let { entity.phone = it }
        customerRepository.persist(entity)
        return entity
    }

    /** 100円（10000銭）につき1ポイント付与 (#141) */
    @Transactional
    fun earnPoints(customerId: UUID, transactionTotal: Long, transactionId: UUID?): Long {
        tenantFilterService.enableFilter()
        val customer = customerRepository.findById(customerId)
            ?: throw IllegalArgumentException("Customer not found: $customerId")
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val points = transactionTotal / 10000
        if (points <= 0) return 0
        customer.points += points
        customerRepository.persist(customer)

        val pt = PointTransactionEntity().apply {
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

    @Transactional
    fun redeemPoints(customerId: UUID, points: Long, transactionId: UUID?): Boolean {
        tenantFilterService.enableFilter()
        val customer = customerRepository.findById(customerId)
            ?: throw IllegalArgumentException("Customer not found: $customerId")
        if (customer.points < points) return false
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        customer.points -= points
        customerRepository.persist(customer)

        val pt = PointTransactionEntity().apply {
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
