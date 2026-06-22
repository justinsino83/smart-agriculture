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
    @GetMapping("/realtime/{clientId}")
    public Result<?> getRealTimeData(@PathVariable String clientId) {
        try {
            if (!StringUtils.hasText(clientId)) {
                return Result.fail("设备号不能为空");
            }
            Object data = soilService.getRealTimeData(clientId);
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取实时数据失败, clientId: {}", clientId, e);
            return Result.fail("获取实时数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取传感器历史数据
     */
    @GetMapping("/history/{clientId}")
    public Result<List<?>> getHistoryData(
            @PathVariable String clientId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        try {
            if (!StringUtils.hasText(clientId)) {
                return Result.fail("设备号不能为空");
            }
            
            LocalDateTime startTime = null;
            LocalDateTime endTime = null;
            
            if (StringUtils.hasText(start)) {
                try {
                    startTime = LocalDateTime.parse(start.replace("Z", "").substring(0, 19));
                } catch (DateTimeParseException e) {
                    return Result.fail("开始时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
                }
            }
            if (StringUtils.hasText(end)) {
                try {
                    endTime = LocalDateTime.parse(end.replace("Z", "").substring(0, 19));
                } catch (DateTimeParseException e) {
                    return Result.fail("结束时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
                }
            }
            
            if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
                return Result.fail("开始时间不能大于结束时间");
            }
            
            return Result.success(soilService.getHistoryData(clientId, startTime, endTime));
        } catch (Exception e) {
            log.error("获取历史数据失败, clientId: {}, start: {}, end: {}", clientId, start, end, e);
            return Result.fail("获取历史数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取传感器历史数据（分页）
     */
    @GetMapping("/history/{clientId}/page")
    public Result<Map<String, Object>> getHistoryDataPage(
            @PathVariable String clientId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            if (!StringUtils.hasText(clientId)) {
                return Result.fail("设备号不能为空");
            }
            if (page <= 0) {
                return Result.fail("页码必须大于0");
            }
            if (size <= 0 || size > 100) {
                return Result.fail("每页数量必须在1-100之间");
            }
            
            LocalDateTime startTime = null;
            LocalDateTime endTime = null;
            
            if (StringUtils.hasText(start)) {
                try {
                    startTime = LocalDateTime.parse(start.replace("Z", "").substring(0, 19));
                } catch (DateTimeParseException e) {
                    return Result.fail("开始时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
                }
            }
            if (StringUtils.hasText(end)) {
                try {
                    endTime = LocalDateTime.parse(end.replace("Z", "").substring(0, 19));
                } catch (DateTimeParseException e) {
                    return Result.fail("结束时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
                }
            }
            
            if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
                return Result.fail("开始时间不能大于结束时间");
            }
            
            return Result.success(soilService.getHistoryDataPage(clientId, startTime, endTime, page, size));
        } catch (Exception e) {
            log.error("获取历史数据失败, clientId: {}, start: {}, end: {}, page: {}, size: {}",
                    clientId, start, end, page, size, e);
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
    @GetMapping("/trend/{clientId}")
    public Result<Map<String, Object>> getTrend(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "7") int days) {
        try {
            if (!StringUtils.hasText(clientId)) {
                return Result.fail("设备号不能为空");
            }
            if (days <= 0 || days > 365) {
                return Result.fail("天数必须在 1-365 之间");
            }
            return Result.success(soilService.analyzeTrend(clientId, days));
        } catch (Exception e) {
            log.error("获取趋势数据失败, clientId: {}, days: {}", clientId, days, e);
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
