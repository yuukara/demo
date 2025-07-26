package com.example123.demo.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example123.demo.domain.Employee;
import com.example123.demo.repository.EmployeeMapper;

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

    // 従業員データ生成用の定数
    private static final String[] LAST_NAMES = {
        "佐藤", "鈴木", "高橋", "田中", "伊藤", "渡辺", "山本", "中村", "小林", "加藤",
        "吉田", "山田", "佐々木", "山口", "松本", "井上", "木村", "林", "斎藤", "清水"
    };

    private static final String[] FIRST_NAMES = {
        "翔太", "陽子", "優子", "達也", "美咲", "健一", "愛", "豊", "舞", "誠",
        "香織", "大輔", "さくら", "悟", "恵", "拓也", "美穂", "剛", "智子", "隆"
    };

    private static final String[] DEPARTMENTS = {
        "営業部", "総務部", "人事部", "経理部", "開発部", "製造部", "品質管理部", "マーケティング部",
        "カスタマーサービス部", "研究開発部"
    };

    private static final String[] POSITIONS = {
        "部長", "次長", "課長", "係長", "主任", "担当", "アシスタント"
    };

    private static final String[] EMPLOYMENT_STATUSES = {
        "正社員", "契約社員", "パートタイマー", "アルバイト"
    };

    private final EmployeeMapper employeeMapper;
    private final Random random = new Random();

    @Autowired
    public EmployeeService(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    /**
     * 3000件のランダムな従業員データを生成し、一括UPSERTを行います。
     * 既存データとの重複を含み、MERGE文によるUPSERT処理をテストします。
     */
    public void generateAndUpsertRandomEmployees() {
        List<Employee> employees = createRandomEmployees(3000);
        upsertEmployeesInBatches(employees);
    }

    /**
     * 指定された件数のランダムな従業員データを生成します。
     * 日本の名前や一般的な部署名などを使用し、よりリアルなデータを生成します。
     *
     * @param count 生成する従業員データの件数
     * @return 生成された従業員情報のリスト
     */
    private List<Employee> createRandomEmployees(int count) {
        List<Employee> employees = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // 既存のID範囲: E000000-E009999
        // 新規のID範囲: E010000-E099999
        for (int i = 0; i < count; i++) {
            String id;
            if (random.nextDouble() < 0.15) { // 15%の確率で新規ID
                id = String.format("E%06d", random.nextInt(90000) + 10000); // 新規ID: E010000-E099999
            } else {
                id = String.format("E%06d", random.nextInt(10000)); // 既存ID: E000000-E009999
            }
            Employee employee = new Employee();
            employee.setId(id);
            employee.setName(LAST_NAMES[random.nextInt(LAST_NAMES.length)] + 
                           " " + FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]);
            employee.setDepartment(DEPARTMENTS[random.nextInt(DEPARTMENTS.length)]);
            employee.setPosition(POSITIONS[random.nextInt(POSITIONS.length)]);
            employee.setEmployment_status(EMPLOYMENT_STATUSES[random.nextInt(EMPLOYMENT_STATUSES.length)]);
            
            // 入社日は過去10年以内
            employee.setHire_date(LocalDate.now().minusDays(random.nextInt(3650)));
            
            // 電話番号は東京の市外局番を使用
            employee.setPhone_number(String.format("03-%04d-%04d", 
                random.nextInt(10000), random.nextInt(10000)));
            
            employee.setEmail("employee" + id + "@example.com");
            
            // 生年月日は22-60歳の範囲
            employee.setBirth_date(LocalDate.now()
                .minusYears(22 + random.nextInt(38))
                .minusDays(random.nextInt(365)));
            
            employee.setGender(random.nextBoolean() ? "男性" : "女性");
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
     * 従業員情報を一括でUPSERTします。
     * バッチサイズごとに分割して処理を行います。
     *
     * @param employees UPSERT対象の従業員情報のリスト
     */
    private void upsertEmployeesInBatches(List<Employee> employees) {
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
            employee.setPhone_number("03-" + String.format("%04d", i % 10000) + "-" + 
                String.format("%04d", (i + 1) % 10000));
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
