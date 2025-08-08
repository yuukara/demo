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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example123.demo.aop.Loggable;
import com.example123.demo.aop.PerformanceMonitoring;

import com.example123.demo.domain.EmployeeAssignmentHistory;
import com.example123.demo.repository.EmployeeAssignmentHistoryKey;
import com.example123.demo.repository.EmployeeAssignmentHistoryMapper;

@Service
public class EmployeeAssignmentHistoryService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeAssignmentHistoryService.class);
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
    @Loggable(level = Loggable.LogLevel.INFO, includeArgs = false, includeResult = false, value = "配属履歴一括UPSERT処理")
    @PerformanceMonitoring(threshold = 5000, operation = "ASSIGNMENT_HISTORY_UPSERT")
    public void upsertHistories(List<EmployeeAssignmentHistory> historyList) {
        if (historyList == null || historyList.isEmpty()) {
            return;
        }
        
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

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
                    log.error("Error getting result from an assignment history upsert task", e);
                }
            }
            
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("Executor did not terminate in the specified time.");
                    List<Runnable> droppedTasks = executor.shutdownNow();
                    log.warn("Executor was abruptly shut down. {} tasks were dropped.", droppedTasks.size());
                }
            } catch (InterruptedException e) {
                log.warn("Executor termination was interrupted.", e);
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
    @Loggable(level = Loggable.LogLevel.INFO, includeArgs = true, includeResult = false, value = "配属履歴テストデータ生成")
    @PerformanceMonitoring(threshold = 3000, operation = "ASSIGNMENT_HISTORY_DATA_GENERATION")
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