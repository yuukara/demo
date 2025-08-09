package com.example123.demo.service;

import com.example123.demo.aop.Loggable;
import com.example123.demo.aop.PerformanceMonitoring;
import com.example123.demo.domain.Employee;
import java.util.List;
import org.springframework.stereotype.Service;

/** 従業員情報を管理するコアサービスクラス 基本的なCRUD操作とデータベース接続機能を提供します */
@Service
public class EmployeeService {

  private final DataGenerationService dataGenerationService;
  private final CsvExportService csvExportService;
  private final EmployeeDataService employeeDataService;

  public EmployeeService(
      DataGenerationService dataGenerationService,
      CsvExportService csvExportService,
      EmployeeDataService employeeDataService) {
    this.dataGenerationService = dataGenerationService;
    this.csvExportService = csvExportService;
    this.employeeDataService = employeeDataService;
  }

  /**
   * ダミーの従業員データを生成します 生成されるデータには基本情報、個人情報、システム管理情報が含まれます
   *
   * @param count 生成する従業員データの件数
   * @return 生成された従業員情報のリスト
   */
  /** 6000件のランダムな従業員データを生成し、一括UPSERTを行います。 既存データとの重複を含み、MERGE文によるUPSERT処理をテストします。 */
  public void generateAndUpsertRandomEmployees() {
    generateAndUpsertRandomEmployees(6000);
  }

  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = true,
      includeResult = false,
      value = "従業員データ生成・UPSERT処理")
  @PerformanceMonitoring(threshold = 5000, operation = "BATCH_UPSERT_WITH_DATA_GENERATION")
  public void generateAndUpsertRandomEmployees(int count) {
    // 1) 基礎データ（更新対象）を事前投入
    employeeDataService.prepareBaseDataForUpsert();

    // 2) UPSERT用データを生成（80%更新・20%新規）
    List<Employee> employees = dataGenerationService.createRandomEmployees(count);

    // 3) バッチUPSERT実行
    employeeDataService.upsertEmployeesInBatches(employees);
  }

  /**
   * 6000件のランダムな従業員データを生成し、一時テーブルを使用した一括UPSERTを行います。 既存データとの重複を含み、一時テーブルによるUPSERT処理をテストします。
   *
   * @return 処理件数を含むMap（updateCount: 更新件数, insertCount: 挿入件数）
   */
  public java.util.Map<String, Integer> generateAndUpsertRandomEmployeesViaTempTable() {
    return generateAndUpsertRandomEmployeesViaTempTable(6000);
  }

  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = true,
      includeResult = false,
      value = "一時テーブルUPSERT処理")
  @PerformanceMonitoring(threshold = 5000, operation = "TEMP_TABLE_UPSERT_WITH_DATA_GENERATION")
  public java.util.Map<String, Integer> generateAndUpsertRandomEmployeesViaTempTable(int count) {
    // 1) 基礎データ（更新対象）を事前投入
    employeeDataService.prepareBaseDataForUpsert();

    // 2) UPSERT用データを生成（80%更新・20%新規）
    List<Employee> employees = dataGenerationService.createRandomEmployees(count);

    // 3) 一時テーブル経由バッチUPSERT実行
    return employeeDataService.upsertEmployeesViaTempTableInBatches(employees);
  }

  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = false,
      includeResult = false,
      value = "従業員テーブル全削除")
  @PerformanceMonitoring(threshold = 2000, operation = "TRUNCATE_EMPLOYEES")
  public void truncateEmployeesTable() {
    employeeDataService.truncateEmployeesTable();
  }

  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = false,
      includeResult = false,
      value = "従業員データ保存処理")
  @PerformanceMonitoring(threshold = 3000, operation = "BULK_INSERT_EMPLOYEES")
  public void saveEmployees(List<Employee> employees) {
    employeeDataService.saveEmployees(employees);
  }

  /**
   * ダミーの従業員データを生成します 生成されるデータには基本情報、個人情報、システム管理情報が含まれます
   *
   * @param count 生成する従業員データの件数
   * @return 生成された従業員情報のリスト
   */
  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = true,
      includeResult = false,
      value = "ダミー従業員データ生成")
  @PerformanceMonitoring(threshold = 2000, operation = "DUMMY_DATA_GENERATION")
  public List<Employee> createDummyEmployees(int count) {
    return dataGenerationService.createDummyEmployees(count);
  }

  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = false,
      includeResult = false,
      value = "CSV出力処理（シングルスレッド）")
  @PerformanceMonitoring(threshold = 5000, operation = "CSV_EXPORT_SINGLE_THREAD")
  public void writeToCsvSingleThread(List<Employee> employees, String filePath) {
    csvExportService.writeToCsvSingleThread(employees, filePath);
  }

  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = false,
      includeResult = false,
      value = "CSV出力処理（マルチスレッド）")
  @PerformanceMonitoring(threshold = 5000, operation = "CSV_EXPORT_MULTI_THREAD")
  public void writeToCsv(List<Employee> employees, String filePath) {
    csvExportService.writeToCsv(employees, filePath);
  }
}
