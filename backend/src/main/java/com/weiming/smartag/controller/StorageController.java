package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.StorageRecord;
import com.weiming.smartag.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 仓储管理控制器
 */
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
        return Result.success(storageService.list());
    }
    
    /**
     * 获取在库列表
     */
    @GetMapping("/stock")
    public Result<List<StorageRecord>> getStockList() {
        return Result.success(storageService.getStockList());
    }
    
    /**
     * 入库登记
     */
    @PostMapping("/in")
    public Result<?> storageIn(@RequestBody StorageRecord record) {
        boolean success = storageService.storageIn(record);
        return success ? Result.success() : Result.error("入库失败");
    }
    
    /**
     * 出库登记
     */
    @PostMapping("/out/{batchNo}")
    public Result<?> storageOut(@PathVariable String batchNo) {
        boolean success = storageService.storageOut(batchNo);
        return success ? Result.success() : Result.error("出库失败");
    }
    
    /**
     * 获取库存统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(storageService.getStatistics());
    }
    
    /**
     * 获取出入库趋势
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getEntryExitTrend(
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(storageService.getEntryExitTrend(days));
    }
}
