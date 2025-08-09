package com.example123.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example123.demo.service.EmployeeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** EmployeeController HTTP レスポンステストクラス APIのHTTPレスポンスとしてのJSON形式出力を確認します */
@ExtendWith(MockitoExtension.class)
public class EmployeeControllerHttpResponseTest {

  private static final Logger log =
      LoggerFactory.getLogger(EmployeeControllerHttpResponseTest.class);

  @Mock private EmployeeService employeeService;

  private EmployeeController employeeController;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    employeeController = new EmployeeController(employeeService);
    objectMapper = new ObjectMapper();
  }

  @Test
  void testMergeUpsertApiHttpResponse() throws JsonProcessingException {
    log.info("=== Testing MERGE UPSERT API HTTP Response ===");

    // API実行
    Map<String, Object> response = employeeController.testMergeUpsert(6000);

    // JSONレスポンスの生成と検証
    String jsonResponse = objectMapper.writeValueAsString(response);

    log.info("HTTP Response (JSON):");
    log.info(jsonResponse);

    // JSONが正しく生成されることを確認
    assertNotNull(jsonResponse, "JSON response should not be null");
    assertTrue(
        jsonResponse.contains("\"method\":\"MERGE-based UPSERT\""),
        "JSON should contain method field");
    assertTrue(
        jsonResponse.contains("\"status\":\"completed\""), "JSON should contain status field");
    assertTrue(
        jsonResponse.contains("\"executionTime\""), "JSON should contain executionTime field");

    // JSONを再度パースして内容を確認
    @SuppressWarnings("unchecked")
    Map<String, Object> parsedResponse = objectMapper.readValue(jsonResponse, Map.class);
    assertEquals("MERGE-based UPSERT", parsedResponse.get("method"));
    assertEquals("completed", parsedResponse.get("status"));
    assertTrue(parsedResponse.containsKey("executionTime"));

    log.info("MERGE UPSERT HTTP Response Test - SUCCESS ✓");
  }

  @Test
  void testTempTableUpsertApiHttpResponse() throws JsonProcessingException {
    log.info("=== Testing TEMP TABLE UPSERT API HTTP Response ===");

    // サービスのモック設定
    Map<String, Integer> mockResult = Map.of("updateCount", 4800, "insertCount", 1200);
    org.mockito.Mockito.when(employeeService.generateAndUpsertRandomEmployeesViaTempTable(6000))
        .thenReturn(mockResult);

    // API実行
    Map<String, Object> response = employeeController.testTempTableUpsert(6000);

    // JSONレスポンスの生成と検証
    String jsonResponse = objectMapper.writeValueAsString(response);

    log.info("HTTP Response (JSON):");
    log.info(jsonResponse);

    // JSONが正しく生成されることを確認
    assertNotNull(jsonResponse, "JSON response should not be null");
    assertTrue(
        jsonResponse.contains("\"method\":\"Temp Table-based UPSERT\""),
        "JSON should contain method field");
    assertTrue(
        jsonResponse.contains("\"status\":\"completed\""), "JSON should contain status field");
    assertTrue(
        jsonResponse.contains("\"executionTime\""), "JSON should contain executionTime field");
    assertTrue(
        jsonResponse.contains("\"updateCount\":4800"), "JSON should contain updateCount field");
    assertTrue(
        jsonResponse.contains("\"insertCount\":1200"), "JSON should contain insertCount field");

    // JSONを再度パースして内容を確認
    @SuppressWarnings("unchecked")
    Map<String, Object> parsedResponse = objectMapper.readValue(jsonResponse, Map.class);
    assertEquals("Temp Table-based UPSERT", parsedResponse.get("method"));
    assertEquals("completed", parsedResponse.get("status"));
    assertEquals(4800, parsedResponse.get("updateCount"));
    assertEquals(1200, parsedResponse.get("insertCount"));
    assertTrue(parsedResponse.containsKey("executionTime"));

    log.info("TEMP TABLE UPSERT HTTP Response Test - SUCCESS ✓");
  }

  @Test
  void testBothApisJsonResponseStructure() throws JsonProcessingException {
    log.info("=== Testing Both APIs JSON Response Structure ===");

    // 1. MERGE UPSERT APIのJSON構造確認
    Map<String, Object> mergeResponse = employeeController.testMergeUpsert(6000);
    String mergeJson = objectMapper.writeValueAsString(mergeResponse);

    // 2. Temp Table UPSERT APIのJSON構造確認
    Map<String, Integer> mockResult = Map.of("updateCount", 3000, "insertCount", 3000);
    org.mockito.Mockito.when(employeeService.generateAndUpsertRandomEmployeesViaTempTable(6000))
        .thenReturn(mockResult);
    Map<String, Object> tempTableResponse = employeeController.testTempTableUpsert(6000);
    String tempTableJson = objectMapper.writeValueAsString(tempTableResponse);

    log.info("API Response Structure Comparison:");
    log.info("1. MERGE UPSERT JSON: {}", mergeJson);
    log.info("2. TEMP TABLE UPSERT JSON: {}", tempTableJson);

    // 両方のJSONが有効であることを確認
    assertNotNull(mergeJson);
    assertNotNull(tempTableJson);
    assertTrue(mergeJson.length() > 0);
    assertTrue(tempTableJson.length() > 0);

    // JSON構造の違いを確認
    assertTrue(mergeJson.split(",").length == 3, "MERGE JSON should have 3 fields");
    assertTrue(tempTableJson.split(",").length >= 5, "Temp Table JSON should have 5 fields");

    log.info("JSON Response Structure Test - SUCCESS ✓");
  }

  @Test
  void testHttpStatusCodeEquivalent() {
    log.info("=== Testing HTTP Status Code Equivalent ===");

    // APIが例外をスローせず、正常にレスポンスを返すことを確認
    try {
      // MERGE UPSERT API
      Map<String, Object> mergeResponse = employeeController.testMergeUpsert(6000);
      assertNotNull(mergeResponse);
      assertEquals("completed", mergeResponse.get("status"));
      log.info("✓ MERGE UPSERT API returns valid response (equivalent to HTTP 200)");

      // Temp Table UPSERT API
      Map<String, Integer> mockResult = Map.of("updateCount", 1000, "insertCount", 2000);
      org.mockito.Mockito.when(employeeService.generateAndUpsertRandomEmployeesViaTempTable(6000))
          .thenReturn(mockResult);
      Map<String, Object> tempTableResponse = employeeController.testTempTableUpsert(6000);
      assertNotNull(tempTableResponse);
      assertEquals("completed", tempTableResponse.get("status"));
      log.info("✓ TEMP TABLE UPSERT API returns valid response (equivalent to HTTP 200)");

    } catch (Exception e) {
      log.error("API threw exception: {}", e.getMessage(), e);
      throw e; // テスト失敗
    }

    log.info("HTTP Status Code Equivalent Test - SUCCESS ✓");
    log.info("Both APIs would return HTTP 200 OK in real scenario");
  }
}
