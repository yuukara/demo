package com.example123.demo;

import com.example123.demo.domain.Employee;
import com.example123.demo.domain.PopulationData;
import com.example123.demo.repository.EmployeeMapper;
import com.example123.demo.service.EmployeeService;
import com.example123.demo.service.PopulationDataService;
import com.example123.demo.service.SafeDataProcessingService;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 従業員情報管理デモアプリケーション
 *
 * <p>以下の機能を提供します： 1. 大量の従業員データの生成とデータベース保存 2. マルチスレッドとシングルスレッドでのCSVファイル生成の性能比較 3.
 * 安全なデータ処理サービスによる並列処理テスト 4. 人口データの読み込みテスト
 */
@SpringBootApplication
public class DemoApplication {

  private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

  /**
   * アプリケーションのエントリーポイント
   *
   * @param args コマンドライン引数
   */
  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  /**
   * アプリケーション起動時に実行されるコマンドラインランナー 各種データ処理とパフォーマンステストを実行します
   *
   * @param populationDataService 人口データ管理サービス
   * @param safeService 安全なデータ処理サービス
   * @param employeeMapper 従業員情報マッパー
   * @return CommandLineRunner インスタンス
   */
  @Bean
  public CommandLineRunner run(
      PopulationDataService populationDataService,
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
   * 従業員データと人口データの処理を実行します データの生成、CSV出力、データベース保存を行います
   *
   * @param employeeService 従業員情報管理サービス
   * @param populationDataService 人口データ管理サービス
   * @param employeeMapper 従業員情報マッパー
   */
  @SuppressWarnings({"unused"})
  private void runEmployeeAndPopulationTasks(
      EmployeeService employeeService,
      PopulationDataService populationDataService,
      EmployeeMapper employeeMapper) {
    log.info("Starting employee data generation and CSV creation process...");

    // テーブルのデータを削除
    log.info("Truncating employees table...");
    long startDelete = System.currentTimeMillis();
    employeeMapper.truncateTable();
    long deleteTime = System.currentTimeMillis() - startDelete;
    log.info("Deletion completed in {} seconds", String.format("%.2f", deleteTime / 1000.0));

    // データ生成
    long startGeneration = System.currentTimeMillis();
    log.info("Generating dummy employee data...");
    List<Employee> employees = employeeService.createDummyEmployees(1000000);
    long generationTime = System.currentTimeMillis() - startGeneration;
    log.info(
        "Data generation completed in {} seconds", String.format("%.2f", generationTime / 1000.0));

    // マルチスレッドでのCSV生成
    log.info("\n=== Multi-threaded CSV Generation ===");
    long startMultiThread = System.currentTimeMillis();
    employeeService.writeToCsv(employees, "employees_multi.csv");
    long multiThreadTime = System.currentTimeMillis() - startMultiThread;
    log.info(
        "Multi-threaded processing completed in {} seconds",
        String.format("%.2f", multiThreadTime / 1000.0));

    // シングルスレッドでのCSV生成
    log.info("\n=== Single-threaded CSV Generation ===");
    long startSingleThread = System.currentTimeMillis();
    employeeService.writeToCsvSingleThread(employees, "employees_single.csv");
    long singleThreadTime = System.currentTimeMillis() - startSingleThread;
    log.info(
        "Single-threaded processing completed in {} seconds",
        String.format("%.2f", singleThreadTime / 1000.0));

    // 性能比較の表示
    log.info("\n=== Performance Comparison ===");
    log.info("Single-thread time: {} seconds", String.format("%.2f", singleThreadTime / 1000.0));
    log.info("Multi-thread time: {} seconds", String.format("%.2f", multiThreadTime / 1000.0));
    double speedup = (double) singleThreadTime / multiThreadTime;
    log.info("Speed-up ratio: {}x", String.format("%.2f", speedup));

    // データベースへの保存
    log.info("\n=== Database Save Test ===");
    employeeService.saveEmployees(employees);

    // 人口データの読み込みテスト
    log.info("\n=== Population Data Loading Test ===");
    try {
      long startPopulation = System.currentTimeMillis();
      List<PopulationData> populationData = populationDataService.loadPopulationData();
      long populationTime = System.currentTimeMillis() - startPopulation;

      log.info(
          "Successfully loaded {} population records in {} seconds",
          populationData.size(),
          String.format("%.2f", populationTime / 1000.0));

      // サンプルデータの表示
      if (!populationData.isEmpty()) {
        log.info("\nSample record:");
        PopulationData sample = populationData.get(0);
        log.info(
            "Prefecture: {}, Year: {}, Total Population: {}",
            sample.getPrefecture(),
            sample.getYear(),
            sample.getTotalPopulationEstimate());
      }
    } catch (IOException | RuntimeException e) {
      log.error("Error loading population data: {}", e.getMessage(), e);
    }
  }

  /**
   * データ処理サービスの安全性テストを実行します SafeDataProcessingServiceを使用して安全なデータ処理を行います
   *
   * @param safeService 安全なデータ処理サービス
   */
  @SuppressWarnings("unused")
  private void runDataProcessingTasks(SafeDataProcessingService safeService) {
    log.info("\n\n=== Safe Data Processing Service Test ===");
    final int DATA_SIZE = 10_000_000;

    // --- Safe (Chunked) Service ---
    log.info("\n--- Running SafeDataProcessingService ---");
    long startSafe = System.currentTimeMillis();
    List<Integer> safeResult = safeService.processData(DATA_SIZE);
    long safeTime = System.currentTimeMillis() - startSafe;
    log.info(
        "Safe service processed {} records in {} seconds",
        safeResult.size(),
        String.format("%.2f", safeTime / 1000.0));

    // データ整合性チェック
    boolean dataIntegrityOk = safeResult.size() == DATA_SIZE;
    log.info(
        "Data integrity check: {} (Expected: {}, Actual: {})",
        dataIntegrityOk ? "PASS" : "FAIL",
        DATA_SIZE,
        safeResult.size());

    log.info("Safe data processing test completed successfully.");
  }
}
