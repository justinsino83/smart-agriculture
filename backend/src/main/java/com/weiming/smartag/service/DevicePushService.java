package com.weiming.smartag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weiming.smartag.entity.DevicePushData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 设备推送数据服务接口
 */
public interface DevicePushService extends IService<DevicePushData> {
    
    /**
     * 保存设备推送数据
     */
    boolean savePushData(DevicePushData data);
    
    /**
     * 获取最新一条数据
     */
    DevicePushData getLatestData(String clientId);
    
    /**
     * 获取历史数据（分页）
     */
    Map<String, Object> getHistoryData(String clientId, LocalDateTime startTime, 
                                      LocalDateTime endTime, int page, int size);
    
    /**
     * 获取设备数据统计
     */
    Map<String, Object> getStatistics(String clientId);
    
    /**
     * 获取数据趋势（用于图表展示）
     */
    Map<String, List<Object>> getTrendData(String clientId, int hours);
    
    /**
     * 获取所有在线设备列表
     */
    List<Map<String, Object>> getActiveDevices();
    
    /**
     * 获取仪表盘综合数据（对接DevicePushData）
     */
    Map<String, Object> getDashboardOverview();
    
    /**
     * 历史数据去重
     */
    Map<String, Object> deduplicateHistoryData();
}
