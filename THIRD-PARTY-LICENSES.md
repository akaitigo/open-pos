# Third-Party Licenses

open-pos uses the following open-source libraries. We are grateful to their authors and contributors.

## Backend (Kotlin / JVM)

| Library | License |
|---------|---------|
| [Quarkus](https://quarkus.io/) | Apache 2.0 |
| [Kotlin](https://kotlinlang.org/) | Apache 2.0 |
| [Hibernate ORM](https://hibernate.org/orm/) | LGPL 2.1 |
| [Hibernate Validator](https://hibernate.org/validator/) | Apache 2.0 |
| [SmallRye Reactive Messaging](https://smallrye.io/) | Apache 2.0 |
| [SmallRye Health](https://smallrye.io/) | Apache 2.0 |
| [gRPC Java](https://grpc.io/) | Apache 2.0 |
| [Flyway](https://flywaydb.org/) | Apache 2.0 |
| [Jackson](https://github.com/FasterXML/jackson) | Apache 2.0 |
| [Lettuce (Redis)](https://lettuce.io/) | Apache 2.0 |
| [Testcontainers](https://testcontainers.com/) | MIT |
| [JUnit 5](https://junit.org/junit5/) | EPL 2.0 |
| [Mockito](https://site.mockito.org/) | MIT |
| [JaCoCo](https://www.jacoco.org/) | EPL 2.0 |

## Frontend (TypeScript / React)

| Library | License |
|---------|---------|
| [React](https://react.dev/) | MIT |
| [React DOM](https://react.dev/) | MIT |
| [React Router](https://reactrouter.com/) | MIT |
| [TypeScript](https://www.typescriptlang.org/) | Apache 2.0 |
| [Vite](https://vitejs.dev/) | MIT |
| [Tailwind CSS](https://tailwindcss.com/) | MIT |
| [Radix UI](https://www.radix-ui.com/) | MIT |
| [Zustand](https://zustand-demo.pmnd.rs/) | MIT |
| [Zod](https://zod.dev/) | MIT |
| [Recharts](https://recharts.org/) | MIT |
| [Lucide React](https://lucide.dev/) | ISC |
| [Dexie.js](https://dexie.org/) | Apache 2.0 |
| [html5-qrcode](https://github.com/mebjas/html5-qrcode) | Apache 2.0 |
| [class-variance-authority](https://cva.style/) | Apache 2.0 |
| [tailwind-merge](https://github.com/dcastil/tailwind-merge) | MIT |
| [React Hook Form](https://react-hook-form.com/) | MIT |
| [Vitest](https://vitest.dev/) | MIT |
| [Playwright](https://playwright.dev/) | Apache 2.0 |
| [ESLint](https://eslint.org/) | MIT |

## Infrastructure

| Software | License |
|----------|---------|
| [PostgreSQL](https://www.postgresql.org/) | PostgreSQL License |
| [Redis](https://redis.io/) | RSALv2 / SSPLv1 (server), MIT (client library) |
| [RabbitMQ](https://www.rabbitmq.com/) | MPL 2.0 |
| [ORY Hydra](https://www.ory.sh/hydra/) | Apache 2.0 |

## Protocol Buffers

| Tool | License |
|------|---------|
| [buf](https://buf.build/) | Apache 2.0 |
| [Protocol Buffers](https://protobuf.dev/) | BSD 3-Clause |

---

> **Note**: Hibernate ORM is licensed under LGPL 2.1. open-pos uses it as a library dependency (linking), which is permitted under the LGPL without affecting the project's MIT license.

> **Note**: This file is manually maintained. Run `pnpm licenses list` and `./gradlew dependencies` to verify current dependency licenses.
