# CLAUDE.md - AI開発支援ドキュメント

> **目的**: このプロジェクト専用のClaude Code AI開発支援設定ファイル  
> **更新日**: 2025-08-06  
> **プロジェクト**: Spring Boot Employee Management System

## 🛠️ **技術スタック**

### **フレームワーク・ライブラリ**
- **Spring Boot**: 3.5.3 (メインフレームワーク)
- **MyBatis**: 3.0.3 (データベースマッピング)
- **Jackson**: CSV/JSON データ処理
- **Lombok**: コード生成（getter/setter等）

### **言語・環境**
- **Java**: 21 (プロジェクト標準)
- **Maven**: 依存関係管理・ビルドツール
- **SQL Server**: メインデータベース

### **開発ツール**
- **Spring Boot DevTools**: ホットリロード
- **Spring Boot Test**: 自動テストフレームワーク

## 📋 **コーディング規約**

### **Javaコーディングスタイル**
```java
// ✅ 推奨: 明確なクラス構造
@Service
public class EmployeeService {
    private static final int BATCH_SIZE = 100;
    
    @Autowired
    private EmployeeMapper employeeMapper;
    
    /**
     * Javadocコメントを必須とする
     */
    public List<Employee> getAllEmployees() {
        // 実装
    }
}
```

### **命名規約**
- **クラス**: PascalCase (`EmployeeService`, `PopulationData`)
- **メソッド**: camelCase (`getAllEmployees`, `generateTestData`)
- **定数**: UPPER_SNAKE_CASE (`BATCH_SIZE`, `LAST_NAMES`)
- **パッケージ**: `com.example123.demo.{layer}`

### **パッケージ構造**
```
src/main/java/com/example123/demo/
├── controller/     # REST APIエンドポイント
├── service/        # ビジネスロジック
├── repository/     # データアクセス層
└── domain/         # エンティティクラス
```

## 🔐 **セキュリティルール**

### **必須セキュリティ要件**
1. **機密情報の取り扱い**
   - パスワード・APIキーは環境変数使用必須
   - ハードコード禁止
   ```properties
   # ✅ 正しい方法
   spring.datasource.password=${DB_PASSWORD}
   
   # ❌ 禁止
   spring.datasource.password=hardcoded_password_example
   ```

2. **入力値検証**
   - 全公開エンドポイントで入力値検証実装
   - SQLインジェクション対策（MyBatis使用）

3. **ログ出力**
   - 機密情報をログに出力禁止
   - デバッグレベルでのPII情報制限

### **データベースセキュリティ**
- 環境変数によるDB認証情報管理
- 接続文字列での`trustServerCertificate=true`は開発環境のみ

## 🧪 **テスト方針**

### **テスト戦略**
- **単体テスト**: 各Service/Controllerクラス
- **統合テスト**: データベース接続を含むテスト
- **カバレッジ目標**: 80%以上

### **テストファイル命名**
```
src/test/java/com/example123/demo/
├── service/EmployeeServiceTest.java
├── controller/EmployeeControllerTest.java
└── repository/EmployeeMapperTest.java
```

### **モックとスタブ**
- 外部依存はMockito使用
- データベーステストは@SpringBootTest

## 🏗️ **アーキテクチャガイドライン**

### **レイヤー分離**
- **Controller**: HTTPリクエスト処理のみ
- **Service**: ビジネスロジック集約
- **Repository**: データアクセス専用

### **クラス設計**
- **単一責任原則**: 1クラス1責務
- **最大行数制限**: 200行以下を推奨
- **大きなクラスは分割**: EmployeeService (612行) → 分割対象

### **例外処理**
- カスタム例外クラス作成推奨
- 汎用Exception使用を避ける

## 📊 **パフォーマンス要件**

### **データベース最適化**
- バッチ処理: BATCH_SIZE = 100
- SQLServerパラメータ制限考慮 (2100個制限)
- 大量データ処理時のメモリ効率

### **レスポンス時間目標**
- API応答時間: 95%tile で 50ms以下
- CSVエクスポート: 10万件で10秒以内

## 🚀 **開発ワークフロー**

### **ブランチ戦略**
- 現在: `fix/code-review-issues`
- 機能開発: `feature/feature-name`
- バグ修正: `fix/issue-description`

### **コミットメッセージ**
```
feat: 新機能追加
fix: バグ修正
refactor: リファクタリング
docs: ドキュメント更新
security: セキュリティ修正
```

### **必須チェック項目**
- [ ] セキュリティ要件遵守
- [ ] テストケース作成
- [ ] Javadoc記述
- [ ] 命名規約準拠
- [ ] 例外処理実装

## 🔄 **現在の重要課題**

### **修正済み** ✅
- Task 1.1: データベースパスワード環境変数化

