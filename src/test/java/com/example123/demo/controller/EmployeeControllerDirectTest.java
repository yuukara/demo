package com.example123.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example123.demo.service.EmployeeService;

/**
 * EmployeeController直接テストクラス
 * SpringBootコンテキストを使用せずに、コントローラーロジックを直接テストします
 * これによりAPIの動作確認を行います
 */
@ExtendWith(MockitoExtension.class)
public class EmployeeControllerDirectTest {

    @Mock
    private EmployeeService employeeService;

    private EmployeeController employeeController;

    @BeforeEach
    void setUp() {
        employeeController = new EmployeeController(employeeService);
    }

    @Test
    void testMergeUpsertApiLogic() {
        System.out.println("\n=== Testing MERGE-based UPSERT API Logic ===");
        
        // APIのビジネスロジックをテスト実行
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = employeeController.testMergeUpsert(6000);
        long endTime = System.currentTimeMillis();
        
        // 結果の検証
        assertNotNull(result, "Result should not be null");
        assertEquals("MERGE-based UPSERT", result.get("method"), "Method should be MERGE-based UPSERT");
        assertEquals("completed", result.get("status"), "Status should be completed");
        assertTrue(result.containsKey("executionTime"), "Should contain execution time");
        
        // サービス呼び出しの検証
        verify(employeeService, times(1)).generateAndUpsertRandomEmployees(6000);
        verify(employeeService, times(1)).generateAndUpsertRandomEmployeesViaTempTable(6000);
        
        System.out.println("Expected API Behavior:");
        System.out.println("1. MERGE UPSERT: truncate → generateAndUpsert ✓");
        System.out.println("2. Temp Table UPSERT: truncate → generateAndUpsertViaTempTable ✓");
        System.out.println("API Endpoint Behavior Test - SUCCESS ✓");
    }
}