package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.dto.TestFieldDataDTO;
import com.weiming.smartag.entity.DevicePushData;
import com.weiming.smartag.mapper.DevicePushDataMapper;
import com.weiming.smartag.service.FacilityService;
import com.weiming.smartag.service.MenuModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    /** 视频摄像头 device_type（需排除） */
    private static final int DEVICE_TYPE_CAMERA = 1;

    @Value("${iot.platform.base-url:}")
    private String iotBaseUrl;

    @Value("${iot.platform.token:}")
    private String iotToken;

    private final FacilityService facilityService;
    private final DevicePushDataMapper devicePushDataMapper;
    private final InsectDataMapper insectDataMapper;
    private final MenuModelConfigService menuModelConfigService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/sensors")
    @Operation(summary = "获取试验田传感器数据", description = "基于总览数据结构，外加外部 IoT 平台的排水阀 / 水位计实时数据")
    public Result<TestFieldDataDTO> getTestFieldSensors(
            @Parameter(description = "设施ID")
            @RequestParam(required = false) Long facilityId) {
        try {
            // 1) 设施不存在时直接按 id 查询；不关心设施具体字段，仅保留对菜单配置中 facilityId → 农场名映射
            if (facilityId != null && facilityService.getById(facilityId) == null) {
                return Result.error("未找到试验田设施");
            }

            // 2) 匹配该设施所在农场对应的 device_push_data.client_id
            String clientId = resolveClientIdForFacility(facilityId);

            TestFieldDataDTO.TestFieldDataDTOBuilder builder = TestFieldDataDTO.builder();

            // 3) 环境监测：近5天 温度 / 湿度
            builder.environment(buildEnvironmentData(clientId));

            // 4) 土壤：最新 pH
            builder.soil(buildSoilData(clientId));

            // 5) 气象：最新 pressure / wind_speed / rainfall / light_intensity / dew_temp / co2
            builder.weather(buildWeatherData(clientId));

            // 6) 排水阀 & 水位计 实时数据
            ExternalIotAggregate iot = fetchExternalIotData(facilityId);
            builder.valve(buildValveData(iot));
            builder.waterMeter(buildWaterMeterData(iot));

            // 7) 虫情数据
            builder.insectData(buildInsectData(facilityId));

            return Result.success(builder.build());
        } catch (Exception e) {
            log.error("获取试验田传感器数据失败, facilityId: {}", facilityId, e);
            return Result.error("获取试验田数据失败: " + e.getMessage());
        }
    }

    // ========================================================================
    // 根据 facilityId 推断 device_push_data.client_id
    // 策略：先从菜单配置中找到包含该 facilityId 的子项 → 父菜单名 → 再去所有 clientId 中包含匹配；
    //      找不到则取所有 clientId 按字典序第一个
    // ========================================================================
    @SuppressWarnings("unchecked")
    private String resolveClientIdForFacility(Long facilityId) {
        String farmNameHint = null;
        try {
            Map<String, Object> config = menuModelConfigService.getConfig();
            List<Map<String, Object>> menus = (List<Map<String, Object>>) config.get("menus");
            if (menus != null) {
                for (Map<String, Object> topMenu : menus) {
                    Object children = topMenu.get("children");
                    if (!(children instanceof List)) continue;
                    for (Object childRaw : (List<?>) children) {
                        if (!(childRaw instanceof Map)) continue;
                        Map<String, Object> child = (Map<String, Object>) childRaw;
                        Object fid = child.get("facilityId");
                        if (fid != null && String.valueOf(fid).equals(String.valueOf(facilityId))) {
                            farmNameHint = (String) topMenu.get("name");
                            break;
                        }
                    }
                    if (farmNameHint != null) break;
                }
            }
        } catch (Exception e) {
            log.warn("解析菜单配置获取农场名失败, facilityId={}", facilityId, e);
        }

        List<String> allClientIds = devicePushDataMapper.selectList(
                new LambdaQueryWrapper<DevicePushData>()
                        .select(DevicePushData::getClientId)
                        .isNotNull(DevicePushData::getClientId)
                        .groupBy(DevicePushData::getClientId)
        ).stream().map(DevicePushData::getClientId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        if (farmNameHint != null) {
            for (String cid : allClientIds) {
                if (cid.contains(farmNameHint) || farmNameHint.contains(cid.substring(0, Math.min(3, cid.length())))) {
                    return cid;
                }
            }
        }

        if (!allClientIds.isEmpty()) {
            Collections.sort(allClientIds);
            return allClientIds.get(0);
        }
        return null;
    }

    // ========================================================================
    // 环境监测：近5天每天一条（温/湿）
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

            TestFieldDataDTO.EnvData item = TestFieldDataDTO.EnvData.builder()
                    .date(target.format(DATE_FMT))
                    .ambientTemperature(data != null ? data.getAmbientTemperature() : null)
                    .ambientHumidity(data != null ? data.getAmbientHumidity() : null)
                    .build();
            result.add(item);
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
    // 排水阀 / 水位计 数据封装
    // ========================================================================
    private TestFieldDataDTO.ValveData buildValveData(ExternalIotAggregate iot) {
        if (iot == null || iot.getValveDevices() == null || iot.getValveDevices().isEmpty()) return null;
        return TestFieldDataDTO.ValveData.builder()
                .status(iot.getValveStatusText())
                .pressure1(iot.getPressure1())
                .pressure2(iot.getPressure2())
                .pos(iot.getPos())
                .current(iot.getCurrent())
                .voltage(iot.getVoltage())
                .protectTorque(iot.getProtectTorque())
                .count(iot.getValveDevices().size())
                .build();
    }

    private TestFieldDataDTO.WaterMeterData buildWaterMeterData(ExternalIotAggregate iot) {
        if (iot == null || iot.getWaterMeterDevices() == null || iot.getWaterMeterDevices().isEmpty()) return null;
        return TestFieldDataDTO.WaterMeterData.builder()
                .waterLevel(iot.getWaterLevel())
                .hasWater(iot.getHasWater())
                .status(iot.getWaterStatus())
                .count(iot.getWaterMeterDevices().size())
                .build();
    }

    // ========================================================================
    // 虫情数据：解析 insect_data.detect_result 中 JSON 数组做虫名聚合
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

    /**
     * 尝试从菜单配置中找到当前 facilityId 对应节点的虫情设备号(insectDeviceId)；
     * 未找到时返回 null，此时会退化为按记录时间倒序的最近 100 条。
     */
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
    // 外部 IoT 拉取
    // ========================================================================
    private ExternalIotAggregate fetchExternalIotData(Long facilityId) {
        if (iotBaseUrl == null || iotBaseUrl.trim().isEmpty()
                || iotToken == null || iotToken.trim().isEmpty()) {
            return null;
        }

        try {
            List<String> farmKeywords = resolveFarmKeywordsForFacility(facilityId);
            if (farmKeywords.isEmpty()) return null;

            String listUrl = appendPath(iotBaseUrl, "listAllDevices");
            Map<String, Object> listPayload = Collections.singletonMap("token", iotToken);
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

            List<Map<String, Object>> valveDevices = new ArrayList<>();
            List<Map<String, Object>> waterMeterDevices = new ArrayList<>();
            for (Map<String, Object> device : allDevices) {
                Object rawType = device.get("device_type");
                Object station = device.get("station_name");
                if (rawType == null || station == null) continue;
                int deviceType;
                try {
                    deviceType = Integer.parseInt(String.valueOf(rawType));
                } catch (Exception ex) { continue; }
                if (deviceType == DEVICE_TYPE_CAMERA) continue;
                String stationName = String.valueOf(station);
                boolean matchFarm = farmKeywords.stream().anyMatch(stationName::contains);
                if (!matchFarm) continue;
                if (deviceType == DEVICE_TYPE_VALVE) valveDevices.add(device);
                else if (deviceType == DEVICE_TYPE_WATER_LEVEL) waterMeterDevices.add(device);
            }

            ExternalIotAggregate result = new ExternalIotAggregate();
            result.setValveDevices(valveDevices);
            result.setWaterMeterDevices(waterMeterDevices);

            // 聚合排水阀实时值
            BigDecimal pressure1Sum = BigDecimal.ZERO;
            BigDecimal pressure2Sum = BigDecimal.ZERO;
            int p1 = 0, p2 = 0, onlineValve = 0, openValve = 0;
            for (Map<String, Object> valve : valveDevices) {
                Object id = valve.get("id");
                if (id == null) continue;
                Map<String, Object> values = fetchDeviceValues(String.valueOf(id));
                if (values == null) continue;

                Object status = values.get("status");
                if (status != null && !"0".equals(String.valueOf(status))) {
                    onlineValve++;
                    openValve++;
                }

                BigDecimal v1 = toBigDecimal(values.get("pressure1"));
                BigDecimal v2 = toBigDecimal(values.get("pressure2"));
                if (v1 != null) { pressure1Sum = pressure1Sum.add(v1); p1++; }
                if (v2 != null) { pressure2Sum = pressure2Sum.add(v2); p2++; }

                if (result.getPos() == null) {
                    result.setPos(toBigDecimal(values.get("pos")));
                    result.setCurrent(toBigDecimal(values.get("i")));
                    result.setVoltage(toBigDecimal(values.get("v")));
                    result.setProtectTorque(toBigDecimal(values.get("protectTorque")));
                }
            }
            if (p1 > 0) result.setPressure1(round2(pressure1Sum.divide(BigDecimal.valueOf(p1), 4, BigDecimal.ROUND_HALF_UP)));
            if (p2 > 0) result.setPressure2(round2(pressure2Sum.divide(BigDecimal.valueOf(p2), 4, BigDecimal.ROUND_HALF_UP)));
            result.setValveStatusText(String.format("运行中 - 在线 %d 台 / 共 %d 台（已开启 %d 台）",
                    onlineValve, valveDevices.size(), openValve));

            // 聚合水位计实时值
            BigDecimal waterLevelSum = BigDecimal.ZERO;
            int waterLevelCount = 0;
            int hasWaterCount = 0;
            for (Map<String, Object> meter : waterMeterDevices) {
                Object id = meter.get("id");
                if (id == null) continue;
                Map<String, Object> values = fetchDeviceValues(String.valueOf(id));
                if (values == null) continue;

                BigDecimal wl = toBigDecimal(values.get("waterLevel"));
                if (wl != null) { waterLevelSum = waterLevelSum.add(wl); waterLevelCount++; }
                Object hasWater = values.get("hasWater");
                if (hasWater != null) {
                    try {
                        int hw = Integer.parseInt(String.valueOf(hasWater));
                        if (hw > 0) hasWaterCount++;
                    } catch (Exception ignored) {}
                }
            }
            if (waterLevelCount > 0) {
                result.setWaterLevel(round2(waterLevelSum.divide(BigDecimal.valueOf(waterLevelCount), 4, BigDecimal.ROUND_HALF_UP)));
            }
            result.setHasWater(hasWaterCount);
            if (!waterMeterDevices.isEmpty()) {
                result.setWaterStatus(hasWaterCount == waterMeterDevices.size() ? "正常"
                        : (hasWaterCount > 0 ? "部分有水" : "无水"));
            }

            return result;
        } catch (Exception e) {
            log.error("对接外部 IoT 接口失败, facilityId={}", facilityId, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveFarmKeywordsForFacility(Long facilityId) {
        List<String> keywords = new ArrayList<>();
        try {
            Map<String, Object> config = menuModelConfigService.getConfig();
            List<Map<String, Object>> menus = (List<Map<String, Object>>) config.get("menus");
            if (menus != null) {
                for (Map<String, Object> topMenu : menus) {
                    Object farmName = topMenu.get("name");
                    if (farmName == null) continue;
                    Object children = topMenu.get("children");
                    boolean facilityMatched = false;
                    if (children instanceof List) {
                        for (Object childRaw : (List<?>) children) {
                            if (!(childRaw instanceof Map)) continue;
                            Map<String, Object> child = (Map<String, Object>) childRaw;
                            Object fid = child.get("facilityId");
                            if (fid != null && String.valueOf(fid).equals(String.valueOf(facilityId))) {
                                facilityMatched = true;
                                break;
                            }
                        }
                    }
                    if (facilityMatched) {
                        keywords.add(0, String.valueOf(farmName));
                    } else {
                        keywords.add(String.valueOf(farmName));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析菜单配置失败", e);
        }

        // 保底关键字：如果 IoT 平台的 station_name 里直接用了"维明农场/红耕农场"字样
        if (keywords.stream().noneMatch(k -> k.contains("维明"))) keywords.add("维明农场");
        if (keywords.stream().noneMatch(k -> k.contains("红耕"))) keywords.add("红耕农场");
        return keywords;
    }

    private Map<String, Object> fetchDeviceValues(String deviceId) {
        try {
            String url = appendPath(iotBaseUrl, "deviceValues");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("deviceId", deviceId);
            payload.put("token", iotToken);
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

    private static BigDecimal round2(BigDecimal v) {
        if (v == null) return null;
        return v.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /** 外部 IoT 聚合中间对象 */
    private static class ExternalIotAggregate {
        private List<Map<String, Object>> valveDevices;
        private List<Map<String, Object>> waterMeterDevices;
        private String valveStatusText;
        private BigDecimal pressure1;
        private BigDecimal pressure2;
        private BigDecimal pos;
        private BigDecimal current;
        private BigDecimal voltage;
        private BigDecimal protectTorque;
        private BigDecimal waterLevel;
        private Integer hasWater;
        private String waterStatus;

        public List<Map<String, Object>> getValveDevices() { return valveDevices; }
        public void setValveDevices(List<Map<String, Object>> v) { this.valveDevices = v; }
        public List<Map<String, Object>> getWaterMeterDevices() { return waterMeterDevices; }
        public void setWaterMeterDevices(List<Map<String, Object>> v) { this.waterMeterDevices = v; }
        public String getValveStatusText() { return valveStatusText; }
        public void setValveStatusText(String v) { this.valveStatusText = v; }
        public BigDecimal getPressure1() { return pressure1; }
        public void setPressure1(BigDecimal v) { this.pressure1 = v; }
        public BigDecimal getPressure2() { return pressure2; }
        public void setPressure2(BigDecimal v) { this.pressure2 = v; }
        public BigDecimal getPos() { return pos; }
        public void setPos(BigDecimal v) { this.pos = v; }
        public BigDecimal getCurrent() { return current; }
        public void setCurrent(BigDecimal v) { this.current = v; }
        public BigDecimal getVoltage() { return voltage; }
        public void setVoltage(BigDecimal v) { this.voltage = v; }
        public BigDecimal getProtectTorque() { return protectTorque; }
        public void setProtectTorque(BigDecimal v) { this.protectTorque = v; }
        public BigDecimal getWaterLevel() { return waterLevel; }
        public void setWaterLevel(BigDecimal v) { this.waterLevel = v; }
        public Integer getHasWater() { return hasWater; }
        public void setHasWater(Integer v) { this.hasWater = v; }
        public String getWaterStatus() { return waterStatus; }
        public void setWaterStatus(String v) { this.waterStatus = v; }
    }
}
