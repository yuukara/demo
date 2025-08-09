package com.example123.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example123.demo.domain.Employee;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** DataGenerationService単体テストクラス データ生成機能が正しく動作することを確認します */
public class DataGenerationServiceTest {

  private DataGenerationService dataGenerationService;

  @BeforeEach
  void setUp() {
    dataGenerationService = new DataGenerationService();
  }

  @Test
  void testCreateDummyEmployees() {
    // テスト実行
    int count = 5;
    List<Employee> employees = dataGenerationService.createDummyEmployees(count);

    // 基本検証
    assertNotNull(employees, "Generated employees should not be null");
    assertEquals(count, employees.size(), "Generated count should match requested count");

    // 各従業員データの詳細検証
    for (int i = 0; i < employees.size(); i++) {
      Employee emp = employees.get(i);

      // 基本フィールドの検証
      assertNotNull(emp.getId(), "Employee ID should not be null");
      assertEquals(String.valueOf(i + 1), emp.getId(), "Employee ID should be sequential");
      assertEquals("Employee " + (i + 1), emp.getName(), "Employee name should follow pattern");

      // 部署の検証
      assertNotNull(emp.getDepartment(), "Department should not be null");
      assertTrue(emp.getDepartment().startsWith("Department"), "Department should follow pattern");

      // メールアドレスの検証
      assertNotNull(emp.getEmail(), "Email should not be null");
      assertTrue(emp.getEmail().contains("@example.com"), "Email should contain @example.com");
      assertTrue(emp.getEmail().startsWith("employee"), "Email should start with employee");

      // 雇用形態の検証
      assertNotNull(emp.getEmployment_status(), "Employment status should not be null");
      assertTrue(
          emp.getEmployment_status().matches("正社員|契約社員|パートタイマー"),
          "Employment status should be valid");

      // 電話番号の検証
      assertNotNull(emp.getPhone_number(), "Phone number should not be null");
      assertTrue(
          emp.getPhone_number().matches("03-\\d{4}-\\d{4}"),
          "Phone number should follow format 03-XXXX-XXXX");

      // 日付フィールドの検証
      assertNotNull(emp.getHire_date(), "Hire date should not be null");
      assertNotNull(emp.getBirth_date(), "Birth date should not be null");

      // 性別の検証
      assertNotNull(emp.getGender(), "Gender should not be null");
      assertTrue(emp.getGender().matches("男性|女性"), "Gender should be valid");

      // システムフィールドの検証
      assertEquals("SYSTEM", emp.getCreated_by(), "Created by should be SYSTEM");
      assertEquals("SYSTEM", emp.getUpdated_by(), "Updated by should be SYSTEM");
      assertEquals(Long.valueOf(0), emp.getVersion(), "Version should be 0");
      assertNotNull(emp.getCreated_at(), "Created at should not be null");
      assertNotNull(emp.getUpdated_at(), "Updated at should not be null");
    }
  }

  @Test
  void testCreateRandomEmployees() {
    // テスト実行（小さなサイズでテスト）
    int count = 10;
    List<Employee> employees = dataGenerationService.createRandomEmployees(count);

    // 基本検証
    assertNotNull(employees, "Generated employees should not be null");
    assertEquals(count, employees.size(), "Generated count should match requested count");

    // ランダムデータの検証
    for (Employee emp : employees) {
      // ID形式の検証（EXXXXXXフォーマット）
      assertNotNull(emp.getId(), "Employee ID should not be null");
      assertTrue(emp.getId().matches("E\\d{6}"), "Employee ID should follow format EXXXXXX");

      // 名前の検証（日本語名前パターン）
      assertNotNull(emp.getName(), "Name should not be null");
      assertTrue(emp.getName().contains(" "), "Name should contain space (surname + given name)");

      // 部署の検証
      assertNotNull(emp.getDepartment(), "Department should not be null");
      String[] validDepartments = {
        "営業部", "総務部", "人事部", "経理部", "開発部", "製造部", "品質管理部", "マーケティング部", "カスタマーサービス部", "研究開発部"
      };
      boolean departmentValid = false;
      for (String dept : validDepartments) {
        if (emp.getDepartment().equals(dept)) {
          departmentValid = true;
          break;
        }
      }
      assertTrue(departmentValid, "Department should be one of predefined values");

      // 電話番号の検証（03-XXXX-XXXXフォーマット）
      assertNotNull(emp.getPhone_number(), "Phone number should not be null");
      assertTrue(
          emp.getPhone_number().matches("03-\\d{4}-\\d{4}"),
          "Phone number should follow Tokyo format");

      // メールアドレスの検証
      assertNotNull(emp.getEmail(), "Email should not be null");
      assertTrue(
          emp.getEmail().startsWith("employee" + emp.getId()),
          "Email should start with employee + ID");
      assertTrue(emp.getEmail().endsWith("@example.com"), "Email should end with @example.com");
    }
  }

  @Test
  void testCreateBaseDataForUpsert() {
    // ベースデータ生成のテスト
    List<Employee> baseData = dataGenerationService.createBaseDataForUpsert();

    // 基本検証
    assertNotNull(baseData, "Base data should not be null");
    assertEquals(10000, baseData.size(), "Base data should contain 10000 records");

    // ID範囲の検証（E000000-E009999）
    for (int i = 0; i < Math.min(100, baseData.size()); i++) { // 最初の100件をサンプル検証
      Employee emp = baseData.get(i);
      String expectedId = String.format("E%06d", i);
      assertEquals(
          expectedId,
          emp.getId(),
          "Base data ID should follow sequential pattern starting from E000000");
    }
  }

  @Test
  void testCreateEmployeeWithId() {
    // 指定ID付き従業員作成のテスト
    String testId = "E123456";
    java.time.LocalDateTime testTime = java.time.LocalDateTime.now();

    Employee emp = dataGenerationService.createEmployeeWithId(testId, testTime);

    // 基本検証
    assertNotNull(emp, "Created employee should not be null");
    assertEquals(testId, emp.getId(), "Employee ID should match specified ID");

    // タイムスタンプの検証
    assertEquals(testTime, emp.getCreated_at(), "Created timestamp should match specified time");
    assertEquals(testTime, emp.getUpdated_at(), "Updated timestamp should match specified time");

    // その他フィールドの検証
    assertNotNull(emp.getName(), "Name should be generated");
    assertNotNull(emp.getDepartment(), "Department should be generated");
    assertNotNull(emp.getEmployment_status(), "Employment status should be generated");
    assertNotNull(emp.getHire_date(), "Hire date should be generated");
    assertNotNull(emp.getBirth_date(), "Birth date should be generated");
    assertNotNull(emp.getGender(), "Gender should be generated");

    // システムフィールドの検証
    assertEquals("SYSTEM", emp.getCreated_by(), "Created by should be SYSTEM");
    assertEquals("SYSTEM", emp.getUpdated_by(), "Updated by should be SYSTEM");
    assertEquals(Long.valueOf(0), emp.getVersion(), "Version should be 0");
  }
}
