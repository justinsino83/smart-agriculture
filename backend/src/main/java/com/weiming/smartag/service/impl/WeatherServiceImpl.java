package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weiming.smartag.entity.DevicePushData;
import com.weiming.smartag.entity.WeatherForecast;
import com.weiming.smartag.mapper.DevicePushDataMapper;
import com.weiming.smartag.mapper.WeatherForecastMapper;
import com.weiming.smartag.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 气象服务实现
 * 数据来源统一切换到 device_push_data。
 */
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final DevicePushDataMapper devicePushDataMapper;
    private final WeatherForecastMapper weatherForecastMapper;

    @Override
    public Map<String, Object> getCurrentWeather(String deviceCode) {
        String clientId = resolveClientId(deviceCode);
        DevicePushData latest = getLatestWeatherRecord(clientId);
        Map<String, Object> result = new HashMap<>();
        result.put("deviceCode", clientId != null ? clientId : deviceCode);

        if (latest == null) {
            result.put("temperature", null);
            result.put("humidity", null);
            result.put("windSpeed", null);
            result.put("windDirection", null);
            result.put("pressure", null);
            result.put("rainfall", null);
            result.put("lightIntensity", null);
            result.put("dewTemp", null);
            result.put("co2", null);
            result.put("weatherText", null);
            result.put("collectTime", null);
            return result;
        }

        result.put("temperature", toDouble(latest.getAmbientTemperature()));
        result.put("humidity", toDouble(latest.getAmbientHumidity()));
        result.put("windSpeed", toDouble(latest.getWindSpeed()));
        result.put("windDirection", latest.getWindDirection());
        result.put("pressure", toDouble(latest.getPressure()));
        result.put("rainfall", toDouble(latest.getRainfall()));
        result.put("lightIntensity", toDouble(latest.getLightIntensity()));
        result.put("dewTemp", toDouble(latest.getDewTemp()));
        result.put("co2", toDouble(latest.getCo2()));
        result.put("weatherText", null);
        result.put("collectTime", getRecordTime(latest));
        return result;
    }

    @Override
    public Map<String, Object> get24HourTrend(String deviceCode, LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        LocalDateTime start = startTime != null ? startTime : end.minusHours(24);
        String clientId = resolveClientId(deviceCode);
        List<DevicePushData> records = listWeatherRecords(clientId, start, end);

        Map<String, Object> result = new HashMap<>();
        result.put("temperatures", records.stream()
                .map(DevicePushData::getAmbientTemperature)
                .map(this::toDouble)
                .collect(Collectors.toList()));
        result.put("labels", records.stream()
                .map(this::getRecordTime)
                .filter(Objects::nonNull)
                .map(time -> time.format(DateTimeFormatter.ofPattern("HH:mm")))
                .collect(Collectors.toList()));
        result.put("deviceCode", clientId != null ? clientId : deviceCode);
        result.put("startTime", start);
        result.put("endTime", end);
        return result;
    }

    @Override
    public Map<String, Object> get24HourHumidityTrend(String deviceCode, LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        LocalDateTime start = startTime != null ? startTime : end.minusHours(24);
        String clientId = resolveClientId(deviceCode);
        List<DevicePushData> records = listWeatherRecords(clientId, start, end);

        Map<String, Object> result = new HashMap<>();
        result.put("humidity", records.stream()
                .map(DevicePushData::getAmbientHumidity)
                .map(this::toDouble)
                .collect(Collectors.toList()));
        result.put("labels", records.stream()
                .map(this::getRecordTime)
                .filter(Objects::nonNull)
                .map(time -> time.format(DateTimeFormatter.ofPattern("HH:mm")))
                .collect(Collectors.toList()));
        result.put("deviceCode", clientId != null ? clientId : deviceCode);
        result.put("startTime", start);
        result.put("endTime", end);
        return result;
    }

    @Override
    public Map<String, Object> get24HourWindDirectionTrend(String deviceCode, LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        LocalDateTime start = startTime != null ? startTime : end.minusHours(24);
        String clientId = resolveClientId(deviceCode);
        List<DevicePushData> records = listWeatherRecords(clientId, start, end);

        Map<Integer, Long> windDirCount = records.stream()
                .filter(d -> d.getWindDirection() != null)
                .collect(Collectors.groupingBy(
                        d -> ((d.getWindDirection() + 22) / 45) * 45 % 360,
                        Collectors.counting()
                ));

        String[] directionNames = {"北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"};
        List<String> directions = new ArrayList<>();
        List<Integer> windScale = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int degree = i * 45;
            directions.add(directionNames[i]);
            windScale.add(windDirCount.getOrDefault(degree, 0L).intValue());
        }

        List<Map<String, Object>> hourlyWindDirection = records.stream()
                .map(d -> {
                    Map<String, Object> item = new HashMap<>();
                    LocalDateTime recordTime = getRecordTime(d);
                    item.put("time", recordTime != null ? recordTime.format(DateTimeFormatter.ofPattern("HH:mm")) : null);
                    item.put("direction", d.getWindDirection());
                    item.put("directionName", getDirectionName(d.getWindDirection()));
                    item.put("windSpeed", toDouble(d.getWindSpeed()));
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("directions", directions);
        result.put("windScale", windScale);
        result.put("hourlyWindDirection", hourlyWindDirection);
        result.put("deviceCode", clientId != null ? clientId : deviceCode);
        result.put("startTime", start);
        result.put("endTime", end);
        return result;
    }

    @Override
    public List<Map<String, Object>> getForecast() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(3);
        List<WeatherForecast> forecasts = weatherForecastMapper.selectForecastList(today, endDate);
        List<Map<String, Object>> result = new ArrayList<>();
        String[] dayNames = {"今天", "明天", "后天"};

        if (forecasts == null || forecasts.isEmpty()) {
            return result;
        }

        for (int i = 0; i < forecasts.size() && i < 3; i++) {
            WeatherForecast forecast = forecasts.get(i);
            Map<String, Object> day = new HashMap<>();
            day.put("date", dayNames[i]);
            day.put("weatherCode", forecast.getWeatherCode());
            day.put("weatherText", getWeatherText(forecast.getWeatherCode()));
            day.put("tempHigh", forecast.getTempHigh());
            day.put("tempLow", forecast.getTempLow());
            day.put("icon", getIconName(forecast.getWeatherCode()));
            result.add(day);
        }
        return result;
    }

    private String resolveClientId(String deviceCode) {
        List<String> clientIds = listWeatherClientIds();
        if (clientIds.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(deviceCode) && clientIds.contains(deviceCode)) {
            return deviceCode;
        }
        return clientIds.get(0);
    }

    private List<String> listWeatherClientIds() {
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNotNull(DevicePushData::getAmbientTemperature)
                        .or().isNotNull(DevicePushData::getAmbientHumidity)
                        .or().isNotNull(DevicePushData::getPressure)
                        .or().isNotNull(DevicePushData::getWindSpeed)
                        .or().isNotNull(DevicePushData::getWindDirection)
                        .or().isNotNull(DevicePushData::getRainfall)
                        .or().isNotNull(DevicePushData::getLightIntensity)
                        .or().isNotNull(DevicePushData::getDewTemp)
                        .or().isNotNull(DevicePushData::getCo2))
                .select(DevicePushData::getClientId)
                .groupBy(DevicePushData::getClientId);
        return devicePushDataMapper.selectList(wrapper).stream()
                .map(DevicePushData::getClientId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private DevicePushData getLatestWeatherRecord(String clientId) {
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(clientId)) {
            wrapper.eq(DevicePushData::getClientId, clientId);
        }
        wrapper.and(w -> w.isNotNull(DevicePushData::getAmbientTemperature)
                        .or().isNotNull(DevicePushData::getAmbientHumidity)
                        .or().isNotNull(DevicePushData::getPressure)
                        .or().isNotNull(DevicePushData::getWindSpeed)
                        .or().isNotNull(DevicePushData::getWindDirection)
                        .or().isNotNull(DevicePushData::getRainfall)
                        .or().isNotNull(DevicePushData::getLightIntensity)
                        .or().isNotNull(DevicePushData::getDewTemp)
                        .or().isNotNull(DevicePushData::getCo2))
                .orderByDesc(DevicePushData::getDetectedTime)
                .orderByDesc(DevicePushData::getCreateTime)
                .last("LIMIT 1");
        return devicePushDataMapper.selectOne(wrapper);
    }

    private List<DevicePushData> listWeatherRecords(String clientId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(clientId)) {
            wrapper.eq(DevicePushData::getClientId, clientId);
        }
        wrapper.and(w -> w.isNotNull(DevicePushData::getAmbientTemperature)
                        .or().isNotNull(DevicePushData::getAmbientHumidity)
                        .or().isNotNull(DevicePushData::getPressure)
                        .or().isNotNull(DevicePushData::getWindSpeed)
                        .or().isNotNull(DevicePushData::getWindDirection)
                        .or().isNotNull(DevicePushData::getRainfall)
                        .or().isNotNull(DevicePushData::getLightIntensity)
                        .or().isNotNull(DevicePushData::getDewTemp)
                        .or().isNotNull(DevicePushData::getCo2));
        if (start != null) {
            wrapper.ge(DevicePushData::getCreateTime, start);
        }
        if (end != null) {
            wrapper.le(DevicePushData::getCreateTime, end);
        }
        wrapper.orderByAsc(DevicePushData::getCreateTime);
        return devicePushDataMapper.selectList(wrapper);
    }

    private LocalDateTime getRecordTime(DevicePushData data) {
        if (data == null) {
            return null;
        }
        return data.getDetectedTime() != null ? data.getDetectedTime() : data.getCreateTime();
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String getDirectionName(Integer windDirection) {
        if (windDirection == null) return "未知";
        int index = ((windDirection + 22) / 45) % 8;
        String[] names = {"北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"};
        return names[index];
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
