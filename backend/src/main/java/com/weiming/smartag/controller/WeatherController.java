package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 气象数据控制器
 */
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
            @RequestParam(required = false) String deviceCode) {
        return Result.success(weatherService.getCurrentWeather(deviceCode));
    }

    /**
     * 获取24小时趋势
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> get24HourTrend(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(weatherService.get24HourTrend(deviceCode, startTime, endTime));
    }

    /**
     * 获取24小时湿度趋势
     */
    @GetMapping("/humidity-trend")
    public Result<Map<String, Object>> get24HourHumidityTrend(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(weatherService.get24HourHumidityTrend(deviceCode, startTime, endTime));
    }

    /**
     * 获取24小时风向统计
     */
    @GetMapping("/wind-direction-trend")
    public Result<Map<String, Object>> get24HourWindDirectionTrend(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(weatherService.get24HourWindDirectionTrend(deviceCode, startTime, endTime));
    }

    /**
     * 获取天气预报
     */
    @GetMapping("/forecast")
    public Result<?> getForecast() {
        return Result.success(weatherService.getForecast());
    }

    /**
     * 获取全部天气数据
     */
    @GetMapping("/all")
    public Result<Map<String, Object>> getAll(
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Map<String, Object> all = Map.of(
                "current", weatherService.getCurrentWeather(deviceCode),
                "trend", weatherService.get24HourTrend(deviceCode, startTime, endTime),
                "humidityTrend", weatherService.get24HourHumidityTrend(deviceCode, startTime, endTime),
                "windDirectionTrend", weatherService.get24HourWindDirectionTrend(deviceCode, startTime, endTime),
                "forecast", weatherService.getForecast()
        );
        return Result.success(all);
    }
}