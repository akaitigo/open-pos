package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.WebhookEntity
import com.openpos.store.repository.WebhookRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Webhook サービス。
 * 外部サービスへのイベント通知設定の管理と、イベントトリガーを提供する。
 * 署名は HMAC-SHA256 で生成する。
 */
@ApplicationScoped
class WebhookService {
    @Inject
    lateinit var webhookRepository: WebhookRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    companion object {
        private val logger: Logger = Logger.getLogger(WebhookService::class::class.java)
    }

    fun listByOrganizationId(organizationId: UUID): List<WebhookEntity> {
        tenantFilterService.enableFilter()
        return webhookRepository.findByOrganizationId(organizationId)
    }

    @Transactional
    fun create(
        url: String,
        events: String,
        secret: String,
    ): WebhookEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        val entity =
            WebhookEntity().apply {
                this.organizationId = orgId
                this.url = url
                this.events = events
                this.secret = secret
            }
        webhookRepository.persist(entity)
        logger.info("Created webhook for org=$orgId url=$url")
        return entity
    }

    @Transactional
    fun update(
        id: UUID,
        url: String?,
        events: String?,
        isActive: Boolean?,
    ): WebhookEntity? {
        tenantFilterService.enableFilter()
        // findById() は em.find() ベースのため Hibernate Filter をバイパスする。
        // HQL クエリで organizationFilter を適用してテナント隔離を保証する。
        val entity = webhookRepository.find("id = ?1", id).firstResult() ?: return null
        url?.let { entity.url = it }
        events?.let { entity.events = it }
        isActive?.let { entity.isActive = it }
        webhookRepository.persist(entity)
        return entity
    }

    @Transactional
    fun delete(id: UUID): Boolean {
        tenantFilterService.enableFilter()
        val entity = webhookRepository.find("id = ?1", id).firstResult() ?: return false
        webhookRepository.delete(entity)
        return true
    }

    /**
     * イベントをトリガーする（プレースホルダー実装）。
     * 本番実装では非同期 HTTP POST + リトライを行う。
     */
    fun trigger(
        organizationId: UUID,
        event: String,
        payload: String,
    ) {
        val webhooks = webhookRepository.findActiveByOrganizationId(organizationId)
        for (webhook in webhooks) {
            @Suppress("UNUSED_VARIABLE")
            val signature = generateSignature(payload, webhook.secret)
            logger.info(
                "Webhook trigger: event=$event, webhookId=${webhook.id} (placeholder - no HTTP call)",
            )
        }
    }

    /**
     * HMAC-SHA256 署名を生成する。
     */
    private fun generateSignature(
        payload: String,
        secret: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val hash = mac.doFinal(payload.toByteArray())
        return "sha256=" + hash.joinToString("") { "%02x".format(it) }
    }
}
