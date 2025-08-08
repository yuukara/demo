package com.example123.demo.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * メソッドのパフォーマンス監視を行うためのアノテーション
 * 実行時間が閾値を超えた場合に警告ログを出力する
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceMonitoring {
    
    /**
     * 警告を出力する実行時間の閾値（ミリ秒）
     * @return 閾値（デフォルト: 5000ms = 5秒）
     */
    long threshold() default 5000L;
    
    /**
     * パフォーマンス測定の単位
     * @return 測定単位（デフォルト: MILLISECONDS）
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;
    
    /**
     * 処理名（ログ出力時に使用）
     * @return 処理名（空文字の場合はメソッド名を使用）
     */
    String operation() default "";
    
    /**
     * 時間単位定義
     */
    enum TimeUnit {
        NANOSECONDS, MILLISECONDS, SECONDS
    }
}