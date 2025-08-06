package com.example123.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example123.demo.domain.Employee;

import jakarta.validation.constraints.Min;

/**
 * ランダムな従業員データを生成するサービスクラス
 * テストデータ作成やデモンストレーション用途に使用されます
 */
@Service
@Validated
public class DataGenerationService {
    
    // 従業員データ生成用の定数
    private static final String[] LAST_NAMES = {
        "佐藤", "鈴木", "高橋", "田中", "伊藤", "渡辺", "山本", "中村", "小林", "加藤",
        "吉田", "山田", "佐々木", "山口", "松本", "井上", "木村", "林", "斎藤", "清水"
    };

    private static final String[] FIRST_NAMES = {
        "翔太", "陽子", "優子", "達也", "美咲", "健一", "愛", "豊", "舞", "誠",
        "香織", "大輔", "さくら", "悟", "恵", "拓也", "美穂", "剛", "智子", "隆"
    };

    private static final String[] DEPARTMENTS = {
        "営業部", "総務部", "人事部", "経理部", "開発部", "製造部", "品質管理部", "マーケティング部",
        "カスタマーサービス部", "研究開発部"
    };

    private static final String[] POSITIONS = {
        "部長", "次長", "課長", "係長", "主任", "担当", "アシスタント"
    };

    private static final String[] EMPLOYMENT_STATUSES = {
        "正社員", "契約社員", "パートタイマー", "アルバイト"
    };

    private final Random random = new Random();

    /**
     * 指定された件数のランダムな従業員データを生成します。
     * 日本の名前や一般的な部署名などを使用し、よりリアルなデータを生成します。
     * 80%更新・20%新規の比率で、既存IDと新規IDの範囲を分けて生成します。
     *
     * @param count 生成する従業員データの件数
     * @return 生成された従業員情報のリスト
     */
    public List<Employee> createRandomEmployees(@Min(value = 1, message = "生成件数は1以上である必要があります") int count) {
        List<Employee> employees = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // 80%更新、20%新規の比率で生成
        int updateCount = (int) Math.round(count * 0.8);
        int insertCount = count - updateCount;
        
        // 1) 更新用データ: 既存ID範囲から生成（E000000-E009999）
        Set<String> usedUpdateIds = new HashSet<>();
        for (int i = 0; i < updateCount; i++) {
            String id;
            do {
                id = String.format("E%06d", random.nextInt(10000)); // 既存ID: E000000-E009999
            } while (usedUpdateIds.contains(id));
            usedUpdateIds.add(id);

            Employee employee = createEmployeeWithId(id, now);
            employees.add(employee);
        }
        
        // 2) 新規用データ: 新規ID範囲から生成（E010000-E099999）
        Set<String> usedInsertIds = new HashSet<>();
        for (int i = 0; i < insertCount; i++) {
            String id;
            do {
                id = String.format("E%06d", random.nextInt(90000) + 10000); // 新規ID: E010000-E099999
            } while (usedInsertIds.contains(id));
            usedInsertIds.add(id);

            Employee employee = createEmployeeWithId(id, now);
            employees.add(employee);
        }
        
        return employees;
    }
    
    /**
     * 指定されたIDを持つ従業員データを生成します。
     *
     * @param id 従業員ID
     * @param now 現在時刻
     * @return 生成された従業員オブジェクト
     */
    public Employee createEmployeeWithId(String id, LocalDateTime now) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(LAST_NAMES[random.nextInt(LAST_NAMES.length)] +
                       " " + FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]);
        employee.setDepartment(DEPARTMENTS[random.nextInt(DEPARTMENTS.length)]);
        employee.setPosition(POSITIONS[random.nextInt(POSITIONS.length)]);
        employee.setEmployment_status(EMPLOYMENT_STATUSES[random.nextInt(EMPLOYMENT_STATUSES.length)]);
        
        // 入社日は過去10年以内
        employee.setHire_date(LocalDate.now().minusDays(random.nextInt(3650)));
        
        // 電話番号は東京の市外局番を使用
        employee.setPhone_number(String.format("03-%04d-%04d",
            random.nextInt(10000), random.nextInt(10000)));
        
        employee.setEmail("employee" + id + "@example.com");
        
        // 生年月日は22-60歳の範囲
        employee.setBirth_date(LocalDate.now()
            .minusYears(22 + random.nextInt(38))
            .minusDays(random.nextInt(365)));
        
        employee.setGender(random.nextBoolean() ? "男性" : "女性");
        employee.setCreated_by("SYSTEM");
        employee.setCreated_at(now);
        employee.setUpdated_by("SYSTEM");
        employee.setUpdated_at(now);
        employee.setVersion(0L);
        
        return employee;
    }

    /**
     * ダミーの従業員データを生成します
     * 生成されるデータには基本情報、個人情報、システム管理情報が含まれます
     *
     * @param count 生成する従業員データの件数
     * @return 生成された従業員情報のリスト
     */
    public List<Employee> createDummyEmployees(@Min(value = 1, message = "生成件数は1以上である必要があります") int count) {
        List<Employee> employees = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < count; i++) {
            String id = String.valueOf(i + 1);
            Employee employee = new Employee();
            employee.setId(id);
            employee.setName("Employee " + id);
            employee.setDepartment("Department " + ((i % 5) + 1));
            employee.setPosition("Position " + ((i % 3) + 1));
            employee.setEmployment_status(i % 3 == 0 ? "正社員" : i % 3 == 1 ? "契約社員" : "パートタイマー");
            employee.setHire_date(LocalDate.now().minusYears(i % 10));
            employee.setPhone_number("03-" + String.format("%04d", i % 10000) + "-" + 
                String.format("%04d", (i + 1) % 10000));
            employee.setEmail("employee" + id + "@example.com");
            employee.setBirth_date(LocalDate.now().minusYears(20 + (i % 40)));
            employee.setGender(i % 2 == 0 ? "男性" : "女性");
            employee.setCreated_by("SYSTEM");
            employee.setCreated_at(now);
            employee.setUpdated_by("SYSTEM");
            employee.setUpdated_at(now);
            employee.setVersion(0L);
            
            employees.add(employee);
        }
        return employees;
    }

    /**
     * UPSERT処理用の基礎データを準備します。
     * 更新対象となるデータ（E000000-E009999）を事前にテーブルに投入するためのデータを生成します。
     *
     * @return 基礎データとして使用する従業員リスト
     */
    public List<Employee> createBaseDataForUpsert() {
        List<Employee> baseEmployees = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // E000000-E009999の範囲でベースデータを生成
        for (int i = 0; i < 10000; i++) {
            String id = String.format("E%06d", i);
            Employee employee = createEmployeeWithId(id, now);
            baseEmployees.add(employee);
        }
        
        return baseEmployees;
    }
}