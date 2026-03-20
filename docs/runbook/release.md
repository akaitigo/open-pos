# リリース Runbook

この Runbook では、`open-pos` のリリースを行う際のメンテナー向け最低限のチェックリストを説明します。

## リリースポリシー

- `open-pos` は SemVer に準拠します。
- `1.0.0` までのリリースは `0.x.y` を想定しており、明確に文書化された上で破壊的変更を含む場合があります。
- すべてのリリースには Git タグ、GitHub Release エントリ、および対応する `CHANGELOG.md` の更新が必要です。

## リリース前チェックリスト

1. クリーンな `main` ブランチから開始する。
2. リリースのスコープと未解決のリスクを確認する。
3. [../../CHANGELOG.md](../../CHANGELOG.md) を更新する。
4. ローカルチェックを実行する:

```bash
make doctor
pnpm install
make verify
```

5. リリースがデモフロー、フロントエンドアプリ、シードデータ、認証、または API コントラクトに影響する場合は、以下も実行する:

```bash
pnpm e2e:install
make verify-full
```

6. `main` ブランチでの最新の GitHub Actions 実行がすべてグリーンであることを確認する:
   - `CI`
   - `Security`

## タグ付けとリリース

```bash
git checkout main
git pull --ff-only
git tag -a v0.x.y -m "v0.x.y"
git push origin v0.x.y
gh release create v0.x.y --generate-notes
```

`gh release create --generate-notes` はカテゴリグルーピングに [../../.github/release.yml](../../.github/release.yml) を使用します。

## リリース後

1. GitHub Release のリリースノートが正しいことを確認する。
2. タグが期待するコミットを指していることを確認する。
3. 未完了の Issue を次のマイルストーンに移動する。
4. 必要に応じて [../../CHANGELOG.md](../../CHANGELOG.md) に新しい `Unreleased` セクションを追加する。

## Hotfix

- 影響範囲に応じて、リリースコミットまたは現在の `main` からブランチを作成する。
- 通常リリースと同じチェックリストを実行する。
- 既存タグの強制更新ではなく、フォローアップのパッチリリースを推奨する。
