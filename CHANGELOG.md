# 変更履歴

本プロジェクトの注目すべき変更はすべてこのファイルに記録されます。

フォーマットは [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) に基づいており、本プロジェクトはセマンティックバージョニングに従います。

## [Unreleased]

### 追加

- メンテナーの意思決定と Issue トリアージを明確にするため `GOVERNANCE.md` と `docs/runbook/triage.md` を追加。
- メンテナーのトリアージ一貫性のため、PR コンポーネント自動ラベリングとラベル分類 Runbook を追加。
- ローカル品質ゲートとして `make doctor`、`make verify`、`make verify-full` を追加。
- PostgreSQL バックアップ/リストア、デモリセット、`pos-service` ログテール、grpcurl ベースの gRPC ヘルスチェック用の開発者 Makefile ヘルパーを追加。
- メンテナー向けリリースガイダンスを [docs/runbook/release.md](docs/runbook/release.md) に追加。
- ドキュメントインデックスを [docs/README.md](docs/README.md) に追加。
- 公開 OSS 運用のため [SUPPORT.md](SUPPORT.md) と [MAINTAINERS.md](MAINTAINERS.md) を追加。
- GitHub Issue の連絡先リンクとリリースノートのカテゴリ分類を追加。
- 再現可能なセットアップとリリースメタデータのため `.mise.toml`、`CITATION.cff`、Release Drafter 設定を追加。
- 2 店舗、店舗ごとの端末/スタッフ、在庫、サンプル取引を含む冪等なデモシードデータセットを追加。

### 変更

- GitHub CodeQL スキャンを追加し、文書化されたセキュリティ報告パスを GitHub プライベートアドバイザリのみに厳格化。
- 再利用可能なカートパネル、直接数量編集、明細ごとの小計、税率区分別内訳、全画面レビュー用 `/cart` ページを含む POS 端末のカートフローを拡張。
- 階層的カテゴリタブ、クライアント側商品検索/ページネーション、在庫状況対応の商品タイル、リトライ可能なローディング/エラー状態を含む POS 端末カタログを強化。
- 実際に必要なツール（`curl`、`jq`、`bc`）と対応する検証フローに関するコントリビューターセットアップドキュメントを厳格化。
- 対応する `make local-demo` / `make docker-demo` フロー、生成されるランタイム設定ファイル、現在のトラブルシューティング手順に合わせてローカル開発 Runbook を書き直し。
- ローカルヘルパースクリプトにより明確な前提条件チェックを追加。
- `make docker-build`、`make reset`、`make db-backup`、`make db-restore`、`make logs-pos`、`make grpc-test` を含む文書化されたローカル開発コマンドセットを拡張。
- シード済み在庫と取引履歴がすぐに利用可能になるよう、対応するローカルデモパスに `inventory-service` を含めるよう拡張。
- ワンショット `hydra-migrate` コンテナではなく `hydra` ヘルスを待機するよう Docker ベースの起動フローを修正。
- オープンエンドなセットアップと利用の質問を GitHub Discussions に移行し、Issues はバグと機能作業に集中させるよう変更。
- `main` ブランチ保護、自動マージ対応、GitHub ネイティブのシークレットスキャン / プッシュ保護、GitHub Discussions を有効化。

## [0.1.0] - 初期開発シリーズ

### 追加

- 初期公開リポジトリ構造、ローカルデモフロー、CI、セキュリティスキャン、コアアーキテクチャドキュメント。
