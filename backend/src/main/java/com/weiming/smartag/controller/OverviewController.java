package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.DevicePushData;
import com.weiming.smartag.entity.InsectData;
import com.weiming.smartag.mapper.DevicePushDataMapper;
import com.weiming.smartag.mapper.InsectDataMapper;
import com.weiming.smartag.service.MenuModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    private final MenuModelConfigService menuModelConfigService;
    private final DevicePushDataMapper devicePushDataMapper;
    private final InsectDataMapper insectDataMapper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    @GetMapping
    @Operation(summary = "获取系统总览数据", description = "返回菜单配置、模型配置以及按农场分组的面板数据")
    public Result<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> result = new HashMap<>();

            Map<String, Object> config = menuModelConfigService.getConfig();
            result.put("menus", config.get("menus"));
            result.put("gltfs", config.get("gltfs"));

            List<Map<String, Object>> panelDataList = buildPanelDataList(config);
            result.put("panelData", panelDataList);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取总览数据失败", e);
            return Result.fail("获取总览数据失败: " + e.getMessage());
        }
    }

    /**
     * 按农场构造面板数据列表
     * 通过菜单配置遍历农场（维明农场 / 红耕农场）
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildPanelDataList(Map<String, Object> config) {
        List<Map<String, Object>> panelDataList = new ArrayList<>();

        List<Map<String, Object>> menus = (List<Map<String, Object>>) config.get("menus");
        if (menus == null || menus.isEmpty()) {
            log.warn("菜单配置为空，无法构造面板数据");
            return panelDataList;
        }

        List<String> allClientIds = devicePushDataMapper.selectList(
                new LambdaQueryWrapper<DevicePushData>()
                        .select(DevicePushData::getClientId)
                        .isNotNull(DevicePushData::getClientId)
                        .groupBy(DevicePushData::getClientId)
        ).stream().map(DevicePushData::getClientId).distinct().toList();

        for (Map<String, Object> menu : menus) {
            String farmName = (String) menu.get("name");
            if (farmName == null || "总览".equals(farmName)) {
                continue;
            }

            String clientId = matchClientIdForFarm(farmName, allClientIds);

            Map<String, Object> farmPanel = new HashMap<>();
            farmPanel.put("farmName", farmName);
            farmPanel.put("clientId", clientId);

            if (clientId != null) {
                farmPanel.put("environment", buildEnvironmentData(clientId));
                farmPanel.put("soil", buildSoilData(clientId));
                farmPanel.put("weather", buildWeatherData(clientId));
            } else {
                farmPanel.put("environment", Collections.emptyList());
                farmPanel.put("soil", Collections.emptyMap());
                farmPanel.put("weather", Collections.emptyMap());
            }

            farmPanel.put("insect", buildInsectData());

            panelDataList.add(farmPanel);
        }

        return panelDataList;
    }

    /**
     * 根据农场名从 device_push_data 中已有的 client_id 列表中匹配出对应的 client_id
     * 匹配规则：优先通过名称包含匹配，失败则按顺序为不同农场分配固定的 client_id
     */
    private String matchClientIdForFarm(String farmName, List<String> allClientIds) {
        if (allClientIds == null || allClientIds.isEmpty()) {
            return null;
        }

        String matched = null;
        for (String clientId : allClientIds) {
            if (clientId == null) {
                continue;
            }
            String lower = clientId.toLowerCase();
            if (farmName.contains("维明") && (lower.contains("weiming") || lower.contains("wm")
                    || lower.contains("维明"))) {
                matched = clientId;
                break;
            }
            if (farmName.contains("红耕") && (lower.contains("honggeng") || lower.contains("hg")
                    || lower.contains("红耕"))) {
                matched = clientId;
                break;
            }
        }

        if (matched != null) {
            return matched;
        }

        List<String> sortedIds = allClientIds.stream().filter(Objects::nonNull).sorted().toList();
        if (sortedIds.size() >= 2) {
            if (farmName.contains("维明")) {
                return sortedIds.get(0);
            }
            if (farmName.contains("红耕")) {
                return sortedIds.get(1);
            }
        }
        if (!sortedIds.isEmpty()) {
            return sortedIds.get(0);
        }
        return null;
    }

    /**
     * 环境监测数据：查询近5天每天的环境温度与湿度
     * 每天取当天最新一条记录，若当天无数据则向前回退
     */
    private List<Map<String, Object>> buildEnvironmentData(String clientId) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 4; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            LocalDateTime startOfDay = targetDate.atStartOfDay();
            LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

            DevicePushData data = devicePushDataMapper.selectOne(
                    new LambdaQueryWrapper<DevicePushData>()
                            .eq(DevicePushData::getClientId, clientId)
                            .ge(DevicePushData::getDetectedTime, startOfDay)
                            .lt(DevicePushData::getDetectedTime, endOfDay)
                            .orderByDesc(DevicePushData::getDetectedTime)
                            .last("LIMIT 1")
            );

            Map<String, Object> dayItem = new HashMap<>();
            dayItem.put("date", targetDate.format(DATE_FORMATTER));
            dayItem.put("ambientTemperature", data != null && data.getAmbientTemperature() != null
                    ? data.getAmbientTemperature() : null);
            dayItem.put("ambientHumidity", data != null && data.getAmbientHumidity() != null
                    ? data.getAmbientHumidity() : null);
            result.add(dayItem);
        }

        return result;
    }

    /**
     * 土壤监测数据：最新的土壤PH值
     */
    private Map<String, Object> buildSoilData(String clientId) {
        Map<String, Object> result = new HashMap<>();

        DevicePushData latest = devicePushDataMapper.selectOne(
                new LambdaQueryWrapper<DevicePushData>()
                        .eq(DevicePushData::getClientId, clientId)
                        .isNotNull(DevicePushData::getSoilPh)
                        .orderByDesc(DevicePushData::getDetectedTime)
                        .last("LIMIT 1")
        );

        if (latest != null && latest.getSoilPh() != null) {
            result.put("soilPh", latest.getSoilPh());
            if (latest.getDetectedTime() != null) {
                result.put("detectedTime", latest.getDetectedTime());
            }
        }

        return result;
    }

    /**
     * 气象监测数据：最新的气压、风速、累计雨量、光照强度、露点温度、CO2
     */
    private Map<String, Object> buildWeatherData(String clientId) {
        Map<String, Object> result = new HashMap<>();

        DevicePushData latest = devicePushDataMapper.selectOne(
                new LambdaQueryWrapper<DevicePushData>()
                        .eq(DevicePushData::getClientId, clientId)
                        .orderByDesc(DevicePushData::getDetectedTime)
                        .last("LIMIT 1")
        );

        if (latest == null) {
            return result;
        }

        if (latest.getPressure() != null) {
            result.put("pressure", latest.getPressure());
        }
        if (latest.getWindSpeed() != null) {
            result.put("windSpeed", latest.getWindSpeed());
        }
        if (latest.getRainfall() != null) {
            result.put("rainfall", latest.getRainfall());
        }
        if (latest.getLightIntensity() != null) {
            result.put("lightIntensity", latest.getLightIntensity());
        }
        if (latest.getDewTemp() != null) {
            result.put("dewTemp", latest.getDewTemp());
        }
        if (latest.getCo2() != null) {
            result.put("co2", latest.getCo2());
        }
        if (latest.getDetectedTime() != null) {
            result.put("detectedTime", latest.getDetectedTime());
        }

        return result;
    }

    /**
     * 虫情数据：保留原有的 JSON 清洗+按虫害类型统计逻辑
     */
    private Map<String, Object> buildInsectData() {
        Map<String, Object> result = new HashMap<>();

        List<InsectData> insectList = insectDataMapper.selectList(
                new LambdaQueryWrapper<InsectData>()
                        .orderByDesc(InsectData::getRecordTime)
                        .last("LIMIT 100")
        );

        if (insectList == null || insectList.isEmpty()) {
            return result;
        }

        result.put("latestRecord", insectList.get(0));

        Map<String, Integer> typeCountMap = new HashMap<>();
        for (InsectData item : insectList) {
            String detectResult = item.getDetectResult();
            if (detectResult == null || detectResult.trim().isEmpty()) {
                continue;
            }

            int startIndex = detectResult.indexOf('[');
            int endIndex = detectResult.lastIndexOf(']');
            if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
                continue;
            }

            try {
                String cleanJson = detectResult.substring(startIndex, endIndex + 1)
                        .replace("\\n", "")
                        .replace("\\r", "")
                        .replace("\\t", "")
                        .replace("\n", "")
                        .replace("\r", "")
                        .replace("\\", "");

                List<Map<String, Object>> detectList = objectMapper.readValue(
                        cleanJson, new TypeReference<List<Map<String, Object>>>() {});

                for (Map<String, Object> detectItem : detectList) {
                    String insectName = (String) detectItem.get("name");
                    if (insectName == null || insectName.isEmpty()) {
                        insectName = "未知虫害";
                    }
                    typeCountMap.merge(insectName, 1, Integer::sum);
                }
            } catch (Exception e) {
                log.warn("解析虫情检测结果失败, id={}, raw={}", item.getId(), detectResult, e);
            }
        }

        List<Map<String, Object>> statistics = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("name", entry.getKey());
            stat.put("count", entry.getValue());
            statistics.add(stat);
        }
        statistics.sort((a, b) -> (Integer) b.get("count") - (Integer) a.get("count"));
        result.put("statistics", statistics);

        return result;
    }
}
