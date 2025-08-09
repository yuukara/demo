package com.example123.demo.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 職員配属履歴テーブル (employee_assignment_history) に対応するドメインクラス */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAssignmentHistory {

  // Composite Primary Key
  private String employeeId;
  private String orgCode;
  private String jobCode;
  private LocalDate effectiveFrom;
  private Integer seqNo;

  // Attributes
  private LocalDate effectiveTo;
  private String statusCode;
  private String baseLocationCode;
  private String employmentType;
  private String gradeCode;
  private String salaryBandCode;
  private String managerEmpId;
  private String projectCode;
  private String costCenterCode;
  private String workPatternCode;
  private String shiftGroupCode;
  private Boolean allowRemote;
  private BigDecimal fteRatio;

  // Dummy Attributes
  private String attr1;
  private String attr2;
  private String attr3;
  private String attr4;
  private String attr5;
  private String attr6;
  private String attr7;
  private String attr8;
  private String attr9;
  private String attr10;
  private String attr11;
  private String attr12;

  // Audit
  private LocalDateTime createdAt;
  private String createdBy;
  private LocalDateTime updatedAt;
  private String updatedBy;
  private byte[] rv; // ROWVERSION
}
