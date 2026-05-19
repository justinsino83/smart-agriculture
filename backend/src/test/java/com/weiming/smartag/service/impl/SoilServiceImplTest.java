package com.weiming.smartag.service.impl;

import com.weiming.smartag.entity.SoilData;
import com.weiming.smartag.entity.SoilSensor;
import com.weiming.smartag.mapper.SoilDataMapper;
import com.weiming.smartag.mapper.SoilSensorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SoilServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SoilServiceImplTest {

    @Mock
    private SoilSensorMapper soilSensorMapper;

    @Mock
    private SoilDataMapper soilDataMapper;

    @InjectMocks
    private SoilServiceImpl soilService;

    private SoilSensor testSensor;
    private SoilData testData;

    @BeforeEach
    void setUp() {
        testSensor = new SoilSensor();
        testSensor.setId(1L);
        testSensor.setDeviceCode("S001");
        testSensor.setDeviceName("1号田传感器");
        testSensor.setFieldId(1L);
        testSensor.setStatus(1);
        testSensor.setLocation("1号田");

        testData = new SoilData();
        testData.setId(1L);
        testData.setSensorId(1L);
        testData.setMoisture(45.5);
        testData.setTemperature(22.0);
        testData.setPh(6.8);
        testData.setEc(1.2);
        testData.setCollectTime(LocalDateTime.now());
    }

    @Test
    void testGetRealTimeData() {
        when(soilDataMapper.selectLatestBySensorId(1L)).thenReturn(testData);

        SoilData result = soilService.getRealTimeData(1L);

        assertNotNull(result);
        assertEquals(45.5, result.getMoisture());
        assertEquals(1L, result.getSensorId());
        verify(soilDataMapper).selectLatestBySensorId(1L);
    }

    @Test
    void testGetHistoryData() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<SoilData> mockList = Collections.singletonList(testData);

        when(soilDataMapper.selectHistoryData(1L, start, end)).thenReturn(mockList);

        List<SoilData> result = soilService.getHistoryData(1L, start, end);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(45.5, result.get(0).getMoisture());
    }

    @Test
    void testGetSoilOverview() {
        when(soilSensorMapper.selectOnlineSensors()).thenReturn(Collections.singletonList(testSensor));
        when(soilDataMapper.selectLatestBySensorId(1L)).thenReturn(testData);

        List<Map<String, Object>> result = soilService.getSoilOverview();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1号田传感器", result.get(0).get("deviceName"));
        assertNotNull(result.get(0).get("healthStatus"));
    }

    @Test
    void testEvaluateHealth_Optimal() {
        SoilData data = new SoilData();
        data.setMoisture(50.0);
        data.setPh(6.5);

        Map<String, Object> health = soilService.evaluateHealth(data);

        assertNotNull(health);
        assertEquals(100, health.get("score"));
        assertEquals("excellent", health.get("level"));
        assertEquals("optimal", health.get("moistureStatus"));
        assertEquals("optimal", health.get("phStatus"));
    }

    @Test
    void testEvaluateHealth_PoorMoisture() {
        SoilData data = new SoilData();
        data.setMoisture(15.0);
        data.setPh(6.5);

        Map<String, Object> health = soilService.evaluateHealth(data);

        assertNotNull(health);
        assertEquals("needs_attention", health.get("level"));
        assertEquals("poor", health.get("moistureStatus"));
        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) health.get("suggestions");
        assertTrue(suggestions.size() > 0);
    }

    @Test
    void testGetAlerts() {
        SoilData alertData = new SoilData();
        alertData.setMoisture(15.0); // 低于20触发预警
        alertData.setPh(5.0); // 低于5.5触发预警
        alertData.setCollectTime(LocalDateTime.now());

        when(soilSensorMapper.selectOnlineSensors()).thenReturn(Collections.singletonList(testSensor));
        when(soilDataMapper.selectLatestBySensorId(1L)).thenReturn(alertData);

        List<Map<String, Object>> alerts = soilService.getAlerts();

        assertNotNull(alerts);
        assertTrue(alerts.size() >= 2);
        assertTrue(alerts.stream().anyMatch(a -> "moisture".equals(a.get("type"))));
        assertTrue(alerts.stream().anyMatch(a -> "ph".equals(a.get("type"))));
    }

    @Test
    void testGetIrrigationRecommendations() {
        SoilData dryData = new SoilData();
        dryData.setMoisture(30.0);
        dryData.setCollectTime(LocalDateTime.now());

        when(soilSensorMapper.selectOnlineSensors()).thenReturn(Collections.singletonList(testSensor));
        when(soilDataMapper.selectLatestBySensorId(1L)).thenReturn(dryData);

        List<Map<String, Object>> recommendations = soilService.getIrrigationRecommendations();

        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        assertEquals("1号田", recommendations.get(0).get("fieldName"));
    }

    @Test
    void testGetStatistics() {
        when(soilSensorMapper.selectOnlineSensors()).thenReturn(Collections.singletonList(testSensor));
        when(soilDataMapper.selectLatestBySensorId(1L)).thenReturn(testData);
        when(soilSensorMapper.selectCount(any())).thenReturn(1L);

        Map<String, Object> stats = soilService.getStatistics();

        assertNotNull(stats);
        assertEquals(1L, stats.get("totalSensors"));
        assertNotNull(stats.get("avgMoisture"));
        assertNotNull(stats.get("avgTemperature"));
    }
}
