# 設計書：従業員データCSV出力機能

## 1. プロジェクト概要

このプロジェクトは、Spring Bootを利用して大量のダミー従業員データを生成し、それをCSVファイルとして出力するJavaアプリケーションです。

- **起動処理 ([`src/main/java/com/example123/demo/DemoApplication.java`](src/main/java/com/example123/demo/DemoApplication.java)):** アプリケーション起動時に10,000人分の従業員データを生成し、`employees.csv`に書き込みます。
- **データモデル ([`src/main/java/com/example123/demo/Employee.java`](src/main/java/com/example123/demo/Employee.java)):** 従業員は`id`, `name`, `department`, `email`の情報を持ちます。
- **ビジネスロジック ([`src/main/java/com/example123/demo/EmployeeService.java`](src/main/java/com/example123/demo/EmployeeService.java)):** ダミーデータ生成とCSV書き込み処理を担当します。

## 2. 現状のアーキテクチャ

現在のCSV書き込み処理 (`writeToCsv`) は、スレッドプールを利用して並列処理を行っていますが、ファイルへの書き込み部分で同期処理を行っています。

```mermaid
graph TD
    subgraph EmployeeService
        A[1. createDummyEmployees] --> B[2. writeToCsv];
    end

    subgraph writeToCsv
        B -- 10,000 employees --> C{ExecutorService (Thread Pool)};
        C --> D1[Thread 1];
        C --> D2[Thread 2];
        C --> Dn[...];
        D1 -- synchronized block --> E{BufferedWriter};
        D2 -- synchronized block --> E;
        Dn -- synchronized block --> E;
    end

    E --> F[employees.csv];
```

## 3. 課題

現状のアーキテクチャには、以下の2つの主要な課題が存在します。

### 3.1. パフォーマンスのボトルネック

複数のスレッドが同時にファイルへ書き込もうとする際、`synchronized`ブロックによって一度に1つのスレッドしか書き込み処理を実行できません。これにより、他のスレッドはロックが解放されるのを待つ必要があり、スレッド間の競合が発生します。結果として、マルチスレッドの利点を十分に活かせず、パフォーマンスのボトルネックとなっています。

### 3.2. データの順序が保証されない

スレッドはOSのスケジューリングによって非同期に実行されるため、CSVファイルに書き込まれる従業員の順序は、元のリストの順序と一致しません。[`employees.csv`](employees.csv)を確認すると、IDがランダムな順序で書き込まれていることがわかります。これは、データの整合性や後続処理の正確性に影響を与える可能性があります。

## 4. 改善案

現状の課題を解決するため、以下のアーキテクチャ変更を提案します。

### 4.1. 改善アプローチ

1.  **タスクの分割:** 従業員リストを複数のサブリストに分割し、各スレッドが1つのサブリストを担当します。
2.  **メモリ上での並列処理:** 各スレッドは、担当するサブリストの従業員データをCSV形式の文字列に変換し、ファイルではなくメモリ上のバッファに結果を保持します。
3.  **結果の集約と一括書き込み:** 全てのスレッドの処理が完了した後、メインスレッドが各スレッドの結果を**元の順序通りに**集約し、結合された単一の文字列として一度にファイルへ書き込みます。

このアプローチにより、`synchronized`ブロックが不要になりスレッド間の競合が解消されます。また、ファイルへの書き込みが一度で済むため、I/Oのオーバーヘッドも大幅に削減できます。

### 4.2. 新しいアーキテクチャ

```mermaid
graph TD
    subgraph Improved writeToCsv
        A[1. 従業員リストをサブリストに分割] --> B{ExecutorService (スレッドプール)};
        A -- サブリスト1 --> C1[スレッド1: CSV文字列を生成];
        A -- サブリスト2 --> C2[スレッド2: CSV文字列を生成];
        A -- サブリストn --> Cn[...];

        subgraph "各スレッドの処理"
            C1 --> D1[結果をメモリに保持];
            C2 --> D2[結果をメモリに保持];
            Cn --> Dn[結果をメモリに保持];
        end

        B -- 全てのスレッドが完了 --> E[メインスレッド];
        E -- 順序通りに結果を集約 --> F[文字列を結合];
        F -- 一括書き込み --> G[employees.csv];
    end
```

### 4.3. スレッド管理

本実装におけるスレッド数は、固定値ではなく、**プログラムを実行するマシンのCPUコア数に応じて動的に決定されます**。

これは、`EmployeeService`クラス内の以下のコードによって実現されています。

```java
int numThreads = Runtime.getRuntime().availableProcessors();
ExecutorService executor = Executors.newFixedThreadPool(numThreads);
```

`Runtime.getRuntime().availableProcessors()`メソッドは、Java仮想マシンが利用可能なプロセッサ（コア）数を返します。これにより、例えば8コアCPUのマシンで実行すれば8つのスレッドが、16コアのマシンなら16のスレッドが作成され、ハードウェアの処理能力を最大限に活用します。

10,000件のデータは1,000件ずつのバッチ（合計10タスク）に分割され、これらのタスクが動的に作成されたスレッドプールに割り当てられて並列実行されます。このアプローチにより、どのような実行環境でも最適なパフォーマンスを発揮できる、汎用性の高い実装となっています。