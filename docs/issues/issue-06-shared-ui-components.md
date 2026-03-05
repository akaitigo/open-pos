# UI コンポーネントを共有パッケージに移動する（DRY 違反の解消）

**ラベル**: `type:refactor`, `app:pos-terminal`, `app:admin-dashboard`, `P2:medium`

## 概要

`pos-terminal` と `admin-dashboard` の `src/components/ui/` に 20 以上の同一ファイルが重複している。これは DRY（Don't Repeat Yourself）原則に違反しており、メンテナンスコストが高い。

## 現状の問題

以下のファイルが完全に同一（diff なし）：

```
apps/pos-terminal/src/components/ui/button.tsx
apps/admin-dashboard/src/components/ui/button.tsx

apps/pos-terminal/src/components/ui/input.tsx
apps/admin-dashboard/src/components/ui/input.tsx

apps/pos-terminal/src/components/ui/card.tsx
apps/admin-dashboard/src/components/ui/card.tsx

apps/pos-terminal/src/components/ui/dialog.tsx
apps/admin-dashboard/src/components/ui/dialog.tsx

... 他 16+ ファイル
```

## 対応内容

### 1. 共有 UI パッケージの作成

```
packages/
├── shared-types/     # 既存
└── ui/               # 新規作成
    ├── package.json
    ├── tsconfig.json
    └── src/
        └── components/
            ├── button.tsx
            ├── input.tsx
            ├── card.tsx
            ├── dialog.tsx
            └── ...
```

### 2. pnpm workspace 設定の更新

`pnpm-workspace.yaml` に `packages/ui` を追加。

### 3. 各アプリの import パスを更新

```typescript
// Before
import { Button } from '@/components/ui/button'

// After
import { Button } from '@openpos/ui'
```

### 4. Tailwind CSS の設定

共有パッケージの Tailwind クラスが各アプリで正しくスキャンされるよう `tailwind.config.ts` の `content` パスを更新。

## 備考

- shadcn/ui のコンポーネントは通常プロジェクトにコピーして使う設計だが、モノリポで複数アプリがある場合は共有パッケージ化が推奨される
- 型安全性のため、共有パッケージも `@openpos/` スコープで管理する

## チェックリスト

- [ ] `packages/ui` パッケージの初期化
- [ ] 共通 UI コンポーネントの移動
- [ ] Tailwind CSS 設定の調整
- [ ] 各アプリの import パス更新
- [ ] ビルド・テストの確認
