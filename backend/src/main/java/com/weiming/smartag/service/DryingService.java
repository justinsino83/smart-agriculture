package com.weiming.smartag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weiming.smartag.entity.DryingBatch;

import java.util.List;
import java.util.Map;

/**
 * 烘干服务接口
 */
public interface DryingService extends IService<DryingBatch> {
    
    /**
     * 开始烘干
     */
    boolean startBatch(Long batchId);
    
    /**
     * 停止烘干
     */
    boolean stopBatch(Long batchId);
    
    /**
     * 暂停烘干
     */
    boolean pauseBatch(Long batchId);
    
    /**
     * 恢复烘干
     */
    boolean resumeBatch(Long batchId);
    
    /**
     * 获取工艺曲线数据
     */
    Map<String, Object> getCurveData(Long batchId);
    
    /**
     * 获取统计信息
     */
    Map<String, Object> getStatistics();
    
    /**
     * 获取最近批次
     */
    List<Map<String, Object>> getRecentBatches(int limit);
}
