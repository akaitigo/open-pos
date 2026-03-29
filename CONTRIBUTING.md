# Contributing to open-pos

> [!IMPORTANT]
> **このリポジトリは AI 駆動開発の実験プロジェクトです。**
> コミュニティサポート、外部コントリビューションの受付、メンテナンスの保証は行いません。

詳細なコントリビューションガイドは [docs/guides/contributing.md](docs/guides/contributing.md) を参照してください。

## クイックリファレンス

### ブランチ戦略

- `main`: 安定版。直接コミット禁止。
- `feature/{issue番号}-短い説明`: 機能開発ブランチ

### 開発ワークフロー

1. Issue 確認 → ブランチ作成
2. `make doctor` で前提確認
3. コード実装 + テスト
4. `make verify` でローカル品質ゲート
5. PR 作成（`gh pr create`）→ オートマージ設定（`gh pr merge --squash --auto`）

### コミット規約

```
<type>(<scope>): <subject>

type: feat | fix | docs | style | refactor | test | chore
scope: サービス名 or アプリ名（任意）
```

### テスト

```bash
./gradlew build           # バックエンド
pnpm -r run test          # フロントエンド
make verify               # 全体検証
make verify-full          # E2E 含む全検証
```

## ライセンス

MIT License -- 詳細は [LICENSE](LICENSE) を参照。
