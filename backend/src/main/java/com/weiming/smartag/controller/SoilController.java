package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.service.SoilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * 土壤监测控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/soil")
@RequiredArgsConstructor
@CrossOrigin
public class SoilController {
    
    private final SoilService soilService;
    
    /**
     * 获取所有传感器列表（包含实时监测数据）
     */
    @GetMapping("/sensors")
    public Result<List<?>> listSensors() {
        try {
            return Result.success(soilService.listSensorsWithRealTimeData());
        } catch (Exception e) {
            log.error("获取传感器列表失败", e);
            return Result.fail("获取传感器列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取传感器实时数据
     */
    @GetMapping("/realtime/{sensorId}")
    public Result<?> getRealTimeData(@PathVariable Long sensorId) {
        try {
            if (sensorId == null || sensorId <= 0) {
                return Result.fail("传感器ID必须大于0");
            }
            Object data = soilService.getRealTimeData(sensorId);
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取实时数据失败, sensorId: {}", sensorId, e);
            return Result.fail("获取实时数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取传感器历史数据
     */
    @GetMapping("/history/{sensorId}")
    public Result<List<?>> getHistoryData(
            @PathVariable Long sensorId,
            @RequestParam String start,
            @RequestParam String end) {
        try {
            if (sensorId == null || sensorId <= 0) {
                return Result.fail("传感器ID必须大于0");
            }
            if (!StringUtils.hasText(start)) {
                return Result.fail("开始时间不能为空");
            }
            if (!StringUtils.hasText(end)) {
                return Result.fail("结束时间不能为空");
            }
            
            LocalDateTime startTime = LocalDateTime.parse(start.replace("Z", "").substring(0, 19));
            LocalDateTime endTime = LocalDateTime.parse(end.replace("Z", "").substring(0, 19));
            
            if (startTime.isAfter(endTime)) {
                return Result.fail("开始时间不能大于结束时间");
            }
            
            return Result.success(soilService.getHistoryData(sensorId, startTime, endTime));
        } catch (DateTimeParseException e) {
            log.error("日期格式错误, start: {}, end: {}", start, end, e);
            return Result.fail("日期格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
        } catch (Exception e) {
            log.error("获取历史数据失败, sensorId: {}, start: {}, end: {}", sensorId, start, end, e);
            return Result.fail("获取历史数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建空的土壤数据，用于处理异常情况
     */
    private Map<String, Object> createEmptySoilData() {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("moisture", 45.0);
        data.put("temperature", 22.0);
        data.put("ph", 6.8);
        data.put("ec", 1.2);
        data.put("nitrogen", 80.0);
        data.put("phosphorus", 40.0);
        data.put("potassium", 60.0);
        data.put("collectTime", java.time.LocalDateTime.now().toString());
        data.put("healthStatus", "good");
        return data;
    }
    
    /**
     * 获取土壤概况
     */
    @GetMapping("/overview")
    public Result<List<Map<String, Object>>> getOverview() {
        try {
            return Result.success(soilService.getSoilOverview());
        } catch (Exception e) {
            log.error("获取土壤概况失败", e);
            return Result.fail("获取土壤概况失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取数据趋势
     */
    @GetMapping("/trend/{sensorId}")
    public Result<Map<String, List<Double>>> getTrend(
            @PathVariable Long sensorId,
            @RequestParam(defaultValue = "7") int days) {
        try {
            if (sensorId == null || sensorId <= 0) {
                return Result.fail("传感器ID必须大于0");
            }
            if (days <= 0 || days > 365) {
                return Result.fail("天数必须在 1-365 之间");
            }
            return Result.success(soilService.analyzeTrend(sensorId, days));
        } catch (Exception e) {
            log.error("获取趋势数据失败, sensorId: {}, days: {}", sensorId, days, e);
            return Result.fail("获取趋势数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取土壤统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        try {
            return Result.success(soilService.getStatistics());
        } catch (Exception e) {
            log.error("获取土壤统计失败", e);
            return Result.fail("获取土壤统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取预警信息
     */
    @GetMapping("/alerts")
    public Result<List<Map<String, Object>>> getAlerts() {
        try {
            return Result.success(soilService.getAlerts());
        } catch (Exception e) {
            log.error("获取预警信息失败", e);
            return Result.fail("获取预警信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取灌溉建议
     */
    @GetMapping("/recommendations")
    public Result<List<Map<String, Object>>> getRecommendations() {
        try {
            return Result.success(soilService.getIrrigationRecommendations());
        } catch (Exception e) {
            log.error("获取灌溉建议失败", e);
            return Result.fail("获取灌溉建议失败: " + e.getMessage());
        }
    }
}
