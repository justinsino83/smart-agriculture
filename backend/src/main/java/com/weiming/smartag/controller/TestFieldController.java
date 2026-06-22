package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.config.IotProperties;
import com.weiming.smartag.dto.TestFieldDataDTO;
import com.weiming.smartag.entity.DevicePushData;
import com.weiming.smartag.mapper.DevicePushDataMapper;
import com.weiming.smartag.mapper.InsectDataMapper;
import com.weiming.smartag.service.FacilityService;
import com.weiming.smartag.service.MenuModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/testfield")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "试验田管理", description = "试验田综合数据接口")
public class TestFieldController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** 排水阀 device_type */
    private static final int DEVICE_TYPE_VALVE = 120;
    /** 水位计 device_type */
    private static final int DEVICE_TYPE_WATER_LEVEL = 121;
    /** 视频摄像头 device_type */
    private static final int DEVICE_TYPE_CAMERA = 1;

    private final IotProperties iotProperties;
    private final FacilityService facilityService;
    private final DevicePushDataMapper devicePushDataMapper;
    private final InsectDataMapper insectDataMapper;
    private final MenuModelConfigService menuModelConfigService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/sensors")
    @Operation(summary = "获取试验田传感器数据", description = "基于总览数据结构，外加外部 IoT 平台的排水阀 / 水位计 / 摄像头实时数据")
    public Result<TestFieldDataDTO> getTestFieldSensors(
            @Parameter(description = "设施ID")
            @RequestParam(required = false) Long facilityId) {
        try {
            if (facilityId != null && facilityService.getById(facilityId) == null) {
                return Result.error("未找到试验田设施");
            }

            // 1) 先从菜单中找到 facilityId 对应的农场名；再用农场名到 IotProperties 精确匹配
            String farmName = resolveFarmNameForFacility(facilityId);
            IotProperties.CameraFarm farmCfg = resolveFarmConfig(farmName);

            log.info("[试验田] facilityId={} -> 农场名={} -> farmCfg(name={}, clientId={}, stationName={}, ids={})",
                    facilityId, farmName,
                    farmCfg == null ? null : farmCfg.getName(),
                    farmCfg == null ? null : farmCfg.getClientId(),
                    farmCfg == null ? null : farmCfg.getStationName(),
                    farmCfg == null ? null : farmCfg.getIds());

            // 2) 环境/土壤/气象：用配置中的 clientId 查询 device_push_data
            String clientId = farmCfg == null ? null : emptyToNull(farmCfg.getClientId());
            TestFieldDataDTO.TestFieldDataDTOBuilder builder = TestFieldDataDTO.builder();
            builder.environment(buildEnvironmentData(clientId));
            builder.soil(buildSoilData(clientId));
            builder.weather(buildWeatherData(clientId));

            // 准备 IoT 基础参数
            IotProperties.Platform platform = iotProperties.getPlatform();
            String baseUrl = platform == null ? null : emptyToNull(platform.getBaseUrl());
            String token   = platform == null ? null : emptyToNull(platform.getToken());

            // 3) IoT 平台：先拉 listAllDevices，再对每个排水阀/水位计分别调 deviceValues 拿实时值
            ExternalIotAggregate iot = fetchExternalIotData(baseUrl, token, farmCfg);
            builder.valves(buildValveItems(iot));
            builder.waterMeter(buildWaterMeterItem(iot));
            builder.cameras(buildCameras(iot));

            // 4) 虫情
            builder.insectData(buildInsectData(facilityId));

            return Result.success(builder.build());
        } catch (Exception e) {
            log.error("获取试验田传感器数据失败, facilityId: {}", facilityId, e);
            return Result.error("获取试验田数据失败: " + e.getMessage());
        }
    }

    // ========================================================================
    // facilityId → 农场名（从菜单配置中找到，找不到返回 null）
    // ========================================================================
    @SuppressWarnings("unchecked")
    private String resolveFarmNameForFacility(Long facilityId) {
        if (facilityId == null) return null;
        try {
            Map<String, Object> config = menuModelConfigService.getConfig();
            List<Map<String, Object>> menus = (List<Map<String, Object>>) config.get("menus");
            if (menus == null) return null;
            for (Map<String, Object> topMenu : menus) {
                Object children = topMenu.get("children");
                if (!(children instanceof List)) continue;
                for (Object childRaw : (List<?>) children) {
                    if (!(childRaw instanceof Map)) continue;
                    Map<String, Object> child = (Map<String, Object>) childRaw;
                    Object fid = child.get("facilityId");
                    if (fid != null && String.valueOf(fid).equals(String.valueOf(facilityId))) {
                        return (String) topMenu.get("name");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析菜单配置获取农场名失败, facilityId={}", facilityId, e);
        }
        return null;
    }

    // ========================================================================
    // 农场名 → 配置对象（从 IotProperties 中找精确匹配；找不到返回 null）
    // ========================================================================
    private IotProperties.CameraFarm resolveFarmConfig(String farmName) {
        if (farmName == null) return null;
        IotProperties.Camera camera = iotProperties.getCamera();
        List<IotProperties.CameraFarm> farms = camera == null ? Collections.emptyList() : camera.getFarms();
        if (farms == null || farms.isEmpty()) return null;

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

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    // ========================================================================
    // 环境 / 土壤 / 气象
    // ========================================================================
    private List<TestFieldDataDTO.EnvData> buildEnvironmentData(String clientId) {
        List<TestFieldDataDTO.EnvData> result = new ArrayList<>();
        if (clientId == null) return result;

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
                            .last("LIMIT 1")
            );

            result.add(TestFieldDataDTO.EnvData.builder()
                    .date(target.format(DATE_FMT))
                    .ambientTemperature(data != null ? data.getAmbientTemperature() : null)
                    .ambientHumidity(data != null ? data.getAmbientHumidity() : null)
                    .build());
        }
        return result;
    }

    private TestFieldDataDTO.SoilData buildSoilData(String clientId) {
        if (clientId == null) return null;
        DevicePushData latest = devicePushDataMapper.selectOne(
                new LambdaQueryWrapper<DevicePushData>()
                        .eq(DevicePushData::getClientId, clientId)
                        .isNotNull(DevicePushData::getSoilPh)
                        .orderByDesc(DevicePushData::getDetectedTime)
                        .last("LIMIT 1")
        );
        if (latest == null || latest.getSoilPh() == null) return null;
        return TestFieldDataDTO.SoilData.builder()
                .soilPh(latest.getSoilPh())
                .detectedTime(latest.getDetectedTime() != null ? latest.getDetectedTime().format(TIME_FMT) : null)
                .build();
    }

    private TestFieldDataDTO.WeatherData buildWeatherData(String clientId) {
        if (clientId == null) return null;
        DevicePushData latest = devicePushDataMapper.selectOne(
                new LambdaQueryWrapper<DevicePushData>()
                        .eq(DevicePushData::getClientId, clientId)
                        .orderByDesc(DevicePushData::getDetectedTime)
                        .last("LIMIT 1")
        );
        if (latest == null) return null;
        return TestFieldDataDTO.WeatherData.builder()
                .pressure(latest.getPressure())
                .windSpeed(latest.getWindSpeed())
                .rainfall(latest.getRainfall())
                .lightIntensity(latest.getLightIntensity())
                .dewTemp(latest.getDewTemp())
                .co2(latest.getCo2())
                .detectedTime(latest.getDetectedTime() != null ? latest.getDetectedTime().format(TIME_FMT) : null)
                .build();
    }

    // ========================================================================
    // 排水阀 / 水位计 / 摄像头
    //
    // 说明：
    //   - 排水阀改为数组，每个元素里包含从 deviceValues 拿到的实时压力、开度、电流、电压等
    //   - 水位计改为单个对象（取第一个），里面包含实时的 waterLevel / hasWater / status
    // ========================================================================
    private List<TestFieldDataDTO.ValveItem> buildValveItems(ExternalIotAggregate iot) {
        if (iot == null || iot.getValveItems() == null || iot.getValveItems().isEmpty()) {
            return Collections.emptyList();
        }
        return iot.getValveItems();
    }

    private TestFieldDataDTO.WaterMeterItem buildWaterMeterItem(ExternalIotAggregate iot) {
        if (iot == null || iot.getWaterMeterItems() == null || iot.getWaterMeterItems().isEmpty()) return null;
        return iot.getWaterMeterItems().get(0);
    }

    private List<TestFieldDataDTO.CameraItem> buildCameras(ExternalIotAggregate iot) {
        if (iot == null || iot.getCameraDevices() == null || iot.getCameraDevices().isEmpty()) {
            return Collections.emptyList();
        }
        List<TestFieldDataDTO.CameraItem> result = new ArrayList<>();
        for (Map<String, Object> cam : iot.getCameraDevices()) {
            result.add(TestFieldDataDTO.CameraItem.builder()
                    .deviceId(toStringNull(cam.get("id")))
                    .name(toStringNull(cam.get("name")))
                    .enable(cam.get("enable"))
                    .status(cam.get("status"))
                    .gbId(toStringNull(cam.get("gb_id")))
                    .httpsFlvUrl(trimUrl(cam.get("https_flv_url")))
                    .stationName(toStringNull(cam.get("station_name")))
                    .deviceTypeName(toStringNull(cam.get("device_type_name")))
                    .build());
        }
        return result;
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

    // ========================================================================
    // 虫情
    // ========================================================================
    private TestFieldDataDTO.InsectData buildInsectData(Long facilityId) {
        String imei = resolveInsectImei(facilityId);
        List<TestFieldDataDTO.InsectStatItem> statistics = new ArrayList<>();
        Object latestRecord = null;

        try {
            List<com.weiming.smartag.entity.InsectData> list;
            if (imei != null && !imei.trim().isEmpty()) {
                list = insectDataMapper.selectList(
                        new LambdaQueryWrapper<com.weiming.smartag.entity.InsectData>()
                                .eq(com.weiming.smartag.entity.InsectData::getImei, imei)
                                .orderByDesc(com.weiming.smartag.entity.InsectData::getRecordTime)
                                .last("LIMIT 100"));
            } else {
                list = insectDataMapper.selectList(
                        new LambdaQueryWrapper<com.weiming.smartag.entity.InsectData>()
                                .orderByDesc(com.weiming.smartag.entity.InsectData::getRecordTime)
                                .last("LIMIT 100"));
            }

            if (list != null && !list.isEmpty()) {
                latestRecord = list.get(0);
                Map<String, Integer> typeCountMap = new HashMap<>();
                for (com.weiming.smartag.entity.InsectData item : list) {
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

                for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
                    statistics.add(TestFieldDataDTO.InsectStatItem.builder()
                            .name(entry.getKey())
                            .count(entry.getValue())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("查询虫情数据失败, facilityId={}", facilityId, e);
        }

        return TestFieldDataDTO.InsectData.builder()
                .latestRecord(latestRecord)
                .statistics(statistics)
                .build();
    }

    @SuppressWarnings("unchecked")
    private String resolveInsectImei(Long facilityId) {
        if (facilityId == null) return null;
        try {
            Map<String, Object> config = menuModelConfigService.getConfig();
            List<Map<String, Object>> menus = (List<Map<String, Object>>) config.get("menus");
            if (menus == null) return null;
            for (Map<String, Object> topMenu : menus) {
                Object children = topMenu.get("children");
                if (!(children instanceof List)) continue;
                for (Object childRaw : (List<?>) children) {
                    if (!(childRaw instanceof Map)) continue;
                    Map<String, Object> child = (Map<String, Object>) childRaw;
                    Object fid = child.get("facilityId");
                    if (fid != null && String.valueOf(fid).equals(String.valueOf(facilityId))) {
                        Object imei = child.get("insectDeviceId");
                        return imei != null ? String.valueOf(imei) : null;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析菜单配置获取虫情设备号失败, facilityId={}", facilityId, e);
        }
        return null;
    }

    // ========================================================================
    // 外部 IoT 拉取：
    //   1) listAllDevices 获取完整设备列表，并按配置的 ids / stationName 过滤
    //   2) 对每个排水阀 / 水位计分别调用 deviceValues，拿到真实实时值后按设备返回
    // ========================================================================
    private ExternalIotAggregate fetchExternalIotData(String baseUrl, String token, IotProperties.CameraFarm farmCfg) {
        if (baseUrl == null || token == null || farmCfg == null) return null;

        try {
            // 1) listAllDevices
            String listUrl = appendPath(baseUrl, "listAllDevices");
            Map<String, Object> listPayload = new LinkedHashMap<>();
            listPayload.put("token", token);
            String listResp = postJson(listUrl, listPayload);
            if (listResp == null) return null;

            Map<String, Object> listJson = objectMapper.readValue(listResp,
                    new TypeReference<Map<String, Object>>() {});
            Object rawCode = listJson.get("code");
            if (rawCode == null || !"0".equals(String.valueOf(rawCode))) return null;

            Object rawData = listJson.get("data");
            if (!(rawData instanceof List)) return null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allDevices = (List<Map<String, Object>>) rawData;

            // 2) 按配置筛选
            List<String> cameraIds = new ArrayList<>();
            if (farmCfg.getIds() != null) {
                for (String id : farmCfg.getIds()) {
                    if (id != null && !id.trim().isEmpty()) cameraIds.add(id.trim());
                }
            }
            Set<String> cameraIdSet = new HashSet<>(cameraIds);
            String stationKeyword = emptyToNull(farmCfg.getStationName());

            List<Map<String, Object>> cameraDevices = new ArrayList<>();
            List<Map<String, Object>> valveDevices = new ArrayList<>();
            List<Map<String, Object>> waterMeterDevices = new ArrayList<>();

            for (Map<String, Object> device : allDevices) {
                if (device == null) continue;
                Object rawType = device.get("device_type");
                int deviceType;
                try {
                    deviceType = rawType == null ? -1 : Integer.parseInt(String.valueOf(rawType));
                } catch (Exception ignored) { deviceType = -1; }

                if (deviceType == DEVICE_TYPE_CAMERA) {
                    if (!cameraIdSet.isEmpty() && device.get("id") != null
                            && cameraIdSet.contains(String.valueOf(device.get("id")).trim())) {
                        cameraDevices.add(device);
                    }
                    continue;
                }

                if (stationKeyword == null) continue;
                Object station = device.get("station_name");
                if (station == null) continue;
                String stationName = String.valueOf(station);
                if (!stationName.contains(stationKeyword)) continue;

                if (deviceType == DEVICE_TYPE_VALVE)              valveDevices.add(device);
                else if (deviceType == DEVICE_TYPE_WATER_LEVEL)    waterMeterDevices.add(device);
            }

            log.info("[试验田-IoT] 农场={} 过滤后：摄像头={} 个, 排水阀={} 个, 水位计={} 个",
                    farmCfg.getName(), cameraDevices.size(), valveDevices.size(), waterMeterDevices.size());

            ExternalIotAggregate result = new ExternalIotAggregate();
            result.setCameraDevices(cameraDevices);

            // 3) 对每个排水阀单独调 deviceValues 拿真实实时值
            List<TestFieldDataDTO.ValveItem> valveItems = new ArrayList<>();
            for (Map<String, Object> valve : valveDevices) {
                Object id = valve.get("id");
                if (id == null) continue;
                String deviceId = String.valueOf(id);
                Map<String, Object> values = fetchDeviceValues(baseUrl, token, deviceId);
                if (values == null) {
                    valveItems.add(TestFieldDataDTO.ValveItem.builder()
                            .deviceId(deviceId)
                            .name(toStringNull(valve.get("name")))
                            .stationName(toStringNull(valve.get("station_name")))
                            .build());
                    continue;
                }
                valveItems.add(TestFieldDataDTO.ValveItem.builder()
                        .deviceId(deviceId)
                        .name(toStringNull(valve.get("name")))
                        .stationName(toStringNull(valve.get("station_name")))
                        .pressure1(toBigDecimal(values.get("pressure1")))
                        .pressure2(toBigDecimal(values.get("pressure2")))
                        .pos(toBigDecimal(values.get("pos")))
                        .current(toBigDecimal(values.get("i")))
                        .voltage(toBigDecimal(values.get("v")))
                        .protectTorque(toBigDecimal(values.get("protectTorque")))
                        .status(values.get("status") == null ? null : String.valueOf(values.get("status")))
                        .build());
            }

            // 4) 对每个水位计单独调 deviceValues 拿真实实时值
            List<TestFieldDataDTO.WaterMeterItem> waterMeterItems = new ArrayList<>();
            for (Map<String, Object> meter : waterMeterDevices) {
                Object id = meter.get("id");
                if (id == null) continue;
                String deviceId = String.valueOf(id);
                Map<String, Object> values = fetchDeviceValues(baseUrl, token, deviceId);
                if (values == null) {
                    waterMeterItems.add(TestFieldDataDTO.WaterMeterItem.builder()
                            .deviceId(deviceId)
                            .name(toStringNull(meter.get("name")))
                            .stationName(toStringNull(meter.get("station_name")))
                            .build());
                    continue;
                }
                waterMeterItems.add(TestFieldDataDTO.WaterMeterItem.builder()
                        .deviceId(deviceId)
                        .name(toStringNull(meter.get("name")))
                        .stationName(toStringNull(meter.get("station_name")))
                        .waterLevel(toBigDecimal(values.get("waterLevel")))
                        .hasWater(values.get("hasWater"))
                        .status(values.get("status") == null ? null : String.valueOf(values.get("status")))
                        .build());
            }

            result.setValveItems(valveItems);
            result.setWaterMeterItems(waterMeterItems);
            return result;
        } catch (Exception e) {
            log.error("对接外部 IoT 接口失败, farm={}", farmCfg == null ? null : farmCfg.getName(), e);
            return null;
        }
    }

    private Map<String, Object> fetchDeviceValues(String baseUrl, String token, String deviceId) {
        try {
            String url = appendPath(baseUrl, "deviceValues");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("deviceId", deviceId);
            payload.put("token", token);
            String resp = postJson(url, payload);
            if (resp == null) return null;

            Map<String, Object> json = objectMapper.readValue(resp,
                    new TypeReference<Map<String, Object>>() {});
            Object code = json.get("code");
            if (code == null || !"0".equals(String.valueOf(code))) return null;

            Object data = json.get("data");
            if (!(data instanceof Map)) return new HashMap<>();

            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            return dataMap;
        } catch (Exception e) {
            log.warn("获取设备实时值失败, deviceId={}", deviceId, e);
            return null;
        }
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

    private static String appendPath(String base, String path) {
        if (base == null || base.isEmpty()) return path;
        return base.endsWith("/") ? (base + path) : (base + "/" + path);
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        try {
            double d = Double.parseDouble(String.valueOf(o));
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return BigDecimal.valueOf(d);
        } catch (Exception e) {
            return null;
        }
    }

    /** 外部 IoT 聚合中间对象（摄像头保留 map，排水阀 / 水位计直接用 DTO 列表） */
    private static class ExternalIotAggregate {
        private List<Map<String, Object>> cameraDevices;
        private List<TestFieldDataDTO.ValveItem> valveItems;
        private List<TestFieldDataDTO.WaterMeterItem> waterMeterItems;

        public List<Map<String, Object>> getCameraDevices() { return cameraDevices; }
        public void setCameraDevices(List<Map<String, Object>> v) { this.cameraDevices = v; }
        public List<TestFieldDataDTO.ValveItem> getValveItems() { return valveItems; }
        public void setValveItems(List<TestFieldDataDTO.ValveItem> v) { this.valveItems = v; }
        public List<TestFieldDataDTO.WaterMeterItem> getWaterMeterItems() { return waterMeterItems; }
        public void setWaterMeterItems(List<TestFieldDataDTO.WaterMeterItem> v) { this.waterMeterItems = v; }
    }
}
