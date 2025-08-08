package com.example123.demo.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example123.demo.util.LoggingUtils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * グローバル例外ハンドラー
 * アプリケーション全体の例外処理とエラーレスポンスを統一的に管理します
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean Validation例外をハンドリングします
     * @Valid アノテーションによる検証エラーの処理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        
        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        
        // フィールドエラーを取得
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });
        
        // バリデーションエラーログを出力（セキュリティ考慮）
        Object target = ex.getBindingResult().getTarget();
        String targetClassName = target != null ? target.getClass().getSimpleName() : "Unknown";
        log.warn("入力値検証エラー: 検証対象={}, エラー件数={}", 
            targetClassName, 

            fieldErrors.size());
        
        errorResponse.put("status", "validation_error");
        errorResponse.put("message", "入力データに検証エラーがあります");
        errorResponse.put("errors", fieldErrors);
        errorResponse.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Service層での検証例外をハンドリングします
     * @Validated アノテーションによる検証エラーの処理
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        
        Map<String, Object> errorResponse = new HashMap<>();
        
        // 制約違反エラーメッセージを収集
        String errorMessages = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        errorResponse.put("status", "constraint_violation");
        errorResponse.put("message", errorMessages);
        errorResponse.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Bindingエラー例外をハンドリングします
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        
        // フィールドエラーを取得
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });
        
        errorResponse.put("status", "binding_error");
        errorResponse.put("message", "データバインディングエラーが発生しました");
        errorResponse.put("errors", fieldErrors);
        errorResponse.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * その他の一般的な例外をハンドリングします
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        
        // 重要なエラー情報をログ出力
        LoggingUtils.logError(log, "予期しないエラーが発生しました", ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("status", "internal_error");
        errorResponse.put("message", "内部サーバーエラーが発生しました");
        // 本番環境では詳細なエラーメッセージを隠蔽
        errorResponse.put("detail", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}