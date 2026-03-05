# admin-dashboard の非同期処理にエラーハンドリングを追加する

**ラベル**: `type:bug`, `app:admin-dashboard`, `P1:high`

## 概要

admin-dashboard の複数のルートコンポーネントで、API コールの非同期処理にエラーハンドリングが欠如している。API 呼び出しが失敗した場合、ユーザーにフィードバックがなくサイレントに失敗する。

## 該当箇所

### `.then()` に `.catch()` がない箇所

| ファイル | 行 | 関数 |
|---------|-----|------|
| `apps/admin-dashboard/src/routes/categories.tsx` | 45 | `handleDelete` |
| `apps/admin-dashboard/src/routes/categories.tsx` | 49-56 | `handleSubmit` |
| `apps/admin-dashboard/src/routes/products.tsx` | 70-82 | `handleDelete`, `handleSubmit` |
| `apps/admin-dashboard/src/routes/staff.tsx` | 42-53 | `.then()` チェーン |
| `apps/admin-dashboard/src/routes/staff.tsx` | 84-91 | `handleSubmit` |
| `apps/admin-dashboard/src/routes/stores.tsx` | 51-58 | `handleSubmit` |
| `apps/admin-dashboard/src/routes/stores.tsx` | 255 | `.then()` チェーン |

### サイレントエラーハンドラ

| ファイル | 行 | 内容 |
|---------|-----|------|
| `apps/pos-terminal/src/routes/products.tsx` | 213 | `.catch(() => {})` でエラーを握りつぶし |

## 対応内容

### 1. 共通エラーハンドリング Hook の作成

```typescript
// hooks/use-api-mutation.ts
function useApiMutation<T>(mutationFn: () => Promise<T>) {
  // toast 通知付きのエラーハンドリング
}
```

### 2. 各ルートの修正

- `try-catch` または `.catch()` でエラーをキャッチ
- ユーザーへのフィードバック（toast / エラーメッセージ）を追加
- ローディング状態の管理

### 3. サイレントキャッチの修正

- `products.tsx:213` の `.catch(() => {})` にログ記録を追加（スキャナー停止失敗は UX には影響しないが、デバッグのため記録）

## チェックリスト

- [ ] エラーハンドリング用の共通パターンを決定
- [ ] `categories.tsx` のエラーハンドリング追加
- [ ] `products.tsx` のエラーハンドリング追加
- [ ] `staff.tsx` のエラーハンドリング追加
- [ ] `stores.tsx` のエラーハンドリング追加
- [ ] `pos-terminal/products.tsx` のサイレントキャッチ修正
