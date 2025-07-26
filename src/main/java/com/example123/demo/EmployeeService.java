package com.example123.demo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 従業員情報を管理するサービスクラス
 * データの生成、データベース保存、CSVファイル出力などの機能を提供します
 */
@Service
public class EmployeeService {
    /** 
     * バッチ処理のサイズ
     * SQLServerのパラメーター制限(2100)を考慮し、15カラム × 100レコード = 1500パラメーターとなるように設定
     */
    private static final int BATCH_SIZE = 100;

    private final EmployeeMapper employeeMapper;

    @Autowired
    public EmployeeService(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    /**
     * 従業員情報をデータベースに保存します
     * 内部で並列処理による保存を行います
     *
     * @param employees 保存する従業員情報のリスト
     */
    public void saveEmployees(List<Employee> employees) {
        saveEmployeesInParallel(employees);
    }

    /**
     * 従業員情報を並列処理でデータベースに保存します
     * 利用可能なプロセッサ数に基づいてスレッドプールを作成し、
     * バッチサイズごとに分割して並列で保存を行います
     *
     * @param employees 保存する従業員情報のリスト
     */
    public void saveEmployeesInParallel(List<Employee> employees) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long startTime = System.currentTimeMillis();
        System.out.printf("Saving %d employees to the database in parallel with %d threads...%n", employees.size(), numThreads);

        try {
            List<List<Employee>> batches = new ArrayList<>();
            for (int i = 0; i < employees.size(); i += BATCH_SIZE) {
                batches.add(new ArrayList<>(employees.subList(i, Math.min(i + BATCH_SIZE, employees.size()))));
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
                    System.err.println("Executor was abruptly shut down. " + droppedTasks.size() + " tasks were dropped.");
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
     * ダミーの従業員データを生成します
     * 生成されるデータには基本情報、個人情報、システム管理情報が含まれます
     *
     * @param count 生成する従業員データの件数
     * @return 生成された従業員情報のリスト
     */
    public List<Employee> createDummyEmployees(int count) {
        List<Employee> employees = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < count; i++) {
            String id = String.valueOf(i + 1);
            Employee employee = new Employee();
            employee.setId(id);
            employee.setName("Employee " + id);
            employee.setDepartment("Department " + ((i % 5) + 1));
            employee.setPosition("Position " + ((i % 3) + 1));
            employee.setEmployment_status(i % 3 == 0 ? "正社員" : i % 3 == 1 ? "契約社員" : "パートタイマー");
            employee.setHire_date(LocalDate.now().minusYears(i % 10));
            employee.setPhone_number("03-" + String.format("%04d", i % 10000) + "-" + String.format("%04d", (i + 1) % 10000));
            employee.setEmail("employee" + id + "@example.com");
            employee.setBirth_date(LocalDate.now().minusYears(20 + (i % 40)));
            employee.setGender(i % 2 == 0 ? "男性" : "女性");
            employee.setCreated_by("SYSTEM");
            employee.setCreated_at(now);
            employee.setUpdated_by("SYSTEM");
            employee.setUpdated_at(now);
            employee.setVersion(0L);
            
            employees.add(employee);
        }
        return employees;
    }

    /**
     * 従業員情報をCSVファイルに出力します（シングルスレッド処理）
     * 全データを1つのスレッドで逐次的に処理します
     *
     * @param employees 出力する従業員情報のリスト
     * @param filePath 出力先のCSVファイルパス
     */
    public void writeToCsvSingleThread(List<Employee> employees, String filePath) {
        System.out.println("Using single thread for CSV generation");
        System.out.printf("Processing %d employees%n", employees.size());
        
        long startTime = System.currentTimeMillis();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // ヘッダーの書き込み
            writer.write("ID,Name,Department,Position,EmploymentStatus,HireDate,PhoneNumber,Email," +
                        "BirthDate,Gender,CreatedBy,CreatedAt,UpdatedBy,UpdatedAt,Version\n");
            
            // 全データを1つのスレッドで処理
            for (Employee employee : employees) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d\n",
                    employee.getId(),
                    employee.getName(),
                    employee.getDepartment(),
                    employee.getPosition(),
                    employee.getEmployment_status(),
                    employee.getHire_date(),
                    employee.getPhone_number(),
                    employee.getEmail(),
                    employee.getBirth_date(),
                    employee.getGender(),
                    employee.getCreated_by(),
                    employee.getCreated_at(),
                    employee.getUpdated_by(),
                    employee.getUpdated_at(),
                    employee.getVersion()));
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.printf("Single thread processing completed in %.2f seconds%n", totalTime / 1000.0);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 従業員情報をCSVファイルに出力します（マルチスレッド処理）
     * 利用可能なプロセッサ数に基づいてスレッドプールを作成し、
     * データを分割して並列で処理します
     *
     * @param employees 出力する従業員情報のリスト
     * @param filePath 出力先のCSVファイルパス
     */
    public void writeToCsv(List<Employee> employees, String filePath) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        System.out.printf("Using %d threads for CSV generation (based on available processors)%n", numThreads);
        System.out.printf("Processing %d employees in batches of %d%n", employees.size(), BATCH_SIZE);
        
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
            System.out.printf("Batch processing completed in %.2f seconds%n", batchProcessingTime / 1000.0);

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
                    System.err.println("Executor was abruptly shut down. " + droppedTasks.size() + " tasks were dropped.");
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
     */
    private static class CsvGeneratorTask implements Callable<String> {
        private final List<Employee> employees;

        /**
         * コンストラクタ
         *
         * @param employees 処理対象の従業員情報リスト
         */
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
                            employee.getPosition(),
                            employee.getEmployment_status(),
                            employee.getHire_date().toString(),
                            employee.getPhone_number(),
                            employee.getEmail(),
                            employee.getBirth_date().toString(),
                            employee.getGender(),
                            employee.getCreated_by(),
                            employee.getCreated_at().toString(),
                            employee.getUpdated_by(),
                            employee.getUpdated_at().toString(),
                            String.valueOf(employee.getVersion())))
                    .collect(Collectors.joining("\n", "", "\n"));
        }
    }
}
