package com.example123.demo.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example123.demo.service.EmployeeService;
import com.example123.demo.service.PopulationDataService;
import com.example123.demo.service.SafeDataProcessingService;
import com.example123.demo.repository.EmployeeMapper;

/**
 * EmployeeController WebMvcテストクラス
 * MockMvcを使用してHTTPレスポンスとAPIの動作を確認します
 */
@WebMvcTest(EmployeeController.class)
public class EmployeeControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;
    
    // These beans are required for DemoApplication.run() method during Spring context loading
    // Suppressing unused warnings as they're not directly used in tests but needed for context initialization
    @SuppressWarnings("unused")
    @MockitoBean
    private PopulationDataService populationDataService;
    
    @SuppressWarnings("unused")
    @MockitoBean
    private SafeDataProcessingService safeDataProcessingService;
    
    @SuppressWarnings("unused")
    @MockitoBean
    private EmployeeMapper employeeMapper;

    @Test
    public void testMergeUpsertApiEndpoint() throws Exception {
        System.out.println("Testing MERGE-based UPSERT API endpoint...");
        
        // APIの呼び出しとレスポンスの検証
        mockMvc.perform(post("/api/employees/test-merge-upsert")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.method").value("MERGE-based UPSERT"))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.executionTime").exists());
        
        System.out.println("MERGE-based UPSERT API endpoint test - SUCCESS");
    }

    @Test
    public void testTempTableUpsertApiEndpoint() throws Exception {
        System.out.println("Testing Temp Table-based UPSERT API endpoint...");
        
        // Mockの設定 - サービスが返すUPSERT結果をモック
        Map<String, Integer> mockUpsertResult = new HashMap<>();
        mockUpsertResult.put("updateCount", 4800);
        mockUpsertResult.put("insertCount", 1200);
        when(employeeService.generateAndUpsertRandomEmployeesViaTempTable()).thenReturn(mockUpsertResult);
        
        // APIの呼び出しとレスポンスの検証
        mockMvc.perform(post("/api/employees/test-temp-table-upsert")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.method").value("Temp Table-based UPSERT"))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.executionTime").exists())
                .andExpect(jsonPath("$.updateCount").value(4800))
                .andExpect(jsonPath("$.insertCount").value(1200));
        
        System.out.println("Temp Table-based UPSERT API endpoint test - SUCCESS");
    }

    @Test
    public void testApiEndpointsExist() throws Exception {
        System.out.println("Testing that API endpoints exist and are accessible...");
        
        // 両方のエンドポイントが存在し、HTTPメソッドが正しいことを確認
        mockMvc.perform(post("/api/employees/test-merge-upsert"))
                .andExpect(status().isOk());
        
        Map<String, Integer> mockResult = Map.of("updateCount", 100, "insertCount", 200);
        when(employeeService.generateAndUpsertRandomEmployeesViaTempTable()).thenReturn(mockResult);
        
        mockMvc.perform(post("/api/employees/test-temp-table-upsert"))
                .andExpect(status().isOk());
        
        System.out.println("API endpoints accessibility test - SUCCESS");
    }
}