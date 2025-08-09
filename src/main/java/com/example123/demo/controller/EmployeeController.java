package com.example123.demo.controller;

import com.example123.demo.aop.Loggable;
import com.example123.demo.aop.PerformanceMonitoring;
import com.example123.demo.domain.Employee;
import com.example123.demo.service.EmployeeService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

  private final EmployeeService employeeService;

  public EmployeeController(EmployeeService employeeService) {
    this.employeeService = employeeService;
  }

  @PostMapping("/test-merge-upsert")
  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = true,
      includeResult = false,
      value = "MERGE-UPSERT処理")
  @PerformanceMonitoring(threshold = 10000, operation = "API_MERGE_UPSERT")
  public Map<String, Object> testMergeUpsert(@RequestParam(defaultValue = "6000") int count) {
    employeeService.truncateEmployeesTable();

    long startTime = System.currentTimeMillis();
    employeeService.generateAndUpsertRandomEmployees(count);
    long executionTime = System.currentTimeMillis() - startTime;

    Map<String, Object> result = new HashMap<>();
    result.put("method", "MERGE-based UPSERT");
    result.put("dataCount", count);
    result.put("executionTime", executionTime);
    result.put("status", "completed");

    return result;
  }

  @PostMapping("/test-temp-table-upsert")
  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = true,
      includeResult = false,
      value = "一時テーブルUPSERT処理")
  @PerformanceMonitoring(threshold = 10000, operation = "API_TEMP_TABLE_UPSERT")
  public Map<String, Object> testTempTableUpsert(@RequestParam(defaultValue = "6000") int count) {
    employeeService.truncateEmployeesTable();

    long startTime = System.currentTimeMillis();
    Map<String, Integer> upsertResult =
        employeeService.generateAndUpsertRandomEmployeesViaTempTable(count);
    long executionTime = System.currentTimeMillis() - startTime;

    Map<String, Object> result = new HashMap<>();
    result.put("method", "Temp Table-based UPSERT");
    result.put("dataCount", count);
    result.put("executionTime", executionTime);
    result.put("updateCount", upsertResult.get("updateCount"));
    result.put("insertCount", upsertResult.get("insertCount"));
    result.put("status", "completed");

    return result;
  }

  @GetMapping("/test-aop-logging")
  @Loggable(
      level = Loggable.LogLevel.INFO,
      includeArgs = true,
      includeResult = false,
      value = "AOPログ機能テスト")
  @PerformanceMonitoring(threshold = 1000, operation = "API_AOP_TEST")
  public Map<String, Object> testAopLogging(@RequestParam(defaultValue = "100") int count) {
    List<Employee> employees = employeeService.createDummyEmployees(count);

    Map<String, Object> result = new HashMap<>();
    result.put("message", "AOPログ出力テスト完了");
    result.put("method", "employeeService.createDummyEmployees");
    result.put("count", count);
    result.put("generatedCount", employees.size());
    result.put("timestamp", java.time.LocalDateTime.now());

    return result;
  }
}
