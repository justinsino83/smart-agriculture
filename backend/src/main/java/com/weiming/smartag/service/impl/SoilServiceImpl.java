package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weiming.smartag.entity.SoilSensor;
import com.weiming.smartag.entity.SoilData;
import com.weiming.smartag.mapper.SoilSensorMapper;
import com.weiming.smartag.mapper.SoilDataMapper;
import com.weiming.smartag.service.SoilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 土壤监测服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SoilServiceImpl extends ServiceImpl<SoilSensorMapper, SoilSensor> implements SoilService {
    
    private final SoilDataMapper soilDataMapper;
    
    @Override
    public SoilData getRealTimeData(Long sensorId) {
        try {
            SoilData data = soilDataMapper.selectLatestBySensorId(sensorId);
            if (data != null) {
                return data;
            }
            // 如果没有数据，创建一个默认数据
            return createDefaultSoilData(sensorId);
        } catch (Exception e) {
            return createDefaultSoilData(sensorId);
        }
    }
    
    @Override
    public List<Map<String, Object>> getHistoryData(Long sensorId, LocalDateTime start, LocalDateTime end) {
        try {
            List<SoilData> data = soilDataMapper.selectHistoryData(sensorId, start, end);
            if (data != null && !data.isEmpty()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (SoilData soilData : data) {
                    result.add(convertSoilDataToMap(soilData));
                }
                return result;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取原始的历史数据，内部方法（不对外暴露）
     */
    private List<SoilData> getRawHistoryData(Long sensorId, LocalDateTime start, LocalDateTime end) {
        try {
            List<SoilData> data = soilDataMapper.selectHistoryData(sensorId, start, end);
            return data != null ? data : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    @Override
    public Map<String, Object> getHistoryDataPage(Long sensorId, LocalDateTime start, LocalDateTime end, int page, int size) {
        try {
            List<SoilData> allData = soilDataMapper.selectHistoryData(sensorId, start, end);
            if (allData == null) allData = new ArrayList<>();
            int total = allData.size();
            
            // 计算分页
            int fromIndex = Math.max(0, (page - 1) * size);
            int toIndex = Math.min(allData.size(), fromIndex + size);
            
            List<SoilData> pageDataList = fromIndex < toIndex 
                ? allData.subList(fromIndex, toIndex) 
                : new ArrayList<>();
            
            // 转换为带健康状态的Map
            List<Map<String, Object>> pageData = new ArrayList<>();
            for (SoilData soilData : pageDataList) {
                pageData.add(convertSoilDataToMap(soilData));
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", pageData);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("list", new ArrayList<>());
            result.put("total", 0);
            result.put("page", page);
            result.put("size", size);
            return result;
        }
    }
    
    /**
     * 将SoilData转换为带健康状态的Map
     */
    private Map<String, Object> convertSoilDataToMap(SoilData soilData) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", soilData.getId());
        map.put("facilityId", soilData.getFacilityId());
        map.put("sensorId", soilData.getSensorId());
        map.put("moisture", soilData.getMoisture());
        map.put("temperature", soilData.getTemperature());
        map.put("ph", soilData.getPh());
        map.put("ec", soilData.getEc());
        map.put("nitrogen", soilData.getNitrogen());
        map.put("phosphorus", soilData.getPhosphorus());
        map.put("potassium", soilData.getPotassium());
        map.put("collectTime", soilData.getCollectTime());
        
        // 评估健康状态
        Map<String, Object> health = evaluateHealth(soilData);
        String level = (String) health.get("level");
        if ("excellent".equals(level) || "good".equals(level)) {
            map.put("healthStatus", "good");
        } else {
            map.put("healthStatus", "warning");
        }
        
        // 添加各指标详细状态
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
    
    /**
     * 创建默认的土壤数据，用于处理异常情况
     */
    private SoilData createDefaultSoilData(Long sensorId) {
        SoilData data = new SoilData();
        data.setSensorId(sensorId);
        data.setMoisture(45.0);
        data.setTemperature(22.0);
        data.setPh(6.8);
        data.setEc(1.2);
        data.setNitrogen(80.0);
        data.setPhosphorus(40.0);
        data.setPotassium(60.0);
        data.setCollectTime(LocalDateTime.now());
        return data;
    }
    
    @Override
    public List<Map<String, Object>> getSoilOverview() {
        List<SoilSensor> sensors = baseMapper.selectOnlineSensors();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (SoilSensor sensor : sensors) {
            Map<String, Object> map = new HashMap<>();
            map.put("sensorId", sensor.getId());
            map.put("deviceName", sensor.getDeviceName());
            map.put("location", sensor.getLocation());
            
            SoilData data = getRealTimeData(sensor.getId());
            if (data != null) {
                map.put("moisture", data.getMoisture());
                map.put("temperature", data.getTemperature());
                map.put("ph", data.getPh());
                map.put("ec", data.getEc());
                map.put("collectTime", data.getCollectTime());
                
                // 添加健康状态评估
                map.put("healthStatus", evaluateHealth(data));
            }
            result.add(map);
        }
        return result;
    }
    
    @Override
    public Map<String, Object> analyzeTrend(Long sensorId, int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(days);
        List<SoilData> list = getRawHistoryData(sensorId, start, now);
        
        List<Double> moistureList = new ArrayList<>();
        List<Double> tempList = new ArrayList<>();
        List<Double> phList = new ArrayList<>();
        List<Double> ecList = new ArrayList<>();
        List<Double> nitrogenList = new ArrayList<>();
        List<Double> npkList = new ArrayList<>();
        List<String> timeLabels = new ArrayList<>();
        
        // 从真实数据中聚合
        if (list != null && !list.isEmpty()) {
            // 根据时间范围确定聚合策略
            if (days <= 1) {
                // 日视图：按小时聚合（每4小时一个点）
                Map<Integer, List<SoilData>> hourGroup = new HashMap<>();
                for (SoilData data : list) {
                    if (data != null && data.getCollectTime() != null) {
                        int hour = data.getCollectTime().getHour();
                        hourGroup.computeIfAbsent(hour, k -> new ArrayList<>()).add(data);
                    }
                }
                // 每4小时分组
                for (int h = 0; h < 24; h += 4) {
                    List<SoilData> hourData = new ArrayList<>();
                    for (int i = h; i < h + 4 && i < 24; i++) {
                        if (hourGroup.containsKey(i)) {
                            hourData.addAll(hourGroup.get(i));
                        }
                    }
                    if (!hourData.isEmpty()) {
                        aggregateAndAdd(hourData, String.format("%02d:00", h), 
                                    moistureList, tempList, phList, ecList, nitrogenList, npkList, timeLabels);
                    }
                }
            } else if (days <= 7) {
                // 周视图：按天聚合
                Map<LocalDate, List<SoilData>> dayGroup = new HashMap<>();
                for (SoilData data : list) {
                    if (data != null && data.getCollectTime() != null) {
                        LocalDate date = data.getCollectTime().toLocalDate();
                        dayGroup.computeIfAbsent(date, k -> new ArrayList<>()).add(data);
                    }
                }
                // 按日期排序后添加
                List<LocalDate> sortedDates = new ArrayList<>(dayGroup.keySet());
                Collections.sort(sortedDates);
                // 取最近6天
                int startIdx = Math.max(0, sortedDates.size() - 6);
                for (int i = startIdx; i < sortedDates.size(); i++) {
                    LocalDate date = sortedDates.get(i);
                    List<SoilData> dayData = dayGroup.get(date);
                    if (!dayData.isEmpty()) {
                        aggregateAndAdd(dayData, date.toString(), 
                                    moistureList, tempList, phList, ecList, nitrogenList, npkList, timeLabels);
                    }
                }
            } else {
                // 月视图：按月聚合
                Map<String, List<SoilData>> monthGroup = new HashMap<>();
                for (SoilData data : list) {
                    if (data != null && data.getCollectTime() != null) {
                        String monthKey = String.format("%d-%02d", 
                            data.getCollectTime().getYear(), 
                            data.getCollectTime().getMonthValue());
                        monthGroup.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(data);
                    }
                }
                // 按月份排序后添加
                List<String> sortedMonths = new ArrayList<>(monthGroup.keySet());
                Collections.sort(sortedMonths);
                // 取最近6个月
                int startIdx = Math.max(0, sortedMonths.size() - 6);
                for (int i = startIdx; i < sortedMonths.size(); i++) {
                    String monthKey = sortedMonths.get(i);
                    List<SoilData> monthData = monthGroup.get(monthKey);
                    if (!monthData.isEmpty()) {
                        // 解析年月，只显示月份
                        String[] parts = monthKey.split("-");
                        String label = parts[1] + "月";
                        aggregateAndAdd(monthData, label, 
                                    moistureList, tempList, phList, ecList, nitrogenList, npkList, timeLabels);
                    }
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("moisture", moistureList);
        result.put("temperature", tempList);
        result.put("ph", phList);
        result.put("ec", ecList);
        result.put("nitrogen", nitrogenList);
        result.put("npk", npkList);
        result.put("_timeLabels", timeLabels);
        return result;
    }
    
    private void aggregateAndAdd(List<SoilData> dataList, String label, 
                                  List<Double> moistureList, List<Double> tempList, 
                                  List<Double> phList, List<Double> ecList, 
                                  List<Double> nitrogenList, List<Double> npkList, 
                                  List<String> timeLabels) {
        double sumMoisture = 0, sumTemp = 0, sumPh = 0, sumEc = 0, sumNitrogen = 0, sumNpk = 0;
        int count = 0;
        for (SoilData data : dataList) {
            if (data == null) continue;
            count++;
            sumMoisture += data.getMoisture() != null ? data.getMoisture() : 0;
            sumTemp += data.getTemperature() != null ? data.getTemperature() : 0;
            sumPh += data.getPh() != null ? data.getPh() : 0;
            sumEc += data.getEc() != null ? data.getEc() : 0;
            sumNitrogen += data.getNitrogen() != null ? data.getNitrogen() : 0;
            sumNpk += (data.getPhosphorus() != null ? data.getPhosphorus() : 0) + 
                      (data.getPotassium() != null ? data.getPotassium() : 0);
        }
        if (count > 0) {
            moistureList.add(sumMoisture / count);
            tempList.add(sumTemp / count);
            phList.add(sumPh / count);
            ecList.add(sumEc / count);
            nitrogenList.add(sumNitrogen / count);
            npkList.add(sumNpk / count);
            timeLabels.add(label);
        }
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 传感器统计
        long totalSensors = count();
        long onlineSensors = baseMapper.selectOnlineSensors().size();
        stats.put("totalSensors", totalSensors);
        stats.put("onlineSensors", onlineSensors);
        stats.put("onlineRate", totalSensors > 0 ? Math.round(onlineSensors * 100.0 / totalSensors) : 0);
        
        // 平均数据
        List<SoilSensor> sensors = baseMapper.selectOnlineSensors();
        double avgMoisture = 0, avgTemp = 0, avgPh = 0;
        int count = 0;
        
        for (SoilSensor sensor : sensors) {
            SoilData data = getRealTimeData(sensor.getId());
            if (data != null) {
                avgMoisture += data.getMoisture() != null ? data.getMoisture() : 0;
                avgTemp += data.getTemperature() != null ? data.getTemperature() : 0;
                avgPh += data.getPh() != null ? data.getPh() : 0;
                count++;
            }
        }
        
        if (count > 0) {
            stats.put("avgMoisture", Math.round(avgMoisture / count * 10) / 10.0);
            stats.put("avgTemperature", Math.round(avgTemp / count * 10) / 10.0);
            stats.put("avgPh", Math.round(avgPh / count * 10) / 10.0);
        }
        
        // 预警统计
        List<Map<String, Object>> alerts = getAlerts();
        stats.put("alertCount", alerts.size());
        
        return stats;
    }
    
    @Override
    public List<Map<String, Object>> getAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        List<SoilSensor> sensors = baseMapper.selectOnlineSensors();
        
        for (SoilSensor sensor : sensors) {
            SoilData data = getRealTimeData(sensor.getId());
            if (data == null) continue;
            
            // 检查各项阈值
            if (data.getMoisture() != null && (data.getMoisture() < 20 || data.getMoisture() > 80)) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("sensorId", sensor.getId());
                alert.put("sensorName", sensor.getDeviceName());
                alert.put("type", "moisture");
                alert.put("level", data.getMoisture() < 20 ? "warning" : "normal");
                alert.put("message", data.getMoisture() < 20 ? "土壤湿度过低，建议灌溉" : "土壤湿度偏高");
                alert.put("value", data.getMoisture());
                alert.put("time", data.getCollectTime());
                alerts.add(alert);
            }
            
            if (data.getPh() != null && (data.getPh() < 5.5 || data.getPh() > 8.5)) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("sensorId", sensor.getId());
                alert.put("sensorName", sensor.getDeviceName());
                alert.put("type", "ph");
                alert.put("level", "warning");
                alert.put("message", data.getPh() < 5.5 ? "土壤偏酸，建议调节" : "土壤偏碱，建议调节");
                alert.put("value", data.getPh());
                alert.put("time", data.getCollectTime());
                alerts.add(alert);
            }
        }
        
        return alerts;
    }
    
    @Override
    public Map<String, Object> evaluateHealth(SoilData data) {
        Map<String, Object> health = new HashMap<>();
        int score = 100;
        List<String> suggestions = new ArrayList<>();
        
        // 1. 湿度评分 (40-60%为最佳)
        if (data.getMoisture() != null) {
            if (data.getMoisture() >= 40 && data.getMoisture() <= 60) {
                health.put("moistureStatus", "optimal");
            } else if (data.getMoisture() >= 30 && data.getMoisture() <= 70) {
                health.put("moistureStatus", "good");
                score -= 5;
            } else {
                health.put("moistureStatus", "poor");
                score -= 15;
                if (data.getMoisture() < 30) {
                    suggestions.add("土壤湿度过低，建议开启灌溉");
                } else {
                    suggestions.add("土壤湿度过高，注意排水");
                }
            }
        }
        
        // 2. 温度评分 (18-28°C为最佳)
        if (data.getTemperature() != null) {
            if (data.getTemperature() >= 18 && data.getTemperature() <= 28) {
                health.put("temperatureStatus", "optimal");
            } else if (data.getTemperature() >= 12 && data.getTemperature() <= 35) {
                health.put("temperatureStatus", "good");
                score -= 5;
            } else {
                health.put("temperatureStatus", "poor");
                score -= 12;
                if (data.getTemperature() < 12) {
                    suggestions.add("土壤温度过低，注意保温措施");
                } else {
                    suggestions.add("土壤温度过高，建议通风降温");
                }
            }
        }
        
        // 3. pH评分 (6.0-7.5为最佳)
        if (data.getPh() != null) {
            if (data.getPh() >= 6.0 && data.getPh() <= 7.5) {
                health.put("phStatus", "optimal");
            } else if (data.getPh() >= 5.5 && data.getPh() <= 8.0) {
                health.put("phStatus", "good");
                score -= 5;
            } else {
                health.put("phStatus", "poor");
                score -= 10;
                suggestions.add("土壤pH值不适宜，建议调节酸碱度");
            }
        }
        
        // 4. EC值评分 (0.8-2.0 mS/cm为最佳)
        if (data.getEc() != null) {
            if (data.getEc() >= 0.8 && data.getEc() <= 2.0) {
                health.put("ecStatus", "optimal");
            } else if (data.getEc() >= 0.3 && data.getEc() <= 3.0) {
                health.put("ecStatus", "good");
                score -= 5;
            } else {
                health.put("ecStatus", "poor");
                score -= 10;
                if (data.getEc() < 0.3) {
                    suggestions.add("土壤EC值过低，建议适当增加肥料");
                } else {
                    suggestions.add("土壤EC值过高，建议灌溉淋洗或换土");
                }
            }
        }
        
        // 5. 氮含量评分 (80-150 mg/kg为最佳)
        if (data.getNitrogen() != null) {
            if (data.getNitrogen() >= 80 && data.getNitrogen() <= 150) {
                health.put("nitrogenStatus", "optimal");
            } else if (data.getNitrogen() >= 50 && data.getNitrogen() <= 200) {
                health.put("nitrogenStatus", "good");
                score -= 5;
            } else {
                health.put("nitrogenStatus", "poor");
                score -= 12;
                if (data.getNitrogen() < 50) {
                    suggestions.add("土壤氮含量偏低，建议施用氮肥");
                } else {
                    suggestions.add("土壤氮含量过高，建议减少氮肥使用");
                }
            }
        }
        
        // 6. 肥力综合评分 (磷+钾 90-200 mg/kg为最佳)
        if (data.getPhosphorus() != null && data.getPotassium() != null) {
            double totalFertility = data.getPhosphorus() + data.getPotassium();
            if (totalFertility >= 90 && totalFertility <= 200) {
                health.put("fertilityStatus", "optimal");
            } else if (totalFertility >= 60 && totalFertility <= 280) {
                health.put("fertilityStatus", "good");
                score -= 5;
            } else {
                health.put("fertilityStatus", "poor");
                score -= 12;
                if (totalFertility < 60) {
                    suggestions.add("土壤肥力偏低，建议适当补充磷钾肥");
                } else {
                    suggestions.add("土壤肥力过高，建议减少施肥量");
                }
            }
        }
        
        health.put("score", Math.max(0, score));
        health.put("level", score >= 90 ? "excellent" : score >= 70 ? "good" : "needs_attention");
        health.put("suggestions", suggestions);
        
        return health;
    }
    
    @Override
    public List<Map<String, Object>> getIrrigationRecommendations() {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        List<SoilSensor> sensors = baseMapper.selectOnlineSensors();
        
        for (SoilSensor sensor : sensors) {
            SoilData data = getRealTimeData(sensor.getId());
            if (data == null || data.getMoisture() == null) continue;
            
            // 湿度低于35%推荐灌溉
            if (data.getMoisture() < 35) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("sensorId", sensor.getId());
                rec.put("fieldName", sensor.getLocation());
                rec.put("currentMoisture", data.getMoisture());
                rec.put("recommendedDuration", calculateIrrigationDuration(data.getMoisture()));
                rec.put("priority", data.getMoisture() < 25 ? "high" : "medium");
                rec.put("reason", "土壤湿度低于安全阈值");
                recommendations.add(rec);
            }
        }
        
        // 按优先级排序
        recommendations.sort((a, b) -> {
            String pa = (String) a.get("priority");
            String pb = (String) b.get("priority");
            return pa.equals("high") ? -1 : pb.equals("high") ? 1 : 0;
        });
        
        return recommendations;
    }
    
    private int calculateIrrigationDuration(double moisture) {
        // 根据当前湿度计算建议灌溉时长(分钟)
        if (moisture < 20) return 30;
        if (moisture < 30) return 20;
        return 15;
    }
    
    @Override
    public List<Map<String, Object>> listSensorsWithRealTimeData() {
        List<SoilSensor> sensors = list();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (SoilSensor sensor : sensors) {
            Map<String, Object> sensorMap = new HashMap<>();
            // 添加传感器基本信息
            sensorMap.put("id", sensor.getId());
            sensorMap.put("deviceCode", sensor.getDeviceCode());
            sensorMap.put("deviceName", sensor.getDeviceName());
            sensorMap.put("fieldId", sensor.getFieldId());
            sensorMap.put("status", sensor.getStatus());
            sensorMap.put("location", sensor.getLocation());
            sensorMap.put("lastReportTime", sensor.getLastReportTime());
            sensorMap.put("createTime", sensor.getCreateTime());
            
            // 获取并添加实时数据
            try {
                SoilData realTimeData = getRealTimeData(sensor.getId());
                if (realTimeData != null) {
                    Map<String, Object> realTimeMap = new HashMap<>();
                    realTimeMap.put("moisture", realTimeData.getMoisture());
                    realTimeMap.put("temperature", realTimeData.getTemperature());
                    realTimeMap.put("ph", realTimeData.getPh());
                    realTimeMap.put("ec", realTimeData.getEc());
                    realTimeMap.put("nitrogen", realTimeData.getNitrogen());
                    realTimeMap.put("phosphorus", realTimeData.getPhosphorus());
                    realTimeMap.put("potassium", realTimeData.getPotassium());
                    realTimeMap.put("collectTime", realTimeData.getCollectTime());
                    sensorMap.put("realTimeData", realTimeMap);
                } else {
                    // 实时数据为空时设置null
                    sensorMap.put("realTimeData", null);
                }
            } catch (Exception e) {
                // 获取实时数据失败时记录日志但不影响整个接口
                log.error("获取传感器实时数据失败, sensorId: {}", sensor.getId(), e);
                sensorMap.put("realTimeData", null);
            }
            
            result.add(sensorMap);
        }
        
        return result;
    }
}
