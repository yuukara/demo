package com.example123.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example123.demo.service.EmployeeService;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/test-merge-upsert")
    public Map<String, Object> testMergeUpsert() {
        employeeService.truncateEmployeesTable();
        System.out.println("\n=== Testing MERGE-based UPSERT ===");
        long startTime = System.currentTimeMillis();
        employeeService.generateAndUpsertRandomEmployees();
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("testMergeUpsert execution time: " + executionTime + "ms");
        
        Map<String, Object> result = new HashMap<>();
        result.put("method", "MERGE-based UPSERT");
        result.put("executionTime", executionTime);
        result.put("status", "completed");
        return result;
    }

    @PostMapping("/test-temp-table-upsert")
    public Map<String, Object> testTempTableUpsert() {
        employeeService.truncateEmployeesTable();
        System.out.println("\n=== Testing Temp Table-based UPSERT ===");
        long startTime = System.currentTimeMillis();
        Map<String, Integer> upsertResult = employeeService.generateAndUpsertRandomEmployeesViaTempTable();
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("testTempTableUpsert execution time: " + executionTime + "ms");
        System.out.println("Update count: " + upsertResult.get("updateCount"));
        System.out.println("Insert count: " + upsertResult.get("insertCount"));
        
        Map<String, Object> result = new HashMap<>();
        result.put("method", "Temp Table-based UPSERT");
        result.put("executionTime", executionTime);
        result.put("updateCount", upsertResult.get("updateCount"));
        result.put("insertCount", upsertResult.get("insertCount"));
        result.put("status", "completed");
        return result;
    }
}