package com.example123.demo.repository;

import com.example123.demo.domain.EmployeeAssignmentHistory;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeeAssignmentHistoryMapper {

  /**
   * 一時テーブルを利用して配属履歴の一括Upsertを行います。 内部で一時テーブルの作成、データ挿入、更新、新規挿入をまとめて実行します。
   *
   * @param historyList 挿入または更新する配属履歴のリスト
   * @return Map<String, Integer> "updateCount":更新件数, "insertCount":挿入件数
   */
  Map<String, Integer> upsertViaTempTable(List<EmployeeAssignmentHistory> historyList);

  List<EmployeeAssignmentHistoryKey> selectRandomKeys(@Param("limit") int limit);

  long countAll();
}
