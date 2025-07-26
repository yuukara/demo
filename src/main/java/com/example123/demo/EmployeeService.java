package com.example123.demo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class EmployeeService {
    private static final int BATCH_SIZE = 1000; // サブリストのサイズ

    public List<Employee> createDummyEmployees(int count) {
        List<Employee> employees = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = String.valueOf(i + 1);
            String name = "Employee " + id;
            String department = "Department " + ((i % 5) + 1);
            String email = "employee" + id + "@example.com";
            employees.add(new Employee(id, name, department, email));
        }
        return employees;
    }

    public void writeToCsvSingleThread(List<Employee> employees, String filePath) {
        System.out.println("Using single thread for CSV generation");
        System.out.printf("Processing %d employees%n", employees.size());
        
        long startTime = System.currentTimeMillis();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // ヘッダーの書き込み
            writer.write("ID,Name,Department,Email\n");
            
            // 全データを1つのスレッドで処理
            for (Employee employee : employees) {
                writer.write(String.format("%s,%s,%s,%s\n",
                    employee.getId(),
                    employee.getName(),
                    employee.getDepartment(),
                    employee.getEmail()));
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.printf("Single thread processing completed in %.2f seconds%n", totalTime / 1000.0);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToCsv(List<Employee> employees, String filePath) {
        // 利用可能なプロセッサ数に基づいてスレッドプールを作成
        int numThreads = Runtime.getRuntime().availableProcessors();
        System.out.printf("Using %d threads for CSV generation (based on available processors)%n", numThreads);
        System.out.printf("Processing %d employees in batches of %d%n", employees.size(), BATCH_SIZE);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long startTime = System.currentTimeMillis();

        try {
            // 従業員リストをバッチサイズで分割
            List<List<Employee>> batches = new ArrayList<>();
            for (int i = 0; i < employees.size(); i += BATCH_SIZE) {
                batches.add(employees.subList(i, Math.min(i + BATCH_SIZE, employees.size())));
            }

            // 各バッチをスレッドプールに送信
            List<Future<String>> futures = new ArrayList<>();
            for (List<Employee> batch : batches) {
                futures.add(executor.submit(new CsvGeneratorTask(batch)));
            }

            // 全てのスレッドの結果を集約
            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // バッチ処理時間を計測
            long batchProcessingTime = System.currentTimeMillis() - startTime;
            System.out.printf("Batch processing completed in %.2f seconds%n", batchProcessingTime / 1000.0);

            // 結果を一括でファイルに書き込み
            long writeStartTime = System.currentTimeMillis();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write("ID,Name,Department,Email\n"); // ヘッダー
                for (String result : results) {
                    writer.write(result);
                }
            }
            long writeTime = System.currentTimeMillis() - writeStartTime;
            System.out.printf("File writing completed in %.2f seconds%n", writeTime / 1000.0);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate in the specified time.");
                    List<Runnable> droppedTasks = executor.shutdownNow();
                    System.err.println("Executor was abruptly shut down. " + droppedTasks.size() + " tasks were dropped.");
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // CSVデータを生成するタスク
    private static class CsvGeneratorTask implements Callable<String> {
        private final List<Employee> employees;

        public CsvGeneratorTask(List<Employee> employees) {
            this.employees = employees;
        }

        @Override
        public String call() {
            return employees.stream()
                    .map(employee -> String.join(",",
                            employee.getId(),
                            employee.getName(),
                            employee.getDepartment(),
                            employee.getEmail()))
                    .collect(Collectors.joining("\n", "", "\n"));
        }
    }
}
