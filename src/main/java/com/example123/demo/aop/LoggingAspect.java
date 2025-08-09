package com.example123.demo.aop;


import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example123.demo.util.LoggingUtils;

/**
 * ログ出力を自動化するAspectクラス
 * @Loggableアノテーションが付けられたメソッドの実行前後に自動でログを出力
 */
@Aspect
@Component
public class LoggingAspect {
    
    
    
    /**
     * @Loggableアノテーションが付けられたメソッドの前後でログ出力
     */
    @Around("@annotation(loggable)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        Object[] args = joinPoint.getArgs();
        
        // クラス専用のロガーを取得
        Logger targetLogger = LoggerFactory.getLogger(signature.getDeclaringType());
        
        long startTime = System.nanoTime();
        String operationId;
        
        try {
            // メソッド実行開始ログ
            String methodFullName = String.format("%s.%s", className, methodName);
            String argsInfo = loggable.includeArgs() && args.length > 0 
                ? String.format("args=%s", Arrays.toString(sanitizeArgs(args)))
                : "no-args";
            
            operationId = LoggingUtils.logApiStart(targetLogger, methodFullName, argsInfo);
            
            // ログレベルに応じたメッセージ出力
            String customMessage = !loggable.value().isEmpty() ? loggable.value() : "メソッド実行開始";
            logMessage(targetLogger, loggable.level(), "{}: {} - operationId: {}", 
                customMessage, methodFullName, operationId);
            
            // メソッド実行
            Object result = joinPoint.proceed();
            
            // 実行時間計算
            long executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            
            // メソッド実行終了ログ
            String resultInfo = loggable.includeResult() && result != null 
                ? String.format(", result=%s", sanitizeForLog(result))
                : "";
            
            LoggingUtils.logApiEnd(targetLogger, operationId, executionTime);
            logMessage(targetLogger, loggable.level(), "メソッド実行完了: {} - 実行時間: {}ms{}", 
                methodFullName, executionTime, resultInfo);
            
            return result;
            
        } catch (Exception e) {
            LoggingUtils.logError(targetLogger, 
                String.format("メソッド実行エラー: %s.%s", className, methodName), e);
            throw e;
        }
    }
    
    /**
     * パフォーマンス監視アノテーション対応
     */
    @Around("@annotation(performanceMonitoring)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint, 
                                   PerformanceMonitoring performanceMonitoring) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        String operation = !performanceMonitoring.operation().isEmpty() 
            ? performanceMonitoring.operation() 
            : String.format("%s.%s", className, methodName);
        
        Logger targetLogger = LoggerFactory.getLogger(signature.getDeclaringType());
        
        long startTime = System.nanoTime();
        
        try {
            Object result = joinPoint.proceed();
            
            // 実行時間計算（指定された単位で）
            long executionTime = calculateExecutionTime(startTime, performanceMonitoring.unit());
            
            // パフォーマンスログ出力
            LoggingUtils.logPerformance(operation, executionTime, getRecordCount(result));
            
            // 閾値チェック
            if (executionTime > performanceMonitoring.threshold()) {
                targetLogger.warn("パフォーマンス警告: {} の実行時間が閾値を超えました。実行時間: {}ms, 閾値: {}ms", 
                    operation, executionTime, performanceMonitoring.threshold());
            }
            
            return result;
            
        } catch (Exception e) {
            LoggingUtils.logError(targetLogger, 
                String.format("パフォーマンス監視対象メソッドでエラー: %s", operation), e);
            throw e;
        }
    }
    
    /**
     * ログレベルに応じてメッセージを出力
     */
    private void logMessage(Logger logger, Loggable.LogLevel level, String message, Object... args) {
        switch (level) {
            case TRACE -> logger.trace(message, args);
            case DEBUG -> logger.debug(message, args);
            case INFO -> logger.info(message, args);
            case WARN -> logger.warn(message, args);
            case ERROR -> logger.error(message, args);
        }
    }
    
    /**
     * 引数をログ出力用にサニタイズ
     */
    private Object[] sanitizeArgs(Object[] args) {
        return Arrays.stream(args)
            .map(this::sanitizeForLog)
            .toArray();
    }
    
    /**
     * ログ出力用にデータをサニタイズ（機密情報の除去）
     */
    private Object sanitizeForLog(Object data) {
        if (data == null) return null;
        
        String dataStr = data.toString();
        // パスワード、APIキー等の機密情報をマスキング
        dataStr = dataStr.replaceAll("(?i)(password|passwd|pwd|api[_-]?key|secret|token)=\\w+", "$1=***");
        dataStr = dataStr.replaceAll("(?i)(password|passwd|pwd|api[_-]?key|secret|token)\":\\s*\"[^\"]+\"", "$1\":\"***\"");
        
        return dataStr;
    }
    
    /**
     * 指定された時間単位で実行時間を計算
     */
    private long calculateExecutionTime(long startTimeNanos, PerformanceMonitoring.TimeUnit unit) {
        long nanos = System.nanoTime() - startTimeNanos;
        return switch (unit) {
            case NANOSECONDS -> nanos;
            case MILLISECONDS -> TimeUnit.NANOSECONDS.toMillis(nanos);
            case SECONDS -> TimeUnit.NANOSECONDS.toSeconds(nanos);
        };
    }
    
    /**
     * 戻り値からレコード数を推定
     */
    private int getRecordCount(Object result) {
        if (result == null) return 0;
        if (result instanceof java.util.Collection<?> collection) {
            return collection.size();
        }
        if (result instanceof Object[] array) {
            return array.length;
        }
        return 1; // 単一オブジェクトの場合
    }
}