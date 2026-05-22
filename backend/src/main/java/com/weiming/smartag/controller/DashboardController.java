package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.service.DevicePushService;
import com.weiming.smartag.service.DryingService;
import com.weiming.smartag.service.IrrigationService;
import com.weiming.smartag.service.SoilService;
import com.weiming.smartag.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 智慧大屏数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin
public class DashboardController {
    
    private final SoilService soilService;
    private final IrrigationService irrigationService;
    private final DryingService dryingService;
    private final StorageService storageService;
    private final DevicePushService devicePushService;
    
    /**
     * 获取大屏综合统计数据
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> data = new HashMap<>();
            
            // 土壤监测统计
            Map<String, Object> soilStats = soilService.getStatistics();
            data.put("soil", soilStats);
            
            // 灌溉统计
            Map<String, Object> irrigationStats = irrigationService.getStatistics("day");
            data.put("irrigation", irrigationStats);
            
            // 烘干统计
            Map<String, Object> dryingStats = dryingService.getStatistics();
            data.put("drying", dryingStats);
            
            // 仓储统计
            Map<String, Object> storageStats = storageService.getStatistics();
            data.put("storage", storageStats);
            
            // 综合预警
            List<Map<String, Object>> alerts = new ArrayList<>();
            
            // 土壤预警
            List<Map<String, Object>> soilAlerts = soilService.getAlerts();
            for (Map<String, Object> alert : soilAlerts) {
                alert.put("category", "soil");
                alerts.add(alert);
            }
            
            // 仓储预警
            long storageWarnings = ((Number) storageStats.getOrDefault("warningCount", 0)).longValue();
            if (storageWarnings > 0) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("category", "storage");
                alert.put("type", "longStorage");
                alert.put("level", "warning");
                alert.put("message", storageWarnings + "批次粮食存储超过90天");
                alerts.add(alert);
            }
            
            data.put("alerts", alerts);
            data.put("alertCount", alerts.size());
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取大屏综合数据失败", e);
            return Result.fail("获取数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取大屏统计数据
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> data = new HashMap<>();
        
        // 地块统计
        data.put("fieldCount", 12);
        data.put("cropArea", 856.5);
        
        // 环境监测
        data.put("soilSensors", 24);
        data.put("onlineRate", 95.8);
        data.put("avgMoisture", 45.2);
        data.put("avgTemperature", 22.5);
        
        // 设备状态
        data.put("irrigationDevices", 8);
        data.put("runningDevices", 2);
        data.put("dryingDevices", 3);
        data.put("alertCount", 2);
        
        // 仓储统计
        data.put("storageTotal", 2560);
        data.put("grainTypes", 4);
        
        return Result.success(data);
    }
    
    /**
     * 获取实时环境数据（从设备推送数据获取）
     */
    @GetMapping("/realtime")
    public Result<Map<String, Object>> getRealtimeData() {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // 获取仪表盘综合数据
            Map<String, Object> overview = devicePushService.getDashboardOverview();
            
            // 提取环境数据
            Map<String, Object> environment = (Map<String, Object>) overview.get("environment");
            if (environment != null) {
                data.putAll(environment);
            }
            
            // 补充统计数据
            data.put("updateTime", new Date());
            
            return Result.success(data);
        } catch (Exception e) {
            // 降级到模拟数据
            data.put("temperature", 24.5 + Math.random());
            data.put("humidity", 65.0 + Math.random() * 5);
            data.put("windSpeed", 2.5 + Math.random());
            data.put("pressure", 1013.0);
            data.put("updateTime", new Date());
            
            return Result.success(data);
        }
    }
    
    /**
     * 获取综合仪表盘数据（集成设备推送数据）
     */
    @GetMapping("/integrated")
    public Result<Map<String, Object>> getIntegratedData() {
        Map<String, Object> result = new HashMap<>();
        
        // 从设备推送服务获取数据
        try {
            Map<String, Object> deviceData = devicePushService.getDashboardOverview();
            result.put("deviceData", deviceData);
        } catch (Exception e) {
            result.put("deviceData", Collections.emptyMap());
        }
        
        // 从土壤服务获取数据
        try {
            result.put("soilData", soilService.getStatistics());
        } catch (Exception e) {
            result.put("soilData", Collections.emptyMap());
        }
        
        // 从其他服务获取数据
        try {
            result.put("irrigationData", irrigationService.getStatistics("day"));
        } catch (Exception e) {
            result.put("irrigationData", Collections.emptyMap());
        }
        
        try {
            result.put("dryingData", dryingService.getStatistics());
        } catch (Exception e) {
            result.put("dryingData", Collections.emptyMap());
        }
        
        try {
            result.put("storageData", storageService.getStatistics());
        } catch (Exception e) {
            result.put("storageData", Collections.emptyMap());
        }
        
        return Result.success(result);
    }
    
    /**
     * 获取地块分布数据（用于地图）
     */
    @GetMapping("/fields")
    public Result<List<Map<String, Object>>> getFieldDistribution() {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        String[] crops = {"水稻", "小麦", "玉米", "大豆"};
        String[] stages = {"抽穗期", "灌浆期", "成熟期", "播种期"};
        
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> field = new HashMap<>();
            field.put("id", i);
            field.put("name", i + "号田");
            field.put("crop", crops[i % 4]);
            field.put("stage", stages[i % 4]);
            field.put("area", 100 + i * 20);
            field.put("moisture", 40 + i * 2);
            field.put("lat", 32.0 + i * 0.01);
            field.put("lng", 118.5 + i * 0.01);
            fields.add(field);
        }
        
        return Result.success(fields);
    }
    
    /**
     * 获取能耗统计
     */
    @GetMapping("/energy")
    public Result<Map<String, Object>> getEnergyStats() {
        Map<String, Object> data = new HashMap<>();
        
        data.put("todayPower", 856.5);
        data.put("todayCost", 513.9);
        data.put("savingRate", 30.0);
        data.put("carbonSaved", 2450.0);
        
        // 用电趋势
        List<Double> trend = Arrays.asList(25.0, 22.0, 20.0, 18.0, 20.0, 28.0, 35.0, 42.0, 48.0, 52.0, 55.0, 58.0);
        data.put("trend", trend);
        
        return Result.success(data);
    }
    
    /**
     * 获取设备运行状态
     */
    @GetMapping("/devices/status")
    public Result<Map<String, Object>> getDeviceStatus() {
        Map<String, Object> data = new HashMap<>();
        
        // 灌溉设备状态
        data.put("irrigation", irrigationService.getStatistics("day"));
        
        // 烘干设备状态
        data.put("drying", dryingService.getStatistics());
        
        return Result.success(data);
    }
    
    /**
     * 获取最新动态
     */
    @GetMapping("/activities")
    public Result<List<Map<String, Object>>> getRecentActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        // 模拟一些活动记录
        String[] types = {"irrigation", "drying", "storage"};
        String[] titles = {"灌溉任务完成", "烘干批次开始", "粮食入库登记"};
        String[] descs = {"1号田灌溉完成，用水量2.5吨", "批次DH2026032101开始烘干", "水稻入库120吨"};
        
        for (int i = 0; i < 5; i++) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", types[i % 3]);
            activity.put("title", titles[i % 3]);
            activity.put("description", descs[i % 3]);
            activity.put("time", new Date(System.currentTimeMillis() - i * 3600000));
            activities.add(activity);
        }
        
        return Result.success(activities);
    }
}
