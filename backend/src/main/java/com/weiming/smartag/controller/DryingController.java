package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.DryingBatch;
import com.weiming.smartag.service.DryingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 烘干管理控制器
 */
@Slf4j
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
        try {
            return Result.success(dryingService.list());
        } catch (Exception e) {
            log.error("获取烘干批次列表失败", e);
            return Result.fail("获取批次列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最近批次
     */
    @GetMapping("/batches/recent")
    public Result<List<Map<String, Object>>> getRecentBatches(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (limit < 1 || limit > 100) {
                limit = 10;
            }
            return Result.success(dryingService.getRecentBatches(limit));
        } catch (Exception e) {
            log.error("获取最近烘干批次失败, limit: {}", limit, e);
            return Result.fail("获取批次失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取批次详情
     */
    @GetMapping("/batch/{batchId}")
    public Result<DryingBatch> getBatch(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            return Result.success(dryingService.getById(batchId));
        } catch (Exception e) {
            log.error("获取烘干批次详情失败, batchId: {}", batchId, e);
            return Result.fail("获取批次详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建烘干批次
     */
    @PostMapping("/batch")
    public Result<?> createBatch(@RequestBody DryingBatch batch) {
        try {
            if (batch == null) {
                return Result.fail("批次数据不能为空");
            }
            boolean success = dryingService.save(batch);
            return success ? Result.success("创建成功") : Result.error("创建失败");
        } catch (Exception e) {
            log.error("创建烘干批次失败, batch: {}", batch, e);
            return Result.fail("创建批次失败: " + e.getMessage());
        }
    }
    
    /**
     * 开始烘干
     */
    @PostMapping("/batch/{batchId}/start")
    public Result<?> startBatch(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            boolean success = dryingService.startBatch(batchId);
            return success ? Result.success("启动成功") : Result.error("启动失败");
        } catch (Exception e) {
            log.error("启动烘干批次失败, batchId: {}", batchId, e);
            return Result.fail("启动批次失败: " + e.getMessage());
        }
    }
    
    /**
     * 暂停烘干
     */
    @PostMapping("/batch/{batchId}/pause")
    public Result<?> pauseBatch(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            boolean success = dryingService.pauseBatch(batchId);
            return success ? Result.success("暂停成功") : Result.error("暂停失败");
        } catch (Exception e) {
            log.error("暂停烘干批次失败, batchId: {}", batchId, e);
            return Result.fail("暂停批次失败: " + e.getMessage());
        }
    }
    
    /**
     * 恢复烘干
     */
    @PostMapping("/batch/{batchId}/resume")
    public Result<?> resumeBatch(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            boolean success = dryingService.resumeBatch(batchId);
            return success ? Result.success("恢复成功") : Result.error("恢复失败");
        } catch (Exception e) {
            log.error("恢复烘干批次失败, batchId: {}", batchId, e);
            return Result.fail("恢复批次失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止烘干
     */
    @PostMapping("/batch/{batchId}/stop")
    public Result<?> stopBatch(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            boolean success = dryingService.stopBatch(batchId);
            return success ? Result.success("停止成功") : Result.error("停止失败");
        } catch (Exception e) {
            log.error("停止烘干批次失败, batchId: {}", batchId, e);
            return Result.fail("停止批次失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取工艺曲线数据
     */
    @GetMapping("/batch/{batchId}/curve")
    public Result<Map<String, Object>> getCurveData(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            return Result.success(dryingService.getCurveData(batchId));
        } catch (Exception e) {
            log.error("获取工艺曲线数据失败, batchId: {}", batchId, e);
            return Result.fail("获取曲线数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        try {
            return Result.success(dryingService.getStatistics());
        } catch (Exception e) {
            log.error("获取烘干统计失败", e);
            return Result.fail("获取统计失败: " + e.getMessage());
        }
    }
}
