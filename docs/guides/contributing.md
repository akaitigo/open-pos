# コントリビューションガイド

## ブランチ戦略

- `main`: 安定版。直接コミット禁止。
- `feature/{issue番号}-短い説明`: 機能開発ブランチ

### 例
```
feature/12-product-crud
feature/25-offline-sync
```

## 開発ワークフロー

1. **Issue 確認**: GitHub Issue の要件・受け入れ条件を確認
2. **ブランチ作成**: `git checkout -b feature/12-product-crud`
3. **前提確認**: `make doctor`
4. **開発**: コード実装 + テスト
5. **ローカル品質ゲート**: `make verify`
6. **必要なら E2E**: `pnpm e2e:install && make verify-full`
7. **PR 作成**: `gh pr create`（本文に `Closes #12` を記載）
8. **CI 通過**: GitHub Actions の全チェック通過を確認
9. **マージ**: Squash Merge → Issue 自動クローズ

## コミットメッセージ

```
<type>(<scope>): <subject>
```

### Type
- `feat`: 新機能
- `fix`: バグ修正
- `refactor`: リファクタリング
- `test`: テスト追加/修正
- `docs`: ドキュメント
- `chore`: ビルド/設定変更

### Scope
- `pos`, `product`, `inventory`, `analytics`, `store`, `gateway`
- `pos-terminal`, `admin`, `shared-types`
- `proto`, `infra`, `ci`

### 例
```
feat(product): 商品CRUD gRPC実装
fix(pos): 税額計算の丸め誤差修正
chore(infra): Docker Compose Redis バージョン更新
```
