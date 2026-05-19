package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.IrrigationDevice;
import com.weiming.smartag.entity.IrrigationTask;
import com.weiming.smartag.service.IrrigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 灌溉控制控制器
 */
@RestController
@RequestMapping("/api/irrigation")
@RequiredArgsConstructor
@CrossOrigin
public class IrrigationController {
    
    private final IrrigationService irrigationService;
    
    /**
     * 获取所有灌溉设备
     */
    @GetMapping("/devices")
    public Result<List<IrrigationDevice>> listDevices() {
        return Result.success(irrigationService.listDevices());
    }
    
    /**
     * 获取设备详情
     */
    @GetMapping("/device/{deviceId}")
    public Result<Map<String, Object>> getDeviceDetail(@PathVariable Long deviceId) {
        return Result.success(irrigationService.getDeviceDetail(deviceId));
    }
    
    /**
     * 控制设备开关
     */
    @PostMapping("/device/{deviceId}/control")
    public Result<?> controlDevice(
            @PathVariable Long deviceId,
            @RequestParam Boolean on) {
        boolean success = irrigationService.controlDevice(deviceId, on);
        return success ? Result.success() : Result.error("操作失败");
    }
    
    /**
     * 获取灌溉任务列表
     */
    @GetMapping("/tasks")
    public Result<List<IrrigationTask>> listTasks() {
        return Result.success(irrigationService.listTasks());
    }
    
    /**
     * 创建灌溉任务
     */
    @PostMapping("/task")
    public Result<?> createTask(@RequestBody IrrigationTask task) {
        boolean success = irrigationService.createTask(task);
        return success ? Result.success() : Result.error("创建失败");
    }
    
    /**
     * 执行任务
     */
    @PostMapping("/task/{taskId}/execute")
    public Result<?> executeTask(@PathVariable Long taskId) {
        boolean success = irrigationService.executeTask(taskId);
        return success ? Result.success() : Result.error("执行失败");
    }
    
    /**
     * 完成任务
     */
    @PostMapping("/task/{taskId}/complete")
    public Result<?> completeTask(
            @PathVariable Long taskId,
            @RequestParam(required = false) Double actualWater) {
        boolean success = irrigationService.completeTask(taskId, actualWater);
        return success ? Result.success() : Result.error("完成失败");
    }
    
    /**
     * 取消灌溉任务
     */
    @PostMapping("/task/{taskId}/cancel")
    public Result<?> cancelTask(@PathVariable Long taskId) {
        boolean success = irrigationService.cancelTask(taskId);
        return success ? Result.success() : Result.error("取消失败");
    }
    
    /**
     * 获取用水统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(defaultValue = "day") String period) {
        return Result.success(irrigationService.getStatistics(period));
    }
    
    /**
     * 获取用水趋势
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getWaterUsageTrend(
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(irrigationService.getWaterUsageTrend(days));
    }
}
