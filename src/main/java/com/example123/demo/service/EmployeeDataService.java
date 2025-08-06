package com.example123.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example123.demo.domain.Employee;
import com.example123.demo.repository.EmployeeMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * 従業員データの操作を行うサービスクラス
 * データベースへの保存、UPSERT処理、並列処理などの機能を提供します
 */
@Service
@Validated
public class EmployeeDataService {
    
    /** 
     * バッチ処理のサイズ
     * SQLServerのパラメーター制限(2100)を考慮し、15カラム × 100レコード = 1500パラメーターとなるように設定
     */
    private static final int BATCH_SIZE = 100;

    private final EmployeeMapper employeeMapper;
    private final DataGenerationService dataGenerationService;

    public EmployeeDataService(EmployeeMapper employeeMapper, DataGenerationService dataGenerationService) {
        this.employeeMapper = employeeMapper;
        this.dataGenerationService = dataGenerationService;
    }

    /**
     * 従業員情報をデータベースに保存します
     * 内部で並列処理による保存を行います
     *
     * @param employees 保存する従業員情報のリスト
     */
    public void saveEmployees(@NotEmpty(message = "保存する従業員リストが空です") @Valid List<Employee> employees) {
        saveEmployeesInParallel(employees);
    }

    /**
     * 従業員情報を並列処理でデータベースに保存します
     * 利用可能なプロセッサ数に基づいてスレッドプールを作成し、
     * バッチサイズごとに分割して並列で保存を行います
     *
     * @param employees 保存する従業員情報のリスト
     */
    public void saveEmployeesInParallel(@NotEmpty(message = "保存する従業員リストが空です") @Valid List<Employee> employees) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long startTime = System.currentTimeMillis();
        System.out.printf("Saving %d employees to the database in parallel with %d threads...%n", 
            employees.size(), numThreads);

