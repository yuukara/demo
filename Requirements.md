# システム仕様書 - Spring Boot従業員情報管理システム

## 1. 機能概要

### 1.1 従業員データ管理
- **従業員情報の一括登録・管理**
  - 15個のカラムを持つ詳細な従業員情報
  - **基本情報**（ID、氏名、部署、役職など）
    - 従業員ID (VARCHAR(255), PRIMARY KEY)
    - 従業員名 (VARCHAR(255), NOT NULL)
    - 部署 (VARCHAR(255), NOT NULL)
    - 役職 (VARCHAR(255))
  - **個人情報**（生年月日、性別、連絡先など）
    - 雇用形態 (VARCHAR(255), NOT NULL) - 正社員/契約社員/パートタイマー/アルバイト
    - 入社日 (DATE, NOT NULL)
    - 電話番号 (VARCHAR(255)) - 形式: 03-1234-5678
    - メールアドレス (VARCHAR(255), NOT NULL)
    - 生年月日 (DATE, NOT NULL)
    - 性別 (VARCHAR(50)) - 男性/女性
  - **管理情報**（登録者、更新者、タイムスタンプ、バージョン）
    - 作成者 (VARCHAR(255))
    - 作成日時 (DATETIME)
    - 更新者 (VARCHAR(255))
    - 更新日時 (DATETIME)
    - バージョン (BIGINT) - 楽観的ロック用

### 1.2 従業員配属履歴管理
- **複合キー**（従業員ID、組織コード、職務コード、効力開始日、連番）による配属履歴管理
  - 従業員ID (NVARCHAR(20), NOT NULL)
  - 組織コード (NVARCHAR(10), NOT NULL)
  - 職務コード (NVARCHAR(10), NOT NULL)
  - 効力開始日 (DATE, NOT NULL)
  - 連番 (INT, NOT NULL)

- **30個を超えるカラムを持つ詳細な配属情報**
  - **基本配属情報**（組織、職務、ロケーション、雇用形態など）
    - 効力終了日 (DATE)
    - ステータスコード (NVARCHAR(10), DEFAULT 'ACTIVE')
    - 基点ロケーションコード (NVARCHAR(10))
    - 雇用タイプ (NVARCHAR(10))
  - **管理情報**（グレード、給与バンド、マネージャー、プロジェクトなど）
    - グレードコード (NVARCHAR(10))
    - 給与バンドコード (NVARCHAR(10))
    - マネージャー従業員ID (NVARCHAR(20))
    - プロジェクトコード (NVARCHAR(20))
    - コストセンターコード (NVARCHAR(20))
  - **勤務パターン**（リモートワーク可否、FTE比率、シフト情報など）
    - ワークパターンコード (NVARCHAR(10))
    - シフトグループコード (NVARCHAR(10))
    - リモート勤務可否 (BIT)
    - FTE比率 (DECIMAL(5,2))
  - **拡張属性**（attr1-attr12）と監査情報
    - attr1～attr12 (NVARCHAR(100)) - カスタム属性
    - 作成日時 (DATETIME2(3), DEFAULT SYSUTCDATETIME())
    - 作成者 (NVARCHAR(50))
    - 更新日時 (DATETIME2(3), DEFAULT SYSUTCDATETIME())
    - 更新者 (NVARCHAR(50))
    - ROWVERSION (rv) - 楽観制御・同時更新検知向け

- **UPDATE後INSERT方式による一括UPSERT処理**
  - 一時テーブルを活用した効率的な更新・挿入処理
  - マルチスレッド対応による高速処理
  - バッチサイズ: 50レコード/バッチ（パラメーター制限対応）

### 1.3 高速データ処理
- **マルチスレッドによる並列処理**
  - CPU コア数に応じた自動スレッド数調整
  - 独立したトランザクション管理
  - データ分割戦略によるスレッド間競合最小化

- **SQLServerのパラメーター制限に対応**（1回の処理で2100パラメーター以下）
  - 従業員テーブル: 15カラム × 100レコード = 1500パラメーター
  - 配属履歴テーブル: 30+カラム × 50レコード = 1500パラメーター

- **バッチサイズの最適化**（100レコード/バッチ）
  - 従業員データ処理: 100レコード/バッチ
  - 配属履歴データ処理: 50レコード/バッチ

- **MERGE文による効率的な一括UPSERT処理**
  - 新規レコードと更新の自動判別
  - バージョン管理による楽観的ロック
  - 新規レコード15%、更新85%の比率制御
  - ID範囲による制御: E000000-E009999（更新）、E010000-E099999（新規）

## 2. API設計

### 2.1 現在のAPI構造分析

