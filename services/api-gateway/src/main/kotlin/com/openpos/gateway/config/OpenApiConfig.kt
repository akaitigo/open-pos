package com.openpos.gateway.config

import jakarta.ws.rs.core.Application
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition
import org.eclipse.microprofile.openapi.annotations.info.Contact
import org.eclipse.microprofile.openapi.annotations.info.Info
import org.eclipse.microprofile.openapi.annotations.tags.Tag

/**
 * OpenAPI 定義。
 * /q/openapi でJSON仕様を、/q/swagger-ui でSwagger UIを提供する。
 */
@OpenAPIDefinition(
    info =
        Info(
            title = "OpenPOS API",
            version = "1.0.0",
            description = "汎用POSシステム REST API",
            contact = Contact(name = "OpenPOS Team"),
        ),
    tags = [
        Tag(name = "Products", description = "商品管理"),
        Tag(name = "Categories", description = "カテゴリ管理"),
        Tag(name = "Stores", description = "店舗管理"),
        Tag(name = "Transactions", description = "取引管理"),
        Tag(name = "Inventory", description = "在庫管理"),
        Tag(name = "Sync", description = "オフライン同期"),
    ],
)
class OpenApiConfig : Application()
