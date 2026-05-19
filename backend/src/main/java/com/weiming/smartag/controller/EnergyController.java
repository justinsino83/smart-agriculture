package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.service.EnergyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 能耗管理控制器
 */
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
        return Result.success(energyService.listByPage(page, size));
    }
    
    /**
     * 获取今日能耗统计
     */
    @GetMapping("/today")
    public Result<Map<String, Object>> getTodayStats() {
        return Result.success(energyService.getTodayStats());
    }
    
    /**
     * 获取设备能耗占比
     */
    @GetMapping("/device-usage")
    public Result<List<Map<String, Object>>> getDeviceUsage() {
        return Result.success(energyService.getDeviceUsage());
    }
}