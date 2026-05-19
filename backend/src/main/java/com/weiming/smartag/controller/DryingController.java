package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.DryingBatch;
import com.weiming.smartag.service.DryingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 烘干管理控制器
 */
@RestController
@RequestMapping("/api/drying")
@RequiredArgsConstructor
@CrossOrigin
public class DryingController {
    
    private final DryingService dryingService;
    
    /**
     * 获取所有烘干批次
     */
    @GetMapping("/batches")
    public Result<List<DryingBatch>> listBatches() {
        return Result.success(dryingService.list());
    }
    
    /**
     * 获取最近批次
     */
    @GetMapping("/batches/recent")
    public Result<List<Map<String, Object>>> getRecentBatches(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(dryingService.getRecentBatches(limit));
    }
    
    /**
     * 获取批次详情
     */
    @GetMapping("/batch/{batchId}")
    public Result<DryingBatch> getBatch(@PathVariable Long batchId) {
        return Result.success(dryingService.getById(batchId));
    }
    
    /**
     * 创建烘干批次
     */
    @PostMapping("/batch")
    public Result<?> createBatch(@RequestBody DryingBatch batch) {
        boolean success = dryingService.save(batch);
        return success ? Result.success() : Result.error("创建失败");
    }
    
    /**
     * 开始烘干
     */
    @PostMapping("/batch/{batchId}/start")
    public Result<?> startBatch(@PathVariable Long batchId) {
        boolean success = dryingService.startBatch(batchId);
        return success ? Result.success() : Result.error("启动失败");
    }
    
    /**
     * 暂停烘干
     */
    @PostMapping("/batch/{batchId}/pause")
    public Result<?> pauseBatch(@PathVariable Long batchId) {
        boolean success = dryingService.pauseBatch(batchId);
        return success ? Result.success() : Result.error("暂停失败");
    }
    
    /**
     * 恢复烘干
     */
    @PostMapping("/batch/{batchId}/resume")
    public Result<?> resumeBatch(@PathVariable Long batchId) {
        boolean success = dryingService.resumeBatch(batchId);
        return success ? Result.success() : Result.error("恢复失败");
    }
    
    /**
     * 停止烘干
     */
    @PostMapping("/batch/{batchId}/stop")
    public Result<?> stopBatch(@PathVariable Long batchId) {
        boolean success = dryingService.stopBatch(batchId);
        return success ? Result.success() : Result.error("停止失败");
    }
    
    /**
     * 获取工艺曲线数据
     */
    @GetMapping("/batch/{batchId}/curve")
    public Result<Map<String, Object>> getCurveData(@PathVariable Long batchId) {
        return Result.success(dryingService.getCurveData(batchId));
    }
    
    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(dryingService.getStatistics());
    }
}
