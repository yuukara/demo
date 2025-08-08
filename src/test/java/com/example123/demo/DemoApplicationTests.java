package com.example123.demo;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example123.demo.domain.Employee;
import com.example123.demo.service.EmployeeService;
import com.example123.demo.service.OptimizedEmployeeService;

@SpringBootTest
class DemoApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(DemoApplicationTests.class);

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private OptimizedEmployeeService optimizedEmployeeService;

    @Test
    void contextLoads() {
    }

    @Test
    void testEmployeeServicePerformance() throws IOException {
        log.info("Starting employee data generation and CSV creation process...");

        // ダミーデータの生成 (OptimizedEmployeeServiceから生成し、シャッフルされたリストを得る)
        long dataGenStartTime = System.currentTimeMillis();
        log.info("Generating and shuffling dummy employee data...");
        List<Employee> employees = optimizedEmployeeService.createDummyEmployees(1_000_000);
        long dataGenTime = System.currentTimeMillis() - dataGenStartTime;
        log.info("Data generation completed in {} seconds\n", String.format("%.2f", dataGenTime / 1000.0));

        // --- 既存のマルチスレッド処理 (ソートなし) ---
        log.info("=== Original Multi-threaded CSV Generation (Unsorted) ===");
        long multiThreadStartTime = System.currentTimeMillis();
        employeeService.writeToCsv(employees, "employees_multi_original_unsorted.csv");
        long multiThreadTime = System.currentTimeMillis() - multiThreadStartTime;
        log.info("Original multi-threaded processing completed in {} seconds\n", String.format("%.2f", multiThreadTime / 1000.0));

        // --- 最適化されたマルチスレッド処理 (外部マージソート) ---
        log.info("=== Optimized Multi-threaded CSV Generation (External Merge Sort) ===");
        long optimizedStartTime = System.currentTimeMillis();
        optimizedEmployeeService.writeToCsv(employees, "employees_multi_optimized_sorted.csv");
        long optimizedTime = System.currentTimeMillis() - optimizedStartTime;
        log.info("Optimized multi-threaded processing with sort completed in {} seconds\n", String.format("%.2f", optimizedTime / 1000.0));
        
        // --- シングルスレッド処理 (ソートなし) ---
        log.info("=== Single-threaded CSV Generation (Unsorted) ===");
        long singleThreadStartTime = System.currentTimeMillis();
        employeeService.writeToCsvSingleThread(employees, "employees_single_unsorted.csv");
        long singleThreadTime = System.currentTimeMillis() - singleThreadStartTime;
        log.info("Single-threaded processing completed in {} seconds\n", String.format("%.2f", singleThreadTime / 1000.0));


        // --- パフォーマンス比較 ---
        log.info("=== Performance Summary ===");
        log.info("Single-thread (unsorted):         {} seconds", String.format("%.2f", singleThreadTime / 1000.0));
        log.info("Multi-thread (unsorted):          {} seconds", String.format("%.2f", multiThreadTime / 1000.0));
        log.info("Multi-thread (external sort):     {} seconds", String.format("%.2f", optimizedTime / 1000.0));
        
        log.info("\nPlease verify that 'employees_multi_optimized_sorted.csv' is sorted by employee ID.");
    }
}
