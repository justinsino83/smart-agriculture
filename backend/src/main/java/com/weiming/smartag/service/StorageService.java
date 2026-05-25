package com.weiming.smartag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weiming.smartag.entity.StorageRecord;

import java.util.List;
import java.util.Map;

/**
 * 仓储服务接口
 */
public interface StorageService extends IService<StorageRecord> {
    
    /**
     * 入库登记
     */
    boolean storageIn(StorageRecord record);
    
    /**
     * 出库登记
     */
    boolean storageOut(String batchNo);
    
    /**
     * 获取库存统计
     */
    Map<String, Object> getStatistics();
    
    /**
     * 获取在库列表
     */
    List<StorageRecord> getStockList();
    
    /**
     * 获取出入库趋势
     */
    List<Map<String, Object>> getEntryExitTrend(int days);
    
    /**
     * 获取仓储概览
     */
    Map<String, Object> getOverview();
    
    /**
     * 分页获取库存列表
     */
    Map<String, Object> getStockListPage(int page, int size);
}
