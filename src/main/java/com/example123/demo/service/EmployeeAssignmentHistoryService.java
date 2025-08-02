package com.example123.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void upsertHistories(List<EmployeeAssignmentHistory> historyList) {
        if (historyList == null || historyList.isEmpty()) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        System.out.printf("Upserting %d assignment histories...%n", historyList.size());

        // バッチ処理
        List<List<EmployeeAssignmentHistory>> batches = new ArrayList<>();
        for (int i = 0; i < historyList.size(); i += BATCH_SIZE) {
            batches.add(new ArrayList<>(historyList.subList(i,
                    Math.min(i + BATCH_SIZE, historyList.size()))));
        }

        int totalUpdates = 0;
        int totalInserts = 0;

        for(List<EmployeeAssignmentHistory> batch : batches) {
            Map<String, Integer> counts = mapper.upsertViaTempTable(batch);
            totalUpdates += counts.get("updateCount");
            totalInserts += counts.get("insertCount");
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("Assignment history upsert completed in %.2f seconds%n", totalTime / 1000.0);
        System.out.printf("Updates: %d records, Inserts: %d records%n", totalUpdates, totalInserts);

        // サンプルデータのパターンを表示
        if (!historyList.isEmpty()) {
            EmployeeAssignmentHistory sample = historyList.get(0);
            System.out.println("\nEmployee ID pattern examples:");
            System.out.printf("Update pattern (random): E%06d%n", random.nextInt(historyList.size()));
            System.out.printf("Insert pattern (sequential): E%06d%n", historyList.size() - 1);
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