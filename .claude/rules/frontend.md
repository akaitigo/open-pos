---
globs:
  - "apps/**/*.ts"
  - "apps/**/*.tsx"
  - "packages/**/*.ts"
---

# フロントエンド開発ルール

## コンポーネント構造
```
src/
├── components/   # 再利用可能コンポーネント
├── features/     # 機能別モジュール
├── hooks/        # カスタムフック
├── lib/          # ユーティリティ
├── stores/       # Zustand ストア
├── types/        # 型定義
└── routes/       # ページコンポーネント
```

## 型安全性
- `any` 禁止 → `unknown` + 型ガード
- API レスポンスは Zod でランタイムバリデーション
- `@shared-types/openpos` の型を使用

## 金額表示
- `formatMoney()` を使用（shared-types）
- 計算は銭単位のまま実行、表示時のみ変換

## オフライン（pos-terminal のみ）
- Dexie.js でローカル DB
- `navigator.onLine` でネットワーク状態監視
- オフライン中の取引は `client_id` (UUID) 付きでローカル保存
