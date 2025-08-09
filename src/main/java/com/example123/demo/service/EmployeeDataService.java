package com.example123.demo.service;

import com.example123.demo.aop.Loggable;
import com.example123.demo.aop.PerformanceMonitoring;
import com.example123.demo.domain.Employee;
import com.example123.demo.repository.EmployeeMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/** 従業員データの操作を行うサービスクラス データベースへの保存、UPSERT処理、並列処理などの機能を提供します */
@Service
@Validated
public class EmployeeDataService {

  private static final Logger log = LoggerFactory.getLogger(EmployeeDataService.class);

  /** バッチ処理のサイズ SQLServerのパラメーター制限(2100)を考慮し、15カラム × 100レコード = 1500パラメーターとなるように設定 */
  private static final int BATCH_SIZE = 100;

  private final EmployeeMapper employeeMapper;
  private final DataGenerationService dataGenerationService;

  public EmployeeDataService(
      EmployeeMapper employeeMapper, DataGenerationService dataGenerationService) {
    this.employeeMapper = employeeMapper;
    this.dataGenerationService = dataGenerationService;
  }

  /**
   * 従業員情報をデータベースに保存します 内部で並列処理による保存を行います
   *
   * @param employees 保存する従業員情報のリスト
   */
  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = false,
      includeResult = false,
      value = "従業員データ保存処理")
  @PerformanceMonitoring(threshold = 3000, operation = "EMPLOYEE_SAVE")
  public void saveEmployees(@NotEmpty(message = "保存する従業員リストが空です") @Valid List<Employee> employees) {
    saveEmployeesInParallel(employees);
  }

  /**
   * 従業員情報を並列処理でデータベースに保存します 利用可能なプロセッサ数に基づいてスレッドプールを作成し、 バッチサイズごとに分割して並列で保存を行います
   *
   * @param employees 保存する従業員情報のリスト
   */
  public void saveEmployeesInParallel(
      @NotEmpty(message = "保存する従業員リストが空です") @Valid List<Employee> employees) {
    int numThreads = Runtime.getRuntime().availableProcessors();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);

    try {
      List<List<Employee>> batches = new ArrayList<>();
      for (int i = 0; i < employees.size(); i += BATCH_SIZE) {
        batches.add(
            new ArrayList<>(employees.subList(i, Math.min(i + BATCH_SIZE, employees.size()))));
      }

      List<Future<?>> futures = new ArrayList<>();
      for (List<Employee> batch : batches) {
        futures.add(executor.submit(() -> employeeMapper.bulkInsert(batch)));
      }

      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (ExecutionException | InterruptedException e) {
          log.error("Error during parallel bulk insert", e);
          if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
        }
      }
    } finally {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
          log.warn("Executor did not terminate in the specified time.");
          List<Runnable> droppedTasks = executor.shutdownNow();
          log.warn("Executor was abruptly shut down. {} tasks were dropped.", droppedTasks.size());
        }
      } catch (InterruptedException e) {
        log.warn("Executor termination was interrupted.", e);
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * 従業員情報を一括でUPSERTします。 バッチサイズごとに分割して処理を行います。
   *
   * @param employees UPSERT対象の従業員情報のリスト
   */
  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = false,
      includeResult = false,
      value = "従業員MERGE-UPSERT処理")
  @PerformanceMonitoring(threshold = 5000, operation = "MERGE_UPSERT_BATCH")
  public void upsertEmployeesInBatches(List<Employee> employees) {
    int numThreads = Runtime.getRuntime().availableProcessors();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);

    try {
      List<List<Employee>> batches = new ArrayList<>();
      for (int i = 0; i < employees.size(); i += BATCH_SIZE) {
        batches.add(
            new ArrayList<>(employees.subList(i, Math.min(i + BATCH_SIZE, employees.size()))));
      }

      List<Future<?>> futures = new ArrayList<>();
      for (List<Employee> batch : batches) {
        futures.add(executor.submit(() -> employeeMapper.bulkUpsert(batch)));
      }

      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (ExecutionException | InterruptedException e) {
          log.error("Error during parallel MERGE upsert", e);
          if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
        }
      }
    } finally {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
          log.warn("Executor did not terminate in the specified time.");
          List<Runnable> droppedTasks = executor.shutdownNow();
          log.warn("Executor was abruptly shut down. {} tasks were dropped.", droppedTasks.size());
        }
      } catch (InterruptedException e) {
        log.warn("Executor termination was interrupted.", e);
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * 従業員情報を一時テーブル方式で一括UPSERTします。 バッチサイズごとに分割して処理を行います。
   *
   * @param employees UPSERT対象の従業員情報のリスト
   * @return 全体の処理件数を含むMap（updateCount: 更新件数合計, insertCount: 挿入件数合計）
   */
  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = false,
      includeResult = false,
      value = "従業員一時テーブルUPSERT処理")
  @PerformanceMonitoring(threshold = 5000, operation = "TEMP_TABLE_UPSERT_BATCH")
  public java.util.Map<String, Integer> upsertEmployeesViaTempTableInBatches(
      List<Employee> employees) {
    int numThreads = Runtime.getRuntime().availableProcessors();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);

    int totalUpdateCount = 0;
    int totalInsertCount = 0;

    try {
      // バッチ処理
      List<List<Employee>> batches = new ArrayList<>();
      for (int i = 0; i < employees.size(); i += BATCH_SIZE) {
        batches.add(
            new ArrayList<>(employees.subList(i, Math.min(i + BATCH_SIZE, employees.size()))));
      }

      // 並列処理でバッチを実行
      List<Future<java.util.Map<String, Integer>>> futures = new ArrayList<>();
      for (List<Employee> batch : batches) {
        futures.add(executor.submit(() -> employeeMapper.bulkUpsertViaTempTable(batch)));
      }

      for (Future<java.util.Map<String, Integer>> future : futures) {
        try {
          java.util.Map<String, Integer> result = future.get();
          totalUpdateCount += result.get("updateCount");
          totalInsertCount += result.get("insertCount");
        } catch (ExecutionException | InterruptedException e) {
          log.error("Error during parallel Temp Table upsert", e);
          if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
        }
      }
    } finally {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
          log.warn("Executor did not terminate in the specified time.");
          List<Runnable> droppedTasks = executor.shutdownNow();
          log.warn("Executor was abruptly shut down. {} tasks were dropped.", droppedTasks.size());
        }
      } catch (InterruptedException e) {
        log.warn("Executor termination was interrupted.", e);
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    java.util.Map<String, Integer> result = new java.util.HashMap<>();
    result.put("updateCount", totalUpdateCount);
    result.put("insertCount", totalInsertCount);
    return result;
  }

  /** 従業員テーブルのデータを全て削除します。 */
  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = false,
      includeResult = false,
      value = "従業員テーブル全削除")
  @PerformanceMonitoring(threshold = 2000, operation = "TRUNCATE_EMPLOYEES")
  public void truncateEmployeesTable() {
    employeeMapper.truncateTable();
  }

  /** UPSERT処理用の基礎データを準備します。 更新対象となるデータ（E000000-E009999）を事前にテーブルに投入します。 */
  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = false,
      includeResult = false,
      value = "UPSERT基礎データ準備")
  @PerformanceMonitoring(threshold = 3000, operation = "PREPARE_BASE_DATA")
  public void prepareBaseDataForUpsert() {

    List<Employee> baseEmployees = dataGenerationService.createBaseDataForUpsert();

    // バッチでベースデータを投入
    saveEmployeesInParallel(baseEmployees);
  }
}
