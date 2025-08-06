package com.example123.demo.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.example123.demo.domain.Employee;

@Service
public class OptimizedEmployeeService {
    private static final int BATCH_SIZE = 1000;

    public List<Employee> createDummyEmployees(int count) {
        List<Employee> employees = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = String.valueOf(i + 1);
            String name = "Employee " + id;
            String department = "Department " + ((i % 5) + 1);
            String email = "employee" + id + "@example.com";
            employees.add(new Employee(id, name, department, email));
        }
        // 従業員IDがランダムになるようにシャッフル
        Collections.shuffle(employees);
        return employees;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void writeToCsv(List<Employee> employees, String filePath) throws IOException {
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<File> tempFiles = new ArrayList<>();
        
        try {
            // ステップ1 & 2: 並列でソート済み一時ファイルを作成
            List<Future<File>> futures = new ArrayList<>();
            for (int i = 0; i < employees.size(); i += BATCH_SIZE) {
                List<Employee> batch = employees.subList(i, Math.min(i + BATCH_SIZE, employees.size()));
                futures.add(executor.submit(new SortAndWriteTask(batch)));
            }

            for (Future<File> future : futures) {
                tempFiles.add(future.get());
            }

            // ステップ3: 一時ファイルをマージ
            mergeSortedFiles(tempFiles, filePath);

        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            // 意図的なデバッグ出力のため警告を抑制
            e.printStackTrace();
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // ステップ4: クリーンアップ
            for (File tempFile : tempFiles) {
                tempFile.delete();
            }
        }
    }

    /**
     * チャンクをソートして一時ファイルに書き込むタスク (Java 6互換)
     */
    private static class SortAndWriteTask implements Callable<File> {
        private final List<Employee> employees;

        public SortAndWriteTask(List<Employee> employees) {
            this.employees = new ArrayList<>(employees); // defensive copy
        }

        @Override
        public File call() throws IOException {
            // 従業員IDでソート (Comparatorを匿名クラスで実装)
            Collections.sort(this.employees, (e1, e2) -> {
                // IDを整数として比較
                return Integer.valueOf(e1.getId()).compareTo(Integer.valueOf(e2.getId()));
            });

            // 一時ファイルを作成（セキュアな権限で）
            Path tempFile = Files.createTempFile("sort-", ".csv");
            
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
                // 拡張forループとStringBuilderでCSV文字列を生成
                for (Employee employee : this.employees) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(employee.getId()).append(",");
                    sb.append(employee.getName()).append(",");
                    sb.append(employee.getDepartment()).append(",");
                    sb.append(employee.getEmail()).append("\n");
                    writer.write(sb.toString());
                }
            }
            return tempFile.toFile();
        }
    }

    /**
     * 複数のソート済み一時ファイルをマージする (Java 6互換)
     */
    @SuppressWarnings("CallToPrintStackTrace")
    private void mergeSortedFiles(List<File> files, String outputPath) throws IOException {
        PriorityQueue<FileRecord> pq = new PriorityQueue<>(files.size(), 
            (r1, r2) -> Integer.valueOf(r1.getEmployeeId()).compareTo(Integer.valueOf(r2.getEmployeeId())));

        List<BufferedReader> readers = new ArrayList<>();
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

        try {
            // ヘッダーを書き込み
            writer.write("ID,Name,Department,Email\n");

            // 各ファイルを開き、最初の行を読み込んでキューに追加
            for (File file : files) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                readers.add(reader);
                String line = reader.readLine();
                if (line != null) {
                    pq.add(new FileRecord(reader, line));
                }
            }

            // キューが空になるまでマージ処理
            while (!pq.isEmpty()) {
                FileRecord record = pq.poll();
                writer.write(record.getLine());
                writer.newLine();

                // 同じファイルから次の行を読み込んでキューに追加
                String nextLine = record.getReader().readLine();
                if (nextLine != null) {
                    pq.add(new FileRecord(record.getReader(), nextLine));
                }
            }
        } finally {
            for (BufferedReader reader : readers) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * マージ処理で使うヘルパークラス
     */
    private static class FileRecord {
        private final BufferedReader reader;
        private final String line;
        private final String employeeId;

        public FileRecord(BufferedReader reader, String line) {
            this.reader = reader;
            this.line = line;
            this.employeeId = line.split(",")[0];
        }

        public BufferedReader getReader() { return reader; }
        public String getLine() { return line; }
        public String getEmployeeId() { return employeeId; }
    }
}