# モバイルアプリ対応戦略

## 推奨: PWA 強化（Phase 1）+ React Native（Phase 2）

### 結論

短期的には既存の React アプリを PWA として強化し、中長期で React Native 専用アプリを開発する。

| 観点 | PWA 強化 | React Native | 判定 |
|------|---------|-------------|------|
| 開発コスト | 低（既存コード流用） | 高（新規開発） | PWA |
| 配布 | URL でアクセス | App Store / Play Store | RN |
| オフライン | Service Worker + IndexedDB | SQLite + AsyncStorage | 同等 |
| ハードウェア連携 | 制限あり（BLE, NFC は API 依存） | フルアクセス | RN |
| パフォーマンス | 十分（POS 用途） | ネイティブ性能 | RN |
| 保守工数 | 1 コードベース | 2 コードベース | PWA |

## Phase 1: PWA 強化（推奨: v1.1）

### 1.1 Service Worker 最適化

```
pos-terminal/
├── public/
│   ├── manifest.json        # 既存
│   └── sw.js               # 新規: カスタム Service Worker
├── src/
│   ├── workers/
│   │   └── sync.worker.ts  # 既存: オフライン同期
│   └── hooks/
│       └── use-touch-gestures.ts  # 既存: タッチ操作
```

**実装項目**:
- Workbox による precaching 戦略（App Shell + API レスポンスキャッシュ）
- Background Sync API でオフライン取引の自動同期
- 周期的バックグラウンド同期（在庫・価格の定期更新）

### 1.2 Web Push 通知

- 在庫アラート（閾値以下で通知）
- シフト開始リマインダー
- 日次売上サマリー

### 1.3 インストール体験の改善

- `beforeinstallprompt` イベントでカスタムインストール UI
- スプラッシュスクリーン設定（manifest.json の拡張）
- iOS Safari 対応（apple-touch-icon, meta タグ）

### 1.4 タッチ操作の最適化

既存の `use-touch-gestures.ts` を基盤に:
- スワイプ: カート内アイテムの削除（左スワイプ）
- ピンチズーム: 商品画像の拡大
- 長押し: コンテキストメニュー（商品詳細・割引適用）
- 大きなタップターゲット（最小 48x48px）

## Phase 2: React Native（推奨: v2.0）

### コード共有戦略

```
packages/
├── shared-types/       # 既存: 型定義（共有）
├── shared-logic/       # 新規: ビジネスロジック（共有）
│   ├── cart.ts         # カート計算ロジック
│   ├── tax.ts          # 税計算ロジック
│   └── money.ts        # 金額フォーマット
└── shared-api/         # 新規: API クライアント（共有）
    └── openpos-client.ts

apps/
├── pos-terminal/       # 既存: Web PWA
├── admin-dashboard/    # 既存: Web
└── pos-mobile/         # 新規: React Native
    ├── ios/
    ├── android/
    └── src/
        ├── screens/
        ├── navigation/
        └── native/     # ネイティブ固有機能
            ├── printer.ts    # レシートプリンタ連携
            ├── scanner.ts    # バーコードスキャナ
            └── payment.ts    # 決済端末連携
```

### ネイティブ機能

| 機能 | ライブラリ | 用途 |
|------|----------|------|
| バーコードスキャン | react-native-camera / expo-barcode-scanner | 商品読み取り |
| BLE プリンタ | react-native-ble-plx | レシート印刷 |
| NFC | react-native-nfc-manager | 電子マネー |
| Push | @react-native-firebase/messaging | 通知 |
| Biometrics | react-native-biometrics | スタッフ認証 |

### オフライン戦略（React Native）

- SQLite (expo-sqlite) でローカル DB
- Watermelon DB で同期（conflict resolution 付き）
- ネットワーク復帰時のバッチ同期

## 評価基準

Phase 2 への移行判断基準:

1. PWA で対応不可能なハードウェア連携要件が発生
2. App Store / Play Store 配布が必要（法人顧客要件）
3. 月間アクティブユーザー 1,000+ でネイティブ性能が必要
4. BLE プリンタ / NFC 決済の需要が 30%+ の顧客から

## タイムライン

| フェーズ | 期間 | 成果物 |
|---------|------|--------|
| Phase 1.1 | 2週間 | Service Worker + Workbox |
| Phase 1.2 | 1週間 | Web Push 通知 |
| Phase 1.3 | 1週間 | インストール体験改善 |
| Phase 1.4 | 1週間 | タッチ操作最適化 |
| Phase 2 | 8-12週間 | React Native MVP |
