package com.example123.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * EmployeeController API統合テストクラス
 * 実際のHTTPリクエストを通じて2つのAPIエンドポイントの動作を確認します
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EmployeeControllerApiTest {

    private static final Logger log = LoggerFactory.getLogger(EmployeeControllerApiTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("null")
    public void testMergeUpsertApi() {
        // API URL構築
        String url = "http://localhost:" + port + "/api/employees/test-merge-upsert";
        
        log.info("Testing MERGE-based UPSERT API: {}", url);
        
        // API呼び出し
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url, HttpMethod.POST, null, new ParameterizedTypeReference<Map<String, Object>>() {});
        
        // レスポンス検証
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Response status should be 200 OK");
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        
        // レスポンス内容の検証
        assertEquals("MERGE-based UPSERT", responseBody != null ? responseBody.get("method") : null, 
            "Method should be MERGE-based UPSERT");
        assertEquals("completed", responseBody.get("status"), 
            "Status should be completed");
        assertTrue(responseBody.containsKey("executionTime"), 
            "Response should contain execution time");
        
        // 実行時間が妥当な範囲内であることを確認（0以上）
        Object executionTimeObj = responseBody.get("executionTime");
        assertNotNull(executionTimeObj, "Execution time should not be null");
        assertTrue(executionTimeObj instanceof Number, "Execution time should be a number");
        
        long executionTime = ((Number) executionTimeObj).longValue();
        assertTrue(executionTime >= 0, "Execution time should be non-negative");
        
        log.info("MERGE UPSERT API Test - SUCCESS");
        log.info("Execution Time: {}ms", executionTime);
        log.debug("Response: {}", responseBody);
    }

    @Test
    @SuppressWarnings("null")
    public void testTempTableUpsertApi() {
        // API URL構築
        String url = "http://localhost:" + port + "/api/employees/test-temp-table-upsert";
        
        log.info("Testing Temp Table-based UPSERT API: {}", url);
        
        // API呼び出し
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url, HttpMethod.POST, null, new ParameterizedTypeReference<Map<String, Object>>() {});
        
        // レスポンス検証
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Response status should be 200 OK");
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        
        // レスポンス内容の検証
        assertEquals("Temp Table-based UPSERT", responseBody.get("method"), 
            "Method should be Temp Table-based UPSERT");
        assertEquals("completed", responseBody.get("status"), 
            "Status should be completed");
        assertTrue(responseBody.containsKey("executionTime"), 
            "Response should contain execution time");
        assertTrue(responseBody.containsKey("updateCount"), 
            "Response should contain update count");
        assertTrue(responseBody.containsKey("insertCount"), 
            "Response should contain insert count");
        
        // 実行時間が妥当な範囲内であることを確認
        Object executionTimeObj = responseBody.get("executionTime");
        assertNotNull(executionTimeObj, "Execution time should not be null");
        assertTrue(executionTimeObj instanceof Number, "Execution time should be a number");
        
        long executionTime = ((Number) executionTimeObj).longValue();
        assertTrue(executionTime >= 0, "Execution time should be non-negative");
        
        // 更新・挿入件数の検証
        Object updateCountObj = responseBody.get("updateCount");
        Object insertCountObj = responseBody.get("insertCount");
        
        assertNotNull(updateCountObj, "Update count should not be null");
        assertNotNull(insertCountObj, "Insert count should not be null");
        assertTrue(updateCountObj instanceof Number, "Update count should be a number");
        assertTrue(insertCountObj instanceof Number, "Insert count should be a number");
        
        int updateCount = ((Number) updateCountObj).intValue();
        int insertCount = ((Number) insertCountObj).intValue();
        
        assertTrue(updateCount >= 0, "Update count should be non-negative");
        assertTrue(insertCount >= 0, "Insert count should be non-negative");
        
        // 合計件数が6000件であることを確認（API仕様による）
        int totalCount = updateCount + insertCount;
        assertEquals(6000, totalCount, "Total processed records should be 6000");
        
        log.info("TEMP TABLE UPSERT API Test - SUCCESS");
        log.info("Execution Time: {}ms", executionTime);
        log.info("Update Count: {}, Insert Count: {}, Total Count: {}", updateCount, insertCount, totalCount);
        log.debug("Response: {}", responseBody);
    }

    @Test
    @SuppressWarnings("null")
    public void testBothApisSequentially() {
        // 両方のAPIを順次呼び出して、干渉がないことを確認
        log.info("=== Testing Both APIs Sequentially ===");
        
        // 1. MERGE UPSERT APIの呼び出し
        String mergeUrl = "http://localhost:" + port + "/api/employees/test-merge-upsert";
        ResponseEntity<Map<String, Object>> mergeResponse = restTemplate.exchange(
            mergeUrl, HttpMethod.POST, null, new ParameterizedTypeReference<Map<String, Object>>() {});
        assertEquals(HttpStatus.OK, mergeResponse.getStatusCode());
        
        Map<String, Object> mergeResult = mergeResponse.getBody();
        assertNotNull(mergeResult, "Merge result should not be null");
        log.info("First API (MERGE) completed: {}", mergeResult.get("status"));
        
        // 2. Temp Table UPSERT APIの呼び出し
        String tempTableUrl = "http://localhost:" + port + "/api/employees/test-temp-table-upsert";
        ResponseEntity<Map<String, Object>> tempTableResponse = restTemplate.exchange(
            tempTableUrl, HttpMethod.POST, null, new ParameterizedTypeReference<Map<String, Object>>() {});
        assertEquals(HttpStatus.OK, tempTableResponse.getStatusCode());
        
        Map<String, Object> tempTableResult = tempTableResponse.getBody();
        assertNotNull(tempTableResult, "Temp table result should not be null");
        log.info("Second API (Temp Table) completed: {}", tempTableResult.get("status"));
        
        // 両方とも成功していることを確認
        assertEquals("completed", mergeResult.get("status"));
        assertEquals("completed", tempTableResult.get("status"));
        
        log.info("Sequential API Test - SUCCESS");
    }
}