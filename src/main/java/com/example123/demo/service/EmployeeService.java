package com.example123.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example123.demo.domain.Employee;
import com.example123.demo.repository.EmployeeMapper;

/**
 * 従業員情報を管理するコアサービスクラス
 * 基本的なCRUD操作とデータベース接続機能を提供します
 */
@Service
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final DataGenerationService dataGenerationService;
    private final CsvExportService csvExportService;
    private final EmployeeDataService employeeDataService;

    public EmployeeService(EmployeeMapper employeeMapper, 
                          DataGenerationService dataGenerationService,
                          CsvExportService csvExportService,
                          EmployeeDataService employeeDataService) {
        this.employeeMapper = employeeMapper;
        this.dataGenerationService = dataGenerationService;
        this.csvExportService = csvExportService;
        this.employeeDataService = employeeDataService;
    }

    /**
     * 6000件のランダムな従業員データを生成し、一括UPSERTを行います。
     * 既存データとの重複を含み、MERGE文によるUPSERT処理をテストします。
     */
    public void generateAndUpsertRandomEmployees() {
        // 1) 基礎データ（更新対象）を事前投入
        employeeDataService.prepareBaseDataForUpsert();
        
        // 2) UPSERT用データを生成（80%更新・20%新規）
        List<Employee> employees = dataGenerationService.createRandomEmployees(6000);
        employeeDataService.upsertEmployeesInBatches(employees);
    }

    /**
     * 6000件のランダムな従業員データを生成し、一時テーブルを使用した一括UPSERTを行います。
     * 既存データとの重複を含み、一時テーブルによるUPSERT処理をテストします。
     *
     * @return 処理件数を含むMap（updateCount: 更新件数, insertCount: 挿入件数）
     */
    public java.util.Map<String, Integer> generateAndUpsertRandomEmployeesViaTempTable() {
        // 1) 基礎データ（更新対象）を事前投入
        employeeDataService.prepareBaseDataForUpsert();
        
        // 2) UPSERT用データを生成（80%更新・20%新規）
        List<Employee> employees = dataGenerationService.createRandomEmployees(6000);
        return employeeDataService.upsertEmployeesViaTempTableInBatches(employees);
    }

    /**
     * 従業員テーブルのデータを全て削除します。
     */
    public void truncateEmployeesTable() {
        employeeDataService.truncateEmployeesTable();
    }

    /**
     * 従業員情報をデータベースに保存します
     * 内部で並列処理による保存を行います
     *
     * @param employees 保存する従業員情報のリスト
     */
    public void saveEmployees(List<Employee> employees) {
        employeeDataService.saveEmployees(employees);
    }

    /**
     * ダミーの従業員データを生成します
     * 生成されるデータには基本情報、個人情報、システム管理情報が含まれます
     *
     * @param count 生成する従業員データの件数
     * @return 生成された従業員情報のリスト
     */
    public List<Employee> createDummyEmployees(int count) {
        return dataGenerationService.createDummyEmployees(count);
    }

    /**
     * 従業員情報をCSVファイルに出力します（シングルスレッド処理）
     * 全データを1つのスレッドで逐次的に処理します
     * 出力されるCSVには全カラムの情報が含まれます
     *
     * @param employees 出力する従業員情報のリスト
     * @param filePath 出力先のCSVファイルパス
     */
    public void writeToCsvSingleThread(List<Employee> employees, String filePath) {
        csvExportService.writeToCsvSingleThread(employees, filePath);
    }

    /**
     * 従業員情報をCSVファイルに出力します（マルチスレッド処理）
     * 利用可能なプロセッサ数に基づいてスレッドプールを作成し、
     * データを分割して並列で処理します
     * 出力されるCSVには全カラムの情報が含まれます
     *
     * @param employees 出力する従業員情報のリスト
     * @param filePath 出力先のCSVファイルパス
     */
    public void writeToCsv(List<Employee> employees, String filePath) {
        csvExportService.writeToCsv(employees, filePath);
    }

}
