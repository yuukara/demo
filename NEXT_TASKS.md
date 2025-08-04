# 次に取り組む課題・タスク計画

> **最終更新**: 2025-08-05  
> **基準**: REVIEW.md と開発効率化記事の分析結果

## 🔴 **最優先タスク（今週中）**

### 1. セキュリティ改善
**目標**: 残り1つのクリティカル問題を解決

#### Task 1.1: データベースパスワードの環境変数化
- **ファイル**: `src/main/resources/application.properties`
- **現状**: パスワード `Sql2025!` がハードコード
- **対応**: 
  - 環境変数 `DB_PASSWORD` の設定
  - Docker Compose での環境変数管理
  - 本番環境向けの設定分離

**Expected Time**: 2時間  
**Priority**: 🔴 Critical

#### Task 1.2: CLAUDE.md の作成
- **目的**: プロジェクト専用のAI開発支援ファイル
- **内容**:
  - 技術スタック定義
  - コーディング規約
  - セキュリティルール
  - テスト方針

**Expected Time**: 1時間  
**Priority**: 🟡 High

## 🟡 **次スプリント（2週間以内）**

### 2. アーキテクチャ改善
**目標**: 保守性向上とコード品質改善

#### Task 2.1: EmployeeService のリファクタリング
- **問題**: 612行の巨大クラス
- **分割案**:
  ```
  EmployeeService (コア機能)
  ├── DataGenerationService (データ生成)
  ├── CsvExportService (CSV操作)
  └── EmployeeDataService (データ操作)
  ```

**Expected Time**: 8時間  
**Priority**: 🟡 High

#### Task 2.2: 入力値検証の追加
- **対象**: 全Serviceクラス
- **実装**: `@Valid`, `@NotNull` などのアノテーション
- **テスト**: バリデーション例外のテストケース追加

**Expected Time**: 6時間  
**Priority**: 🟡 High

#### Task 2.3: UnsafeDataProcessingService の置き換え
- **問題**: 競合状態とリソースリーク
- **対応**: SafeDataProcessingService パターンに統一
- **影響範囲**: 関連するController、Test

**Expected Time**: 4時間  
**Priority**: 🟡 High

### 3. 開発効率化
**目標**: CI/CDパイプラインとテスト自動化

#### Task 3.1: 自動テストスクリプト作成
```bash
# 提案するカスタムコマンド
./scripts/test-and-report.sh    # テスト実行 + カバレッジレポート
./scripts/security-check.sh     # セキュリティ監査
./scripts/performance-test.sh   # パフォーマンステスト
```

**Expected Time**: 4時間  
**Priority**: 🟢 Medium

#### Task 3.2: GitHub Actions ワークフロー
- **機能**: PR時の自動テスト、静的解析
- **ツール**: SpotBugs, PMD, JaCoCo
- **通知**: Slack連携

**Expected Time**: 3時間  
**Priority**: 🟢 Medium

## 🟢 **長期計画（1-2ヶ月）**

### 4. コード品質向上
#### Task 4.1: 命名規則の統一
- **対象**: Domain クラスのフィールド名
- **問題**: スネークケースとキャメルケースの混在
- **作業**: 一括置換 + テスト修正

#### Task 4.2: 例外処理の改善
- **現状**: 汎用的なException処理
- **改善**: カスタム例外クラスの作成と使用

#### Task 4.3: 設定値の外部化
- **対象**: 
  - Email domain "@example.com"
  - Status codes
  - その他マジックナンバー

### 5. 機能拡張
#### Task 5.1: APIドキュメント自動生成
- **ツール**: OpenAPI 3.0 + Swagger UI
- **統合**: Spring Boot Actuator

#### Task 5.2: 監視・ログ改善
- **メトリクス**: Micrometer + Prometheus
- **ログ**: 構造化ログ（JSON形式）
- **トレーシング**: Spring Cloud Sleuth

## 📊 **進捗管理**

### 完了済み ✅
- [x] EmployeeService.java エスケープシーケンス修正
- [x] OptimizedEmployeeService.java 一時ファイルセキュリティ修正
- [x] REVIEW.md 最新状況反映

### 進行中 🔄
- [ ] 次タスクの計画策定 ← **現在ここ**

### 待機中 ⏳
- [ ] 上記全タスク

## 🎯 **成功指標**

### 短期（1週間）
- [ ] セキュリティ問題 0件
- [ ] CLAUDE.md 作成完了

### 中期（1ヶ月）
- [ ] コードカバレッジ 80%以上
- [ ] クラス平均行数 200行以下
- [ ] CI/CD パイプライン稼働

### 長期（3ヶ月）
- [ ] API応答時間 50ms以下（95%tile）
- [ ] 自動デプロイ環境構築
- [ ] 監視ダッシュボード運用開始

## 📝 **備考**

- **開発環境**: Windows PowerShell
- **DB**: SQL Server
- **現在のブランチ**: `fix/code-review-issues`
- **課題管理**: このファイル + REVIEW.md で進捗追跡

---

**Next Review**: 各タスク完了時に進捗をこのファイルに反映