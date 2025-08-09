package com.example123.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/** Bean Validationの設定クラス Service層での@Validatedアノテーションによる検証を有効にします */
@Configuration
public class ValidationConfig {

  /** Method Validationを有効にするプロセッサーを設定します Service層でのメソッドパラメーター検証に必要です */
  @Bean
  public MethodValidationPostProcessor methodValidationPostProcessor() {
    MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
    processor.setValidator(localValidatorFactoryBean());
    return processor;
  }

  /** Local Validator Factory Beanを設定します カスタムバリデーションメッセージなどの拡張に対応します */
  @Bean
  public LocalValidatorFactoryBean localValidatorFactoryBean() {
    return new LocalValidatorFactoryBean();
  }
}
