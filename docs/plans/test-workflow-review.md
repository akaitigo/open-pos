# テスト基盤・開発ワークフロー準備状況レビュー

**レビュー日**: 2026-03-05
**対象**: open-pos プロジェクト
**レビューアー**: test-workflow-reviewer

---

## 現在のテスト状況

### Backend（Kotlin/Quarkus）

| 項目 | 状況 |
|------|------|
| テストフレームワーク | JUnit 5 + `@QuarkusTest` + REST Assured ✓ |
| テストファイル数 | 11 ファイル（全サービス合計） |
| テストレベル | ヘルスチェック + BaseEntity フィルタテスト のみ |
| 実装済みテスト | `HealthCheckTest.kt`（9個） + `BaseEntityFilterTest.kt`（2個） |
| JaCoCo 設定 | **未導入** ❌ |
| Testcontainers | **未導入** ❌ |
| WireMock | **未導入** ❌ |

**テストファイル詳細**:
```
services/
├── api-gateway/src/test/kotlin/com/openpos/gateway/
│   └── HealthCheckTest.kt
├── product-service/src/test/kotlin/com/openpos/product/
│   ├── HealthCheckTest.kt
│   └── entity/BaseEntityFilterTest.kt
├── store-service/src/test/kotlin/com/openpos/store/
│   ├── HealthCheckTest.kt
│   └── entity/BaseEntityFilterTest.kt
├── pos-service/src/test/kotlin/com/openpos/pos/
│   ├── HealthCheckTest.kt
│   └── entity/BaseEntityFilterTest.kt
├── inventory-service/src/test/kotlin/com/openpos/inventory/
│   ├── HealthCheckTest.kt
│   └── entity/BaseEntityFilterTest.kt
└── analytics-service/src/test/kotlin/com/openpos/analytics/
    ├── HealthCheckTest.kt
    └── entity/BaseEntityFilterTest.kt
```

### Frontend（TypeScript/React）

| 項目 | 状況 |
|------|------|
| テストフレームワーク | Vitest 4.0.18 + React Testing Library ✓ |
| テストファイル数 | 2 ファイル（App.test.tsx のみ） |
| テストレベル | スモークテスト / ライティングテスト のみ |
| Coverage ツール | **未設定** ❌ |
| E2E テスト | **未導入** ❌ Playwright 依存なし |
| setup.ts | 実装済み ✓ |

**実装済みテスト**:
- `apps/pos-terminal/src/App.test.tsx` — 基本レンダリング + UI要素テスト
- `apps/admin-dashboard/src/App.test.tsx` — 基本レンダリング + サイドバーテスト

### CI/CD パイプライン

| 項目 | 状況 |
|------|------|
| Proto lint | ✓ `buf lint` + breaking 検出 |
| Backend build | ✓ `./gradlew build -x test` |
| Backend test | ✓ `./gradlew test` |
| Frontend build | ✓ `pnpm -r build` |
| Frontend test | ✓ `pnpm -r test -- --run` |
| E2E test | **未実装** ❌ |
| Testcontainers | **未実装** ❌ |
| カバレッジレポート | **未実装** ❌ |
| ブランチ保護 | **未設定** ❌ |

---

## テスト戦略との主要ギャップ

### 🔴 **重大ギャップ**（Phase 2 開始前に必須対応）

#### 1. 結合テスト基盤が全くない

**docs/guides/testing.md での定義**:
```markdown
### 3. 結合テスト（Integration Test）
- フレームワーク: `@QuarkusTest` + Testcontainers（PostgreSQL, Redis, RabbitMQ）
- 対象: Repository ↔ DB、Service ↔ RabbitMQ、gRPC Client ↔ Server
- 場所: src/test/kotlin/**/integration/
```

**現状**:
- Testcontainers **未導入** → build.gradle.kts に依存なし
- WireMock（外部API スタブ）**未導入**
- RabbitMQ テスト **ゼロ**
- Redis キャッシュテスト **ゼロ**
- gRPC サービス間通信テスト **ゼロ**

**インパクト**:
- DB/キャッシュ/MQ 統合点のバグが本番環境で初めて検出される
- マイクロサービス間の通信仕様が保証されない

#### 2. E2E テスト基盤が完全に欠落

**docs/guides/testing.md での定義**:
```markdown
### 4. E2Eテスト（End-to-End Test）
- フレームワーク: Playwright（TypeScript）
- 対象: クリティカルなユーザーフローのみ
- 場所: e2e/
```

**現状**:
- Playwright **未導入** （package.json に依存なし）
- `e2e/` ディレクトリ **存在しない**
- クリティカルパステスト **ゼロ** (5つ定義済み)
  - POS 端末: 商品選択 → 支払 → レシート
  - 管理画面: 商品 CRUD
  - 認証: PIN ログイン
  - オフライン同期

**インパクト**:
- フロントエンド/バックエンド統合後のユーザーフロー検証ができない
- UI 変更時のリグレッション検出ができない

#### 3. カバレッジツール・指標が未導入

