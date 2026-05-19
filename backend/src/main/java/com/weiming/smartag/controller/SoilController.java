package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.service.SoilService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 土壤监测控制器
 */
@RestController
@RequestMapping("/api/soil")
@RequiredArgsConstructor
@CrossOrigin
public class SoilController {
    
    private final SoilService soilService;
    
    /**
     * 获取所有传感器列表
     */
    @GetMapping("/sensors")
    public Result<List<?>> listSensors() {
        return Result.success(soilService.list());
    }
    
    /**
     * 获取传感器实时数据
     */
    @GetMapping("/realtime/{sensorId}")
    public Result<?> getRealTimeData(@PathVariable Long sensorId) {
        return Result.success(soilService.getRealTimeData(sensorId));
    }
    
    /**
     * 获取传感器历史数据
     */
    @GetMapping("/history/{sensorId}")
    public Result<List<?>> getHistoryData(
            @PathVariable Long sensorId,
            @RequestParam String start,
            @RequestParam String end) {
        LocalDateTime startTime = LocalDateTime.parse(start.replace("Z", "").substring(0, 19));
        LocalDateTime endTime = LocalDateTime.parse(end.replace("Z", "").substring(0, 19));
        return Result.success(soilService.getHistoryData(sensorId, startTime, endTime));
    }
    
    /**
     * 获取土壤概况
     */
    @GetMapping("/overview")
    public Result<List<Map<String, Object>>> getOverview() {
        return Result.success(soilService.getSoilOverview());
    }
    
    /**
     * 获取数据趋势
     */
    @GetMapping("/trend/{sensorId}")
    public Result<Map<String, List<Double>>> getTrend(
            @PathVariable Long sensorId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(soilService.analyzeTrend(sensorId, days));
    }
    
    /**
     * 获取土壤统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(soilService.getStatistics());
    }
    
    /**
     * 获取预警信息
     */
    @GetMapping("/alerts")
    public Result<List<Map<String, Object>>> getAlerts() {
        return Result.success(soilService.getAlerts());
    }
    
    /**
     * 获取灌溉建议
     */
    @GetMapping("/recommendations")
    public Result<List<Map<String, Object>>> getRecommendations() {
        return Result.success(soilService.getIrrigationRecommendations());
    }
}
