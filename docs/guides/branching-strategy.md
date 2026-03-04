# Branching Strategy

## GitHub Flow ベース（軽量版）

```
main ──────────────────────────────────────→ 本番リリース
  │
  ├── feature/42-transaction-entity ──→ PR → main
  ├── feature/43-apply-discount ──→ PR → main
  ├── fix/99-gateway-error ──→ PR → main
  └── chore/119-ci-improvement ──→ PR → main
```

すべてのブランチは `main` から分岐し、PR を経て `main` にマージされる。
長寿命ブランチ（develop, release 等）は使用しない。

## ブランチ命名規則

| プレフィックス | 用途 | 例 |
|---|---|---|
| `feature/` | 新機能 | `feature/42-transaction-entity` |
| `fix/` | バグ修正 | `fix/99-gateway-error` |
| `chore/` | メンテナンス | `chore/119-ci-improvement` |
| `docs/` | ドキュメント | `docs/55-api-reference` |
| `refactor/` | リファクタリング | `refactor/78-extract-service` |

**フォーマット**: `{type}/{issue番号}-{短い説明}`

- Issue 番号は必須（トレーサビリティ確保）
- 説明はケバブケース（小文字、ハイフン区切り）
- 短く簡潔に（3-4語以内）

## ルール

### main ブランチの保護

- main への直接 push は禁止
- すべての変更は PR 経由でマージ
- CI が pass していること（ci / proto-lint, ci / backend, ci / frontend）
- 自己レビュー可（チーム規模が小さい間）

### コミットメッセージ

[Conventional Commits](https://www.conventionalcommits.org/) に準拠:

```
feat(pos): add transaction creation endpoint
fix(gateway): handle timeout on payment callback
chore(ci): upgrade GraalVM to 21.0.3
docs(api): update gRPC service documentation
refactor(inventory): extract stock validation logic
test(pos): add integration tests for discount
```

**スコープ例**: `pos`, `gateway`, `inventory`, `proto`, `ci`, `api`, `auth`

### PR マージ戦略

- **Squash merge のみ**（1 PR = 1 コミット）
- マージ後のブランチは自動削除
- Squash コミットのタイトル: PR タイトル
- Squash コミットのメッセージ: PR ボディ

### Issue リンク

PR 作成時に関連 Issue をリンクする:

```
Closes #42
```

複数 Issue を閉じる場合:

```
Closes #42, Closes #43
```

## ワークフロー

```bash
# 1. main から最新を取得
git checkout main && git pull

# 2. ブランチ作成
git checkout -b feature/42-transaction-entity

# 3. 開発・コミット
git add <files>
git commit -m "feat(pos): implement transaction entity"

# 4. リモートへ push
git push -u origin feature/42-transaction-entity

# 5. PR 作成
gh pr create --title "feat(pos): implement transaction entity" --body "Closes #42"

# 6. CI pass + レビュー → Squash merge
```
