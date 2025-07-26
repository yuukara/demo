# 従業員情報管理システム

## 概要
このプロジェクトは、大規模な従業員データを効率的に管理・処理するデモアプリケーションです。
マルチスレッド処理を活用し、高速なデータ処理と性能比較機能を提供します。

## 主な機能

### 1. 従業員データ管理
- 従業員情報の一括登録・管理
- 15個のカラムを持つ詳細な従業員情報
  - 基本情報（ID、氏名、部署、役職など）
  - 個人情報（生年月日、性別、連絡先など）
  - 管理情報（登録者、更新者、タイムスタンプ、バージョン）

### 2. 高速データ処理
- マルチスレッドによる並列処理
- SQLServerのパラメーター制限に対応（1回の処理で2100パラメーター以下）
- バッチサイズの最適化（100レコード/バッチ）

### 3. CSV出力機能
- マルチスレッド処理によるCSVファイル生成
- シングルスレッド処理との性能比較機能
- 全カラムのデータをCSV形式で出力

### 4. 性能測定・比較
- 処理時間の測定と表示
- マルチスレッドと単一スレッドの性能比較
- 安全な処理と非安全な処理の性能比較

## 技術スタック
- Java 23
- Spring Boot 3.5.3
- MyBatis
- Microsoft SQL Server
- Maven

## 必要要件
- JDK 23以上
- Microsoft SQL Server 2022
- Maven 3.x

## セットアップ方法

1. データベースの準備
```sql
CREATE DATABASE company_db;
USE company_db;
CREATE TABLE employees (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    department VARCHAR(255),
    position VARCHAR(255),
    employment_status VARCHAR(255),
    hire_date DATE,
    phone_number VARCHAR(255),
    email VARCHAR(255),
    birth_date DATE,
    gender VARCHAR(50),
    created_by VARCHAR(255),
    created_at DATETIME,
    updated_by VARCHAR(255),
    updated_at DATETIME,
    version BIGINT
);
```

2. アプリケーション設定
- `src/main/resources/application.properties`のデータベース接続設定を環境に合わせて修正

3. ビルドと実行
```bash
mvn clean install
mvn spring-boot:run
```

## 性能特性
- 100万件のデータ処理時の実行結果例：
  - データ生成: 約1.6秒
  - CSV出力（マルチスレッド）: 約1.2秒
  - CSV出力（シングルスレッド）: 約3.8秒
  - データベース保存: 約17.5秒
  - 速度向上比: 約3.2倍（マルチスレッド vs シングルスレッド）

## 設計上の考慮点
1. スケーラビリティ
   - マルチスレッド処理による高速化
   - バッチサイズの最適化

2. データ整合性
   - 楽観的ロックによる排他制御
   - トランザクション管理

3. パフォーマンス
   - SQLServerのパラメーター制限への対応
   - 効率的なバッチ処理

## 今後の展望
- WebUIの追加
- より詳細な検索機能
- レポート出力機能
- バッチ処理の更なる最適化

## ライセンス
MIT License