package com.weiming.smartag.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 气象服务接口
 */
public interface WeatherService {

    /**
     * 获取当前天气
     */
    Map<String, Object> getCurrentWeather(String deviceCode);

    /**
     * 获取24小时趋势
     */
    Map<String, Object> get24HourTrend(String deviceCode, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取24小时湿度趋势
     */
    Map<String, Object> get24HourHumidityTrend(String deviceCode, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取24小时风向统计
     */
    Map<String, Object> get24HourWindDirectionTrend(String deviceCode, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取天气预报
     */
    List<Map<String, Object>> getForecast();
}