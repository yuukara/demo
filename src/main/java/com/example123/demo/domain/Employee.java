package com.example123.demo.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 従業員情報を管理するエンティティクラス 従業員の基本情報、個人情報、システム管理情報を保持します */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
  /** 従業員ID - 主キー */
  @NotBlank(message = "従業員IDは必須です")
  @Size(max = 20, message = "従業員IDは20文字以内で入力してください")
  private String id;

  /** 従業員氏名 */
  @NotBlank(message = "従業員名は必須です")
  @Size(max = 100, message = "従業員名は100文字以内で入力してください")
  private String name;

  /** 所属部署 */
  @NotBlank(message = "部署名は必須です")
  @Size(max = 100, message = "部署名は100文字以内で入力してください")
  private String department;

  /** 役職 */
  @Size(max = 50, message = "役職は50文字以内で入力してください")
  private String position;

  /** 雇用形態（正社員、契約社員、パートタイマー等） */
  @NotBlank(message = "雇用形態は必須です")
  @Pattern(regexp = "正社員|契約社員|パートタイマー|アルバイト", message = "雇用形態は正社員、契約社員、パートタイマー、アルバイトのいずれかを選択してください")
  private String employment_status;

  /** 入社日 */
  @NotNull(message = "入社日は必須です")
  private LocalDate hire_date;

  /** 電話番号 */
  @Pattern(
      regexp = "^[0-9]{2,3}-[0-9]{4}-[0-9]{4}$",
      message = "電話番号は正しい形式で入力してください (例: 03-1234-5678)")
  private String phone_number;

  /** メールアドレス */
  @NotBlank(message = "メールアドレスは必須です")
  @Email(message = "有効なメールアドレスを入力してください")
  @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
  private String email;

  /** 生年月日 */
  @NotNull(message = "生年月日は必須です")
  private LocalDate birth_date;

  /** 性別 */
  @Pattern(regexp = "男性|女性", message = "性別は男性または女性を選択してください")
  private String gender;

  /** レコード登録者 */
  private String created_by;

  /** レコード登録日時 */
  private LocalDateTime created_at;

  /** レコード更新者 */
  private String updated_by;

  /** レコード更新日時 */
  private LocalDateTime updated_at;

  /** 排他制御用バージョン番号 - 楽観的ロックに使用 */
  private Long version;

  public Employee(String id, String name, String department, String email) {
    this.id = id;
    this.name = name;
    this.department = department;
    this.email = email;
  }
}
