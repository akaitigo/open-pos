package com.openpos.inventory.event

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

/**
 * 在庫低下イベントを RabbitMQ に発行するパブリッシャー。
 * 在庫調整後、在庫数が閾値以下になった場合に StockLowEvent を発行する。
 */
@ApplicationScoped
class StockLowEventPublisher {
    @Inject
    @Channel("stock-low-events")
    lateinit var emitter: Emitter<String>

    @Inject
    lateinit var objectMapper: ObjectMapper

    private val log = Logger.getLogger(StockLowEventPublisher::class.java)

    /**
     * StockLowEvent を発行する。
     *
     * @param organizationId テナントID
     * @param productId 商品ID
     * @param storeId 店舗ID
     * @param currentQuantity 現在の在庫数
     * @param threshold アラート閾値
     */
    fun publish(
        organizationId: UUID,
        productId: UUID,
        storeId: UUID,
        currentQuantity: Int,
        threshold: Int,
    ) {
        val payload =
            StockLowPayload(
                productId = productId.toString(),
                storeId = storeId.toString(),
                currentQuantity = currentQuantity,
                threshold = threshold,
            )
        val envelope =
            EventEnvelopeDto(
                eventId = UUID.randomUUID().toString(),
                eventType = "stock.low",
                timestamp = Instant.now().toString(),
                organizationId = organizationId.toString(),
                payload = objectMapper.writeValueAsString(payload),
                source = "inventory-service",
            )
        val message = objectMapper.writeValueAsString(envelope)
        emitter.send(message)
        log.infof(
            "Published StockLowEvent: product=%s, store=%s, quantity=%d, threshold=%d",
            productId,
            storeId,
            currentQuantity,
            threshold,
        )
    }
}