#### 2.1.1 従業員管理API
**EmployeeApiController** (`/api/v1/employees`)
- `POST /api/v1/employees/create` - 単一従業員登録
  - 入力: EmployeeDTO（Bean Validation適用）
  - 出力: 標準JSONレスポンス（status, message, employeeId, timestamp）
  - セキュリティ: @Valid による入力値検証

**EmployeeController** (`/api/employees`)  
- `POST /api/employees/test-merge-upsert` - MERGE方式UPSERT性能テスト
- `POST /api/employees/test-temp-table-upsert` - 一時テーブル方式UPSERT性能テスト

#### 2.1.2 配属履歴管理API
**EmployeeAssignmentHistoryController** (`/assignment-histories`)
- `POST /assignment-histories/upsert?count={数値}` - 配属履歴一括UPSERT
  - パラメーター: count (デフォルト: 10000)
  - 機能: 指定件数のダミー配属履歴データ生成・UPSERT処理

### 2.2 API設計の改良提案

#### 2.2.1 設計上の課題
1. **エンドポイント命名の不整合**
   - `/api/v1/employees` と `/api/employees` の混在
   - バージョン管理戦略が不統一

2. **機能的課題**
   - 一括登録機能の不足（現在は単一登録のみ）
   - 検索・取得API の不足
   - 更新・削除API の不足
   - ページネーション機能の不足

3. **レスポンス形式の不統一**
   - 一部はMap<String, Object>、一部は標準JSONオブジェクト

#### 2.2.2 推奨API設計

**従業員管理API** (`/api/v1/employees`)
```
GET    /api/v1/employees                    # 従業員一覧取得（ページネーション）
GET    /api/v1/employees/{id}               # 従業員詳細取得
POST   /api/v1/employees                    # 単一従業員登録
POST   /api/v1/employees/batch              # 一括従業員登録
PUT    /api/v1/employees/{id}               # 従業員情報更新
DELETE /api/v1/employees/{id}               # 従業員削除
GET    /api/v1/employees/search             # 従業員検索（部署・役職等）
```

**配属履歴管理API** (`/api/v1/assignment-histories`)
```
GET    /api/v1/assignment-histories                    # 配属履歴一覧
GET    /api/v1/assignment-histories/employee/{id}      # 特定従業員の配属履歴
POST   /api/v1/assignment-histories                    # 配属履歴登録
POST   /api/v1/assignment-histories/batch              # 配属履歴一括登録
PUT    /api/v1/assignment-histories/{composite-key}    # 配属履歴更新
DELETE /api/v1/assignment-histories/{composite-key}    # 配属履歴削除
```

**システム管理API** (`/api/v1/system`)
```
POST   /api/v1/system/performance/employees/merge-upsert      # MERGE UPSERT性能テスト
POST   /api/v1/system/performance/employees/temp-table-upsert # 一時テーブルUPSERT性能テスト
POST   /api/v1/system/data/generate                           # テストデータ生成
```

