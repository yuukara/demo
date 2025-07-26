package com.example123.demo;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private OptimizedEmployeeService optimizedEmployeeService;

    @Test
    void contextLoads() {
    }

    @Test
    void testEmployeeServicePerformance() throws IOException {
        System.out.println("Starting employee data generation and CSV creation process...");

        // ダミーデータの生成 (OptimizedEmployeeServiceから生成し、シャッフルされたリストを得る)
        long dataGenStartTime = System.currentTimeMillis();
        System.out.println("Generating and shuffling dummy employee data...");
        List<Employee> employees = optimizedEmployeeService.createDummyEmployees(1_000_000);
        long dataGenTime = System.currentTimeMillis() - dataGenStartTime;
        System.out.printf("Data generation completed in %.2f seconds%n%n", dataGenTime / 1000.0);

        // --- 既存のマルチスレッド処理 (ソートなし) ---
        System.out.println("=== Original Multi-threaded CSV Generation (Unsorted) ===");
        long multiThreadStartTime = System.currentTimeMillis();
        employeeService.writeToCsv(employees, "employees_multi_original_unsorted.csv");
        long multiThreadTime = System.currentTimeMillis() - multiThreadStartTime;
        System.out.printf("Original multi-threaded processing completed in %.2f seconds%n%n", multiThreadTime / 1000.0);

        // --- 最適化されたマルチスレッド処理 (外部マージソート) ---
        System.out.println("=== Optimized Multi-threaded CSV Generation (External Merge Sort) ===");
        long optimizedStartTime = System.currentTimeMillis();
        optimizedEmployeeService.writeToCsv(employees, "employees_multi_optimized_sorted.csv");
        long optimizedTime = System.currentTimeMillis() - optimizedStartTime;
        System.out.printf("Optimized multi-threaded processing with sort completed in %.2f seconds%n%n", optimizedTime / 1000.0);
        
        // --- シングルスレッド処理 (ソートなし) ---
        System.out.println("=== Single-threaded CSV Generation (Unsorted) ===");
        long singleThreadStartTime = System.currentTimeMillis();
        employeeService.writeToCsvSingleThread(employees, "employees_single_unsorted.csv");
        long singleThreadTime = System.currentTimeMillis() - singleThreadStartTime;
        System.out.printf("Single-threaded processing completed in %.2f seconds%n%n", singleThreadTime / 1000.0);


        // --- パフォーマンス比較 ---
        System.out.println("=== Performance Summary ===");
        System.out.printf("Single-thread (unsorted):         %.2f seconds%n", singleThreadTime / 1000.0);
        System.out.printf("Multi-thread (unsorted):          %.2f seconds%n", multiThreadTime / 1000.0);
        System.out.printf("Multi-thread (external sort):     %.2f seconds%n", optimizedTime / 1000.0);
        
        System.out.println("\nPlease verify that 'employees_multi_optimized_sorted.csv' is sorted by employee ID.");
    }
}
