package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.config.IotProperties;
import com.weiming.smartag.entity.DevicePushData;
import com.weiming.smartag.entity.InsectData;
import com.weiming.smartag.mapper.DevicePushDataMapper;
import com.weiming.smartag.mapper.InsectDataMapper;
import com.weiming.smartag.service.MenuModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/overview")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "总览管理", description = "系统总览数据接口")
public class OverviewController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final IotProperties iotProperties;
    private final MenuModelConfigService menuModelConfigService;
    private final DevicePushDataMapper devicePushDataMapper;
    private final InsectDataMapper insectDataMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "获取系统总览数据", description = "按农场分组返回环境/土壤/气象/虫情/摄像头")
    public Result<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> result = new HashMap<>();

            Map<String, Object> config = menuModelConfigService.getConfig();
            result.put("menus", config.get("menus"));
            result.put("gltfs", config.get("gltfs"));

            List<String> allClientIds = devicePushDataMapper.selectList(
                    new LambdaQueryWrapper<DevicePushData>()
                            .select(DevicePushData::getClientId)
                            .isNotNull(DevicePushData::getClientId)
                            .groupBy(DevicePushData::getClientId)
            ).stream().map(DevicePushData::getClientId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

            List<Map<String, Object>> cameraDevicesFromIot = fetchCameraDevices();

            List<Map<String, Object>> panelDataList = buildPanelDataList(config, allClientIds, cameraDevicesFromIot);
            result.put("panelData", panelDataList);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取总览数据失败", e);
            return Result.fail("获取总览数据失败: " + e.getMessage());
        }
    }

    // ======================================================================
    // 按农场分组构造面板
    // 关键：优先使用 iot.camera.farms[i].name 精确匹配，再用其中的 clientId / ids
    // ======================================================================
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildPanelDataList(Map<String, Object> config,
                                                          List<String> allClientIds,
                                                          List<Map<String, Object>> cameraDevicesFromIot) {
        List<Map<String, Object>> panelDataList = new ArrayList<>();
        List<IotProperties.CameraFarm> farms = iotProperties.getCamera() == null
                ? Collections.emptyList() : iotProperties.getCamera().getFarms();

        List<Map<String, Object>> menus = (List<Map<String, Object>>) config.get("menus");
        if (menus == null) return panelDataList;

        for (Map<String, Object> menu : menus) {
            String farmName = (String) menu.get("name");
            if (farmName == null || "总览".equals(farmName)) continue;

            // 1) 精确匹配 iot.camera.farms[i].name
            IotProperties.CameraFarm farmCfg = findFarmByName(farms, farmName);
            String clientId = farmCfg != null ? emptyToNull(farmCfg.getClientId()) : null;

            // 2) 若配置里没写 clientId，走 clientId 兜底（按农场名模糊猜）
            if (clientId == null) {
                clientId = matchClientIdForFarm(farmName, allClientIds);
            }

            Map<String, Object> farmPanel = new LinkedHashMap<>();
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
            farmPanel.put("cameras", buildCamerasForFarm(farmName, farmCfg, cameraDevicesFromIot));

            log.info("[总览-农场] farmName={}, farmCfg(name={}, clientId={}, ids={}), 实际 clientId={}, IoT 摄像头命中={}",
                    farmName,
                    farmCfg == null ? null : farmCfg.getName(),
                    farmCfg == null ? null : farmCfg.getClientId(),
                    farmCfg == null ? null : farmCfg.getIds(),
                    clientId,
                    ((List<?>) farmPanel.get("cameras")).size());

            panelDataList.add(farmPanel);
        }
        return panelDataList;
    }

    private static IotProperties.CameraFarm findFarmByName(List<IotProperties.CameraFarm> farms, String farmName) {
        if (farms == null || farmName == null) return null;
        String key = farmName.replaceAll("\\s+", "");
        for (IotProperties.CameraFarm f : farms) {
            if (f == null || f.getName() == null) continue;
            String n = f.getName().replaceAll("\\s+", "");
            if (n.equalsIgnoreCase(key)) return f;
        }
        for (IotProperties.CameraFarm f : farms) {
            if (f == null || f.getName() == null) continue;
            String n = f.getName().replaceAll("\\s+", "");
            if (n.contains(key) || key.contains(n)) return f;
        }
        return null;
    }

    // clientId 兜底：在 device_push_data 的 client_id 列表中根据农场名模糊匹配
    private String matchClientIdForFarm(String farmName, List<String> allClientIds) {
        if (farmName == null || allClientIds == null || allClientIds.isEmpty()) return null;
        for (String cid : allClientIds) {
            if (cid == null) continue;
            String lower = cid.toLowerCase();
            if (farmName.contains("维明") && (lower.contains("weiming")
                    || lower.contains("wm") || lower.contains("维明"))) return cid;
            if (farmName.contains("红耕") && (lower.contains("honggeng")
                    || lower.contains("hg") || lower.contains("红耕"))) return cid;
        }
        // 最后的兜底：按 clientId 排序后给 维明 / 红耕 各分配一个
        List<String> sortedIds = allClientIds.stream().filter(Objects::nonNull).sorted().collect(Collectors.toList());
        if (sortedIds.size() >= 2) {
            if (farmName.contains("维明")) return sortedIds.get(0);
            if (farmName.contains("红耕")) return sortedIds.get(1);
        }
        return sortedIds.isEmpty() ? null : sortedIds.get(0);
    }

    // ======================================================================
    // 环境 / 土壤 / 气象
    // ======================================================================
    private List<Map<String, Object>> buildEnvironmentData(String clientId) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 4; i >= 0; i--) {
            LocalDate target = today.minusDays(i);
            LocalDateTime start = target.atStartOfDay();
            LocalDateTime end = target.plusDays(1).atStartOfDay();

            DevicePushData data = devicePushDataMapper.selectOne(
                    new LambdaQueryWrapper<DevicePushData>()
                            .eq(DevicePushData::getClientId, clientId)
                            .ge(DevicePushData::getDetectedTime, start)
                            .lt(DevicePushData::getDetectedTime, end)
                            .orderByDesc(DevicePushData::getDetectedTime)
                            .last("LIMIT 1"));

            Map<String, Object> dayItem = new LinkedHashMap<>();
            dayItem.put("date", target.format(DATE_FORMATTER));
            dayItem.put("ambientTemperature", data != null ? data.getAmbientTemperature() : null);
            dayItem.put("ambientHumidity", data != null ? data.getAmbientHumidity() : null);
            result.add(dayItem);
        }
        return result;
    }

    private Map<String, Object> buildSoilData(String clientId) {
        Map<String, Object> result = new LinkedHashMap<>();
        DevicePushData latest = devicePushDataMapper.selectOne(
                new LambdaQueryWrapper<DevicePushData>()
                        .eq(DevicePushData::getClientId, clientId)
                        .isNotNull(DevicePushData::getSoilPh)
                        .orderByDesc(DevicePushData::getDetectedTime)
                        .last("LIMIT 1"));
        if (latest != null && latest.getSoilPh() != null) {
            result.put("soilPh", latest.getSoilPh());
            if (latest.getDetectedTime() != null) result.put("detectedTime", latest.getDetectedTime());
        }
        return result;
    }

    private Map<String, Object> buildWeatherData(String clientId) {
        Map<String, Object> result = new LinkedHashMap<>();
        DevicePushData latest = devicePushDataMapper.selectOne(
                new LambdaQueryWrapper<DevicePushData>()
                        .eq(DevicePushData::getClientId, clientId)
                        .orderByDesc(DevicePushData::getDetectedTime)
                        .last("LIMIT 1"));
        if (latest == null) return result;
        if (latest.getPressure() != null) result.put("pressure", latest.getPressure());
        if (latest.getWindSpeed() != null) result.put("windSpeed", latest.getWindSpeed());
        if (latest.getRainfall() != null) result.put("rainfall", latest.getRainfall());
        if (latest.getLightIntensity() != null) result.put("lightIntensity", latest.getLightIntensity());
        if (latest.getDewTemp() != null) result.put("dewTemp", latest.getDewTemp());
        if (latest.getCo2() != null) result.put("co2", latest.getCo2());
        if (latest.getDetectedTime() != null) result.put("detectedTime", latest.getDetectedTime());
        return result;
    }

    // ======================================================================
    // 虫情数据：解析 insect_data.detect_result 中 JSON 数组做虫名聚合
    // ======================================================================
    private Map<String, Object> buildInsectData() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<InsectData> insectList = insectDataMapper.selectList(
                new LambdaQueryWrapper<InsectData>()
                        .orderByDesc(InsectData::getRecordTime)
                        .last("LIMIT 100"));
        if (insectList == null || insectList.isEmpty()) return result;

        result.put("latestRecord", insectList.get(0));
        Map<String, Integer> typeCountMap = new HashMap<>();
        for (InsectData item : insectList) {
            String detectResult = item.getDetectResult();
            if (detectResult == null || detectResult.trim().isEmpty()) continue;

            int startIndex = detectResult.indexOf('[');
            int endIndex = detectResult.lastIndexOf(']');
            if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) continue;

            try {
                String cleanJson = detectResult.substring(startIndex, endIndex + 1)
                        .replace("\\n", "").replace("\\r", "").replace("\\t", "")
                        .replace("\n", "").replace("\r", "").replace("\\", "");
                List<Map<String, Object>> detectList = objectMapper.readValue(
                        cleanJson, new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> detectItem : detectList) {
                    String insectName = (String) detectItem.get("name");
                    if (insectName == null || insectName.isEmpty()) insectName = "未知虫害";
                    typeCountMap.merge(insectName, 1, Integer::sum);
                }
            } catch (Exception e) {
                log.warn("解析虫情检测结果失败, id={}, raw={}", item.getId(), detectResult, e);
            }
        }

        List<Map<String, Object>> statistics = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("name", entry.getKey());
            stat.put("count", entry.getValue());
            statistics.add(stat);
        }
        statistics.sort((a, b) -> (Integer) b.get("count") - (Integer) a.get("count"));
        result.put("statistics", statistics);
        return result;
    }

    // ======================================================================
    // 摄像头数据：调用 IoT 平台 listAllDevices(device_type=1) 拉列表，再按配置的 deviceId 匹配
    // ======================================================================
    private List<Map<String, Object>> fetchCameraDevices() {
        IotProperties.Platform platform = iotProperties.getPlatform();
        String baseUrl = platform == null ? null : emptyToNull(platform.getBaseUrl());
        String token = platform == null ? null : emptyToNull(platform.getToken());
        IotProperties.Camera camera = iotProperties.getCamera();
        boolean hasCameraConfig = camera != null
                && camera.getFarms() != null && !camera.getFarms().isEmpty();

        log.info("[摄像头] iot.baseUrl={}, iot.token={}, camera.farms={}",
                baseUrl, token == null ? null : "已设置",
                camera == null || camera.getFarms() == null ? "[]" :
                        camera.getFarms().stream()
                                .map(f -> f == null ? "null" : f.getName() + "=" + f.getIds())
                                .collect(Collectors.toList()));

        if (baseUrl == null || token == null || !hasCameraConfig) return Collections.emptyList();

        try {
            String url = appendPath(baseUrl, "listAllDevices");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("token", token);
            payload.put("device_type", 1);

            String respBody = postJson(url, payload);
            if (respBody == null) return Collections.emptyList();

            Map<String, Object> resp = objectMapper.readValue(respBody,
                    new TypeReference<Map<String, Object>>() {});
            Object rawCode = resp.get("code");
            if (rawCode == null || !"0".equals(String.valueOf(rawCode))) return Collections.emptyList();

            Object rawData = resp.get("data");
            if (!(rawData instanceof List)) return Collections.emptyList();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) rawData;
            log.info("[摄像头] listAllDevices 返回设备数量={}", list.size());
            return list;
        } catch (Exception e) {
            log.warn("[摄像头] 获取 IoT 摄像头设备列表失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 按农场返回摄像头列表：优先使用 CameraFarm.ids 精确匹配 device.id
     */
    private List<Map<String, Object>> buildCamerasForFarm(String farmName,
                                                           IotProperties.CameraFarm farmCfg,
                                                           List<Map<String, Object>> cameraDevicesFromIot) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (farmName == null || cameraDevicesFromIot == null || cameraDevicesFromIot.isEmpty()) return result;

        List<String> ids = farmCfg == null ? null : farmCfg.getIds();
        Set<String> deviceIdSet = new HashSet<>();
        if (ids != null) {
            for (String id : ids) {
                if (id != null && !id.trim().isEmpty()) deviceIdSet.add(id.trim());
            }
        }
        if (deviceIdSet.isEmpty()) {
            log.warn("[摄像头] 农场={} 的摄像头 ids 未配置，跳过", farmName);
            return result;
        }

        for (Map<String, Object> device : cameraDevicesFromIot) {
            if (device == null) continue;
            String idStr = toStringId(device.get("id"));
            if (idStr == null) continue;
            if (!deviceIdSet.contains(idStr)) continue;

            Map<String, Object> cam = new LinkedHashMap<>();
            cam.put("deviceId", idStr);
            cam.put("name", toStringNull(device.get("name")));
            cam.put("enable", device.get("enable"));
            cam.put("status", device.get("status"));
            cam.put("gbId", toStringNull(device.get("gb_id")));
            cam.put("httpsFlvUrl", trimUrl(device.get("https_flv_url")));
            cam.put("lastTime", device.get("last_time"));
            cam.put("stationName", toStringNull(device.get("station_name")));
            cam.put("templateName", toStringNull(device.get("template_name")));
            cam.put("deviceTypeName", toStringNull(device.get("device_type_name")));
            result.add(cam);
        }
        log.info("[摄像头] 农场={}, 期望 deviceIds={}, 命中={}", farmName, deviceIdSet, result.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractFarmNames(Map<String, Object> config) {
        List<String> names = new ArrayList<>();
        if (config == null) return names;
        List<Map<String, Object>> menus = (List<Map<String, Object>>) config.get("menus");
        if (menus == null) return names;
        for (Map<String, Object> m : menus) {
            String n = (String) m.get("name");
            if (n != null && !"总览".equals(n)) names.add(n);
        }
        return names;
    }

    // ======================================================================
    // 工具方法
    // ======================================================================
    private static String emptyToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private static String toStringId(Object idObj) {
        if (idObj == null) return null;
        if (idObj instanceof String) return ((String) idObj).trim();
        if (idObj instanceof Number) return ((Number) idObj).toString();
        return String.valueOf(idObj).trim();
    }

    private static String toStringNull(Object o) {
        if (o == null) return null;
        if (o instanceof String) {
            String v = ((String) o).trim();
            return v.isEmpty() ? null : v;
        }
        return String.valueOf(o);
    }

    private static String trimUrl(Object urlObj) {
        if (urlObj == null) return null;
        String s = String.valueOf(urlObj).trim();
        if (s.isEmpty()) return null;
        while (s.startsWith("`") || s.startsWith("'") || s.startsWith("\"")) s = s.substring(1).trim();
        while (s.endsWith("`")   || s.endsWith("'")   || s.endsWith("\""))   s = s.substring(0, s.length() - 1).trim();
        return s;
    }

    private static String appendPath(String base, String path) {
        if (base == null || base.isEmpty()) return path;
        return base.endsWith("/") ? (base + path) : (base + "/" + path);
    }

    private String postJson(String url, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            return resp.getBody();
        } catch (Exception e) {
            log.warn("POST {} 失败: {}", url, e.getMessage());
            return null;
        }
    }
}
