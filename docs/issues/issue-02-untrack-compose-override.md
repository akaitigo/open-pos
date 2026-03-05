# compose.override.yml の Git 追跡を解除する

**ラベル**: `type:security`, `P0:critical`

## 概要

`infra/compose.override.yml` は `.gitignore` に登録されているにもかかわらず、Git に追跡されたままになっている。このファイルには開発ツール（pgAdmin, Redis Commander）の認証情報が含まれている。

## 現状の問題

### `.gitignore` に登録済みだが追跡されている

```
# .gitignore L41
docker-compose.override.yml
```

しかし、`.gitignore` に追加される前にコミットされたため、依然として Git の追跡対象。

### 含まれる認証情報

```yaml
# infra/compose.override.yml
PGADMIN_DEFAULT_EMAIL: admin@openpos.dev
PGADMIN_DEFAULT_PASSWORD: admin
```

## 対応内容

```bash
# 追跡を解除（ファイルは削除しない）
git rm --cached infra/compose.override.yml

# .gitignore のパターンを修正（パスを含む形に）
# 現在: docker-compose.override.yml
# 修正: **/compose.override.yml
```

## 備考

- `compose.override.yml` は Docker Compose V2 の命名規則に合わせたファイル名だが、`.gitignore` には旧名称 `docker-compose.override.yml` で登録されている。パターンを `**/compose.override.yml` に修正するか、両方のパターンを追加する必要がある。
