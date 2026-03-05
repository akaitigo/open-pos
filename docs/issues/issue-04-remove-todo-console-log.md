# 未実装 TODO と console.log の残留を解消する

**ラベル**: `type:cleanup`, `app:pos-terminal`, `svc:product-service`, `P2:medium`

## 概要

コードベースに未実装の TODO コメントと console.log が残っており、ポートフォリオとして不完全な印象を与える。

## 該当箇所

### 1. カート追加機能の未実装 + console.log

**ファイル**: `apps/pos-terminal/src/routes/products.tsx:62-64`

```typescript
function handleAddToCart(product: Product) {
  // TODO: Zustand store でカートに追加
  console.log('Add to cart:', product.id, product.name)
}
```

**対応案**:
- A: Zustand store を実装してカート機能を完成させる（推奨）
- B: TODO を削除し、未実装であることを UI 上で明示する（toast 通知など）
- C: 「Phase 2 で対応」として Issue に紐付け、console.log のみ削除

### 2. クーポン割引情報の TODO

**ファイル**: `services/product-service/src/main/kotlin/com/openpos/product/grpc/ProductGrpcService.kt:418`

```kotlin
// TODO: 割引情報も返す場合は discount を設定
```

**対応案**:
- この TODO は将来の拡張に関するもので、コード自体は完成している
- Issue 番号を紐付けるか、コメントを「将来の拡張ポイント」として明確化する

## チェックリスト

- [ ] `products.tsx` の `console.log` を削除
- [ ] `handleAddToCart` を実装 or 適切に処理
- [ ] `ProductGrpcService.kt` の TODO コメントを整理
