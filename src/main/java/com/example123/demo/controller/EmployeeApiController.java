package com.example123.demo.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example123.demo.domain.Employee;
import com.example123.demo.dto.EmployeeDTO;
import com.example123.demo.service.EmployeeDataService;
import com.example123.demo.util.LoggingUtils;

import jakarta.validation.Valid;

/**
 * 従業員API機能を提供するコントローラークラス
 * 入力値検証を含む基本的なCRUD操作を実装しています
 */
@RestController
@RequestMapping("/api/v1/employees")
@Validated
public class EmployeeApiController {
    
    private static final Logger log = LoggerFactory.getLogger(EmployeeApiController.class);
    private final EmployeeDataService employeeDataService;

    public EmployeeApiController(EmployeeDataService employeeDataService) {
        this.employeeDataService = employeeDataService;
    }

    /**
     * 単一の従業員情報を登録します
     * 入力データの検証を行い、エラーがあれば400エラーを返します
     *
     * @param employeeDTO 登録する従業員情報
     * @return 登録結果
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) {
        String operationId = LoggingUtils.logApiStart(log, "/api/v1/employees/create", 
            String.format("employeeId=%s, department=%s", employeeDTO.getId(), employeeDTO.getDepartment()));
        
        try {
            // DTOからEmployeeエンティティに変換
            Employee employee = convertToEntity(employeeDTO);
            log.debug("従業員DTOをエンティティに変換完了: ID={}", employee.getId());
            
            // データベースに保存
            long dbStartTime = System.currentTimeMillis();
            LoggingUtils.logDatabaseStart(log, "INSERT", "employees", 1);
            
            employeeDataService.saveEmployees(java.util.Collections.singletonList(employee));
            
            long dbExecutionTime = System.currentTimeMillis() - dbStartTime;
            LoggingUtils.logDatabaseEnd(log, dbExecutionTime, 1);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "従業員情報が正常に登録されました");
            response.put("employeeId", employee.getId());
            response.put("timestamp", LocalDateTime.now());
            
            LoggingUtils.logApiEnd(log, operationId, System.currentTimeMillis() - Long.parseLong(operationId.substring(0, 8).replaceAll("[^0-9]", "0")));
            log.info("従業員登録成功: ID={}, 部署={}", employee.getId(), employee.getDepartment());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            LoggingUtils.logError(log, "従業員登録処理でエラーが発生", e);
            throw e;
        }
    }

    /**
     * EmployeeDTOからEmployeeエンティティに変換します
     *
     * @param dto 変換元のDTO
     * @return 変換されたEmployeeエンティティ
     */
    private Employee convertToEntity(EmployeeDTO dto) {
        Employee employee = new Employee();
        employee.setId(dto.getId());
        employee.setName(dto.getName());
        employee.setDepartment(dto.getDepartment());
        employee.setPosition(dto.getPosition());
        employee.setEmployment_status(dto.getEmployment_status());
        employee.setHire_date(dto.getHire_date());
        employee.setPhone_number(dto.getPhone_number());
        employee.setEmail(dto.getEmail());
        employee.setBirth_date(dto.getBirth_date());
        employee.setGender(dto.getGender());
        
        // システム管理項目を設定
        LocalDateTime now = LocalDateTime.now();
        employee.setCreated_by("API_USER");
        employee.setCreated_at(now);
        employee.setUpdated_by("API_USER");
        employee.setUpdated_at(now);
        employee.setVersion(0L);
        
        return employee;
    }
}