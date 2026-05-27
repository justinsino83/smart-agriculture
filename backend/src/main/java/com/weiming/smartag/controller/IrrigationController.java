package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.IrrigationDevice;
import com.weiming.smartag.entity.IrrigationTask;
import com.weiming.smartag.service.IrrigationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 灌溉控制控制器
 */
@Slf4j
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
        try {
            return Result.success(irrigationService.listDevices());
        } catch (Exception e) {
            log.error("获取灌溉设备列表失败", e);
            return Result.fail("获取设备列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页获取灌溉设备
     */
    @GetMapping("/devices/page")
    public Result<Map<String, Object>> listDevicesPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return Result.success(irrigationService.listDevicesPage(page, size));
        } catch (Exception e) {
            log.error("获取灌溉设备分页列表失败", e);
            return Result.fail("获取设备列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取设备详情
     */
    @GetMapping("/device/{deviceId}")
    public Result<Map<String, Object>> getDeviceDetail(@PathVariable Long deviceId) {
        try {
            if (deviceId == null || deviceId <= 0) {
                return Result.fail("设备ID必须大于0");
            }
            return Result.success(irrigationService.getDeviceDetail(deviceId));
        } catch (Exception e) {
            log.error("获取设备详情失败, deviceId: {}", deviceId, e);
            return Result.fail("获取设备详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 控制设备开关
     */
    @PostMapping("/device/{deviceId}/control")
    public Result<?> controlDevice(
            @PathVariable Long deviceId,
            @RequestParam Boolean on) {
        try {
            if (deviceId == null || deviceId <= 0) {
                return Result.fail("设备ID必须大于0");
            }
            boolean success = irrigationService.controlDevice(deviceId, on);
            return success ? Result.success("操作成功") : Result.error("操作失败");
        } catch (Exception e) {
            log.error("控制设备失败, deviceId: {}, on: {}", deviceId, on, e);
            return Result.fail("操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取灌溉任务列表
     */
    @GetMapping("/tasks")
    public Result<List<IrrigationTask>> listTasks() {
        try {
            return Result.success(irrigationService.listTasks());
        } catch (Exception e) {
            log.error("获取灌溉任务列表失败", e);
            return Result.fail("获取任务列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页获取灌溉任务列表
     */
    @GetMapping("/tasks/page")
    public Result<Map<String, Object>> listTasksPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        try {
            return Result.success(irrigationService.listTasksPage(page, size, status));
        } catch (Exception e) {
            log.error("获取灌溉任务分页列表失败", e);
            return Result.fail("获取任务列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建灌溉任务
     */
    @PostMapping("/task")
    public Result<?> createTask(@RequestBody IrrigationTask task) {
        try {
            if (task == null) {
                return Result.fail("任务数据不能为空");
            }
            boolean success = irrigationService.createTask(task);
            return success ? Result.success("创建成功") : Result.error("创建失败");
        } catch (Exception e) {
            log.error("创建灌溉任务失败, task: {}", task, e);
            return Result.fail("创建任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行任务
     */
    @PostMapping("/task/{taskId}/execute")
    public Result<?> executeTask(@PathVariable Long taskId) {
        try {
            if (taskId == null || taskId <= 0) {
                return Result.fail("任务ID必须大于0");
            }
            boolean success = irrigationService.executeTask(taskId);
            return success ? Result.success("执行成功") : Result.error("执行失败");
        } catch (Exception e) {
            log.error("执行灌溉任务失败, taskId: {}", taskId, e);
            return Result.fail("执行任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 完成任务
     */
    @PostMapping("/task/{taskId}/complete")
    public Result<?> completeTask(
            @PathVariable Long taskId,
            @RequestParam(required = false) Double actualWater) {
        try {
            if (taskId == null || taskId <= 0) {
                return Result.fail("任务ID必须大于0");
            }
            boolean success = irrigationService.completeTask(taskId, actualWater);
            return success ? Result.success("完成成功") : Result.error("完成失败");
        } catch (Exception e) {
            log.error("完成灌溉任务失败, taskId: {}, actualWater: {}", taskId, actualWater, e);
            return Result.fail("完成任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消灌溉任务
     */
    @PostMapping("/task/{taskId}/cancel")
    public Result<?> cancelTask(@PathVariable Long taskId) {
        try {
            if (taskId == null || taskId <= 0) {
                return Result.fail("任务ID必须大于0");
            }
            boolean success = irrigationService.cancelTask(taskId);
            return success ? Result.success("取消成功") : Result.error("取消失败");
        } catch (Exception e) {
            log.error("取消灌溉任务失败, taskId: {}", taskId, e);
            return Result.fail("取消任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用水统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(defaultValue = "day") String period) {
        try {
            return Result.success(irrigationService.getStatistics(period));
        } catch (Exception e) {
            log.error("获取用水统计失败, period: {}", period, e);
            return Result.fail("获取统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用水趋势
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getWaterUsageTrend(
            @RequestParam(defaultValue = "7") int days) {
        try {
            if (days < 1 || days > 365) {
                days = 7;
            }
            return Result.success(irrigationService.getWaterUsageTrend(days));
        } catch (Exception e) {
            log.error("获取用水趋势失败, days: {}", days, e);
            return Result.fail("获取趋势失败: " + e.getMessage());
        }
    }
}
