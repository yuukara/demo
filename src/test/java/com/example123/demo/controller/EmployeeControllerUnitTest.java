package com.example123.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example123.demo.service.EmployeeService;

/**
 * EmployeeController単体テストクラス
 * コントローラーの基本動作とサービス委譲が正しく動作することを確認します
 */
@ExtendWith(MockitoExtension.class)
public class EmployeeControllerUnitTest {

    @Mock
    private EmployeeService employeeService;

    private EmployeeController employeeController;

    @BeforeEach
    void setUp() {
        employeeController = new EmployeeController(employeeService);
    }

    @Test
    void testTestMergeUpsert() {
        // テスト実行
        Map<String, Object> result = employeeController.testMergeUpsert();
        
        // 結果検証
        assertNotNull(result, "Result should not be null");
        assertEquals("MERGE-based UPSERT", result.get("method"), "Method should be MERGE-based UPSERT");
        assertEquals("completed", result.get("status"), "Status should be completed");
        assertTrue(result.containsKey("executionTime"), "Should contain execution time");
        
        // サービス呼び出しの検証
        verify(employeeService, times(1)).truncateEmployeesTable();
        verify(employeeService, times(1)).generateAndUpsertRandomEmployees();
    }

    @Test
    void testTestTempTableUpsert() {
        // テストデータの準備
        Map<String, Integer> mockUpsertResult = Map.of(
            "updateCount", 4800,
            "insertCount", 1200
        );
        
        // Mockの設定
        org.mockito.Mockito.when(employeeService.generateAndUpsertRandomEmployeesViaTempTable())
            .thenReturn(mockUpsertResult);
        
        // テスト実行
        Map<String, Object> result = employeeController.testTempTableUpsert();
        
        // 結果検証
        assertNotNull(result, "Result should not be null");
        assertEquals("Temp Table-based UPSERT", result.get("method"), "Method should be Temp Table-based UPSERT");
        assertEquals("completed", result.get("status"), "Status should be completed");
        assertEquals(4800, result.get("updateCount"), "Update count should match");
        assertEquals(1200, result.get("insertCount"), "Insert count should match");
        assertTrue(result.containsKey("executionTime"), "Should contain execution time");
        
        // サービス呼び出しの検証
        verify(employeeService, times(1)).truncateEmployeesTable();
        verify(employeeService, times(1)).generateAndUpsertRandomEmployeesViaTempTable();
    }

    @Test
    void testControllerDelegation() {
        // コントローラーがサービスに正しく委譲することを確認
        
        // 1. MERGE UPSERTのテスト
        employeeController.testMergeUpsert();
        verify(employeeService).truncateEmployeesTable();
        verify(employeeService).generateAndUpsertRandomEmployees();
        
        // 2. Temp Table UPSERTのテスト
        Map<String, Integer> mockResult = Map.of("updateCount", 100, "insertCount", 200);
        org.mockito.Mockito.when(employeeService.generateAndUpsertRandomEmployeesViaTempTable())
            .thenReturn(mockResult);
        
        employeeController.testTempTableUpsert();
        verify(employeeService, times(2)).truncateEmployeesTable(); // 2回呼ばれる（上のテストと合わせて）
        verify(employeeService).generateAndUpsertRandomEmployeesViaTempTable();
    }
}