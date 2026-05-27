package com.weiming.smartag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weiming.smartag.entity.IrrigationDevice;
import com.weiming.smartag.entity.IrrigationTask;

import java.util.List;
import java.util.Map;

/**
 * 灌溉服务接口
 */
public interface IrrigationService extends IService<IrrigationDevice> {
    
    /**
     * 获取所有灌溉设备
     */
    List<IrrigationDevice> listDevices();
    
    /**
     * 分页获取灌溉设备
     */
    Map<String, Object> listDevicesPage(int page, int size);
    
    /**
     * 控制设备开关
     */
    boolean controlDevice(Long deviceId, Boolean on);
    
    /**
     * 获取灌溉任务列表
     */
    List<IrrigationTask> listTasks();
    
    /**
     * 分页获取灌溉任务
     */
    Map<String, Object> listTasksPage(int page, int size, Integer status);
    
    /**
     * 创建灌溉任务
     */
    boolean createTask(IrrigationTask task);
    
    /**
     * 取消灌溉任务
     */
    boolean cancelTask(Long taskId);
    
    /**
     * 执行任务
     */
    boolean executeTask(Long taskId);
    
    /**
     * 完成任务
     */
    boolean completeTask(Long taskId, Double actualWater);
    
    /**
     * 获取用水统计
     */
    Map<String, Object> getStatistics(String period);
    
    /**
     * 获取设备详情
     */
    Map<String, Object> getDeviceDetail(Long deviceId);
    
    /**
     * 获取用水趋势
     */
    List<Map<String, Object>> getWaterUsageTrend(int days);
}
