package com.example123.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example123.demo.domain.EmployeeAssignmentHistory;
import com.example123.demo.repository.EmployeeAssignmentHistoryKey;
import com.example123.demo.repository.EmployeeAssignmentHistoryMapper;

@Service
public class EmployeeAssignmentHistoryService {

    private static final int BATCH_SIZE = 50;

    private final EmployeeAssignmentHistoryMapper mapper;
    private final Random random = new Random();

    @Autowired
    public EmployeeAssignmentHistoryService(EmployeeAssignmentHistoryMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 配属履歴を一括でUpsertします。
     * 一時テーブルを利用した UPDATE後INSERT方式 を採用しています。
     *
     * @param historyList Upsert対象の配属履歴リスト
     */
    public void upsertHistories(List<EmployeeAssignmentHistory> historyList) {
        if (historyList == null || historyList.isEmpty()) {
            return;
        }
        
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long startTime = System.currentTimeMillis();
        
        System.out.printf("Upserting %d assignment histories with %d threads...%n", historyList.size(), numThreads);

        try {
            // バッチ処理
            List<List<EmployeeAssignmentHistory>> batches = new ArrayList<>();
            for (int i = 0; i < historyList.size(); i += BATCH_SIZE) {
                batches.add(new ArrayList<>(historyList.subList(i,
                        Math.min(i + BATCH_SIZE, historyList.size()))));
            }

            // 並列処理でバッチを実行
            List<Future<Map<String, Integer>>> futures = new ArrayList<>();
            for (List<EmployeeAssignmentHistory> batch : batches) {
                futures.add(executor.submit(() -> mapper.upsertViaTempTable(batch)));
            }

            // 結果を集計
            int totalUpdates = 0;
            int totalInserts = 0;
            
            for (Future<Map<String, Integer>> future : futures) {
                try {
                    Map<String, Integer> counts = future.get();
                    totalUpdates += counts.get("updateCount");
                    totalInserts += counts.get("insertCount");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.printf("Assignment history upsert completed in %.2f seconds%n", totalTime / 1000.0);
            System.out.printf("Updates: %d records, Inserts: %d records%n", totalUpdates, totalInserts);

            // 実際に処理されたデータのサンプルを表示
            if (!historyList.isEmpty() && (totalUpdates > 0 || totalInserts > 0)) {
                System.out.println("\n=== 処理されたデータのサンプル ===");
                
                // 更新されたデータのサンプル表示
                if (totalUpdates > 0) {
                    System.out.printf("更新データのサンプル（%d件中）:%n", totalUpdates);
                    // 既存データから更新用サンプルを特定（概算）
                    int updateSampleCount = Math.min(3, totalUpdates);
                    for (int i = 0; i < updateSampleCount && i < historyList.size(); i++) {
                        EmployeeAssignmentHistory sample = historyList.get(i);
                        System.out.printf("  - Employee ID: %s, Org: %s, Job: %s, Status: %s%n",
                            sample.getEmployeeId(),
                            sample.getOrgCode(),
                            sample.getJobCode(),
                            sample.getStatusCode());
                    }
                }
                
                // 新規挿入されたデータのサンプル表示
                if (totalInserts > 0) {
                    System.out.printf("新規挿入データのサンプル（%d件中）:%n", totalInserts);
                    // 新規データから挿入用サンプルを特定（概算）
                    int insertSampleCount = Math.min(3, totalInserts);
                    int startIndex = Math.max(0, historyList.size() - totalInserts);
                    for (int i = 0; i < insertSampleCount && (startIndex + i) < historyList.size(); i++) {
                        EmployeeAssignmentHistory sample = historyList.get(startIndex + i);
                        System.out.printf("  - Employee ID: %s, Org: %s, Job: %s, Status: %s%n",
                            sample.getEmployeeId(),
                            sample.getOrgCode(),
                            sample.getJobCode(),
                            sample.getStatusCode());
                    }
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
    }

    /**
     * 更新と挿入を混在させて生成。
     * 既存キーが足りなければ、その分は新規にフォールバック。
     *
     * @param count  全体件数
     * @param updateRatio 更新比率（例: 0.3 → 30%は更新）
     */
    public List<EmployeeAssignmentHistory> createMixedHistories(int count, double updateRatio) {
        List<EmployeeAssignmentHistory> out = new ArrayList<>(count);
        int wantUpdates = (int)Math.round(count * updateRatio);

        long existing = mapper.countAll();
        List<EmployeeAssignmentHistoryKey> existingKeys =
                existing > 0 ? mapper.selectRandomKeys(Math.min(wantUpdates, (int)existing))
                             : Collections.emptyList();

        // 1) 更新用: 既存キーをそのまま使い、非キー列だけ変更
        for (EmployeeAssignmentHistoryKey k : existingKeys) {
            EmployeeAssignmentHistory h = new EmployeeAssignmentHistory();
            h.setEmployeeId(k.getEmployeeId());
            h.setOrgCode(k.getOrgCode());
            h.setJobCode(k.getJobCode());
            h.setEffectiveFrom(k.getEffectiveFrom());
            h.setSeqNo(k.getSeqNo());

            // 非キー列を変更（例）
            h.setStatusCode(randomStatus());
            h.setFteRatio(BigDecimal.valueOf(0.5 + random.nextInt(51) / 100.0));
            h.setUpdatedBy("SYSTEM");
            h.setUpdatedAt(LocalDateTime.now());
            out.add(h);
        }

        // 2) 新規用: 既存と衝突しない複合キーを生成
        // 既存キーのセット化（衝突回避）
        Set<String> usedKeys = new HashSet<>();
        for (EmployeeAssignmentHistoryKey k : existingKeys) {
            usedKeys.add(key(k.getEmployeeId(), k.getOrgCode(), k.getJobCode(), k.getEffectiveFrom(), k.getSeqNo()));
        }

        int needInserts = count - out.size();
        for (int i = 0; i < needInserts; i++) {
            EmployeeAssignmentHistory h = new EmployeeAssignmentHistory();

            // 衝突しないキーを作る
            while (true) {
                String employeeId = "E" + String.format("%06d", random.nextInt(2000000));
                String orgCode    = "ORG" + String.format("%03d", random.nextInt(200));
                String jobCode    = "JOB" + String.format("%03d", random.nextInt(300));
                LocalDate from    = LocalDate.now().minusDays(random.nextInt(1825));
                int seqNo         = 1; // 運用に合わせて

                String k = key(employeeId, orgCode, jobCode, from, seqNo);
                if (!usedKeys.contains(k)) {
                    usedKeys.add(k);

                    h.setEmployeeId(employeeId);
                    h.setOrgCode(orgCode);
                    h.setJobCode(jobCode);
                    h.setEffectiveFrom(from);
                    h.setSeqNo(seqNo);

                    h.setStatusCode(randomStatus());
                    h.setFteRatio(BigDecimal.valueOf(0.5 + random.nextInt(51) / 100.0));
                    h.setCreatedBy("SYSTEM");
                    h.setCreatedAt(LocalDateTime.now());
                    h.setUpdatedBy("SYSTEM");
                    h.setUpdatedAt(LocalDateTime.now());
                    out.add(h);
                    break;
                }
            }
        }
        return out;
    }

    private static String key(String emp, String org, String job, LocalDate from, int seq) {
        return emp + "|" + org + "|" + job + "|" + from + "|" + seq;
    }

    private String randomStatus() {
        String[] c = {"ACTIVE","HOLD","ENDED"};
        return c[random.nextInt(c.length)];
    }
}