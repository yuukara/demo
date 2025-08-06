package com.example123.demo;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example123.demo.domain.Employee;
import com.example123.demo.domain.PopulationData;
import com.example123.demo.repository.EmployeeMapper;
import com.example123.demo.service.EmployeeService;
import com.example123.demo.service.PopulationDataService;
import com.example123.demo.service.SafeDataProcessingService;

/**
 * 従業員情報管理デモアプリケーション
 * 
 * 以下の機能を提供します：
 * 1. 大量の従業員データの生成とデータベース保存
 * 2. マルチスレッドとシングルスレッドでのCSVファイル生成の性能比較
 * 3. 安全なデータ処理サービスによる並列処理テスト
 * 4. 人口データの読み込みテスト
 */
@SpringBootApplication
public class DemoApplication {

    /**
     * アプリケーションのエントリーポイント
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    /**
     * アプリケーション起動時に実行されるコマンドラインランナー
     * 各種データ処理とパフォーマンステストを実行します
     *
     * @param populationDataService 人口データ管理サービス
     * @param safeService 安全なデータ処理サービス
     * @param employeeMapper 従業員情報マッパー
     * @return CommandLineRunner インスタンス
     */
    @Bean
    public CommandLineRunner run(PopulationDataService populationDataService,
                                 SafeDataProcessingService safeService,
                                 EmployeeMapper employeeMapper) {
        return args -> {
            // employeeController.testMergeUpsert();  // 一時的に無効化

            // 以下の処理も引き続き無効化
            // runEmployeeAndPopulationTasks(employeeService, populationDataService, employeeMapper);
            // runDataProcessingTasks(safeService);
        };
    }

    /**
     * 従業員データと人口データの処理を実行します
     * データの生成、CSV出力、データベース保存を行います
     *
     * @param employeeService 従業員情報管理サービス
     * @param populationDataService 人口データ管理サービス
     * @param employeeMapper 従業員情報マッパー
     */
    @SuppressWarnings({"unused", "CallToPrintStackTrace"})
    private void runEmployeeAndPopulationTasks(EmployeeService employeeService,
                                             PopulationDataService populationDataService,
                                             EmployeeMapper employeeMapper) {
        System.out.println("Starting employee data generation and CSV creation process...");

        // テーブルのデータを削除
        System.out.println("Truncating employees table...");
        long startDelete = System.currentTimeMillis();
        employeeMapper.truncateTable();
        long deleteTime = System.currentTimeMillis() - startDelete;
        System.out.printf("Deletion completed in %.2f seconds%n", deleteTime / 1000.0);

        // データ生成
        long startGeneration = System.currentTimeMillis();
        System.out.println("Generating dummy employee data...");
        List<Employee> employees = employeeService.createDummyEmployees(1000000);
        long generationTime = System.currentTimeMillis() - startGeneration;
        System.out.printf("Data generation completed in %.2f seconds%n", generationTime / 1000.0);

        // マルチスレッドでのCSV生成
        System.out.println("\n=== Multi-threaded CSV Generation ===");
        long startMultiThread = System.currentTimeMillis();
        employeeService.writeToCsv(employees, "employees_multi.csv");
        long multiThreadTime = System.currentTimeMillis() - startMultiThread;
        System.out.printf("Multi-threaded processing completed in %.2f seconds%n", multiThreadTime / 1000.0);

        // シングルスレッドでのCSV生成
        System.out.println("\n=== Single-threaded CSV Generation ===");
        long startSingleThread = System.currentTimeMillis();
        employeeService.writeToCsvSingleThread(employees, "employees_single.csv");
        long singleThreadTime = System.currentTimeMillis() - startSingleThread;
        System.out.printf("Single-threaded processing completed in %.2f seconds%n", singleThreadTime / 1000.0);

        // 性能比較の表示
        System.out.println("\n=== Performance Comparison ===");
        System.out.printf("Single-thread time: %.2f seconds%n", singleThreadTime / 1000.0);
        System.out.printf("Multi-thread time: %.2f seconds%n", multiThreadTime / 1000.0);
        double speedup = (double) singleThreadTime / multiThreadTime;
        System.out.printf("Speed-up ratio: %.2fx%n", speedup);

        // データベースへの保存
        System.out.println("\n=== Database Save Test ===");
        employeeService.saveEmployees(employees);

        // 人口データの読み込みテスト
        System.out.println("\n=== Population Data Loading Test ===");
        try {
            long startPopulation = System.currentTimeMillis();
            List<PopulationData> populationData = populationDataService.loadPopulationData();
            long populationTime = System.currentTimeMillis() - startPopulation;

            System.out.printf("Successfully loaded %d population records in %.2f seconds%n",
                populationData.size(), populationTime / 1000.0);

            // サンプルデータの表示
            if (!populationData.isEmpty()) {
                System.out.println("\nSample record:");
                PopulationData sample = populationData.get(0);
                System.out.printf("Prefecture: %s, Year: %s, Total Population: %s%n",
                    sample.getPrefecture(),
                    sample.getYear(),
                    sample.getTotalPopulationEstimate());
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Error loading population data: " + e.getMessage());
            // 意図的なデバッグ出力
            e.printStackTrace();
        }
    }

    /**
     * データ処理サービスの安全性テストを実行します
     * SafeDataProcessingServiceを使用して安全なデータ処理を行います
     *
     * @param safeService 安全なデータ処理サービス
     */
    @SuppressWarnings("unused")
    private void runDataProcessingTasks(SafeDataProcessingService safeService) {
        System.out.println("\n\n=== Safe Data Processing Service Test ===");
        final int DATA_SIZE = 10_000_000;

        // --- Safe (Chunked) Service ---
        System.out.println("\n--- Running SafeDataProcessingService ---");
        long startSafe = System.currentTimeMillis();
        List<Integer> safeResult = safeService.processData(DATA_SIZE);
        long safeTime = System.currentTimeMillis() - startSafe;
        System.out.printf("Safe service processed %d records in %.2f seconds%n",
            safeResult.size(), safeTime / 1000.0);

        // データ整合性チェック
        boolean dataIntegrityOk = safeResult.size() == DATA_SIZE;
        System.out.printf("Data integrity check: %s (Expected: %d, Actual: %d)%n",
            dataIntegrityOk ? "PASS" : "FAIL", DATA_SIZE, safeResult.size());

        System.out.println("Safe data processing test completed successfully.");
    }
}
