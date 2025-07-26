package com.example123.demo.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 従業員情報を管理するエンティティクラス
 * 従業員の基本情報、個人情報、システム管理情報を保持します
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
    /** 従業員ID - 主キー */
    private String id;

    /** 従業員氏名 */
    private String name;

    /** 所属部署 */
    private String department;

    /** 役職 */
    private String position;

    /** 雇用形態（正社員、契約社員、パートタイマー等） */
    private String employment_status;

    /** 入社日 */
    private LocalDate hire_date;

    /** 電話番号 */
    private String phone_number;

    /** メールアドレス */
    private String email;

    /** 生年月日 */
    private LocalDate birth_date;

    /** 性別 */
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
