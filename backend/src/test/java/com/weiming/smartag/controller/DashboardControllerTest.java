package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.service.DryingService;
import com.weiming.smartag.service.IrrigationService;
import com.weiming.smartag.service.SoilService;
import com.weiming.smartag.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DashboardController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private SoilService soilService;

    @Mock
    private IrrigationService irrigationService;

    @Mock
    private DryingService dryingService;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
    }

    @Test
    void testGetOverview() {
        Map<String, Object> soilStats = new HashMap<>();
        soilStats.put("totalSensors", 10);

        Map<String, Object> irrigationStats = new HashMap<>();
        irrigationStats.put("todayUsage", 25.5);

        Map<String, Object> dryingStats = new HashMap<>();
        dryingStats.put("runningCount", 2);

        Map<String, Object> storageStats = new HashMap<>();
        storageStats.put("warningCount", 1);

        when(soilService.getStatistics()).thenReturn(soilStats);
        when(irrigationService.getStatistics("day")).thenReturn(irrigationStats);
        when(dryingService.getStatistics()).thenReturn(dryingStats);
        when(storageService.getStatistics()).thenReturn(storageStats);
        when(soilService.getAlerts()).thenReturn(Collections.emptyList());

        Result<Map<String, Object>> result = dashboardController.getOverview();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertNotNull(result.getData().get("soil"));
        assertNotNull(result.getData().get("irrigation"));
        assertNotNull(result.getData().get("drying"));
        assertNotNull(result.getData().get("storage"));
    }

    @Test
    void testGetStatistics() {
        Result<Map<String, Object>> result = dashboardController.getStatistics();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        Map<String, Object> data = result.getData();
        assertNotNull(data);
        assertNotNull(data.get("fieldCount"));
        assertNotNull(data.get("soilSensors"));
        assertNotNull(data.get("irrigationDevices"));
    }

    @Test
    void testGetRealtimeData() {
        Result<Map<String, Object>> result = dashboardController.getRealtimeData(null);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        Map<String, Object> data = result.getData();
        assertNotNull(data.get("temperature"));
        assertNotNull(data.get("humidity"));
        assertNotNull(data.get("updateTime"));
    }

    @Test
    void testGetFieldDistribution() {
        Result<List<Map<String, Object>>> result = dashboardController.getFieldDistribution();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        List<Map<String, Object>> fields = result.getData();
        assertNotNull(fields);
        assertFalse(fields.isEmpty());
        assertNotNull(fields.get(0).get("name"));
        assertNotNull(fields.get(0).get("lat"));
        assertNotNull(fields.get(0).get("lng"));
    }

    @Test
    void testGetEnergyStats() {
        Result<Map<String, Object>> result = dashboardController.getEnergyStats();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        Map<String, Object> data = result.getData();
        assertNotNull(data.get("todayPower"));
        assertNotNull(data.get("trend"));
    }

    @Test
    void testGetActivities() {
        Result<List<Map<String, Object>>> result = dashboardController.getRecentActivities();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        List<Map<String, Object>> activities = result.getData();
        assertNotNull(activities);
        assertFalse(activities.isEmpty());
    }

    @Test
    void testGetOverviewMvc() throws Exception {
        when(soilService.getStatistics()).thenReturn(new HashMap<>());
        when(irrigationService.getStatistics("day")).thenReturn(new HashMap<>());
        when(dryingService.getStatistics()).thenReturn(new HashMap<>());
        when(storageService.getStatistics()).thenReturn(new HashMap<>());
        when(soilService.getAlerts()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk());
    }
}
