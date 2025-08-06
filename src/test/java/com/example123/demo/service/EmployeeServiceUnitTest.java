package com.example123.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example123.demo.domain.Employee;

/**
 * EmployeeService単体テストクラス
 * リファクタリング後のサービス分割と委譲が正しく動作することを確認します
 */
@ExtendWith(MockitoExtension.class)
public class EmployeeServiceUnitTest {

    @Mock
    private DataGenerationService dataGenerationService;

    @Mock
    private CsvExportService csvExportService;

    @Mock
    private EmployeeDataService employeeDataService;

    private EmployeeService employeeService;

    @BeforeEach
    @SuppressWarnings("unused") // JUnit @BeforeEachによる自動実行のためIDEが使用を検出できない
    void setUp() {
        // EmployeeServiceインスタンスを初期化
        // このメソッドは@BeforeEachにより各テスト実行前に自動で呼び出される
        employeeService = new EmployeeService(dataGenerationService, 
                                            csvExportService, employeeDataService);
    }

    @Test
    void testCreateDummyEmployees() {
        // テストデータの準備
        List<Employee> mockEmployees = Arrays.asList(
            createTestEmployee("1", "Test Employee 1"),
            createTestEmployee("2", "Test Employee 2")
        );
        
        // Mockの設定
        when(dataGenerationService.createDummyEmployees(2)).thenReturn(mockEmployees);
        
        // テスト実行
        List<Employee> result = employeeService.createDummyEmployees(2);
        
        // 結果検証
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return 2 employees");
        assertEquals("1", result.get(0).getId(), "First employee ID should be '1'");
        assertEquals("2", result.get(1).getId(), "Second employee ID should be '2'");
        
        // 委譲が正しく行われることを確認
        verify(dataGenerationService, times(1)).createDummyEmployees(2);
    }

    @Test
    void testSaveEmployees() {
        // テストデータの準備
        List<Employee> testEmployees = Arrays.asList(
            createTestEmployee("1", "Test Employee 1")
        );
        
        // テスト実行
        employeeService.saveEmployees(testEmployees);
        
        // 委譲が正しく行われることを確認
        verify(employeeDataService, times(1)).saveEmployees(testEmployees);
    }

    @Test
    void testTruncateEmployeesTable() {
        // テスト実行
        employeeService.truncateEmployeesTable();
        
        // 委譲が正しく行われることを確認
        verify(employeeDataService, times(1)).truncateEmployeesTable();
    }

    @Test
    void testWriteToCsv() {
        // テストデータの準備
        List<Employee> testEmployees = Arrays.asList(
            createTestEmployee("1", "Test Employee 1")
        );
        String filePath = "test.csv";
        
        // テスト実行
        employeeService.writeToCsv(testEmployees, filePath);
        
        // 委譲が正しく行われることを確認
        verify(csvExportService, times(1)).writeToCsv(testEmployees, filePath);
    }

    @Test
    void testWriteToCsvSingleThread() {
        // テストデータの準備
        List<Employee> testEmployees = Arrays.asList(
            createTestEmployee("1", "Test Employee 1")
        );
        String filePath = "test_single.csv";
        
        // テスト実行
        employeeService.writeToCsvSingleThread(testEmployees, filePath);
        
        // 委譲が正しく行われることを確認
        verify(csvExportService, times(1)).writeToCsvSingleThread(testEmployees, filePath);
    }

    @Test
    void testGenerateAndUpsertRandomEmployees() {
        // テストデータの準備
        List<Employee> mockEmployees = Arrays.asList(
            createTestEmployee("E000001", "Random Employee 1"),
            createTestEmployee("E010001", "Random Employee 2")
        );
        
        // Mockの設定
        when(dataGenerationService.createRandomEmployees(6000)).thenReturn(mockEmployees);
        
        // テスト実行
        employeeService.generateAndUpsertRandomEmployees();
        
        // 委譲が正しく行われることを確認
        verify(employeeDataService, times(1)).prepareBaseDataForUpsert();
        verify(dataGenerationService, times(1)).createRandomEmployees(6000);
        verify(employeeDataService, times(1)).upsertEmployeesInBatches(mockEmployees);
    }

    @Test
    void testGenerateAndUpsertRandomEmployeesViaTempTable() {
        // テストデータの準備
        List<Employee> mockEmployees = Arrays.asList(
            createTestEmployee("E000001", "Random Employee 1"),
            createTestEmployee("E010001", "Random Employee 2")
        );
        Map<String, Integer> mockResult = new HashMap<>();
        mockResult.put("updateCount", 1);
        mockResult.put("insertCount", 1);
        
        // Mockの設定
        when(dataGenerationService.createRandomEmployees(6000)).thenReturn(mockEmployees);
        when(employeeDataService.upsertEmployeesViaTempTableInBatches(mockEmployees)).thenReturn(mockResult);
        
        // テスト実行
        Map<String, Integer> result = employeeService.generateAndUpsertRandomEmployeesViaTempTable();
        
        // 結果検証
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.get("updateCount"), "Update count should be 1");
        assertEquals(1, result.get("insertCount"), "Insert count should be 1");
        
        // 委譲が正しく行われることを確認
        verify(employeeDataService, times(1)).prepareBaseDataForUpsert();
        verify(dataGenerationService, times(1)).createRandomEmployees(6000);
        verify(employeeDataService, times(1)).upsertEmployeesViaTempTableInBatches(mockEmployees);
    }

    @Test
    void testServiceDelegationArchitecture() {
        // サービス分割アーキテクチャのテスト
        // 各メソッドが適切なサービスに委譲されることを確認
        
        List<Employee> testEmployees = Arrays.asList(createTestEmployee("1", "Test"));
        
        // 1. データ生成の委譲
        when(dataGenerationService.createDummyEmployees(anyInt())).thenReturn(testEmployees);
        employeeService.createDummyEmployees(1);
        verify(dataGenerationService).createDummyEmployees(1);
        
        // 2. データ操作の委譲
        employeeService.saveEmployees(testEmployees);
        verify(employeeDataService).saveEmployees(testEmployees);
        
        // 3. CSV出力の委譲
        employeeService.writeToCsv(testEmployees, "test.csv");
        verify(csvExportService).writeToCsv(testEmployees, "test.csv");
        
        // 4. テーブル操作の委譲
        employeeService.truncateEmployeesTable();
        verify(employeeDataService).truncateEmployeesTable();
    }

    /**
     * テスト用のEmployeeオブジェクトを作成するヘルパーメソッド
     */
    private Employee createTestEmployee(String id, String name) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(name);
        employee.setDepartment("Test Department");
        employee.setEmail("test@example.com");
        employee.setEmployment_status("正社員");
        return employee;
    }
}