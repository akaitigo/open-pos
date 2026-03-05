package com.openpos.pos.repository

import com.openpos.pos.entity.TaxSummaryEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 税額集計リポジトリ。
 */
@ApplicationScoped
class TaxSummaryRepository : PanacheRepositoryBase<TaxSummaryEntity, UUID>