**docs/guides/testing.md での定義**:
```markdown
### カバレッジ指標
| 指標 | ツール | 目標 |
|------|--------|------|
| 行カバレッジ（Backend） | JaCoCo | 新規 80%+ |
| 分岐カバレッジ（Backend） | JaCoCo | 新規 70%+ |
| 行カバレッジ（Frontend） | Vitest (v8) | 新規 80%+ |
```

**現状**:
- JaCoCo **未設定** → build.gradle.kts に プラグイン/タスクなし
- Vitest v8 coverage **未設定** → package.json に `--coverage` オプションなし
- CI での カバレッジ計測 **ゼロ** → `.github/workflows/ci.yml` に step なし
- 品質ゲート **ゼロ** → GitHub branch protection rules 未設定

**インパクト**:
- 品質基準の客観的評価ができない
- PR レビューで「テスト不足」の判定根拠がない

#### 4. テスト粒度が不足（機能テストまで）

**現状のテスト構成**:
- ✓ **ヘルスチェック**: アプリ起動確認のみ（9個）
- ✓ **スモークテスト**: コンポーネントレンダリング確認のみ（2個）
- ❌ **単体テスト**: 金額計算、バリデーション、エラーハンドリング → **実装ゼロ**
- ❌ **機能テスト**: 仕様ベース設計（同値分割、デシジョンテーブル）→ **実装ゼロ**
- ❌ **結合テスト**: 統合点の相互作用検証 → **実装ゼロ**

**docs/guides/testing.md での要件**:
```markdown
## テストピラミッド
        /  E2E  \          ← 少数: クリティカルパスのみ
       /  結合テスト \       ← 中量: 統合点ごと
      / 機能テスト    \      ← 中量: 仕様ベース設計技法
     / 単体テスト      \     ← 大量: Fast/Isolated/Repeatable
```

**インパクト**:
- ロジックバグ（例：税計算、端数処理）が単体テストで検出されない
- 新機能追加時の影響範囲テストが弱い

### 🟡 **中程度ギャップ**

#### 5. Backend テスト設定が不完全

- `build.gradle.kts` に `testImplementation("io.quarkus:quarkus-jdbc-h2")` のみ
- インメモリ DB（H2）は Testcontainers のような**実 DB テスト環境に比べて信頼性が低い**
- マイグレーション・SQL 方言のギャップが隠蔽される

#### 6. Frontend カバレッジ計測が未設定

```bash
pnpm --filter {app} test -- --coverage
```
このコマンドが動作しない（Vitest v8 プラグイン + coverage provider が未設定）

#### 7. 開発ワークフローが未整備

| 項目 | 状況 |
|------|------|
| Branch protection | ❌ main に保護ルールなし |
| テスト必須化 | ❌ PR マージ前の テスト実行 未必須化 |
| PR テンプレート | ✓ 存在するが「テスト」section が形式的 |
| Issue テンプレート | ✓ 存在（bug/feature/chore） |
| Code review policy | ✓ CODEOWNERS 設定済み（@akaitigo） |
| コミット規約 | ❓ `.git/hooks` 未確認（conventional-commits 等） |

---

## 開発ワークフロー準備状況

### ✓ 実装済み

1. **PR テンプレート** (`.github/PULL_REQUEST_TEMPLATE.md`)
   - 概要、関連 Issue、変更内容、テスト チェックリスト
   - ただし「テスト」が checkbox 形式で形式的

2. **Issue テンプレート** (`.github/ISSUE_TEMPLATE/`)
   - `bug.md`, `feature.md`, `chore.md` 用意

3. **CODEOWNERS** 設定
   - `* @akaitigo` → 全 PR を Ryusei が レビュー対象に

4. **CI 自動実行**
   - PR/push 時に `ci.yml` が Proto lint, Backend build/test, Frontend build/lint/test を実行

### ❌ 未実装

1. **ブランチ保護ルール**
   - main ブランチに対して「テスト pass 必須」「approved レビュー必須」の設定なし
   - force push が可能（破壊的変更を防げない）

2. **テスト必須化**
   - CI が 失敗しても PR をマージできる（GitHub 設定未実装）

3. **コミットメッセージ規約**
   - Conventional Commits（feat:, fix:, test:, docs: 等）の強制なし
   - Husky + Commitlint による pre-commit hook 未設定

4. **リリースワークフロー**
   - semantic versioning / changelog 生成 / タグ自動化 なし

---

## Phase 2 開発開始前に必要な準備

### 🔴 **最優先**（2-3日で実装）

#### 必須タスク 1: 結合テスト基盤構築
```bash
# 1. Testcontainers 依存を build.gradle.kts に追加
# 2. PostgreSQL + Redis + RabbitMQ テストコンテナ起動
# 3. @QuarkusTest + Testcontainers で 統合テストテンプレート作成
# 4. WireMock で外部API スタブ化テンプレート作成
```

**成果物**:
- `services/{service}/src/test/kotlin/**/integration/` に テストテンプレート
- CI で統合テスト実行ステップ追加

