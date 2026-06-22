package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weiming.smartag.entity.DevicePushData;
import com.weiming.smartag.entity.SoilData;
import com.weiming.smartag.entity.SoilSensor;
import com.weiming.smartag.mapper.DevicePushDataMapper;
import com.weiming.smartag.mapper.SoilSensorMapper;
import com.weiming.smartag.service.SoilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 土壤监测服务实现
 * 数据来源统一切换到 device_push_data。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SoilServiceImpl extends ServiceImpl<SoilSensorMapper, SoilSensor> implements SoilService {

    private final DevicePushDataMapper devicePushDataMapper;

    @Override
    public SoilData getRealTimeData(String clientId) {
        DevicePushData latest = getLatestSoilRecord(clientId);
        if (latest == null) {
            return createEmptySoilData();
        }
        return convertPushToSoilData(latest);
    }

    @Override
    public List<Map<String, Object>> getHistoryData(String clientId, LocalDateTime start, LocalDateTime end) {
        return listSoilRecords(clientId, start, end).stream()
                .map(this::convertPushToMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getHistoryDataPage(String clientId, LocalDateTime start, LocalDateTime end, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;

        LambdaQueryWrapper<DevicePushData> countWrapper = buildSoilRecordQuery(clientId, start, end);
        Long total = devicePushDataMapper.selectCount(countWrapper);

        LambdaQueryWrapper<DevicePushData> pageWrapper = buildSoilRecordQuery(clientId, start, end);
        pageWrapper.orderByDesc(DevicePushData::getDetectedTime)
                .orderByDesc(DevicePushData::getCreateTime)
                .last("LIMIT " + offset + "," + safeSize);

        List<Map<String, Object>> pageData = devicePushDataMapper.selectList(pageWrapper).stream()
                .map(this::convertPushToMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageData);
        result.put("total", total != null ? total : 0);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @Override
    public List<Map<String, Object>> getSoilOverview() {
        List<String> clientIds = listSoilClientIds();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < clientIds.size(); i++) {
            String clientId = clientIds.get(i);
            DevicePushData latest = getLatestSoilRecord(clientId);
            if (latest == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("sensorId", clientId);
            item.put("deviceName", clientId);
            item.put("location", clientId);
            item.put("moisture", null);
            item.put("temperature", null);
            item.put("ph", toDouble(latest.getSoilPh()));
            item.put("ec", null);
            item.put("collectTime", getRecordTime(latest));
            item.put("healthStatus", evaluateHealth(convertPushToSoilData(latest)));
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> analyzeTrend(String clientId, int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(Math.max(days, 1));
        List<DevicePushData> records = listSoilRecords(clientId, start, end);

        Map<String, List<DevicePushData>> grouped = new LinkedHashMap<>();
        if (days <= 1) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:00");
            for (DevicePushData record : records) {
                LocalDateTime time = getRecordTime(record);
                if (time != null) {
                    String key = formatter.format(time.withMinute(0).withSecond(0).withNano(0));
                    grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
                }
            }
        } else if (days <= 7) {
            for (DevicePushData record : records) {
                LocalDateTime time = getRecordTime(record);
                if (time != null) {
                    String key = time.toLocalDate().toString();
                    grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
                }
            }
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM月");
            for (DevicePushData record : records) {
                LocalDateTime time = getRecordTime(record);
                if (time != null) {
                    String key = formatter.format(time);
                    grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
                }
            }
        }

        List<String> labels = new ArrayList<>(grouped.keySet());
        List<Object> phList = new ArrayList<>();
        List<Object> emptyList1 = new ArrayList<>();
        List<Object> emptyList2 = new ArrayList<>();
        List<Object> emptyList3 = new ArrayList<>();
        List<Object> emptyList4 = new ArrayList<>();
        List<Object> emptyList5 = new ArrayList<>();

        for (String label : labels) {
            List<DevicePushData> bucket = grouped.get(label);
            phList.add(average(bucket.stream().map(DevicePushData::getSoilPh).collect(Collectors.toList())));
            emptyList1.add(null);
            emptyList2.add(null);
            emptyList3.add(null);
            emptyList4.add(null);
            emptyList5.add(null);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("moisture", emptyList1);
        result.put("temperature", emptyList2);
        result.put("ph", phList);
        result.put("ec", emptyList3);
        result.put("nitrogen", emptyList4);
        result.put("npk", emptyList5);
        result.put("_timeLabels", labels);
        return result;
    }

    @Override
    public Map<String, Object> getStatistics() {
        List<String> clientIds = listSoilClientIds();
        List<DevicePushData> latestList = clientIds.stream()
                .map(this::getLatestSoilRecord)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSensors", clientIds.size());
        stats.put("onlineSensors", latestList.size());
        stats.put("onlineRate", clientIds.isEmpty() ? 0 : Math.round(latestList.size() * 100.0 / clientIds.size()));
        stats.put("avgMoisture", null);
        stats.put("avgTemperature", null);
        stats.put("avgPh", average(latestList.stream().map(DevicePushData::getSoilPh).collect(Collectors.toList())));
        stats.put("alertCount", getAlerts().size());
        return stats;
    }

    @Override
    public List<Map<String, Object>> getAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        List<String> clientIds = listSoilClientIds();
        for (int i = 0; i < clientIds.size(); i++) {
            String clientId = clientIds.get(i);
            DevicePushData latest = getLatestSoilRecord(clientId);
            if (latest == null || latest.getSoilPh() == null) {
                continue;
            }
            double ph = latest.getSoilPh().doubleValue();
            if (ph < 5.5 || ph > 8.5) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("sensorId", clientId);
                alert.put("sensorName", clientId);
                alert.put("type", "ph");
                alert.put("level", "warning");
                alert.put("message", ph < 5.5 ? "土壤偏酸，建议调节" : "土壤偏碱，建议调节");
                alert.put("value", ph);
                alert.put("time", getRecordTime(latest));
                alerts.add(alert);
            }
        }
        return alerts;
    }

    @Override
    public Map<String, Object> evaluateHealth(SoilData data) {
        Map<String, Object> health = new HashMap<>();
        List<String> suggestions = new ArrayList<>();
        health.put("moistureStatus", null);
        health.put("temperatureStatus", null);
        health.put("ecStatus", null);
        health.put("nitrogenStatus", null);
        health.put("fertilityStatus", null);

        if (data == null || data.getPh() == null) {
            health.put("phStatus", null);
            health.put("score", null);
            health.put("level", "unknown");
            health.put("suggestions", suggestions);
            return health;
        }

        double ph = data.getPh();
        if (ph >= 6.0 && ph <= 7.5) {
            health.put("phStatus", "optimal");
            health.put("score", 95);
            health.put("level", "excellent");
        } else if (ph >= 5.5 && ph <= 8.0) {
            health.put("phStatus", "good");
            health.put("score", 80);
            health.put("level", "good");
        } else {
            health.put("phStatus", "poor");
            health.put("score", 60);
            health.put("level", "needs_attention");
            suggestions.add("当前仅有 soil_ph 有值，建议优先根据 pH 调整土壤酸碱度");
        }
        health.put("suggestions", suggestions);
        return health;
    }

    @Override
    public List<Map<String, Object>> getIrrigationRecommendations() {
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> listSensorsWithRealTimeData() {
        List<String> clientIds = listSoilClientIds();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < clientIds.size(); i++) {
            String clientId = clientIds.get(i);
            DevicePushData latest = getLatestSoilRecord(clientId);
            Map<String, Object> sensor = new HashMap<>();
            sensor.put("id", clientId);
            sensor.put("deviceCode", clientId);
            sensor.put("deviceName", "土壤监测设备");
            sensor.put("fieldId", null);
            sensor.put("status", latest != null ? 1 : 0);
            sensor.put("location", clientId);
            sensor.put("lastReportTime", getRecordTime(latest));
            sensor.put("createTime", getRecordTime(latest));

            if (latest != null) {
                Map<String, Object> realTimeMap = new HashMap<>();
                realTimeMap.put("moisture", null);
                realTimeMap.put("temperature", null);
                realTimeMap.put("ph", toDouble(latest.getSoilPh()));
                realTimeMap.put("ec", null);
                realTimeMap.put("nitrogen", null);
                realTimeMap.put("phosphorus", null);
                realTimeMap.put("potassium", null);
                realTimeMap.put("collectTime", getRecordTime(latest));
                sensor.put("realTimeData", realTimeMap);
            } else {
                sensor.put("realTimeData", null);
            }
            result.add(sensor);
        }
        return result;
    }

    private SoilData createEmptySoilData() {
        SoilData data = new SoilData();
        data.setSensorId(null);
        data.setCollectTime(null);
        return data;
    }

    private SoilData convertPushToSoilData(DevicePushData pushData) {
        SoilData data = new SoilData();
        data.setSensorId(null);
        data.setCollectTime(getRecordTime(pushData));
        data.setPh(toDouble(pushData.getSoilPh()));
        data.setMoisture(null);
        data.setTemperature(null);
        data.setEc(null);
        data.setNitrogen(null);
        data.setPhosphorus(null);
        data.setPotassium(null);
        return data;
    }

    private Map<String, Object> convertPushToMap(DevicePushData pushData) {
        SoilData soilData = convertPushToSoilData(pushData);
        Map<String, Object> map = new HashMap<>();
        map.put("id", pushData.getId());
        map.put("facilityId", pushData.getFacilityId());
        map.put("sensorId", null);
        map.put("moisture", null);
        map.put("temperature", null);
        map.put("ph", toDouble(pushData.getSoilPh()));
        map.put("ec", null);
        map.put("nitrogen", null);
        map.put("phosphorus", null);
        map.put("potassium", null);
        map.put("collectTime", getRecordTime(pushData));

        Map<String, Object> health = evaluateHealth(soilData);
        map.put("healthStatus", health.get("level"));
        map.put("moistureStatus", health.get("moistureStatus"));
        map.put("temperatureStatus", health.get("temperatureStatus"));
        map.put("phStatus", health.get("phStatus"));
        map.put("ecStatus", health.get("ecStatus"));
        map.put("nitrogenStatus", health.get("nitrogenStatus"));
        map.put("fertilityStatus", health.get("fertilityStatus"));
        map.put("healthScore", health.get("score"));
        map.put("healthLevel", health.get("level"));
        map.put("healthSuggestions", health.get("suggestions"));
        return map;
    }

    private List<String> listSoilClientIds() {
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(DevicePushData::getCreateTime, LocalDateTime.now().minusHours(24))
                .isNotNull(DevicePushData::getSoilPh)
                .select(DevicePushData::getClientId)
                .groupBy(DevicePushData::getClientId);
        return devicePushDataMapper.selectList(wrapper).stream()
                .map(DevicePushData::getClientId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private DevicePushData getLatestSoilRecord(String clientId) {
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        if (clientId != null) {
            wrapper.eq(DevicePushData::getClientId, clientId);
        }
        wrapper.isNotNull(DevicePushData::getSoilPh)
                .orderByDesc(DevicePushData::getDetectedTime)
                .orderByDesc(DevicePushData::getCreateTime)
                .last("LIMIT 1");
        return devicePushDataMapper.selectOne(wrapper);
    }

    private List<DevicePushData> listSoilRecords(String clientId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<DevicePushData> wrapper = buildSoilRecordQuery(clientId, start, end);
        wrapper.orderByDesc(DevicePushData::getDetectedTime)
                .orderByDesc(DevicePushData::getCreateTime);
        return devicePushDataMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<DevicePushData> buildSoilRecordQuery(String clientId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        if (clientId != null) {
            wrapper.eq(DevicePushData::getClientId, clientId);
        }
        wrapper.isNotNull(DevicePushData::getSoilPh);
        if (start != null) {
            wrapper.ge(DevicePushData::getCreateTime, start);
        }
        if (end != null) {
            wrapper.le(DevicePushData::getCreateTime, end);
        }
        return wrapper;
    }

    private LocalDateTime getRecordTime(DevicePushData data) {
        if (data == null) {
            return null;
        }
        return data.getDetectedTime() != null ? data.getDetectedTime() : data.getCreateTime();
    }

    private Double average(List<BigDecimal> values) {
        List<BigDecimal> validValues = values.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (validValues.isEmpty()) {
            return null;
        }
        BigDecimal sum = validValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(validValues.size()), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
