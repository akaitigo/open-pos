# .gitignore のパターンを強化する

**ラベル**: `type:chore`, `P3:low`

## 概要

`.gitignore` にセキュリティ関連の追加パターンが不足している。また、`compose.override.yml` のパターンが実際のファイル名と一致していない。

## 対応内容

### 1. 証明書・秘密鍵パターンの追加

```gitignore
# === Private keys & certificates ===
*.pem
*.key
*.p12
*.pfx
*.crt
*.cert
```

### 2. compose.override.yml パターンの修正

```gitignore
# 現在（旧命名規則のみ）
docker-compose.override.yml

# 修正（両方の命名規則をカバー）
docker-compose.override.yml
compose.override.yml
```

### 3. Claude Code ローカル設定の追加（任意）

```gitignore
# === Claude Code (local) ===
.claude/settings.local.json
```

## チェックリスト

- [ ] 証明書パターンを `.gitignore` に追加
- [ ] `compose.override.yml` パターンを修正
- [ ] `.claude/settings.local.json` をパターンに追加（任意）
