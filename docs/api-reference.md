# API Reference

open-pos exposes a REST API through the api-gateway (BFF) and uses gRPC for internal service-to-service communication.

## OpenAPI / Swagger

The api-gateway includes SmallRye OpenAPI (`quarkus-smallrye-openapi`). When the gateway is running, the interactive documentation is available at:

- **Swagger UI**: `http://localhost:8080/q/swagger-ui`
- **OpenAPI JSON**: `http://localhost:8080/q/openapi`

Resource classes under `services/api-gateway/src/main/kotlin/com/openpos/gateway/resource/` use `@Tag` and `@Operation` annotations from MicroProfile OpenAPI.

## REST API Endpoints

All endpoints are prefixed with `/api`. Authentication uses `Authorization: Bearer {token}` (JWT from ORY Hydra). Tenant isolation is automatic via `X-Organization-Id` header injected by the gateway.

### Transactions (POS)

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/transactions` | Create a transaction | CASHIER+ |
| GET | `/api/transactions/{id}` | Get transaction details | CASHIER+ |
| GET | `/api/transactions` | List transactions | MANAGER+ |
| POST | `/api/transactions/{id}/void` | Void a transaction | MANAGER+ |
| POST | `/api/transactions/{id}/return` | Return a transaction | MANAGER+ |
| GET | `/api/transactions/{id}/receipt` | Get receipt | CASHIER+ |

### Products

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/products` | List products (filter, paginate) | CASHIER+ |
| POST | `/api/products` | Create a product | MANAGER+ |
| PUT | `/api/products/{id}` | Update a product | MANAGER+ |
| DELETE | `/api/products/{id}` | Soft-delete a product | MANAGER+ |
| GET | `/api/products/search?barcode=` | Search by barcode | CASHIER+ |

### Categories

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/categories` | List categories (hierarchical) | CASHIER+ |
| POST | `/api/categories` | Create a category | MANAGER+ |
| PUT | `/api/categories/{id}` | Update a category | MANAGER+ |
| DELETE | `/api/categories/{id}` | Delete a category | MANAGER+ |

### Tax Rates

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/tax-rates` | List tax rates | CASHIER+ |
| POST | `/api/tax-rates` | Create a tax rate | MANAGER+ |
| PUT | `/api/tax-rates/{id}` | Update a tax rate | MANAGER+ |

### Discounts and Coupons

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/discounts` | List discounts | CASHIER+ |
| GET | `/api/coupons/validate?code=` | Validate a coupon code | CASHIER+ |

### Inventory

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/inventory?store_id=` | List inventory | MANAGER+ |
| PUT | `/api/inventory/{id}/adjust` | Adjust stock | MANAGER+ |
| POST | `/api/purchase-orders` | Create purchase order | MANAGER+ |
| PUT | `/api/purchase-orders/{id}/receive` | Confirm receipt | MANAGER+ |

### Analytics

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/analytics/daily` | Daily sales | MANAGER+ |
| GET | `/api/analytics/products` | Product-level sales | MANAGER+ |
| GET | `/api/analytics/hourly` | Hourly sales | MANAGER+ |
| GET | `/api/analytics/summary` | Sales summary | MANAGER+ |
| GET | `/api/analytics/abc` | ABC analysis | MANAGER+ |
| GET | `/api/analytics/gross-profit` | Gross profit report | MANAGER+ |
| GET | `/api/analytics/forecast` | Sales forecast | MANAGER+ |

### Stores and Staff

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET | `/api/stores` | List stores | MANAGER+ |
| POST | `/api/stores` | Create a store | OWNER |
| GET | `/api/staff` | List staff | MANAGER+ |
| POST | `/api/staff` | Create staff member | OWNER |
| PUT | `/api/staff/{id}` | Update staff member | OWNER |

### Drawer and Settlement

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/drawers/open` | Open cash drawer | CASHIER+ |
| POST | `/api/drawers/close` | Close cash drawer | CASHIER+ |
| GET | `/api/drawers/status` | Get drawer status | CASHIER+ |
| POST | `/api/settlements` | Create settlement | MANAGER+ |
| GET | `/api/settlements/{id}` | Get settlement | MANAGER+ |

### Auth and Sync

| Method | Path | Description | Role |
|--------|------|-------------|------|
| POST | `/api/auth/pin-login` | Staff PIN login | Terminal-authenticated |
| GET | `/api/sync/master` | Get master data diff | CASHIER+ |
| POST | `/api/sync/transactions` | Batch sync offline txns | CASHIER+ |
| GET | `/api/health` | Health check | None |

