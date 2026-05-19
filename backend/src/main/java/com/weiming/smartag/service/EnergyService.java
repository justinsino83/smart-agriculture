package com.weiming.smartag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weiming.smartag.entity.EnergyRecord;

import java.util.List;
import java.util.Map;

/**
 * 能耗服务接口
 */
public interface EnergyService extends IService<EnergyRecord> {
    
    /**
     * 分页查询能耗记录
     */
    Map<String, Object> listByPage(int page, int size);
    
    /**
     * 获取今日能耗统计
     */
    Map<String, Object> getTodayStats();
    
    /**
     * 获取设备能耗占比
     */
    List<Map<String, Object>> getDeviceUsage();
}