        try {
            List<List<Employee>> batches = new ArrayList<>();
            for (int i = 0; i < employees.size(); i += BATCH_SIZE) {
                batches.add(new ArrayList<>(employees.subList(i, 
                    Math.min(i + BATCH_SIZE, employees.size()))));
            }

            List<Future<?>> futures = new ArrayList<>();
            for (List<Employee> batch : batches) {
                futures.add(executor.submit(() -> employeeMapper.bulkInsert(batch)));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate in the specified time.");
                    List<Runnable> droppedTasks = executor.shutdownNow();
                    System.err.println("Executor was abruptly shut down. " + 
                        droppedTasks.size() + " tasks were dropped.");
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("Parallel database save completed in %.2f seconds%n", totalTime / 1000.0);
    }

    /**
     * 従業員情報を一括でUPSERTします。
     * バッチサイズごとに分割して処理を行います。
     *
     * @param employees UPSERT対象の従業員情報のリスト
     */
    public void upsertEmployeesInBatches(List<Employee> employees) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long startTime = System.currentTimeMillis();
        
        // ID範囲ごとの件数をカウント
        long newRecords = employees.stream()
            .filter(e -> Integer.parseInt(e.getId().substring(1)) >= 10000)
            .count();
        long existingRecords = employees.size() - newRecords;
        
        System.out.println("\nUpsert operation details:");
        System.out.printf("Total records: %d%n", employees.size());
        System.out.printf("Expected new records (ID >= E010000): %d (%.1f%%)%n",
            newRecords, (newRecords * 100.0 / employees.size()));
        System.out.printf("Expected updates (ID < E010000): %d (%.1f%%)%n",
            existingRecords, (existingRecords * 100.0 / employees.size()));
        System.out.println("Starting MERGE operation...\n");

        try {
            List<List<Employee>> batches = new ArrayList<>();
            for (int i = 0; i < employees.size(); i += BATCH_SIZE) {
                batches.add(new ArrayList<>(employees.subList(i, 
                    Math.min(i + BATCH_SIZE, employees.size()))));
            }

            List<Future<?>> futures = new ArrayList<>();
            for (List<Employee> batch : batches) {
                futures.add(executor.submit(() -> employeeMapper.bulkUpsert(batch)));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate in the specified time.");
                    List<Runnable> droppedTasks = executor.shutdownNow();
                    System.err.println("Executor was abruptly shut down. " + 
                        droppedTasks.size() + " tasks were dropped.");
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("Upsert completed in %.2f seconds%n", totalTime / 1000.0);
    }

    /**
     * 従業員情報を一時テーブル方式で一括UPSERTします。
     * バッチサイズごとに分割して処理を行います。
     *
     * @param employees UPSERT対象の従業員情報のリスト
     * @return 全体の処理件数を含むMap（updateCount: 更新件数合計, insertCount: 挿入件数合計）
     */
    public java.util.Map<String, Integer> upsertEmployeesViaTempTableInBatches(List<Employee> employees) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long startTime = System.currentTimeMillis();
        
        // ID範囲ごとの件数をカウント
        long newRecords = employees.stream()
            .filter(e -> Integer.parseInt(e.getId().substring(1)) >= 10000)
            .count();
        long existingRecords = employees.size() - newRecords;
        
        System.out.println("\nUpsert operation details (Temp Table method):");
        System.out.printf("Total records: %d%n", employees.size());
        System.out.printf("Expected new records (ID >= E010000): %d (%.1f%%)%n",
            newRecords, (newRecords * 100.0 / employees.size()));
        System.out.printf("Expected updates (ID < E010000): %d (%.1f%%)%n",
            existingRecords, (existingRecords * 100.0 / employees.size()));
        System.out.println("Starting Temp Table operation...\n");

        int totalUpdateCount = 0;
        int totalInsertCount = 0;

        try {
            // バッチ処理
            List<List<Employee>> batches = new ArrayList<>();
            for (int i = 0; i < employees.size(); i += BATCH_SIZE) {
                batches.add(new ArrayList<>(employees.subList(i,
                    Math.min(i + BATCH_SIZE, employees.size()))));
            }

            // 並列処理でバッチを実行
            List<Future<java.util.Map<String, Integer>>> futures = new ArrayList<>();
            for (List<Employee> batch : batches) {
                futures.add(executor.submit(() -> employeeMapper.bulkUpsertViaTempTable(batch)));
            }

            for (Future<java.util.Map<String, Integer>> future : futures) {
                try {
                    java.util.Map<String, Integer> result = future.get();
                    totalUpdateCount += result.get("updateCount");
                    totalInsertCount += result.get("insertCount");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate in the specified time.");
                    List<Runnable> droppedTasks = executor.shutdownNow();
                    System.err.println("Executor was abruptly shut down. " +
                        droppedTasks.size() + " tasks were dropped.");
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("Temp Table Upsert completed in %.2f seconds%n", totalTime / 1000.0);
        System.out.printf("Total updates: %d, Total inserts: %d%n", totalUpdateCount, totalInsertCount);
        
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        result.put("updateCount", totalUpdateCount);
        result.put("insertCount", totalInsertCount);
        return result;
    }

    /**
     * 従業員テーブルのデータを全て削除します。
     */
    public void truncateEmployeesTable() {
        System.out.println("Truncating employees table...");
        employeeMapper.truncateTable();
        System.out.println("Employees table truncated.");
    }

    /**
     * UPSERT処理用の基礎データを準備します。
     * 更新対象となるデータ（E000000-E009999）を事前にテーブルに投入します。
     */
    public void prepareBaseDataForUpsert() {
        System.out.println("Preparing base data for upsert test...");
        List<Employee> baseEmployees = dataGenerationService.createBaseDataForUpsert();
        
        // バッチでベースデータを投入
        saveEmployeesInParallel(baseEmployees);
        System.out.println("Base data preparation completed: " + baseEmployees.size() + " records inserted.");
    }
}