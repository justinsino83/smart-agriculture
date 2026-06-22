package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weiming.smartag.entity.DevicePushData;
import com.weiming.smartag.entity.SoilData;
import com.weiming.smartag.mapper.DevicePushDataMapper;
import com.weiming.smartag.mapper.SoilSensorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SoilServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SoilServiceImplTest {

    @Mock
    private SoilSensorMapper soilSensorMapper;

    @Mock
    private DevicePushDataMapper devicePushDataMapper;

    @InjectMocks
    private SoilServiceImpl soilService;

    private DevicePushData testPushData;

    @BeforeEach
    void setUp() {
        testPushData = new DevicePushData();
        testPushData.setId(1L);
        testPushData.setClientId("2604073202TXD-01");
        testPushData.setSoilPh(new BigDecimal("6.80"));
        testPushData.setDetectedTime(LocalDateTime.now());
        testPushData.setCreateTime(LocalDateTime.now());
    }

    @Test
    void testGetRealTimeData() {
        when(devicePushDataMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testPushData);

        SoilData result = soilService.getRealTimeData("2604073202TXD-01");

        assertNotNull(result);
        assertEquals(6.8, result.getPh());
        assertNull(result.getMoisture());
        verify(devicePushDataMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void testGetHistoryDataPage() {
        when(devicePushDataMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(devicePushDataMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(testPushData));

        Map<String, Object> result = soilService.getHistoryDataPage(
                "2604073202TXD-01",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                1,
                20
        );

        assertNotNull(result);
        assertEquals(1L, result.get("total"));
        assertEquals(1, result.get("page"));
        assertEquals(20, result.get("size"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
        assertEquals(1, list.size());
        assertEquals(6.8, list.get(0).get("ph"));
    }

    @Test
    void testEvaluateHealth_Optimal() {
        SoilData data = new SoilData();
        data.setPh(6.5);

        Map<String, Object> health = soilService.evaluateHealth(data);

        assertNotNull(health);
        assertEquals(95, health.get("score"));
        assertEquals("excellent", health.get("level"));
        assertEquals("optimal", health.get("phStatus"));
    }

    @Test
    void testGetAlerts() {
        DevicePushData alertData = new DevicePushData();
        alertData.setClientId("2604073202TXD-01");
        alertData.setSoilPh(new BigDecimal("5.00"));
        alertData.setDetectedTime(LocalDateTime.now());
        alertData.setCreateTime(LocalDateTime.now());

        when(devicePushDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(alertData));
        when(devicePushDataMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(alertData);

        List<Map<String, Object>> alerts = soilService.getAlerts();

        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        assertEquals("ph", alerts.get(0).get("type"));
        verify(devicePushDataMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(devicePushDataMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void testGetStatistics() {
        when(devicePushDataMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testPushData));
        when(devicePushDataMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testPushData);

        Map<String, Object> stats = soilService.getStatistics();

        assertNotNull(stats);
        assertEquals(1, stats.get("totalSensors"));
        assertEquals(1, stats.get("onlineSensors"));
        assertEquals(100L, stats.get("onlineRate"));
        assertEquals(6.8, stats.get("avgPh"));
        assertTrue(((Integer) stats.get("alertCount")) >= 0);
    }
}
