# open-pos

汎用POSシステム - Universal Point of Sale System

## アーキテクチャ

マイクロサービス構成のPOSシステム。

| サービス | 技術 | 役割 |
|---------|------|------|
| api-gateway | Quarkus (REST) | BFF, 認証, テナント注入 |
| pos-service | Quarkus (gRPC) | 取引, 支払, レシート |
| product-service | Quarkus (gRPC) | 商品, カテゴリ, 税率 |
| inventory-service | Quarkus (gRPC) | 在庫, 入出庫 |
| analytics-service | Quarkus (gRPC) | 売上分析 |
| store-service | Quarkus (gRPC) | 店舗, スタッフ |
| pos-terminal | React PWA | POS端末（タブレット） |
| admin-dashboard | React SPA | 管理画面（デスクトップ） |

## セットアップ

```bash
# インフラ起動
make up

# proto コード生成
make proto

# バックエンドビルド
make build

# フロントエンドインストール&ビルド
pnpm install
make build-apps
```

## 開発

```bash
# 開発ツール付きで起動（pgAdmin, Redis Commander）
make up-dev

# テスト
make test

# Lint
make lint
```

詳細は [docs/guides/setup.md](docs/guides/setup.md) を参照。

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development workflow, coding standards, and how to submit pull requests.

## License

[MIT License](LICENSE)
