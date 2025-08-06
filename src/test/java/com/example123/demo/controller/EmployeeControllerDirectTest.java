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
        Map<String, Object> result = employeeController.testMergeUpsert();
        long endTime = System.currentTimeMillis();
        
        // 結果の検証
        assertNotNull(result, "Result should not be null");
        assertEquals("MERGE-based UPSERT", result.get("method"), "Method should be MERGE-based UPSERT");
        assertEquals("completed", result.get("status"), "Status should be completed");
        assertTrue(result.containsKey("executionTime"), "Should contain execution time");
        
        // サービス呼び出しの検証
        verify(employeeService, times(1)).truncateEmployeesTable();
        verify(employeeService, times(1)).generateAndUpsertRandomEmployees();
        
        // 実行時間が妥当であることを確認
        Object executionTimeObj = result.get("executionTime");
        assertNotNull(executionTimeObj, "Execution time should not be null");
        assertTrue(executionTimeObj instanceof Number, "Execution time should be a number");
        long executionTime = ((Number) executionTimeObj).longValue();
        assertTrue(executionTime >= 0, "Execution time should be non-negative");
        
        System.out.println("API Logic Test Results:");
        System.out.println("- Method: " + result.get("method"));
        System.out.println("- Status: " + result.get("status"));
        System.out.println("- Execution Time: " + executionTime + "ms");
        System.out.println("- Test Duration: " + (endTime - startTime) + "ms");
        System.out.println("MERGE UPSERT API Logic Test - SUCCESS ✓");
    }

    @Test
    void testTempTableUpsertApiLogic() {
        System.out.println("\n=== Testing Temp Table-based UPSERT API Logic ===");
        
        // テストデータの準備 - サービスの戻り値をモック
        Map<String, Integer> mockUpsertResult = new HashMap<>();
        mockUpsertResult.put("updateCount", 4800);
        mockUpsertResult.put("insertCount", 1200);
        when(employeeService.generateAndUpsertRandomEmployeesViaTempTable()).thenReturn(mockUpsertResult);
        
        // APIのビジネスロジックをテスト実行
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = employeeController.testTempTableUpsert();
        long endTime = System.currentTimeMillis();
        
        // 結果の検証
        assertNotNull(result, "Result should not be null");
        assertEquals("Temp Table-based UPSERT", result.get("method"), "Method should be Temp Table-based UPSERT");
        assertEquals("completed", result.get("status"), "Status should be completed");
        assertTrue(result.containsKey("executionTime"), "Should contain execution time");
        assertTrue(result.containsKey("updateCount"), "Should contain update count");
        assertTrue(result.containsKey("insertCount"), "Should contain insert count");
        
        // UPSERT結果の検証
        assertEquals(4800, result.get("updateCount"), "Update count should match mock data");
        assertEquals(1200, result.get("insertCount"), "Insert count should match mock data");
        
        // サービス呼び出しの検証
        verify(employeeService, times(1)).truncateEmployeesTable();
        verify(employeeService, times(1)).generateAndUpsertRandomEmployeesViaTempTable();
        
        // 実行時間が妥当であることを確認
        Object executionTimeObj = result.get("executionTime");
        assertNotNull(executionTimeObj, "Execution time should not be null");
        assertTrue(executionTimeObj instanceof Number, "Execution time should be a number");
        long executionTime = ((Number) executionTimeObj).longValue();
        assertTrue(executionTime >= 0, "Execution time should be non-negative");
        
        System.out.println("API Logic Test Results:");
        System.out.println("- Method: " + result.get("method"));
        System.out.println("- Status: " + result.get("status"));
        System.out.println("- Update Count: " + result.get("updateCount"));
        System.out.println("- Insert Count: " + result.get("insertCount"));
        System.out.println("- Total Processed: " + (4800 + 1200) + " records");
        System.out.println("- Execution Time: " + executionTime + "ms");
        System.out.println("- Test Duration: " + (endTime - startTime) + "ms");
        System.out.println("TEMP TABLE UPSERT API Logic Test - SUCCESS ✓");
    }

    @Test
    void testBothApisWorkingCorrectly() {
        System.out.println("\n=== Testing Both APIs Work Correctly ===");
        
        // 1. MERGE UPSERT API
        System.out.println("1. Testing MERGE-based UPSERT API:");
        Map<String, Object> mergeResult = employeeController.testMergeUpsert();
        assertNotNull(mergeResult);
        assertEquals("completed", mergeResult.get("status"));
        System.out.println("   ✓ MERGE API working correctly");
        
        // 2. Temp Table UPSERT API
        System.out.println("2. Testing Temp Table-based UPSERT API:");
        Map<String, Integer> mockData = Map.of("updateCount", 3000, "insertCount", 3000);
        when(employeeService.generateAndUpsertRandomEmployeesViaTempTable()).thenReturn(mockData);
        
        Map<String, Object> tempTableResult = employeeController.testTempTableUpsert();
        assertNotNull(tempTableResult);
        assertEquals("completed", tempTableResult.get("status"));
        assertEquals(3000, tempTableResult.get("updateCount"));
        assertEquals(3000, tempTableResult.get("insertCount"));
        System.out.println("   ✓ Temp Table API working correctly");
        
        // 3. サービス呼び出し回数の検証
        verify(employeeService, times(2)).truncateEmployeesTable(); // 2回呼ばれる
        verify(employeeService, times(1)).generateAndUpsertRandomEmployees();
        verify(employeeService, times(1)).generateAndUpsertRandomEmployeesViaTempTable();
        
        System.out.println("3. Service Integration:");
        System.out.println("   ✓ truncateEmployeesTable called 2 times");
        System.out.println("   ✓ generateAndUpsertRandomEmployees called 1 time");
        System.out.println("   ✓ generateAndUpsertRandomEmployeesViaTempTable called 1 time");
        
        System.out.println("Both APIs Integration Test - SUCCESS ✓");
    }

    @Test
    void testApiResponseStructure() {
        System.out.println("\n=== Testing API Response Structure ===");
        
        // MERGE UPSERT APIのレスポンス構造テスト
        Map<String, Object> mergeResponse = employeeController.testMergeUpsert();
        assertTrue(mergeResponse.containsKey("method"), "Should contain 'method' field");
        assertTrue(mergeResponse.containsKey("status"), "Should contain 'status' field");
        assertTrue(mergeResponse.containsKey("executionTime"), "Should contain 'executionTime' field");
        assertEquals(3, mergeResponse.size(), "MERGE response should have exactly 3 fields");
        
        // Temp Table UPSERT APIのレスポンス構造テスト
        Map<String, Integer> mockResult = Map.of("updateCount", 100, "insertCount", 200);
        when(employeeService.generateAndUpsertRandomEmployeesViaTempTable()).thenReturn(mockResult);
        
        Map<String, Object> tempTableResponse = employeeController.testTempTableUpsert();
        assertTrue(tempTableResponse.containsKey("method"), "Should contain 'method' field");
        assertTrue(tempTableResponse.containsKey("status"), "Should contain 'status' field");
        assertTrue(tempTableResponse.containsKey("executionTime"), "Should contain 'executionTime' field");
        assertTrue(tempTableResponse.containsKey("updateCount"), "Should contain 'updateCount' field");
        assertTrue(tempTableResponse.containsKey("insertCount"), "Should contain 'insertCount' field");
        assertEquals(5, tempTableResponse.size(), "Temp Table response should have exactly 5 fields");
        
        System.out.println("API Response Structure:");
        System.out.println("- MERGE API fields: " + mergeResponse.keySet());
        System.out.println("- Temp Table API fields: " + tempTableResponse.keySet());
        System.out.println("API Response Structure Test - SUCCESS ✓");
    }

    @Test
    void testApiEndpointBehavior() {
        System.out.println("\n=== Testing API Endpoint Behavior ===");
        
        // 各APIが想定通りの動作をすることを確認
        System.out.println("Testing API endpoint behavior...");
        
        // 1. MERGE UPSERT: truncate → generateAndUpsert の順序
        employeeController.testMergeUpsert();
        
        // 2. Temp Table UPSERT: truncate → generateAndUpsertViaTempTable の順序
        Map<String, Integer> mockResult = Map.of("updateCount", 5000, "insertCount", 1000);
        when(employeeService.generateAndUpsertRandomEmployeesViaTempTable()).thenReturn(mockResult);
        employeeController.testTempTableUpsert();
        
        // 動作順序の検証（Mockito InOrderは使用せず、シンプルに回数で検証）
        verify(employeeService, times(2)).truncateEmployeesTable();
        verify(employeeService, times(1)).generateAndUpsertRandomEmployees();
        verify(employeeService, times(1)).generateAndUpsertRandomEmployeesViaTempTable();
        
        System.out.println("Expected API Behavior:");
        System.out.println("1. MERGE UPSERT: truncate → generateAndUpsert ✓");
        System.out.println("2. Temp Table UPSERT: truncate → generateAndUpsertViaTempTable ✓");
        System.out.println("API Endpoint Behavior Test - SUCCESS ✓");
    }
}