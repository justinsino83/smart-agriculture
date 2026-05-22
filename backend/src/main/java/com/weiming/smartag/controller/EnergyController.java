package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.service.EnergyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 能耗管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/energy")
@RequiredArgsConstructor
@CrossOrigin
public class EnergyController {
    
    private final EnergyService energyService;
    
    /**
     * 分页获取能耗列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            if (page < 1) {
                page = 1;
            }
            if (size < 1 || size > 100) {
                size = 10;
            }
            return Result.success(energyService.listByPage(page, size));
        } catch (Exception e) {
            log.error("获取能耗列表失败, page: {}, size: {}", page, size, e);
            return Result.fail("获取能耗列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取今日能耗统计
     */
    @GetMapping("/today")
    public Result<Map<String, Object>> getTodayStats() {
        try {
            return Result.success(energyService.getTodayStats());
        } catch (Exception e) {
            log.error("获取今日能耗统计失败", e);
            return Result.fail("获取今日能耗统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取设备能耗占比
     */
    @GetMapping("/device-usage")
    public Result<List<Map<String, Object>>> getDeviceUsage() {
        try {
            return Result.success(energyService.getDeviceUsage());
        } catch (Exception e) {
            log.error("获取设备能耗占比失败", e);
            return Result.fail("获取设备能耗占比失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取能耗趋势数据
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrend(@RequestParam(defaultValue = "day") String period) {
        try {
            if (period == null) {
                period = "day";
            }
            return Result.success(energyService.getTrendData(period));
        } catch (Exception e) {
            log.error("获取能耗趋势数据失败, period: {}", period, e);
            return Result.fail("获取能耗趋势数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取能耗统计概览
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        try {
            return Result.success(energyService.getStatistics());
        } catch (Exception e) {
            log.error("获取能耗统计概览失败", e);
            return Result.fail("获取能耗统计概览失败: " + e.getMessage());
        }
    }
}