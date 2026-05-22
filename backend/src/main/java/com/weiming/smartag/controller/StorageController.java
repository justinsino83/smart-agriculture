package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.StorageRecord;
import com.weiming.smartag.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 仓储管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@CrossOrigin
public class StorageController {
    
    private final StorageService storageService;
    
    /**
     * 获取所有记录
     */
    @GetMapping("/records")
    public Result<List<StorageRecord>> listRecords() {
        try {
            return Result.success(storageService.list());
        } catch (Exception e) {
            log.error("获取仓储记录失败", e);
            return Result.fail("获取仓储记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取在库列表
     */
    @GetMapping("/stock")
    public Result<List<StorageRecord>> getStockList() {
        try {
            return Result.success(storageService.getStockList());
        } catch (Exception e) {
            log.error("获取在库列表失败", e);
            return Result.fail("获取在库列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 入库登记
     */
    @PostMapping("/in")
    public Result<?> storageIn(@RequestBody StorageRecord record) {
        try {
            if (record == null) {
                return Result.fail("入库数据不能为空");
            }
            if (!StringUtils.hasText(record.getBatchNo())) {
                return Result.fail("批次号不能为空");
            }
            boolean success = storageService.storageIn(record);
            return success ? Result.success("入库成功") : Result.error("入库失败");
        } catch (Exception e) {
            log.error("入库失败, record: {}", record, e);
            return Result.fail("入库失败: " + e.getMessage());
        }
    }
    
    /**
     * 出库登记
     */
    @PostMapping("/out/{batchNo}")
    public Result<?> storageOut(@PathVariable String batchNo) {
        try {
            if (!StringUtils.hasText(batchNo)) {
                return Result.fail("批次号不能为空");
            }
            boolean success = storageService.storageOut(batchNo);
            return success ? Result.success("出库成功") : Result.error("出库失败，批次号不存在");
        } catch (Exception e) {
            log.error("出库失败, batchNo: {}", batchNo, e);
            return Result.fail("出库失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取库存统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        try {
            return Result.success(storageService.getStatistics());
        } catch (Exception e) {
            log.error("获取库存统计失败", e);
            return Result.fail("获取库存统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取出入库趋势
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getEntryExitTrend(
            @RequestParam(defaultValue = "7") int days) {
        try {
            if (days <= 0 || days > 365) {
                days = 7;
            }
            return Result.success(storageService.getEntryExitTrend(days));
        } catch (Exception e) {
            log.error("获取出入库趋势失败, days: {}", days, e);
            return Result.fail("获取出入库趋势失败: " + e.getMessage());
        }
    }
}
