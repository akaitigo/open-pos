package com.openpos.product.repository

import com.openpos.product.entity.ReceiptTemplateEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ReceiptTemplateRepository : PanacheRepositoryBase<ReceiptTemplateEntity, UUID> {
    fun findDefault(): ReceiptTemplateEntity? = find("isDefault", true).firstResult()
}
