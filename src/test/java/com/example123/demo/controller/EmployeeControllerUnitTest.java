package com.example123.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example123.demo.service.EmployeeService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** EmployeeController単体テストクラス コントローラーの基本動作とサービス委譲が正しく動作することを確認します */
@ExtendWith(MockitoExtension.class)
public class EmployeeControllerUnitTest {

  @Mock private EmployeeService employeeService;

  private EmployeeController employeeController;

  @BeforeEach
  void setUp() {
    employeeController = new EmployeeController(employeeService);
  }

  @Test
  void testTestMergeUpsert() {
    // テスト実行
    Map<String, Object> result = employeeController.testMergeUpsert(6000);

    // 結果検証
    assertNotNull(result, "Result should not be null");
    assertEquals("MERGE-based UPSERT", result.get("method"), "Method should be MERGE-based UPSERT");
    assertEquals("completed", result.get("status"), "Status should be completed");
    assertTrue(result.containsKey("executionTime"), "Should contain execution time");

    // サービス呼び出しの検証
    verify(employeeService, times(1)).truncateEmployeesTable();
    verify(employeeService, times(1)).generateAndUpsertRandomEmployees(6000);
  }
}
