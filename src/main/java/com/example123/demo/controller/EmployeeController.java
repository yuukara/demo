package com.example123.demo.controller;

import org.springframework.stereotype.Component;

import com.example123.demo.service.EmployeeService;

@Component
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public void testMergeUpsert() {
        System.out.println("\n=== Testing MERGE-based UPSERT ===");
        employeeService.generateAndUpsertRandomEmployees();
    }
}