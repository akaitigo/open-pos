# Third-Party Licenses

open-pos uses the following open-source libraries. We are grateful to their authors and contributors.

> **Last audited**: 2026-03-18 (v1.0 release)

## Backend (Kotlin / JVM)

| Library | License | Notes |
|---------|---------|-------|
| [Quarkus](https://quarkus.io/) | Apache 2.0 | |
| [Kotlin](https://kotlinlang.org/) | Apache 2.0 | |
| [Hibernate ORM / Panache](https://hibernate.org/orm/) | LGPL 2.1 | Used as library dependency; see note below |
| [Hibernate Validator](https://hibernate.org/validator/) | Apache 2.0 | |
| [SmallRye Reactive Messaging](https://smallrye.io/) | Apache 2.0 | RabbitMQ connector |
| [SmallRye Health](https://smallrye.io/) | Apache 2.0 | |
| [SmallRye JWT](https://smallrye.io/) | Apache 2.0 | JWT/OIDC verification |
| [SmallRye OpenAPI](https://smallrye.io/) | Apache 2.0 | Swagger UI / OpenAPI |
| [gRPC Java](https://grpc.io/) | Apache 2.0 | |
| [Flyway](https://flywaydb.org/) | Apache 2.0 | Database migrations |
| [Jackson](https://github.com/FasterXML/jackson) | Apache 2.0 | JSON serialization |
| [Lettuce (Redis)](https://lettuce.io/) | Apache 2.0 | Redis client |
| [bcrypt (favrdev)](https://github.com/patrickfav/bcrypt) | Apache 2.0 | PIN hashing |
| [Micrometer](https://micrometer.io/) | Apache 2.0 | Prometheus metrics |
| [OpenTelemetry](https://opentelemetry.io/) | Apache 2.0 | Distributed tracing |
| [Testcontainers](https://testcontainers.com/) | MIT | Test only |
| [JUnit 5](https://junit.org/junit5/) | EPL 2.0 | Test only |
| [Mockito](https://site.mockito.org/) | MIT | Test only |
| [Mockito-Kotlin](https://github.com/mockito/mockito-kotlin) | MIT | Test only |
| [REST Assured](https://rest-assured.io/) | Apache 2.0 | Test only |
| [JaCoCo](https://www.jacoco.org/) | EPL 2.0 | Test only |
| [H2 Database](https://h2database.com/) | MPL 2.0 / EPL 1.0 | Test only |
| [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/) | Apache 2.0 | Build only |

## Frontend (TypeScript / React)

| Library | License | Notes |
|---------|---------|-------|
| [React](https://react.dev/) | MIT | v19 |
| [React DOM](https://react.dev/) | MIT | v19 |
| [React Router](https://reactrouter.com/) | MIT | v7 |
| [TypeScript](https://www.typescriptlang.org/) | Apache 2.0 | |
| [Vite](https://vitejs.dev/) | MIT | v7 |
| [Tailwind CSS](https://tailwindcss.com/) | MIT | v4 (pos-terminal), v3 (admin-dashboard) |
| [Radix UI](https://www.radix-ui.com/) | MIT | Dialog, Label, Separator, Slot, Tabs, Tooltip, etc. |
| [Zustand](https://zustand-demo.pmnd.rs/) | MIT | v5 |
| [Zod](https://zod.dev/) | MIT | v4 |
| [Recharts](https://recharts.org/) | MIT | v3 (admin-dashboard only) |
| [Lucide React](https://lucide.dev/) | ISC | |
| [Dexie.js](https://dexie.org/) | Apache 2.0 | IndexedDB (pos-terminal only) |
| [html5-qrcode](https://github.com/mebjas/html5-qrcode) | Apache 2.0 | Barcode scanner (pos-terminal only) |
| [class-variance-authority](https://cva.style/) | Apache 2.0 | |
| [tailwind-merge](https://github.com/dcastil/tailwind-merge) | MIT | |
| [tailwindcss-animate](https://github.com/jamiebuilds/tailwindcss-animate) | MIT | |
| [React Hook Form](https://react-hook-form.com/) | MIT | admin-dashboard only |
| [@hookform/resolvers](https://github.com/react-hook-form/resolvers) | MIT | admin-dashboard only |
| [clsx](https://github.com/lukeed/clsx) | MIT | |
| [Vitest](https://vitest.dev/) | MIT | Test only |
| [@testing-library/react](https://testing-library.com/) | MIT | Test only |
| [@testing-library/jest-dom](https://testing-library.com/) | MIT | Test only |
| [@testing-library/user-event](https://testing-library.com/) | MIT | Test only |
| [Playwright](https://playwright.dev/) | Apache 2.0 | E2E test only |
| [ESLint](https://eslint.org/) | MIT | Dev only |
| [Prettier](https://prettier.io/) | MIT | Dev only |
| [jsdom](https://github.com/jsdom/jsdom) | MIT | Test only |

## Infrastructure

| Software | License | Notes |
|----------|---------|-------|
| [PostgreSQL](https://www.postgresql.org/) | PostgreSQL License | Permissive |
| [Redis](https://redis.io/) | RSALv2 / SSPLv1 (server) | Client library (Lettuce) is Apache 2.0 |
| [RabbitMQ](https://www.rabbitmq.com/) | MPL 2.0 | |
| [ORY Hydra](https://www.ory.sh/hydra/) | Apache 2.0 | |
| [Prometheus](https://prometheus.io/) | Apache 2.0 | Monitoring |
| [Grafana](https://grafana.com/) | AGPL 3.0 | Monitoring dashboards; not bundled, separate deployment |
| [Loki](https://grafana.com/oss/loki/) | AGPL 3.0 | Log aggregation; not bundled, separate deployment |

## Protocol Buffers

| Tool | License |
|------|---------|
| [buf](https://buf.build/) | Apache 2.0 |
| [Protocol Buffers](https://protobuf.dev/) | BSD 3-Clause |

---

## License Compatibility Notes

### Hibernate ORM (LGPL 2.1)

Hibernate ORM is licensed under LGPL 2.1. open-pos uses it as a library dependency (dynamic linking via JVM classloading), which is permitted under the LGPL without affecting the project's MIT license. No modifications are made to Hibernate source code.

### Redis Server (RSALv2 / SSPLv1)

Redis server uses a dual license (RSALv2 / SSPLv1) since Redis 7.4. open-pos connects to Redis as an external service via the Lettuce client (Apache 2.0). The Redis server license does not affect open-pos distribution.

### Grafana / Loki (AGPL 3.0)

Grafana and Loki are used as standalone monitoring tools, not embedded or distributed with open-pos. They are referenced in `infra/grafana/` configuration files for convenience. AGPL obligations apply only to modifications of Grafana/Loki themselves.

### Copyleft Assessment

No GPL-licensed dependencies are used in application code. All LGPL usage (Hibernate) is as a library dependency, which is compliant with open-pos's MIT license. No copyleft concerns exist for distribution.

---

> **How to verify**: Run `pnpm licenses list` for frontend dependencies and `./gradlew dependencies` for backend dependencies. This file is manually maintained and should be updated when dependencies change.
