package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 气象数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@CrossOrigin
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 获取当前天气
     */
    @GetMapping("/current")
    public Result<Map<String, Object>> getCurrentWeather(
            @RequestParam(required = false) String clientId) {
        try {
            return Result.success(weatherService.getCurrentWeather(clientId));
        } catch (Exception e) {
            log.error("获取当前天气失败, clientId: {}", clientId, e);
            return Result.fail("获取天气失败: " + e.getMessage());
        }
    }

    /**
     * 获取24小时趋势
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> get24HourTrend(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            return Result.success(weatherService.get24HourTrend(clientId, startTime, endTime));
        } catch (Exception e) {
            log.error("获取24小时趋势失败, clientId: {}, startTime: {}, endTime: {}", clientId, startTime, endTime, e);
            return Result.fail("获取趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取24小时湿度趋势
     */
    @GetMapping("/humidity-trend")
    public Result<Map<String, Object>> get24HourHumidityTrend(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            return Result.success(weatherService.get24HourHumidityTrend(clientId, startTime, endTime));
        } catch (Exception e) {
            log.error("获取24小时湿度趋势失败, clientId: {}, startTime: {}, endTime: {}", clientId, startTime, endTime, e);
            return Result.fail("获取趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取24小时风向统计
     */
    @GetMapping("/wind-direction-trend")
    public Result<Map<String, Object>> get24HourWindDirectionTrend(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            return Result.success(weatherService.get24HourWindDirectionTrend(clientId, startTime, endTime));
        } catch (Exception e) {
            log.error("获取24小时风向趋势失败, clientId: {}, startTime: {}, endTime: {}", clientId, startTime, endTime, e);
            return Result.fail("获取趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取天气预报
     */
    @GetMapping("/forecast")
    public Result<?> getForecast() {
        try {
            return Result.success(weatherService.getForecast());
        } catch (Exception e) {
            log.error("获取天气预报失败", e);
            return Result.fail("获取预报失败: " + e.getMessage());
        }
    }

    /**
     * 获取全部天气数据
     */
    @GetMapping("/all")
    public Result<Map<String, Object>> getAll(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            Map<String, Object> all = Map.of(
                    "current", weatherService.getCurrentWeather(clientId),
                    "trend", weatherService.get24HourTrend(clientId, startTime, endTime),
                    "humidityTrend", weatherService.get24HourHumidityTrend(clientId, startTime, endTime),
                    "windDirectionTrend", weatherService.get24HourWindDirectionTrend(clientId, startTime, endTime),
                    "forecast", weatherService.getForecast()
            );
            return Result.success(all);
        } catch (Exception e) {
            log.error("获取全部天气数据失败, clientId: {}, startTime: {}, endTime: {}", clientId, startTime, endTime, e);
            return Result.fail("获取天气数据失败: " + e.getMessage());
        }
    }
}
