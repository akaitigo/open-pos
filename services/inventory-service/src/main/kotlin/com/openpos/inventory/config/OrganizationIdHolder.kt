package com.openpos.inventory.config

import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 現在の実行スレッドに紐づく organization_id を保持する。
 * inventory-service では gRPC worker や RabbitMQ consumer からも参照されるため、
 * RequestScoped ではなく thread-local で扱う。
 */
@ApplicationScoped
class OrganizationIdHolder {
    private val currentOrganizationId = ThreadLocal<UUID?>()

    var organizationId: UUID?
        get() = currentOrganizationId.get()
        set(value) {
            if (value == null) {
                currentOrganizationId.remove()
            } else {
                currentOrganizationId.set(value)
            }
        }

    fun clear() {
        currentOrganizationId.remove()
    }
}
