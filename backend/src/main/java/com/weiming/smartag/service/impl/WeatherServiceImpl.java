package com.weiming.smartag.service.impl;

import com.weiming.smartag.entity.WeatherData;
import com.weiming.smartag.entity.WeatherForecast;
import com.weiming.smartag.mapper.WeatherDataMapper;
import com.weiming.smartag.mapper.WeatherForecastMapper;
import com.weiming.smartag.mapper.SoilSensorMapper;
import com.weiming.smartag.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 气象服务实现
 */
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final WeatherDataMapper weatherDataMapper;
    private final WeatherForecastMapper weatherForecastMapper;
    private final SoilSensorMapper soilSensorMapper;

    private Long getSoilIdByDeviceCode(String deviceCode) {
        if (deviceCode == null || deviceCode.isEmpty()) {
            return null;
        }
        // deviceCode格式: SS20240001 -> id为1
        // 根据deviceCode查找对应的soil_sensor记录
        var sensor = soilSensorMapper.selectByCode(deviceCode);
        return sensor != null ? sensor.getId() : null;
    }

    @Override
    public Map<String, Object> getCurrentWeather(String deviceCode) {
        Long soilId = getSoilIdByDeviceCode(deviceCode);
        WeatherData latest = weatherDataMapper.selectLatest(soilId);
        Map<String, Object> result = new HashMap<>();

        if (latest != null) {
            result.put("temperature", latest.getTemperature());
            result.put("humidity", latest.getHumidity());
            result.put("windSpeed", latest.getWindSpeed());
            result.put("windDirection", latest.getWindDirection());
            result.put("pressure", latest.getPressure());
            result.put("weatherCode", latest.getWeatherCode());
            result.put("weatherText", getWeatherText(latest.getWeatherCode()));
            result.put("deviceCode", deviceCode != null ? deviceCode : "default");
            result.put("collectTime", latest.getCollectTime());
        } else {
            result.put("temperature", 24.0);
            result.put("humidity", 65);
            result.put("windSpeed", 2.5);
            result.put("windDirection", 180);
            result.put("pressure", 1013);
            result.put("weatherCode", "sunny");
            result.put("weatherText", "晴朗");
            result.put("deviceCode", deviceCode != null ? deviceCode : "default");
        }

        return result;
    }

    @Override
    public Map<String, Object> get24HourTrend(String deviceCode, LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(24);

        Long soilId = getSoilIdByDeviceCode(deviceCode);
        List<WeatherData> dataList = weatherDataMapper.select24HourTrend(startTime, endTime, soilId);
        Map<String, Object> result = new HashMap<>();

        if (dataList == null || dataList.isEmpty()) {
            List<Double> defaultTemps = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                defaultTemps.add(18.0 + i * 0.3);
            }
            List<String> defaultLabels = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                defaultLabels.add(i + ":00");
            }
            result.put("temperatures", defaultTemps);
            result.put("labels", defaultLabels);
        } else {
            List<Double> temperatures = dataList.stream()
                    .map(WeatherData::getTemperature)
                    .collect(Collectors.toList());
            List<String> labels = dataList.stream()
                    .map(d -> d.getCollectTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .collect(Collectors.toList());
            result.put("temperatures", temperatures);
            result.put("labels", labels);
        }

        result.put("deviceCode", deviceCode != null ? deviceCode : "default");
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        return result;
    }

    @Override
    public Map<String, Object> get24HourHumidityTrend(String deviceCode, LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(24);

        Long soilId = getSoilIdByDeviceCode(deviceCode);
        List<WeatherData> dataList = weatherDataMapper.select24HourTrend(startTime, endTime, soilId);
        Map<String, Object> result = new HashMap<>();

        if (dataList == null || dataList.isEmpty()) {
            List<Double> defaultHumidity = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                defaultHumidity.add(60.0 + i * 0.5);
            }
            List<String> defaultLabels = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                defaultLabels.add(i + ":00");
            }
            result.put("humidity", defaultHumidity);
            result.put("labels", defaultLabels);
        } else {
            List<Double> humidity = dataList.stream()
                    .map(WeatherData::getHumidity)
                    .collect(Collectors.toList());
            List<String> labels = dataList.stream()
                    .map(d -> d.getCollectTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .collect(Collectors.toList());
            result.put("humidity", humidity);
            result.put("labels", labels);
        }

        result.put("deviceCode", deviceCode != null ? deviceCode : "default");
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        return result;
    }

    @Override
    public Map<String, Object> get24HourWindDirectionTrend(String deviceCode, LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(24);

        Long soilId = getSoilIdByDeviceCode(deviceCode);
        List<WeatherData> dataList = weatherDataMapper.select24HourTrend(startTime, endTime, soilId);
        Map<String, Object> result = new HashMap<>();

        if (dataList == null || dataList.isEmpty()) {
            List<Integer> defaultWindScale = new ArrayList<>(Arrays.asList(3, 5, 2, 4, 3, 2, 4, 3));
            List<String> defaultDirections = new ArrayList<>(Arrays.asList("北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"));
            result.put("windScale", defaultWindScale);
            result.put("directions", defaultDirections);
            result.put("hourlyWindDirection", new ArrayList<>());
        } else {
            // 按风向分组统计
            Map<Integer, Long> windDirCount = dataList.stream()
                    .filter(d -> d.getWindDirection() != null)
                    .collect(Collectors.groupingBy(
                            d -> (int) (d.getWindDirection() / 45) * 45,
                            Collectors.counting()
                    ));

            String[] directionNames = {"北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"};
            List<String> directions = new ArrayList<>();
            List<Integer> windScale = new ArrayList<>();

            for (int i = 0; i < 8; i++) {
                int dir = i * 45;
                directions.add(directionNames[i]);
                windScale.add(windDirCount.getOrDefault(dir, 0L).intValue());
            }

            // 每小时风向数据
            List<Map<String, Object>> hourlyWindDirection = dataList.stream()
                    .map(d -> {
                        Map<String, Object> hourData = new HashMap<>();
                        hourData.put("time", d.getCollectTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                        hourData.put("direction", d.getWindDirection());
                        hourData.put("directionName", getDirectionName(d.getWindDirection()));
                        hourData.put("windSpeed", d.getWindSpeed());
                        return hourData;
                    })
                    .collect(Collectors.toList());

            result.put("directions", directions);
            result.put("windScale", windScale);
            result.put("hourlyWindDirection", hourlyWindDirection);
        }

        result.put("deviceCode", deviceCode != null ? deviceCode : "default");
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        return result;
    }

    private String getDirectionName(Double windDirection) {
        if (windDirection == null) return "未知";
        int dir = windDirection.intValue();
        int index = (dir + 22) / 45 % 8;
        String[] names = {"北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"};
        return names[index];
    }

    @Override
    public List<Map<String, Object>> getForecast() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(3);

        List<WeatherForecast> forecasts = weatherForecastMapper.selectForecastList(today, endDate);
        List<Map<String, Object>> result = new ArrayList<>();

        String[] dayNames = {"今天", "明天", "后天"};

        if (forecasts == null || forecasts.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                Map<String, Object> day = new HashMap<>();
                day.put("date", dayNames[i]);
                day.put("weatherCode", i == 0 ? "sunny" : (i == 1 ? "cloudy" : "rainy"));
                day.put("weatherText", i == 0 ? "晴" : (i == 1 ? "多云" : "雨"));
                day.put("tempHigh", i == 0 ? 26 : (i == 1 ? 24 : 22));
                day.put("tempLow", i == 0 ? 18 : (i == 1 ? 17 : 16));
                day.put("icon", i == 0 ? "Sunny" : (i == 1 ? "Cloudy" : "Pouring"));
                result.add(day);
            }
        } else {
            for (int i = 0; i < forecasts.size() && i < 3; i++) {
                WeatherForecast f = forecasts.get(i);
                Map<String, Object> day = new HashMap<>();
                day.put("date", dayNames[i]);
                day.put("weatherCode", f.getWeatherCode());
                day.put("weatherText", getWeatherText(f.getWeatherCode()));
                day.put("tempHigh", f.getTempHigh());
                day.put("tempLow", f.getTempLow());
                day.put("icon", getIconName(f.getWeatherCode()));
                result.add(day);
            }
        }

        return result;
    }

    private String getWeatherText(String weatherCode) {
        if (weatherCode == null) return "未知";
        return switch (weatherCode) {
            case "sunny" -> "晴朗";
            case "cloudy" -> "多云";
            case "rainy" -> "小雨";
            case "stormy" -> "暴风雨";
            case "foggy" -> "雾";
            default -> "晴朗";
        };
    }

    private String getIconName(String weatherCode) {
        if (weatherCode == null) return "Sunny";
        return switch (weatherCode) {
            case "sunny" -> "Sunny";
            case "cloudy" -> "Cloudy";
            case "rainy", "stormy" -> "Pouring";
            case "foggy" -> "Cloudy";
            default -> "Sunny";
        };
    }
}