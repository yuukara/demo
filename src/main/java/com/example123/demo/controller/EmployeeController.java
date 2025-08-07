package com.example123.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example123.demo.service.EmployeeService;
import com.example123.demo.util.LoggingUtils;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    
    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/test-merge-upsert")
    public Map<String, Object> testMergeUpsert(@RequestParam(defaultValue = "6000") int count) {
        String operationId = LoggingUtils.logApiStart(log, "/api/employees/test-merge-upsert", "count=" + count);
        
        try {
            LoggingUtils.logDatabaseStart(log, "TRUNCATE", "employees", 0);
            employeeService.truncateEmployeesTable();
            LoggingUtils.logDatabaseEnd(log, 0, 0);
            
            long startTime = System.currentTimeMillis();
            employeeService.generateAndUpsertRandomEmployees(count);
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            LoggingUtils.logPerformance("MERGE-UPSERT", executionTime, count);
            
            Map<String, Object> result = new HashMap<>();
            result.put("method", "MERGE-based UPSERT");
            result.put("dataCount", count);
            result.put("executionTime", executionTime);
            result.put("status", "completed");
            
            LoggingUtils.logApiEnd(log, operationId, System.currentTimeMillis() - Long.parseLong(operationId.replaceAll("[^0-9]", "0")));
            
            return result;
        } catch (Exception e) {
            LoggingUtils.logError(log, "MERGE UPSERT処理でエラーが発生しました", e);
            throw e;
        }
    }

    @PostMapping("/test-temp-table-upsert")
    public Map<String, Object> testTempTableUpsert(@RequestParam(defaultValue = "6000") int count) {
        String operationId = LoggingUtils.logApiStart(log, "/api/employees/test-temp-table-upsert", "count=" + count);
        
        try {
            LoggingUtils.logDatabaseStart(log, "TRUNCATE", "employees", 0);
            employeeService.truncateEmployeesTable();
            LoggingUtils.logDatabaseEnd(log, 0, 0);
            
            long startTime = System.currentTimeMillis();
            Map<String, Integer> upsertResult = employeeService.generateAndUpsertRandomEmployeesViaTempTable(count);
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            LoggingUtils.logPerformance("TEMP-TABLE-UPSERT", executionTime, count);
            
            log.info("Temp Table UPSERT結果: 更新={}件, 挿入={}件", 
                upsertResult.get("updateCount"), upsertResult.get("insertCount"));
            
            Map<String, Object> result = new HashMap<>();
            result.put("method", "Temp Table-based UPSERT");
            result.put("dataCount", count);
            result.put("executionTime", executionTime);
            result.put("updateCount", upsertResult.get("updateCount"));
            result.put("insertCount", upsertResult.get("insertCount"));
            result.put("status", "completed");
            
            LoggingUtils.logApiEnd(log, operationId, System.currentTimeMillis() - startTime);
            
            return result;
        } catch (Exception e) {
            LoggingUtils.logError(log, "Temp Table UPSERT処理でエラーが発生しました", e);
            throw e;
        }
    }
}