#### 2.2.3 標準レスポンス形式
```json
{
  "status": "success|error",
  "message": "処理結果メッセージ",
  "data": {実際のデータ},
  "errors": [バリデーションエラー配列],
  "timestamp": "2025-08-07T10:30:00Z",
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

## 3. データベース設計

### 3.1 テーブル設計

#### 3.1.1 従業員マスターテーブル（employees）
```sql
CREATE TABLE dbo.employees (
    -- 主キー
    id                   VARCHAR(255)      NOT NULL,
    
    -- 基本情報
    name                 VARCHAR(255)      NULL,
    department           VARCHAR(255)      NULL,
    position             VARCHAR(255)      NULL,
    employment_status    VARCHAR(255)      NULL,
    hire_date            DATE              NULL,
    
    -- 連絡先情報
    phone_number         VARCHAR(255)      NULL,
    email                VARCHAR(255)      NULL,
    
    -- 個人情報
    birth_date           DATE              NULL,
    gender               VARCHAR(50)       NULL,
    
    -- 監査情報
    created_by           VARCHAR(255)      NULL,
    created_at           DATETIME          NULL,
    updated_by           VARCHAR(255)      NULL,
    updated_at           DATETIME          NULL,
    version              BIGINT            NULL,  -- 楽観的ロック
    
    CONSTRAINT PK_employees PRIMARY KEY CLUSTERED (id)
);
```

**設計特徴:**
- **主キー**: 従業員ID（文字列）
- **楽観的ロック**: version カラムによる同時更新制御
- **監査証跡**: 作成者/日時、更新者/日時を記録
- **フレキシブル設計**: NULLを許可し段階的データ入力に対応

#### 3.1.2 従業員配属履歴テーブル（employee_assignment_history）
```sql
CREATE TABLE dbo.employee_assignment_history (
    -- 複合主キー（5要素）
    employee_id          NVARCHAR(20)  NOT NULL,
    org_code             NVARCHAR(10)  NOT NULL,
    job_code             NVARCHAR(10)  NOT NULL,
    effective_from       DATE          NOT NULL,
    seq_no               INT           NOT NULL,

    -- 配属期間
    effective_to         DATE              NULL,
    status_code          NVARCHAR(10)      NOT NULL DEFAULT 'ACTIVE',
    
    -- 配属詳細
    base_location_code   NVARCHAR(10)      NULL,
    employment_type      NVARCHAR(10)      NULL,
    grade_code           NVARCHAR(10)      NULL,
    salary_band_code     NVARCHAR(10)      NULL,
    
    -- 組織関係
    manager_emp_id       NVARCHAR(20)      NULL,
    project_code         NVARCHAR(20)      NULL,
    cost_center_code     NVARCHAR(20)      NULL,
    
    -- 勤務パターン
    work_pattern_code    NVARCHAR(10)      NULL,
    shift_group_code     NVARCHAR(10)      NULL,
    allow_remote         BIT               NULL,
    fte_ratio            DECIMAL(5,2)      NULL,

    -- 拡張属性（カスタムフィールド）
    attr1-attr12         NVARCHAR(100)     NULL,

    -- 監査情報
    created_at           DATETIME2(3)      NOT NULL DEFAULT SYSUTCDATETIME(),
    created_by           NVARCHAR(50)      NULL,
    updated_at           DATETIME2(3)      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_by           NVARCHAR(50)      NULL,
    rv                   ROWVERSION,   -- 楽観制御・同時更新検知

    CONSTRAINT PK_employee_assignment_history
        PRIMARY KEY CLUSTERED (employee_id, org_code, job_code, effective_from, seq_no)
);
```

**設計特徴:**
- **複合主キー**: 5つの要素で一意性を保証
- **時系列データ**: effective_from/effective_to による期間管理
- **ROWVERSIONによる楽観制御**: 同時更新検知機能
- **拡張性**: attr1-attr12 による将来的な要件変更対応
- **高精度監査**: DATETIME2(3) による ミリ秒精度のタイムスタンプ

### 3.2 インデックス設計

#### 3.2.1 推奨インデックス
```sql
-- 従業員テーブル
CREATE INDEX IX_employees_department ON employees (department);
CREATE INDEX IX_employees_employment_status ON employees (employment_status);
CREATE INDEX IX_employees_hire_date ON employees (hire_date);

-- 配属履歴テーブル
CREATE INDEX IX_assignment_history_employee_id ON employee_assignment_history (employee_id);
CREATE INDEX IX_assignment_history_effective_dates ON employee_assignment_history (effective_from, effective_to);
CREATE INDEX IX_assignment_history_org_job ON employee_assignment_history (org_code, job_code);
```

### 3.3 パフォーマンス最適化

#### 3.3.1 UPSERT性能比較
- **MERGE方式**: 1,693ms（3,000件処理）
- **一時テーブル方式**: 923ms（6,000操作）- 約45%高速化

#### 3.3.2 制約事項への対応
- **SQL Server パラメーター制限**: 2100個/クエリ
- **バッチサイズ自動調整**: カラム数に基づく動的計算
- **メモリ効率化**: ストリーム処理による大容量データ対応

## 4. システム非機能要件

### 4.1 パフォーマンス要件
- **API応答時間**: 95%tile で 50ms以下
- **CSV エクスポート**: 10万件で10秒以内
- **マルチスレッド効果**: 単一スレッドより3.2倍高速化
- **データベース処理**: 線形スケーラビリティ維持

### 4.2 セキュリティ要件
- **認証情報管理**: 環境変数による機密情報管理
- **入力値検証**: 全公開エンドポイントでBean Validation適用
- **SQLインジェクション対策**: MyBatis パラメーター化クエリ
- **ログセキュリティ**: PII情報のログ出力禁止

### 4.3 可用性・信頼性
- **楽観的ロック**: バージョン番号による同時更新制御
- **トランザクション管理**: 適切な境界設定
- **例外処理**: GlobalExceptionHandler による統一的処理
- **データ整合性**: 制約とバリデーションによる保証

### 4.4 運用・監視
- **構造化ログ**: 適切なレベル設定
- **パフォーマンス監視**: 処理時間測定機能
- **ヘルスチェック**: Spring Boot Actuator対応
- **設定管理**: プロファイル別環境設定