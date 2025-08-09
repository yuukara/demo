package com.example123.demo.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** メソッドの実行ログを自動出力するためのアノテーション このアノテーションを付けたメソッドは実行前後に自動でログが出力される */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {

  /**
   * ログレベルを指定
   *
   * @return ログレベル（デフォルト: INFO）
   */
  LogLevel level() default LogLevel.INFO;

  /**
   * メソッドの引数をログに含めるかどうか
   *
   * @return true: 引数を含める, false: 含めない（デフォルト: true）
   */
  boolean includeArgs() default true;

  /**
   * メソッドの戻り値をログに含めるかどうか
   *
   * @return true: 戻り値を含める, false: 含めない（デフォルト: false）
   */
  boolean includeResult() default false;

  /**
   * カスタムログメッセージ
   *
   * @return カスタムメッセージ（空文字の場合はデフォルトメッセージを使用）
   */
  String value() default "";

  /** ログレベル定義 */
  enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
  }
}
