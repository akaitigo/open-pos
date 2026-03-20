# サポート

ヘルプの求め方や問題の報告先を決めるためにこのガイドを使用してください。

## 何かを開く前に

1. [README.md](README.md) と [docs/README.md](docs/README.md) を読む。
2. `make doctor` を実行してローカルの前提条件を確認する。
3. デモフローを使用している場合は、まず `make local-demo` または `make docker-demo` を実行する。

## 何をどこに報告するか

- **確認済みのバグ**: バグテンプレートで GitHub Issue を作成する。
- **機能要望**: 機能テンプレートで GitHub Issue を作成する。
- **セットアップや利用の問題**: 質問や不明確な動作については [GitHub Discussion](https://github.com/akaitigo/open-pos/discussions) を開始し、`make doctor` の出力、実行したコマンド、正確なエラーを含める。
- **セキュリティ脆弱性**: 公開 Issue を作成 **しない** でください。[GitHub プライベート脆弱性報告](https://github.com/akaitigo/open-pos/security/advisories/new) を使用してください。

## 含めるべき情報

- OS とバージョン
- `java -version`
- `node -v`
- `pnpm -v`
- 実行したコマンド
- 正確なエラー出力
- `make local-demo` と `make docker-demo` のどちらを使用したか

## 対応について

- 現在はメンテナー主導のプロジェクトであり、ベストエフォートのサポートです。
- セキュリティ報告は [SECURITY.md](SECURITY.md) の SLA に従います。
- Discussions、バグ報告、コントリビューションに関する質問は GitHub を通じて非同期で対応します。