### **実行予定**
- Task 2.1: EmployeeService リファクタリング (612行 → 分割)
- Task 2.2: 入力值検証追加
- Task 2.3: UnsafeDataProcessingService 置き換え

## 🔧 **頻繁に使用するMavenコマンド**

> **注意**: Windows PowerShell環境での実行

### **開発・テスト**
- `.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"`: 開発環境起動
- `.\mvnw test`: 単体テスト実行
- `.\mvnw verify`: 統合テスト含む全テスト実行

### **ビルド・デプロイ**
- `.\mvnw clean package "-Pprod"`: 本番用JARファイル作成
- `.\mvnw spring-boot:build-image`: Spring Boot Dockerイメージ作成

### **PowerShell実行の注意点**
- Windows PowerShellでは `.\mvnw` (ドット+バックスラッシュ) を使用
- 実際の実行環境ではUnix形式 `./mvnw` (ドット+スラッシュ) でも動作
- パラメータに`-`が含まれる場合はダブルクォートで囲む
- 例: `"-Dspring-boot.run.profiles=dev"`, `"-Pprod"`

## 💡 **開発時の重要な実装パターン**

### **1. Constructor Injection**
```java
// ✅ 推奨: コンストラクタインジェクション
@Service
public class EmployeeService {
    private final EmployeeMapper employeeMapper;
    
    public EmployeeService(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }
}

// ❌ 非推奨: @Autowired フィールドインジェクション
@Autowired
private EmployeeMapper employeeMapper;
```

### **2. DTOパターン**
```java
// エンティティを直接APIで公開しない
@GetMapping("/employees")
public ResponseEntity<List<EmployeeDTO>> getEmployees() {
    // Employee -> EmployeeDTO 変換
}
```

### **3. Repository抽象化**
```java
// JpaRepositoryの適切な拡張
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByDepartment(String department);
}
```

### **4. Transaction境界**
```java
// @Transactionalの適切な配置
@Service
@Transactional(readOnly = true)
public class EmployeeService {
    
    @Transactional // 書き込み操作のみ
    public Employee saveEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }
}
```

### **5. バリデーション**
```java
// Bean Validationの活用
public class EmployeeDTO {
    @NotBlank(message = "名前は必須です")
    private String name;
    
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;
}
```

## 🤖 **Claude Codeへの指示事項**

### **コード生成ガイドライン**
- ✅ コード生成時は必ずこの規約に従ってください
- ✅ 新機能実装時は既存パターンとの一貫性を保ってください
- ✅ セキュリティ関連のコードでは OWASP ベストプラクティスを適用してください
- ✅ テストコードも併せて生成してください
- ✅ 日本語コメントを適切に含めてください

### **重要な実装原則**
1. **セキュリティファースト**: 環境変数・入力値検証は必須
2. **テスト駆動**: 実装と同時にテストケース作成
3. **保守性重視**: 単一責任原則とクリーンコード
4. **パフォーマンス**: バッチ処理とメモリ効率を考慮

## 🔧 **開発中のトラブルシューティング**

### **Javaプロセス停止（ポート競合解決）**

開発中にSpring Bootアプリケーションが正常に停止しない場合やポート競合が発生した場合の対処法：

#### **基本手順**
```bash
# 1. 実行中のJavaプロセス一覧を確認
jps

# 2. DemoApplicationプロセスのIDを確認して強制終了
powershell "Stop-Process -Id <プロセスID> -Force"

# 3. ポート解放確認（8080ポートの例）
netstat -an | grep :8080

# 4. 正常にアプリケーション再起動
mvn spring-boot:run
```

#### **よくある状況と対処**
- **ポート8080競合**: `Web server failed to start. Port 8080 was already in use.`
- **IDE停止ボタンでプロセス残存**: DevToolsの再起動でプロセスが残る場合
- **logback設定変更後の不正終了**: 設定エラーで異常終了したプロセスの残存

#### **予防策**
- Spring Boot DevToolsによる自動再起動を活用
- 設定ファイル変更時は構文チェックを実施
- IDEのアプリケーション停止機能を適切に使用

### **Maven実行時の注意点**

#### **Windows PowerShell環境での実行**
```bash
# パラメータに'-'が含まれる場合はダブルクォートで囲む
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
.\mvnw clean package "-Pprod"

# プロファイル指定でアプリケーション起動
.\mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

#### **ログ出力確認**
- **開発環境**: コンソール出力 + `logs/application-dev.log`
- **本番環境**: `logs/application.log` + `logs/error.log`
- **パフォーマンス**: `logs/performance.log`（全環境共通）

---

**AI開発支援**: このドキュメントはClaude Codeによる開発支援最適化のために作成されました。