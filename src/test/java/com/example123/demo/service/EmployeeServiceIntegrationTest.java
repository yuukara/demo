package com.example123.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example123.demo.domain.Employee;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** EmployeeService統合テストクラス リファクタリング後のサービス分割が正しく動作することを確認します */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EmployeeServiceIntegrationTest {

  @Autowired private EmployeeService employeeService;

  @Autowired private DataGenerationService dataGenerationService;

  @Autowired private CsvExportService csvExportService;

  @Autowired private EmployeeDataService employeeDataService;

  @Test
  public void testServiceInjection() {
    // 全サービスが正しくインジェクションされていることを確認
    assertNotNull(employeeService, "EmployeeService should be injected");
    assertNotNull(dataGenerationService, "DataGenerationService should be injected");
    assertNotNull(csvExportService, "CsvExportService should be injected");
    assertNotNull(employeeDataService, "EmployeeDataService should be injected");
  }

  @Test
  public void testCreateDummyEmployees() {
    // ダミーデータ生成のテスト
    int testCount = 10;
    List<Employee> employees = employeeService.createDummyEmployees(testCount);

    assertNotNull(employees, "Generated employees should not be null");
    assertEquals(
        testCount, employees.size(), "Generated employee count should match requested count");

    // 生成されたデータの検証
    Employee firstEmployee = employees.get(0);
    assertNotNull(firstEmployee.getId(), "Employee ID should not be null");
    assertNotNull(firstEmployee.getName(), "Employee name should not be null");
    assertNotNull(firstEmployee.getDepartment(), "Employee department should not be null");
    assertNotNull(firstEmployee.getEmail(), "Employee email should not be null");
  }

  @Test
  public void testDataGenerationService() {
    // DataGenerationServiceの直接テスト
    int testCount = 5;
    List<Employee> employees = dataGenerationService.createDummyEmployees(testCount);

    assertNotNull(employees, "Generated employees should not be null");
    assertEquals(
        testCount, employees.size(), "Generated employee count should match requested count");

    // 各従業員データの基本検証
    for (Employee employee : employees) {
      assertNotNull(employee.getId(), "Employee ID should not be null");
      assertNotNull(employee.getName(), "Employee name should not be null");
      assertNotNull(employee.getDepartment(), "Employee department should not be null");
      assertTrue(employee.getEmail().contains("@example.com"), "Email should contain @example.com");
    }
  }

  @Test
  public void testCsvExportService() throws Exception {
    // テスト用データの準備
    List<Employee> employees = dataGenerationService.createDummyEmployees(3);
    String testFilePath = "test_employees.csv";

    try {
      // CSV出力のテスト
      csvExportService.writeToCsvSingleThread(employees, testFilePath);

      // ファイルが作成されていることを確認
      File csvFile = new File(testFilePath);
      assertTrue(csvFile.exists(), "CSV file should be created");
      assertTrue(csvFile.length() > 0, "CSV file should not be empty");

      // CSV内容の基本検証
      List<String> lines = Files.readAllLines(Paths.get(testFilePath));
      assertEquals(4, lines.size(), "Should have header + 3 data rows"); // ヘッダー + データ3行

      // ヘッダー行の確認
      String header = lines.get(0);
      assertTrue(header.contains("ID"), "Header should contain ID");
      assertTrue(header.contains("Name"), "Header should contain Name");
      assertTrue(header.contains("Department"), "Header should contain Department");

    } finally {
      // テストファイルのクリーンアップ
      Files.deleteIfExists(Paths.get(testFilePath));
    }
  }

  @Test
  public void testEmployeeServiceDelegation() {
    // EmployeeServiceがサービス分割後も正しく動作することを確認

    // 1. データ生成の委譲テスト
    List<Employee> employees = employeeService.createDummyEmployees(2);
    assertNotNull(employees, "Employees should be generated through delegation");
    assertEquals(2, employees.size(), "Should generate requested number of employees");

    // 2. CSV出力の委譲テスト
    String testFilePath = "delegation_test.csv";
    try {
      employeeService.writeToCsvSingleThread(employees, testFilePath);
      File csvFile = new File(testFilePath);
      assertTrue(csvFile.exists(), "CSV should be created through delegation");
    } finally {
      // クリーンアップ
      try {
        Files.deleteIfExists(Paths.get(testFilePath));
      } catch (Exception e) {
        // テストクリーンアップエラーは無視
      }
    }
  }

  @Test
  public void testRandomEmployeesGeneration() {
    // ランダムデータ生成のテスト（小さなデータセットで）
    List<Employee> randomEmployees = dataGenerationService.createRandomEmployees(5);

    assertNotNull(randomEmployees, "Random employees should not be null");
    assertEquals(5, randomEmployees.size(), "Should generate 5 random employees");

    // ランダムデータの基本検証
    for (Employee emp : randomEmployees) {
      assertNotNull(emp.getId(), "Random employee ID should not be null");
      assertNotNull(emp.getName(), "Random employee name should not be null");
      assertNotNull(emp.getDepartment(), "Random employee department should not be null");
      assertTrue(
          emp.getEmail().startsWith("employee"), "Random email should start with 'employee'");
      assertTrue(
          emp.getEmployment_status().matches("正社員|契約社員|パートタイマー|アルバイト"),
          "Employment status should be valid");
    }
  }

  @Test
  public void testServiceArchitecture() {
    // アーキテクチャの整合性確認：各サービスが責務を分離していることを確認

    // 1. DataGenerationServiceは純粋にデータ生成のみ
    List<Employee> generated = dataGenerationService.createDummyEmployees(1);
    assertFalse(generated.isEmpty(), "DataGenerationService should generate data");

    // 2. EmployeeServiceは各サービスを組み合わせて使用
    List<Employee> delegated = employeeService.createDummyEmployees(1);
    assertFalse(delegated.isEmpty(), "EmployeeService should delegate to DataGenerationService");

    // 3. データの一貫性確認
    assertEquals(generated.size(), delegated.size(), "Both should generate same number of records");
  }
}
