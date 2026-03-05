package com.openpos.pos.repository

import com.openpos.pos.entity.TransactionDiscountEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 取引割引リポジトリ。
 */
@ApplicationScoped
class TransactionDiscountRepository : PanacheRepositoryBase<TransactionDiscountEntity, UUID>