**見積**: 1-2日

#### 必須タスク 2: E2E テスト基盤構築
```bash
# 1. Playwright を package.json に追加
# 2. e2e/ ディレクトリ + playwright.config.ts 作成
# 3. Page Object Model (POM) テンプレート作成
# 4. クリティカルパス 5つのテストシナリオコード化
```

**成果物**:
- `e2e/pages/` — Page Object Models
- `e2e/tests/critical-path.spec.ts` — 5つのシナリオ
- CI で E2E テスト実行ステップ追加（main merge 時）

**見積**: 2-3日

#### 必須タスク 3: カバレッジ計測・品質ゲート設定
```bash
# 1. JaCoCo を build.gradle.kts に追加 → jacocoTestReport タスク
# 2. Vitest coverage plugin を package.json に追加
# 3. CI で カバレッジ計測ステップ追加
# 4. GitHub branch protection rules 設定（80% 以上必須）
```

**成果物**:
- `build/reports/jacoco/` に レポート生成
- `coverage/` に フロントエンド レポート生成
- GitHub main ブランチに 保護ルール追加

**見積**: 1日

### 🟡 **高優先**（Phase 2 開始前）

#### 高優先タスク 4: ブランチ保護・テスト必須化
```bash
# GitHub Settings:
# - main ブランチ → "Require status checks to pass before merging"
# - "Require code review before merge" (1 approval)
# - "Dismiss stale review approvals when new commits are pushed"
# - Codeowners review 必須化
```

**見積**: 0.5日（設定のみ）

#### 高優先タスク 5: 単体テスト・機能テストの標準テンプレート作成
```bash
# docs/guides/testing-templates.md に以下を追加:
# - Backend: @QuarkusTest / @ParameterizedTest / @InjectMock サンプル
# - Frontend: Vitest / userEvent / waitFor サンプル
# - Backend functional: デシジョンテーブル設計例
# - 金額計算テスト: 境界値テスト テンプレート
```

**見積**: 0.5日

---

## 開発者体験の問題点

### 1. ローカルテスト実行が複雑
```bash
# 現在:
./gradlew test                  # Backend 全サービス
pnpm -r test                    # Frontend 全アプリ
# → 順序不明、結果が分かりにくい

# 改善案:
make test-all                   # Makefile に統一コマンド追加
```

### 2. テストのデバッグが困難
- 結合テスト / E2E テスト がないため、本番環境のバグ再現テストができない
- Testcontainers ログが見れないため、DB 接続エラーが特定できない

### 3. ドキュメントと実装のギャップ
- `docs/guides/testing.md` は詳細に書かれているが、実装テンプレートが存在しない
- エージェントが「何を書くべきか」を判断しづらい

---

## リスク評価

### Phase 1（現在）完了した成果
- ✓ 基本的な単体テスト実行環境（JUnit 5 + Vitest）
- ✓ CI 自動実行（Proto lint + build）
- ✓ スモークテスト（起動確認）

### Phase 2 開始時のリスク
- 🔴 **本番バグリスク**: 結合テスト / E2E テストがないため、統合後に初めてバグが発見される
- 🔴 **品質退行リスク**: カバレッジ計測がないため、新規コード追加時に カバレッジが低下しても検出できない
- 🟡 **開発効率リスク**: テストテンプレートがないため、エージェントが「どう書くか」で時間を費やす
- 🟡 **マージリスク**: ブランチ保護がないため、テスト失敗しても PR をマージできる

---

## 推奨アクションプラン

### Week 1（Phase 2 開始前）
1. **Testcontainers 統合**（1.5日）
2. **Playwright + E2E 基盤**（2.5日）
3. **JaCoCo + カバレッジ計測**（1日）

### Week 2
4. **GitHub ブランチ保護設定**（0.5日）
5. **テストテンプレート ドキュメント作成**（1.5日）
6. **開発者への educate**（0.5日）

### 並行実施
- Phase 2 機能開発（新しいサービス・エンドポイント）は上記完了後に開始

---

## 結論

| 項目 | 評価 | 理由 |
|------|------|------|
| テスト基盤整備度 | 🔴 20% | スモークテストのみ、結合テスト・E2E 皆無 |
| CI/CD 自動化度 | 🟡 60% | Proto lint・build・単体テストは OK、カバレッジ計測・E2E なし |
| 開発ワークフロー | 🟡 40% | PR テンプレ・Issue テンプレ あり、ブランチ保護・テスト必須化 なし |
| 品質ゲート | 🔴 0% | カバレッジ指標、テスト pass 必須化 なし |
| **全体準備度** | **🔴 25%** | **Phase 2 開発開始は不可、1-2週間の準備が必須** |

**推奨**: Phase 1 で実装した 11 個の テストをベースに、Testcontainers + Playwright + カバレッジ計測の 3つの柱を同時に構築し、GitHub ブランチ保護で品質ゲートを有効化してから Phase 2 開始。
