package com.example123.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SafeDataProcessingService {

    private static final Logger log = LoggerFactory.getLogger(SafeDataProcessingService.class);
    private static final int CHUNK_SIZE = 10000;

    public List<Integer> processData(int dataSize) {
        // 元データ生成
        List<Integer> data = new ArrayList<>(dataSize);
        for (int i = 0; i < dataSize; i++) {
            data.add(i);
        }

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CompletionService<List<Integer>> cs = new ExecutorCompletionService<>(pool);

        int taskCount = 0;
        for (int i = 0; i < data.size(); i += CHUNK_SIZE) {
            final int from = i;
            final int to = Math.min(i + CHUNK_SIZE, data.size());
            cs.submit(() -> new ArrayList<>(data.subList(from, to)));
            taskCount++;
        }

        List<Integer> result = new ArrayList<>(dataSize);
        try {
            for (int i = 0; i < taskCount; i++) {
                Future<List<Integer>> f = cs.take();
                result.addAll(f.get());
            }
        } catch (Exception e) {
            log.error("Error during safe data processing", e);
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(1, TimeUnit.MINUTES)) {
                    log.warn("Executor did not terminate in the specified time.");
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Executor termination was interrupted.", e);
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("[Safe] Processing finished.");
        return result;
    }
}