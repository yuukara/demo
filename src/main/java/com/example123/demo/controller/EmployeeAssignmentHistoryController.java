package com.example123.demo.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example123.demo.domain.EmployeeAssignmentHistory;
import com.example123.demo.service.EmployeeAssignmentHistoryService;

@RestController
@RequestMapping("/assignment-histories")
public class EmployeeAssignmentHistoryController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeAssignmentHistoryController.class);
    private final EmployeeAssignmentHistoryService service;

    @Autowired
    public EmployeeAssignmentHistoryController(EmployeeAssignmentHistoryService service) {
        this.service = service;
    }

    /**
     * 指定された件数のダミー配属履歴データを生成し、一時テーブルを利用したUpsert処理を実行します。
     *
     * @param count 生成するデータの件数 (デフォルト: 10000)
     * @return 処理結果を示すメッセージ
     */
    @PostMapping("/upsert")
    public ResponseEntity<String> triggerUpsert(@RequestParam(defaultValue = "10000") int count) {
        try {
            List<EmployeeAssignmentHistory> histories = service.createMixedHistories(count, 0.8);
            service.upsertHistories(histories);
            return ResponseEntity.ok(String.format("Successfully upserted %d assignment histories.", count));
        } catch (Exception e) {
            log.error("An error occurred during the upsert process for assignment histories.", e);
            return ResponseEntity.internalServerError().body("An error occurred during the upsert process: " + e.getMessage());
        }
    }
}