package com.weiming.smartag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weiming.smartag.entity.SoilSensor;
import com.weiming.smartag.entity.SoilData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 土壤监测服务接口
 */
public interface SoilService extends IService<SoilSensor> {
    
    /**
     * 获取传感器实时数据
     */
    SoilData getRealTimeData(Long sensorId);
    
    /**
     * 获取传感器历史数据
     */
    List<SoilData> getHistoryData(Long sensorId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 获取所有地块的土壤概况
     */
    List<Map<String, Object>> getSoilOverview();
    
    /**
     * 分析土壤数据趋势
     */
    Map<String, List<Double>> analyzeTrend(Long sensorId, int days);
    
    /**
     * 获取土壤监测统计
     */
    Map<String, Object> getStatistics();
    
    /**
     * 获取土壤异常预警
     */
    List<Map<String, Object>> getAlerts();
    
    /**
     * 评估土壤健康状态
     */
    Map<String, Object> evaluateHealth(SoilData data);
    
    /**
     * 获取灌溉建议
     */
    List<Map<String, Object>> getIrrigationRecommendations();
}
