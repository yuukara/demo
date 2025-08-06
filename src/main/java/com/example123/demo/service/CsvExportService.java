package com.example123.demo.service;

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

import com.example123.demo.domain.Employee;

/**
 * 従業員データのCSV出力機能を提供するサービスクラス
 * シングルスレッド処理とマルチスレッド処理の両方をサポートします
 */
@Service
public class CsvExportService {
    
    /** 
     * バッチ処理のサイズ
     * CSVエクスポート時のパフォーマンス最適化のために使用
     */
    private static final int BATCH_SIZE = 100;

    /**
     * 従業員情報をCSVファイルに出力します（シングルスレッド処理）
     * 全データを1つのスレッドで逐次的に処理します
     * 出力されるCSVには全カラムの情報が含まれます
     *
     * @param employees 出力する従業員情報のリスト
     * @param filePath 出力先のCSVファイルパス
     */
    public void writeToCsvSingleThread(List<Employee> employees, String filePath) {
        System.out.println("Using single thread for CSV generation");
        System.out.printf("Processing %d employees%n", employees.size());
        
        long startTime = System.currentTimeMillis();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("ID,Name,Department,Position,EmploymentStatus,HireDate,PhoneNumber,Email," +
                        "BirthDate,Gender,CreatedBy,CreatedAt,UpdatedBy,UpdatedAt,Version\n");
            
            for (Employee employee : employees) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d\n",
                    employee.getId(),
                    employee.getName(),
                    employee.getDepartment(),
                    employee.getPosition(),
                    employee.getEmployment_status(),
                    employee.getHire_date() != null ? employee.getHire_date().toString() : "",
                    employee.getPhone_number(),
                    employee.getEmail(),
                    employee.getBirth_date() != null ? employee.getBirth_date().toString() : "",
                    employee.getGender(),
                    employee.getCreated_by(),
                    employee.getCreated_at() != null ? employee.getCreated_at().toString() : "",
                    employee.getUpdated_by(),
                    employee.getUpdated_at() != null ? employee.getUpdated_at().toString() : "",
                    employee.getVersion()));
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.printf("Single thread processing completed in %.2f seconds%n", 
                totalTime / 1000.0);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 従業員情報をCSVファイルに出力します（マルチスレッド処理）
     * 利用可能なプロセッサ数に基づいてスレッドプールを作成し、
     * データを分割して並列で処理します
     * 出力されるCSVには全カラムの情報が含まれます
     *
     * @param employees 出力する従業員情報のリスト
     * @param filePath 出力先のCSVファイルパス
     */
    public void writeToCsv(List<Employee> employees, String filePath) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        System.out.printf("Using %d threads for CSV generation (based on available processors)%n", 
            numThreads);
        System.out.printf("Processing %d employees in batches of %d%n", 
            employees.size(), BATCH_SIZE);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long startTime = System.currentTimeMillis();

        try {
            List<List<Employee>> batches = new ArrayList<>();
            for (int i = 0; i < employees.size(); i += BATCH_SIZE) {
                batches.add(employees.subList(i, Math.min(i + BATCH_SIZE, employees.size())));
            }

            List<Future<String>> futures = new ArrayList<>();
            for (List<Employee> batch : batches) {
                futures.add(executor.submit(new CsvGeneratorTask(batch)));
            }

            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            long batchProcessingTime = System.currentTimeMillis() - startTime;
            System.out.printf("Batch processing completed in %.2f seconds%n", 
                batchProcessingTime / 1000.0);

            long writeStartTime = System.currentTimeMillis();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write("ID,Name,Department,Position,EmploymentStatus,HireDate,PhoneNumber,Email," +
                           "BirthDate,Gender,CreatedBy,CreatedAt,UpdatedBy,UpdatedAt,Version\n");
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
                    System.err.println("Executor was abruptly shut down. " + 
                        droppedTasks.size() + " tasks were dropped.");
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * CSVデータを生成するタスククラス
     * 従業員情報のバッチをCSV形式の文字列に変換します
     * マルチスレッド処理での並列データ変換に使用されます
     */
    private static class CsvGeneratorTask implements Callable<String> {
        private final List<Employee> employees;

        /**
         * CSVジェネレータータスクを初期化します
         *
         * @param employees 処理対象の従業員情報リスト
         */
        public CsvGeneratorTask(List<Employee> employees) {
            this.employees = employees;
        }

        /**
         * タスクを実行し、従業員データをCSV形式の文字列に変換します
         *
         * @return CSV形式に変換されたデータ（改行区切り）
         */
        @Override
        public String call() {
            return employees.stream()
                    .map(employee -> String.join(",",
                            employee.getId(),
                            employee.getName(),
                            employee.getDepartment(),
                            employee.getPosition(),
                            employee.getEmployment_status(),
                            employee.getHire_date() != null ? employee.getHire_date().toString() : "",
                            employee.getPhone_number(),
                            employee.getEmail(),
                            employee.getBirth_date() != null ? employee.getBirth_date().toString() : "",
                            employee.getGender(),
                            employee.getCreated_by(),
                            employee.getCreated_at() != null ? employee.getCreated_at().toString() : "",
                            employee.getUpdated_by(),
                            employee.getUpdated_at() != null ? employee.getUpdated_at().toString() : "",
                            String.valueOf(employee.getVersion())))
                    .collect(Collectors.joining("\n", "", "\n"));
        }
    }
}