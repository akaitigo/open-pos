# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-03-29

### Added

- Product variants (size, color, etc.) with full CRUD via gRPC and REST API (#1068)
- CSV product bulk import endpoint `POST /api/products/import` (#1069)
- Electronic receipt email delivery via Quarkus Mailer (#1070)
- Inter-store stock transfer with gRPC connectivity (#1070)
- Customer CRM profile CRUD with purchase history tracking and loyalty points via gRPC (#1070)
- Customer notes and memo functionality (#1070)
- Staff sales report with per-employee aggregation (#1070)
- Category sales analysis report (#1070)
- Tax report with tax-rate breakdown (#1070)
- Proto definitions for DiscountReason, Reservation, and Supplier domain models (#1072)

## [1.0.0] - 2026-03-22

Initial release.

### Added

- Multi-service architecture: api-gateway, pos-service, product-service, inventory-service, store-service, analytics-service
- gRPC inter-service communication with Protocol Buffers
- REST API gateway powered by Quarkus
- Transaction processing (sales, returns, voids)
- Product management with barcode/SKU support
- Real-time inventory tracking with stock alerts
- Store and staff management
- Discount and tax-rate configuration
- Sales analytics with event-driven processing
- PostgreSQL persistence with Flyway migrations
- Comprehensive test suite (unit, integration, gRPC)
- Docker Compose development environment
- GitHub Actions CI/CD pipeline
- buf-based Protocol Buffers workflow (lint, breaking change detection, formatting)

[2.0.0]: https://github.com/akaitigo/open-pos/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/akaitigo/open-pos/releases/tag/v1.0.0
