package com.example123.demo;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * 従業員情報のデータベース操作を行うマッパーインターフェース
 * MyBatisを使用してSQLとJavaオブジェクトのマッピングを行います
 */
@Mapper
public interface EmployeeMapper {
    /**
     * 従業員テーブルの全レコードを削除します
     * TRUNCATE TABLE文を使用して高速な削除を行います
     */
    void truncateTable();

    /**
     * 従業員情報を一括でデータベースに挿入します
     * バッチ処理による効率的なデータ登録を行います
     *
     * @param employees 登録する従業員情報のリスト
     */
    void bulkInsert(List<Employee> employees);

    /**
     * 従業員情報を一括でUPSERT（更新または挿入）します
     * MERGE文を使用して効率的な一括処理を行います
     * 
     * - 既存レコードの場合：バージョンを増分して更新
     * - 新規レコードの場合：新しいレコードとして挿入
     *
     * @param employees UPSERT対象の従業員情報のリスト
     */
    void bulkUpsert(List<Employee> employees);
}