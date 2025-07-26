package com.example123.demo;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(EmployeeService employeeService, PopulationDataService populationDataService,
                                 UnsafeDataProcessingService unsafeService, SafeDataProcessingService safeService) {
        return args -> {
            // 既存の処理はそのまま残す
            runEmployeeAndPopulationTasks(employeeService, populationDataService);

            // 新しいデータ処理サービスの性能比較
            runDataProcessingTasks(unsafeService, safeService);
        };
    }

    private void runEmployeeAndPopulationTasks(EmployeeService employeeService, PopulationDataService populationDataService) {
        System.out.println("Starting employee data generation and CSV creation process...");

        // データ生成
        long startGeneration = System.currentTimeMillis();
        System.out.println("Generating dummy employee data...");
        List<Employee> employees = employeeService.createDummyEmployees(1000000);
        long generationTime = System.currentTimeMillis() - startGeneration;
        System.out.printf("Data generation completed in %.2f seconds%n", generationTime / 1000.0);

        // マルチスレッドでのCSV生成
        System.out.println("\n=== Multi-threaded CSV Generation ===");
        long startMultiThread = System.currentTimeMillis();
        employeeService.writeToCsv(employees, "employees_multi.csv");
        long multiThreadTime = System.currentTimeMillis() - startMultiThread;
        System.out.printf("Multi-threaded processing completed in %.2f seconds%n", multiThreadTime / 1000.0);

        // シングルスレッドでのCSV生成
        System.out.println("\n=== Single-threaded CSV Generation ===");
        long startSingleThread = System.currentTimeMillis();
        employeeService.writeToCsvSingleThread(employees, "employees_single.csv");
        long singleThreadTime = System.currentTimeMillis() - startSingleThread;
        System.out.printf("Single-threaded processing completed in %.2f seconds%n", singleThreadTime / 1000.0);

        // 性能比較の表示
        System.out.println("\n=== Performance Comparison ===");
        System.out.printf("Single-thread time: %.2f seconds%n", singleThreadTime / 1000.0);
        System.out.printf("Multi-thread time: %.2f seconds%n", multiThreadTime / 1000.0);
        double speedup = (double) singleThreadTime / multiThreadTime;
        System.out.printf("Speed-up ratio: %.2fx%n", speedup);

        // 人口データの読み込みテスト
        System.out.println("\n=== Population Data Loading Test ===");
        try {
            long startPopulation = System.currentTimeMillis();
            List<PopulationData> populationData = populationDataService.loadPopulationData();
            long populationTime = System.currentTimeMillis() - startPopulation;

            System.out.printf("Successfully loaded %d population records in %.2f seconds%n",
                populationData.size(), populationTime / 1000.0);

            // サンプルデータの表示
            if (!populationData.isEmpty()) {
                System.out.println("\nSample record:");
                PopulationData sample = populationData.get(0);
                System.out.printf("Prefecture: %s, Year: %s, Total Population: %s%n",
                    sample.getPrefecture(),
                    sample.getYear(),
                    sample.getTotalPopulationEstimate());
            }
        } catch (Exception e) {
            System.err.println("Error loading population data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runDataProcessingTasks(UnsafeDataProcessingService unsafeService, SafeDataProcessingService safeService) {
        System.out.println("\n\n=== Data Processing Service Comparison ===");
        final int DATA_SIZE = 10_000_000;

        // --- Unsafe (Original) Service ---
        System.out.println("\n--- Running UnsafeDataProcessingService ---");
        long startUnsafe = System.currentTimeMillis();
        List<Integer> unsafeResult = unsafeService.processData(DATA_SIZE);
        long unsafeTime = System.currentTimeMillis() - startUnsafe;
        System.out.printf("Unsafe service processed %d records in %.2f seconds%n", unsafeResult.size(), unsafeTime / 1000.0);

        // --- Safe (Chunked) Service ---
        System.out.println("\n--- Running SafeDataProcessingService ---");
        long startSafe = System.currentTimeMillis();
        List<Integer> safeResult = safeService.processData(DATA_SIZE);
        long safeTime = System.currentTimeMillis() - startSafe;
        System.out.printf("Safe service processed %d records in %.2f seconds%n", safeResult.size(), safeTime / 1000.0);

        // --- Performance Comparison ---
        System.out.println("\n=== Performance Comparison (Data Processing) ===");
        System.out.printf("Unsafe Service Time: %.2f seconds%n", unsafeTime / 1000.0);
        System.out.printf("Safe Service Time:   %.2f seconds%n", safeTime / 1000.0);
        if (safeTime > 0) {
            double speedup = (double) unsafeTime / safeTime;
            System.out.printf("Speed-up ratio: %.2fx%n", speedup);
        }
    }
}
