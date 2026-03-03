# テスト戦略

## バックエンド（Kotlin / Quarkus）

### ユニットテスト
- フレームワーク: JUnit 5
- モック: `@InjectMock`（Quarkus CDI）
- 場所: `src/test/kotlin/`

```kotlin
@QuarkusTest
class ProductServiceTest {

    @InjectMock
    lateinit var productRepository: ProductRepository

    @Inject
    lateinit var productService: ProductService

    @Test
    fun `商品を作成できる`() {
        // ...
    }
}
```

### 統合テスト
- `@QuarkusTest` + 実 DB（テスト用 PostgreSQL）
- Flyway が自動でマイグレーション実行
- テスト後にデータクリーンアップ

## フロントエンド（React / TypeScript）

### ユニットテスト
- フレームワーク: Vitest
- コンポーネント: React Testing Library
- 場所: `src/**/*.test.tsx`

### テスト実行

```bash
# バックエンド全テスト
make test

# フロントエンド全テスト
make test-apps

# 特定サービスのみ
./gradlew :services:pos-service:test

# 特定アプリのみ
pnpm --filter pos-terminal test
```

## テストの原則
1. ビジネスロジック（税額計算、割引適用）は手厚くテスト
2. 外部依存（DB, Redis, RabbitMQ）は統合テストでカバー
3. フロントエンドはユーザー操作のフローをテスト
4. 金額計算は境界値テスト必須（0円、端数、最大値）