### Additional Resources

| Method | Path | Description | Role |
|--------|------|-------------|------|
| GET/POST | `/api/organizations` | Organization management | OWNER |
| GET/POST | `/api/system-settings` | System settings CRUD | OWNER |
| GET/POST | `/api/journals` | Electronic journal entries | MANAGER+ |
| GET/POST | `/api/customers` | Customer management | CASHIER+ |
| GET/POST | `/api/reservations` | Reservation management | CASHIER+ |

## Response Format

### Success

```json
{
  "data": { ... },
  "meta": { "timestamp": "2026-03-18T12:00:00Z" }
}
```

### Paginated

```json
{
  "data": [ ... ],
  "meta": { "page": 0, "size": 20, "total": 150, "totalPages": 8 }
}
```

### Error

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Product name is required",
    "details": [...]
  }
}
```

## Pagination

```
GET /api/products?page=0&size=20&sort=display_order,asc
```

## gRPC Services (Internal)

gRPC is used for service-to-service communication within the cluster. Proto definitions are in `proto/openpos/`.

### Service Definitions

| Package | Service | Proto File |
|---------|---------|-----------|
| `openpos.pos.v1` | `PosService` | `proto/openpos/pos/v1/pos.proto` |
| `openpos.product.v1` | `ProductService` | `proto/openpos/product/v1/product.proto` |
| `openpos.inventory.v1` | `InventoryService` | `proto/openpos/inventory/v1/inventory.proto` |
| `openpos.analytics.v1` | `AnalyticsService` | `proto/openpos/analytics/v1/analytics.proto` |
| `openpos.store.v1` | `StoreService` | `proto/openpos/store/v1/store.proto` |
| `openpos.store.v1` | `SystemSettingService` | `proto/openpos/store/v1/store.proto` |

### Key RPCs

| Service | RPC | Description |
|---------|-----|-------------|
| PosService | `CreateTransaction` | Create a draft transaction |
| PosService | `AddTransactionItem` | Add item to transaction |
| PosService | `FinalizeTransaction` | Complete with payment |
| PosService | `VoidTransaction` | Void a transaction |
| PosService | `SyncOfflineTransactions` | Batch sync from offline terminal |
| PosService | `OpenDrawer` / `CloseDrawer` | Cash drawer operations |
| PosService | `CreateSettlement` | Register settlement |
| ProductService | `ListProducts` | List products with filters |
| ProductService | `GetProductByBarcode` | Barcode lookup |
| ProductService | `ValidateCoupon` | Coupon validation |
| ProductService | `CreateTaxRate` / `ListTaxRates` | Tax rate management |
| InventoryService | `GetStock` / `ListStocks` | Stock queries |
| InventoryService | `AdjustStock` | Manual stock adjustment |
| InventoryService | `StartStocktake` / `CompleteStocktake` | Physical inventory |
| AnalyticsService | `GetDailySales` | Daily sales report |
| AnalyticsService | `GetAbcAnalysis` | ABC product analysis |
| AnalyticsService | `GetSalesForecast` | Moving-average forecast |
| StoreService | `AuthenticateByPin` | Staff PIN auth |
| StoreService | `RegisterTerminal` | Terminal registration |
| SystemSettingService | `UpsertSystemSetting` | Tenant-level settings |

### gRPC Metadata

| Key | Value | Description |
|-----|-------|-------------|
| `authorization` | `Bearer {token}` | Access token |
| `x-organization-id` | UUID | Tenant identifier |
| `x-staff-id` | UUID | Authenticated staff |
| `x-role` | `OWNER` / `MANAGER` / `CASHIER` | Staff role |
| `x-request-id` | UUID | Correlation ID for tracing |

All RPCs use a 5-second deadline: `withDeadlineAfter(5, TimeUnit.SECONDS)`.

### Generating Proto Docs

To generate HTML documentation from proto files:

```bash
buf generate --template buf.gen.doc.yaml
```

Or use `protoc-gen-doc`:

```bash
protoc --doc_out=docs/proto --doc_opt=html,index.html proto/openpos/**/*.proto
```

## RabbitMQ Events

Events are published to the `openpos.events` topic exchange:

| Routing Key | Publisher | Subscribers | Description |
|-------------|-----------|-------------|-------------|
| `sale.completed` | pos-service | inventory-service, analytics-service | Transaction finalized |
| `sale.voided` | pos-service | inventory-service, analytics-service | Transaction voided |
| `stock.low` | inventory-service | store-service | Stock below threshold |
