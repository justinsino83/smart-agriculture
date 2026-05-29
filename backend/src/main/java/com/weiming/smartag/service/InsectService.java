package com.weiming.smartag.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.weiming.smartag.entity.InsectData;
import com.weiming.smartag.entity.InsectDevice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 虫情数据服务接口
 */
public interface InsectService {
    
    String getAccessToken();
    List<InsectDevice> getDeviceList();
    List<InsectData> getInsectImagesByTimeRange(String imei, LocalDateTime startTime, LocalDateTime endTime);
    List<Map<String, Object>> getInsectStatistic(String imei, String startDate, String endDate);
    void syncAllDevicesData();
    void syncDeviceData(String imei);
    IPage<InsectDevice> getLocalDevicePage(int page, int size);
    IPage<InsectData> getLocalDataPage(String imei, String startDate, String endDate, Boolean hasObjectCount, int page, int size);
}