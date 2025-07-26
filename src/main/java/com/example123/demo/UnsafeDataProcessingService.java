package com.example123.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

@Service
public class UnsafeDataProcessingService {

    public List<Integer> processData(int dataSize) {
        final List<Integer> result = Collections.synchronizedList(new ArrayList<>());
        ArrayList<Integer> data = new ArrayList<>();
        for (int i = 0; i < dataSize; i++) {
            data.add(i);
        }
        ExecutorService exec = Executors.newFixedThreadPool(3);

        for (final int s : data) {
            exec.execute(() -> result.add(s));
        }

        // 終了まで待機する
        while (true) {
            System.out.printf("[Unsafe] Input data size: %d, Result data size: %d%n", data.size(), result.size());
            if (data.size() == result.size()) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
        // スレッドタスクの終了
        exec.shutdown();

        System.out.println("[Unsafe] Processing finished.");
        return result;
    }
}