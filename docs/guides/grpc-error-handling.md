# gRPC エラーハンドリングガイド

## Status Code 使い分け

| Status Code | 用途 | 例 |
|------------|------|-----|
| `OK` | 正常完了 | — |
| `INVALID_ARGUMENT` | リクエストパラメータ不正 | UUID形式不正、必須フィールド欠落、負の金額 |
| `NOT_FOUND` | リソースが存在しない | 商品ID未存在、取引ID未存在 |
| `ALREADY_EXISTS` | リソースが既に存在 | 重複クーポンコード、重複メールアドレス |
| `PERMISSION_DENIED` | 認可エラー | ロール不足、他テナントリソースへのアクセス |
| `UNAUTHENTICATED` | 認証エラー | x-organization-id 未設定、無効なトークン |
| `FAILED_PRECONDITION` | ビジネスルール違反 | 在庫不足、ポイント残高不足、クーポン期限切れ |
| `RESOURCE_EXHAUSTED` | レートリミット超過 | API呼び出し制限 |
| `INTERNAL` | サーバー内部エラー | 予期しない例外（詳細は隠蔽） |
| `UNAVAILABLE` | サービス一時停止 | DB接続断、RabbitMQ断 |
| `DEADLINE_EXCEEDED` | タイムアウト | gRPC deadline 超過 |

## サービス別の代表的なエラー

### pos-service
| RPC | エラー | Status Code |
|-----|--------|-------------|
| CreateTransaction | 取引が既にある（clientId重複） | `ALREADY_EXISTS` |
| AddItem | 商品が見つからない | `NOT_FOUND` |
| FinalizeTransaction | 支払い不足 | `FAILED_PRECONDITION` |
| VoidTransaction | 取引が COMPLETED でない | `FAILED_PRECONDITION` |

### product-service
| RPC | エラー | Status Code |
|-----|--------|-------------|
| GetProduct | 商品ID未存在 | `NOT_FOUND` |
| ValidateCoupon | クーポン期限切れ | `FAILED_PRECONDITION` |
| ValidateCoupon | クーポンコード未存在 | `NOT_FOUND` |

### inventory-service
| RPC | エラー | Status Code |
|-----|--------|-------------|
| AdjustStock | 在庫不足（負在庫） | `FAILED_PRECONDITION` |
| CreateStocktake | 棚卸しID重複 | `ALREADY_EXISTS` |

### store-service
| RPC | エラー | Status Code |
|-----|--------|-------------|
| AuthenticateByPin | PIN不一致 | `PERMISSION_DENIED` |
| AuthenticateByPin | アカウントロック | `FAILED_PRECONDITION` |
| CreateStaff | メールアドレス重複 | `ALREADY_EXISTS` |

## エラーレスポンスの構造

gRPC Status にはエラーメッセージのみ含める。内部情報（スタックトレース、SQL、内部ID）は含めない。

```kotlin
// Good: ユーザーに意味のあるメッセージ
throw StatusRuntimeException(
    Status.NOT_FOUND.withDescription("Product not found: $productId")
)

// Bad: 内部情報の漏洩
throw StatusRuntimeException(
    Status.INTERNAL.withDescription("SQL error: relation 'products' does not exist")
)
```

## catch-all ルール

全サービスの `GrpcExceptionMapping` で、未処理の例外は `INTERNAL` + `"Internal server error"` に変換する。デバッグ情報はログのみに出力。
