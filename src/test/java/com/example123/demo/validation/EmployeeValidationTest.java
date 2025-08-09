package com.example123.demo.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example123.demo.dto.EmployeeDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 従業員データの入力値検証テストクラス Bean Validationアノテーションの動作を検証します */
public class EmployeeValidationTest {

  private static final Logger log = LoggerFactory.getLogger(EmployeeValidationTest.class);
  private Validator validator;

  @BeforeEach
  public void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void testValidEmployeeDTO() {
    // 正常なEmployeeDTOオブジェクトを作成
    EmployeeDTO employee = new EmployeeDTO();
    employee.setId("E001");
    employee.setName("山田太郎");
    employee.setDepartment("開発部");
    employee.setPosition("エンジニア");
    employee.setEmployment_status("正社員");
    employee.setHire_date(LocalDate.of(2020, 4, 1));
    employee.setPhone_number("03-1234-5678");
    employee.setEmail("yamada@example.com");
    employee.setBirth_date(LocalDate.of(1990, 1, 1));
    employee.setGender("男性");

    // 検証実行
    Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(employee);

    // 検証エラーがないことを確認
    assertTrue(violations.isEmpty(), "正常なデータでは検証エラーが発生しないはず");
  }

  @Test
  public void testInvalidEmployeeDTO_RequiredFields() {
    // 必須フィールドが空のEmployeeDTOオブジェクトを作成
    EmployeeDTO employee = new EmployeeDTO();

    // 検証実行
    Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(employee);

    // デバッグ用: 検証エラー内容を出力
    log.debug("Validation errors count: {}", violations.size());
    violations.forEach(
        v -> log.debug("Field: {}, Message: {}", v.getPropertyPath(), v.getMessage()));

    // 必須フィールドの検証エラーが発生することを確認
    assertFalse(violations.isEmpty(), "必須フィールドが空の場合は検証エラーが発生するはず");
    // 実際のエラー数は7個（id, name, department, employment_status, hire_date, email, birth_date）
    assertEquals(7, violations.size(), "必須フィールド7個分のエラーが発生するはず");
  }

  @Test
  public void testInvalidEmployeeDTO_InvalidEmail() {
    // 無効なメールアドレスのEmployeeDTOオブジェクトを作成
    EmployeeDTO employee = createValidEmployeeDTO();
    employee.setEmail("invalid-email"); // 無効なメールアドレス

    // 検証実行
    Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(employee);

    // メールアドレスの検証エラーが発生することを確認
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("有効なメールアドレス")));
  }

  @Test
  public void testInvalidEmployeeDTO_InvalidPhoneNumber() {
    // 無効な電話番号のEmployeeDTOオブジェクトを作成
    EmployeeDTO employee = createValidEmployeeDTO();
    employee.setPhone_number("1234567890"); // 無効な電話番号形式

    // 検証実行
    Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(employee);

    // 電話番号の検証エラーが発生することを確認
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("正しい形式で入力")));
  }

  @Test
  public void testInvalidEmployeeDTO_InvalidEmploymentStatus() {
    // 無効な雇用形態のEmployeeDTOオブジェクトを作成
    EmployeeDTO employee = createValidEmployeeDTO();
    employee.setEmployment_status("無効な雇用形態");

    // 検証実行
    Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(employee);

    // 雇用形態の検証エラーが発生することを確認
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("正社員、契約社員、パートタイマー、アルバイト")));
  }

  @Test
  public void testInvalidEmployeeDTO_InvalidGender() {
    // 無効な性別のEmployeeDTOオブジェクトを作成
    EmployeeDTO employee = createValidEmployeeDTO();
    employee.setGender("無効な性別");

    // 検証実行
    Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(employee);

    // 性別の検証エラーが発生することを確認
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("男性または女性")));
  }

  /** 正常なEmployeeDTOオブジェクトを作成するヘルパーメソッド */
  private EmployeeDTO createValidEmployeeDTO() {
    EmployeeDTO employee = new EmployeeDTO();
    employee.setId("E001");
    employee.setName("山田太郎");
    employee.setDepartment("開発部");
    employee.setPosition("エンジニア");
    employee.setEmployment_status("正社員");
    employee.setHire_date(LocalDate.of(2020, 4, 1));
    employee.setPhone_number("03-1234-5678");
    employee.setEmail("yamada@example.com");
    employee.setBirth_date(LocalDate.of(1990, 1, 1));
    employee.setGender("男性");
    return employee;
  }
}
