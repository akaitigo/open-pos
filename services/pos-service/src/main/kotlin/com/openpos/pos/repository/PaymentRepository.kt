package com.openpos.pos.repository

import com.openpos.pos.entity.PaymentEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 決済リポジトリ。
 */
@ApplicationScoped
class PaymentRepository : PanacheRepositoryBase<PaymentEntity, UUID>
