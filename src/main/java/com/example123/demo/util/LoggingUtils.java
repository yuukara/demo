package com.example123.demo.util;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** ログ出力の統一化と構造化を支援するユーティリティクラス MDC（Mapped Diagnostic Context）を使用してコンテキスト情報を管理 */
@Component
public class LoggingUtils {

  private static final Logger performanceLogger = LoggerFactory.getLogger("performance");

  /**
   * API処理開始ログを出力
   *
   * @param logger ロガーインスタンス
   * @param endpoint エンドポイント名
   * @param params パラメータ（機密情報は除く）
   * @return オペレーションID（処理終了時に使用）
   */
  public static String logApiStart(Logger logger, String endpoint, Object params) {
    String operationId = UUID.randomUUID().toString().substring(0, 8);
    MDC.put("operationId", operationId);
    MDC.put("endpoint", endpoint);
    logger.info(
        "API処理開始: {} - operationId: {}, params: {}", endpoint, operationId, sanitizeForLog(params));
    return operationId;
  }

  /**
   * API処理終了ログを出力
   *
   * @param logger ロガーインスタンス
   * @param operationId 処理開始時に取得したオペレーションID
   * @param executionTime 実行時間（ミリ秒）
   */
  public static void logApiEnd(Logger logger, String operationId, long executionTime) {
    try {
      MDC.put("executionTime", String.valueOf(executionTime));
      logger.info("API処理終了: operationId: {}, 実行時間: {}ms", operationId, executionTime);
    } finally {
      clearMDC();
    }
  }

  /**
   * パフォーマンス情報をログ出力
   *
   * @param operation 処理名
   * @param executionTime 実行時間（ミリ秒）
   * @param recordCount 処理件数
   */
  public static void logPerformance(String operation, long executionTime, int recordCount) {
    try {
      MDC.put("operation", operation);
      MDC.put("executionTime", String.valueOf(executionTime));
      MDC.put("recordCount", String.valueOf(recordCount));

      performanceLogger.info(
          "パフォーマンス測定: operation={}, executionTime={}ms, recordCount={}, throughput={}/sec",
          operation,
          executionTime,
          recordCount,
          executionTime > 0 ? (recordCount * 1000.0 / executionTime) : 0);
    } finally {
      clearMDC();
    }
  }

  /**
   * データベース処理開始ログ
   *
   * @param logger ロガーインスタンス
   * @param operation 処理名（INSERT, UPDATE, SELECT等）
   * @param tableName テーブル名
   * @param recordCount 対象件数
   */
  public static void logDatabaseStart(
      Logger logger, String operation, String tableName, int recordCount) {
    MDC.put("dbOperation", operation);
    MDC.put("tableName", tableName);
    MDC.put("recordCount", String.valueOf(recordCount));
    logger.info("DB処理開始: {} on {} - {} records", operation, tableName, recordCount);
  }

  /**
   * データベース処理終了ログ
   *
   * @param logger ロガーインスタンス
   * @param executionTime 実行時間（ミリ秒）
   * @param affectedRows 影響を受けた行数
   */
  public static void logDatabaseEnd(Logger logger, long executionTime, int affectedRows) {
    try {
      logger.info("DB処理終了: 実行時間={}ms, 影響行数={}", executionTime, affectedRows);
    } finally {
      clearMDC();
    }
  }

  /**
   * エラーログ出力（例外情報付き）
   *
   * @param logger ロガーインスタンス
   * @param message エラーメッセージ
   * @param throwable 例外オブジェクト
   */
  public static void logError(Logger logger, String message, Throwable throwable) {
    MDC.put("errorType", throwable.getClass().getSimpleName());
    logger.error("エラー発生: {} - {}", message, throwable.getMessage(), throwable);
    clearMDC();
  }

  /**
   * セキュリティイベントログ出力
   *
   * @param logger ロガーインスタンス
   * @param event イベント名
   * @param userId ユーザーID（マスキング済み）
   * @param details 詳細情報
   */
  public static void logSecurityEvent(Logger logger, String event, String userId, String details) {
    MDC.put("securityEvent", event);
    MDC.put("userId", maskUserId(userId));
    logger.warn("セキュリティイベント: {} - user: {}, details: {}", event, maskUserId(userId), details);
    clearMDC();
  }

  /**
   * ログ出力用にデータをサニタイズ（機密情報の除去）
   *
   * @param data サニタイズ対象データ
   * @return サニタイズ済みデータ
   */
  private static Object sanitizeForLog(Object data) {
    if (data == null) return null;

    String dataStr = data.toString();
    // パスワード、APIキー等の機密情報をマスキング
    dataStr =
        dataStr.replaceAll("(?i)(password|passwd|pwd|api[_-]?key|secret|token)=\\w+", "$1=***");
    dataStr =
        dataStr.replaceAll(
            "(?i)(password|passwd|pwd|api[_-]?key|secret|token)\":\\s*\"[^\"]+\"", "$1\":\"***\"");

    return dataStr;
  }

  /**
   * ユーザーIDをマスキング
   *
   * @param userId 元のユーザーID
   * @return マスキング済みユーザーID
   */
  private static String maskUserId(String userId) {
    if (userId == null || userId.length() <= 2) {
      return "***";
    }
    return userId.substring(0, 1) + "***" + userId.substring(userId.length() - 1);
  }

  /** MDCのクリア */
  private static void clearMDC() {
    MDC.clear();
  }
}
