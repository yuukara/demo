package com.example123.demo.service;

import com.example123.demo.aop.Loggable;
import com.example123.demo.aop.PerformanceMonitoring;
import com.example123.demo.domain.Employee;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 従業員データのCSV出力機能を提供するサービスクラス シングルスレッド処理とマルチスレッド処理の両方をサポートします */
@Service
public class CsvExportService {

  private static final Logger log = LoggerFactory.getLogger(CsvExportService.class);

  /** バッチ処理のサイズ CSVエクスポート時のパフォーマンス最適化のために使用 */
  private static final int BATCH_SIZE = 100;

  /**
   * 従業員情報をCSVファイルに出力します（シングルスレッド処理） 全データを1つのスレッドで逐次的に処理します 出力されるCSVには全カラムの情報が含まれます
   *
   * @param employees 出力する従業員情報のリスト
   * @param filePath 出力先のCSVファイルパス
   */
  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = false,
      includeResult = false,
      value = "CSV出力処理（シングルスレッド）")
  @PerformanceMonitoring(threshold = 5000, operation = "CSV_EXPORT_SINGLE_THREAD")
  public void writeToCsvSingleThread(List<Employee> employees, String filePath) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write(
          """
                ID,Name,Department,Position,EmploymentStatus,HireDate,PhoneNumber,Email,BirthDate,Gender,CreatedBy,CreatedAt,UpdatedBy,UpdatedAt,Version
                """);

      for (Employee employee : employees) {
        writer.write(
            String.format(
                "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d\n",
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getEmployment_status(),
                employee.getHire_date() != null ? employee.getHire_date().toString() : "",
                employee.getPhone_number(),
                employee.getEmail(),
                employee.getBirth_date() != null ? employee.getBirth_date().toString() : "",
                employee.getGender(),
                employee.getCreated_by(),
                employee.getCreated_at() != null ? employee.getCreated_at().toString() : "",
                employee.getUpdated_by(),
                employee.getUpdated_at() != null ? employee.getUpdated_at().toString() : "",
                employee.getVersion()));
      }
    } catch (IOException e) {
      log.error("Error writing to CSV file in single-thread mode", e);
      throw new RuntimeException("CSV出力処理でエラーが発生しました", e);
    }
  }

  /**
   * 従業員情報をCSVファイルに出力します（マルチスレッド処理） 利用可能なプロセッサ数に基づいてスレッドプールを作成し、 データを分割して並列で処理します
   * 出力されるCSVには全カラムの情報が含まれます
   *
   * @param employees 出力する従業員情報のリスト
   * @param filePath 出力先のCSVファイルパス
   */
  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = false,
      includeResult = false,
      value = "CSV出力処理（マルチスレッド）")
  @PerformanceMonitoring(threshold = 5000, operation = "CSV_EXPORT_MULTI_THREAD")
  public void writeToCsv(List<Employee> employees, String filePath) {
    int numThreads = Runtime.getRuntime().availableProcessors();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);

    try {
      List<List<Employee>> batches = new ArrayList<>();
      for (int i = 0; i < employees.size(); i += BATCH_SIZE) {
        batches.add(employees.subList(i, Math.min(i + BATCH_SIZE, employees.size())));
      }

      List<Future<String>> futures = new ArrayList<>();
      for (List<Employee> batch : batches) {
        futures.add(executor.submit(new CsvGeneratorTask(batch)));
      }

      List<String> results = new ArrayList<>();
      for (Future<String> future : futures) {
        try {
          results.add(future.get());
        } catch (ExecutionException | InterruptedException e) {
          log.error("Error getting result from a CSV generator task", e);
          if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
        }
      }

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
        writer.write(
            """
                ID,Name,Department,Position,EmploymentStatus,HireDate,PhoneNumber,Email,BirthDate,Gender,CreatedBy,CreatedAt,UpdatedBy,UpdatedAt,Version
                """);
        for (String result : results) {
          writer.write(result);
        }
      } catch (IOException e) {
        log.error("Error writing to CSV file in multi-thread mode", e);
        throw new RuntimeException("CSV出力処理でエラーが発生しました", e);
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

  /** CSVデータを生成するタスククラス 従業員情報のバッチをCSV形式の文字列に変換します マルチスレッド処理での並列データ変換に使用されます */
  private static class CsvGeneratorTask implements Callable<String> {
    private final List<Employee> employees;

    /**
     * CSVジェネレータータスクを初期化します
     *
     * @param employees 処理対象の従業員情報リスト
     */
    public CsvGeneratorTask(List<Employee> employees) {
      this.employees = employees;
    }

    /**
     * タスクを実行し、従業員データをCSV形式の文字列に変換します
     *
     * @return CSV形式に変換されたデータ（改行区切り）
     */
    @Override
    public String call() {
      return employees.stream()
          .map(
              employee ->
                  String.join(
                      ",",
                      employee.getId(),
                      employee.getName(),
                      employee.getDepartment(),
                      employee.getPosition(),
                      employee.getEmployment_status(),
                      employee.getHire_date() != null ? employee.getHire_date().toString() : "",
                      employee.getPhone_number(),
                      employee.getEmail(),
                      employee.getBirth_date() != null ? employee.getBirth_date().toString() : "",
                      employee.getGender(),
                      employee.getCreated_by(),
                      employee.getCreated_at() != null ? employee.getCreated_at().toString() : "",
                      employee.getUpdated_by(),
                      employee.getUpdated_at() != null ? employee.getUpdated_at().toString() : "",
                      String.valueOf(employee.getVersion())))
          .collect(Collectors.joining("\n", "", "\n"));
    }
  }
}
