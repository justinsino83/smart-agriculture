package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.Facility;
import com.weiming.smartag.entity.InsectData;
import com.weiming.smartag.entity.SoilData;
import com.weiming.smartag.entity.WeatherData;
import com.weiming.smartag.mapper.InsectDataMapper;
import com.weiming.smartag.mapper.SoilDataMapper;
import com.weiming.smartag.mapper.WeatherDataMapper;
import com.weiming.smartag.service.DevicePushService;
import com.weiming.smartag.service.FacilityService;
import com.weiming.smartag.service.MenuModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/overview")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "总览管理", description = "系统总览数据接口")
public class OverviewController {

    private final FacilityService facilityService;
    private final DevicePushService devicePushService;
    private final WeatherDataMapper weatherDataMapper;
    private final InsectDataMapper insectDataMapper;
    private final SoilDataMapper soilDataMapper;
    private final MenuModelConfigService menuModelConfigService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "获取系统总览数据", description = "返回菜单数据、模型数据和面板数据")
    public Result<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> result = new HashMap<>();

            // 1. 获取菜单和模型配置（动态读取JSON文件）
            Map<String, Object> config = menuModelConfigService.getConfig();
            result.put("menus", config.get("menus"));
            result.put("gltfs", config.get("gltfs"));

            // 2. 获取面板数据
            Map<String, Object> panelData = new HashMap<>();

            // 2.1 平台位置信息
            panelData.put("platformLocation", getPlatformLocation());

            // 2.2 环境监测数据
            panelData.put("environment", getEnvironmentMonitorData());

            // 2.3 土壤监测数据
            panelData.put("soil", getSoilMonitorData());

            // 2.4 气象监测数据
            panelData.put("weather", getWeatherMonitorData());

            // 2.5 墒情数据
            panelData.put("soilMoisture", getSoilMoistureData());

            // 2.6 虫情数据
            panelData.put("insect", getPanelInsectData());

            // 2.7 视频监控数据
            panelData.put("video", getVideoMonitorData());

            result.put("panelData", panelData);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取总览数据失败", e);
            return Result.fail("获取总览数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取平台位置信息
     */
    private Map<String, Object> getPlatformLocation() {
        Map<String, Object> platformLocation = new HashMap<>();

        // 从设施表获取第一个设施作为参考
        Facility firstFacility = facilityService.getOne(
                new LambdaQueryWrapper<Facility>().last("LIMIT 1")
        );

        if (firstFacility != null && firstFacility.getLocationName() != null) {
            String locationName = firstFacility.getLocationName();
            // 如果locationName包含特定信息，提取乡镇名
            String townName = locationName;
            if (locationName.contains("泰兴市根思乡")) {
                townName = "泰兴市根思乡";
            }
            platformLocation.put("location", firstFacility.getLocation() != null ? firstFacility.getLocation() : "32.161,119.994");
            platformLocation.put("locationName", townName);
            platformLocation.put("address", "江苏省泰州市" + townName);
        } else {
            // 备用数据
            platformLocation.put("location", "32.161,119.994");
            platformLocation.put("locationName", "泰兴市根思乡");
            platformLocation.put("address", "江苏省泰州市泰兴市根思乡");
        }

        return platformLocation;
    }

    /**
     * 获取环境监测数据（按日期统计）
     */
    private List<Map<String, Object>> getEnvironmentMonitorData() {
        List<Map<String, Object>> dataList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        try {
            // 获取最近7天的数据
            LocalDateTime now = LocalDateTime.now();
            for (int i = 6; i >= 0; i--) {
                LocalDateTime targetDate = now.minusDays(i);
                String dateStr = targetDate.format(formatter);

                // 查询当天的土壤数据
                List<SoilData> dayDataList = soilDataMapper.selectList(
                        new LambdaQueryWrapper<SoilData>()
                                .ge(SoilData::getCollectTime, targetDate.toLocalDate().atStartOfDay())
                                .lt(SoilData::getCollectTime, targetDate.plusDays(1).toLocalDate().atStartOfDay())
                );

                // 计算平均值
                double avgTemp = 0;
                double avgHumidity = 0;
                double avgEc = 0;

                if (!dayDataList.isEmpty()) {
                    for (SoilData data : dayDataList) {
                        if (data.getTemperature() != null) avgTemp += data.getTemperature();
                        if (data.getMoisture() != null) avgHumidity += data.getMoisture();
                        if (data.getEc() != null) avgEc += data.getEc();
                    }
                    avgTemp = avgTemp / dayDataList.size();
                    avgHumidity = avgHumidity / dayDataList.size();
                    avgEc = avgEc / dayDataList.size();
                }

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", dateStr);
                dayData.put("temperature", Math.round(avgTemp * 10) / 10.0);
                dayData.put("airHumidity", Math.round(avgHumidity * 10) / 10.0);
                dayData.put("soilConductivity", Math.round(avgEc * 10) / 10.0);
                dataList.add(dayData);
            }
        } catch (Exception e) {
            log.warn("获取环境监测数据失败", e);
            // 模拟数据
            String[] dates = {"05-27", "05-28", "05-29", "05-30", "05-31", "06-01", "06-02"};
            for (String date : dates) {
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", date);
                dayData.put("temperature", 22.0 + Math.random() * 10);
                dayData.put("airHumidity", 55.0 + Math.random() * 20);
                dayData.put("soilConductivity", 1.2 + Math.random() * 0.5);
                dataList.add(dayData);
            }
        }

        return dataList;
    }

    /**
     * 获取土壤监测数据（最新）
     */
    private Map<String, Object> getSoilMonitorData() {
        Map<String, Object> data = new HashMap<>();

        try {
            SoilData latestSoilData = soilDataMapper.selectOne(
                    new LambdaQueryWrapper<SoilData>()
                            .orderByDesc(SoilData::getCollectTime)
                            .last("LIMIT 1")
            );

            if (latestSoilData != null) {
                data.put("ph", latestSoilData.getPh());
                data.put("nitrogen", latestSoilData.getNitrogen());
                data.put("phosphorus", latestSoilData.getPhosphorus());
                data.put("potassium", latestSoilData.getPotassium());
                data.put("collectTime", latestSoilData.getCollectTime());
            } else {
                // 模拟数据
                data.put("ph", 6.8);
                data.put("nitrogen", 85.5);
                data.put("phosphorus", 42.3);
                data.put("potassium", 120.0);
                data.put("collectTime", LocalDateTime.now());
            }
        } catch (Exception e) {
            log.warn("获取土壤监测数据失败", e);
            // 模拟数据
            data.put("ph", 6.8);
            data.put("nitrogen", 85.5);
            data.put("phosphorus", 42.3);
            data.put("potassium", 120.0);
            data.put("collectTime", LocalDateTime.now());
        }

        return data;
    }

    /**
     * 获取气象监测数据（最新）
     */
    private Map<String, Object> getWeatherMonitorData() {
        Map<String, Object> data = new HashMap<>();

        try {
            WeatherData latestWeatherData = weatherDataMapper.selectOne(
                    new LambdaQueryWrapper<WeatherData>()
                            .orderByDesc(WeatherData::getCollectTime)
                            .last("LIMIT 1")
            );

            if (latestWeatherData != null) {
                data.put("pressure", latestWeatherData.getPressure() != null ? (long) Math.round(latestWeatherData.getPressure()) : 1013);
                data.put("lightIntensity", (long) (35000 + Math.random() * 20000)); // 模拟
                data.put("photosyntheticRadiation", (long) (800 + Math.random() * 400)); // 模拟
                data.put("windSpeed", latestWeatherData.getWindSpeed() != null ? (long) Math.round(latestWeatherData.getWindSpeed()) : 3);
                data.put("cumulativeRainfall", (long) (25 + Math.random() * 10)); // 模拟
                data.put("windDirection", latestWeatherData.getWindDirection() != null ? (long) Math.round(latestWeatherData.getWindDirection()) : 90);
                data.put("totalRadiation", (long) (400 + Math.random() * 200)); // 模拟
                data.put("collectTime", latestWeatherData.getCollectTime());
            } else {
                // 模拟数据
                data.put("pressure", 1013);
                data.put("lightIntensity", 45000);
                data.put("photosyntheticRadiation", 1000);
                data.put("windSpeed", 3);
                data.put("cumulativeRainfall", 29);
                data.put("windDirection", 90);
                data.put("totalRadiation", 520);
                data.put("collectTime", LocalDateTime.now());
            }
        } catch (Exception e) {
            log.warn("获取气象监测数据失败", e);
            // 模拟数据
            data.put("pressure", 1013);
            data.put("lightIntensity", 45000);
            data.put("photosyntheticRadiation", 1000);
            data.put("windSpeed", 3);
            data.put("cumulativeRainfall", 29);
            data.put("windDirection", 90);
            data.put("totalRadiation", 520);
            data.put("collectTime", LocalDateTime.now());
        }

        return data;
    }

    /**
     * 获取虫情统计数据（通用方法）
     */
    private List<Map<String, Object>> getInsectStatisticsData() {
        List<Map<String, Object>> statistics = new ArrayList<>();

        try {
            // 获取所有虫情数据
            List<InsectData> insectDataList = insectDataMapper.selectList(
                    new LambdaQueryWrapper<>()
            );

            Map<String, Integer> typeCountMap = new HashMap<>();
            for (InsectData dataItem : insectDataList) {
                String detectResult = dataItem.getDetectResult();

                if (detectResult == null || detectResult.trim().isEmpty()) {
                    continue;
                }

                try {
                    // --- 终极暴力清洗与提取方案开始 ---

                    int startIndex = detectResult.indexOf('[');
                    int endIndex = detectResult.lastIndexOf(']');

                    if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {

                        // 1. 强行提取出数组部分的字符串
                        String cleanJson = detectResult.substring(startIndex, endIndex + 1);

                        // 2. 清理掉格式化产生的字面转义字符 (\n, \t, \r)
                        // 【关键】必须先清理这些，否则后续去掉反斜杠后，会留下孤立的字母 n 或 t 导致 JSON 报错
                        cleanJson = cleanJson.replace("\\n", "");
                        cleanJson = cleanJson.replace("\\r", "");
                        cleanJson = cleanJson.replace("\\t", "");

                        // 顺手清理真实的换行符（防止系统存了真实的不可见换行）
                        cleanJson = cleanJson.replace("\n", "");
                        cleanJson = cleanJson.replace("\r", "");

                        // 3. 终极杀招：无差别清除剩下的所有反斜杠！
                        // 这一步能把 \\\"、\\\\\\\" 这种不管多少层的恶心嵌套全部扒干净，只留下纯正的双引号
                        cleanJson = cleanJson.replace("\\", "");

                        // 4. 此时 cleanJson 已经是极度干净的标准 JSON，直接解析
                        List<Map<String, Object>> detectList = objectMapper.readValue(
                                cleanJson,
                                new TypeReference<List<Map<String, Object>>>() {}
                        );

                        // 5. 遍历统计
                        for (Map<String, Object> detectItem : detectList) {
                            String insectName = (String) detectItem.get("name");
                            if (insectName == null || insectName.isEmpty()) {
                                insectName = "未知虫害";
                            }
                            typeCountMap.put(insectName, typeCountMap.getOrDefault(insectName, 0) + 1);
                        }
                    } else {
                        log.warn("虫情检测结果不包含有效的数组结构, ID: {}, 数据: {}", dataItem.getId(), detectResult);
                    }

                    // --- 终极暴力清洗与提取方案结束 ---

                } catch (Exception e) {
                    log.warn("解析虫情检测结果失败, ID: {}, 原始数据: {}", dataItem.getId(), detectResult, e);
                }
            }

            // 构建统计结果
            for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
                Map<String, Object> stat = new HashMap<>();
                stat.put("name", entry.getKey());
                stat.put("count", entry.getValue());
                statistics.add(stat);
            }

            // 按次数降序排序
            statistics.sort((a, b) -> (Integer) b.get("count") - (Integer) a.get("count"));

        } catch (Exception e) {
            log.error("获取虫情统计数据失败", e);
            // 模拟数据
            List<Map<String, Object>> mockStats = new ArrayList<>();
            Map<String, Object> stat1 = new HashMap<>();
            stat1.put("name", "摇蚊");
            stat1.put("count", 25);
            mockStats.add(stat1);
            Map<String, Object> stat2 = new HashMap<>();
            stat2.put("name", "果蝇");
            stat2.put("count", 18);
            mockStats.add(stat2);
            Map<String, Object> stat3 = new HashMap<>();
            stat3.put("name", "蚜虫");
            stat3.put("count", 12);
            mockStats.add(stat3);
            return mockStats;
        }

        return statistics;
    }

    /**
     * 获取墒情数据（土壤湿度）
     */
    private Map<String, Object> getSoilMoistureData() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "pending");
        data.put("message", "数据来源为后续湿度传感器接口，当前返回模拟数据");
        data.put("soilMoisture", 62.5);
        data.put("updateTime", LocalDateTime.now());
        return data;
    }

    /**
     * 获取虫情数据
     */
    private Map<String, Object> getPanelInsectData() {
        Map<String, Object> data = new HashMap<>();

        try {
            // 获取最新虫情记录
            InsectData latestInsectData = insectDataMapper.selectOne(
                    new LambdaQueryWrapper<InsectData>()
                            .orderByDesc(InsectData::getRecordTime)
                            .last("LIMIT 1")
            );

            if (latestInsectData != null) {
                data.put("latestRecord", latestInsectData);
            }

            // 获取统计数据
            List<Map<String, Object>> statistics = getInsectStatisticsData();
            data.put("statistics", statistics);

        } catch (Exception e) {
            log.warn("获取面板虫情数据失败", e);
            // 模拟数据
            List<Map<String, Object>> statistics = new ArrayList<>();
            Map<String, Object> stat1 = new HashMap<>();
            stat1.put("name", "摇蚊");
            stat1.put("count", 25);
            statistics.add(stat1);
            Map<String, Object> stat2 = new HashMap<>();
            stat2.put("name", "果蝇");
            stat2.put("count", 18);
            statistics.add(stat2);
            Map<String, Object> stat3 = new HashMap<>();
            stat3.put("name", "蚜虫");
            stat3.put("count", 12);
            statistics.add(stat3);
            data.put("statistics", statistics);
        }

        return data;
    }

    /**
     * 获取视频监控数据
     */
    private Map<String, Object> getVideoMonitorData() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "pending");
        data.put("message", "摄像头切换推流地址接口待开发，当前返回空数据结构");
        data.put("cameras", new ArrayList<>());
        data.put("currentCamera", null);
        data.put("pushUrl", null);
        return data;
    }

    /**
     * 获取虫情统计数据
     */
    @GetMapping("/insect/statistics")
    @Operation(summary = "获取虫情统计数据", description = "按虫害类型统计出现次数")
    public Result<List<Map<String, Object>>> getInsectStatistics() {
        try {
            return Result.success(getInsectStatisticsData());
        } catch (Exception e) {
            log.error("获取虫情统计数据失败", e);
            return Result.fail("获取虫情统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取虫情全量数据
     */
    @GetMapping("/insect/list")
    @Operation(summary = "获取虫情全量数据", description = "获取系统中所有虫害记录")
    public Result<List<InsectData>> getInsectList() {
        try {
            List<InsectData> insectDataList = insectDataMapper.selectList(
                    new LambdaQueryWrapper<InsectData>()
                            .orderByDesc(InsectData::getRecordTime)
            );
            return Result.success(insectDataList);
        } catch (Exception e) {
            log.error("获取虫情全量数据失败", e);
            return Result.fail("获取虫情全量数据失败: " + e.getMessage());
        }
    }
}
