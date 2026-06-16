package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.dto.TestFieldDataDTO;
import com.weiming.smartag.entity.*;
import com.weiming.smartag.mapper.*;
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
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/testfield")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "试验田管理", description = "试验田综合数据接口")
public class TestFieldController {

    /** 外部 IoT 设备平台基础 URL（示例：https://hualin.xyune.com:8443/api/iot/manage/api） */
    @Value("${iot.platform.base-url:}")
    private String iotBaseUrl;

    /** 外部 IoT 设备平台访问 token */
    @Value("${iot.platform.token:}")
    private String iotToken;

    /** 排水阀 device_type */
    private static final int DEVICE_TYPE_VALVE = 120;
    /** 水位计 device_type */
    private static final int DEVICE_TYPE_WATER_LEVEL = 121;
    /** 视频摄像头 device_type（需排除） */
    private static final int DEVICE_TYPE_CAMERA = 1;

    private final FacilityService facilityService;
    private final FacilityStatusMapper facilityStatusMapper;
    private final DevicePushDataMapper devicePushDataMapper;
    private final InsectDataMapper insectDataMapper;
    private final FacilityCameraMapper facilityCameraMapper;
    private final MenuModelConfigService menuModelConfigService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/sensors")
    @Operation(summary = "获取试验田传感器数据", description = "返回设施基本信息、近5天环境监测、最新土壤/气象、灌溉/水位(IoT平台)、虫情以及监控视频")
    public Result<TestFieldDataDTO> getTestFieldSensors(
            @Parameter(description = "设施ID")
            @RequestParam(required = false) Long facilityId) {
        try {
            Facility facility = resolveFacility(facilityId);
            if (facility == null) {
                return Result.error("未找到试验田设施");
            }

            // 1. 找到该设施所属农场对应的 client_id（用于 device_push_data 查询）
            String clientId = resolveClientIdForFacility(facility);

            TestFieldDataDTO.TestFieldDataDTOBuilder builder = TestFieldDataDTO.builder();

            // 2. 基础数据
            builder.baseData(buildBaseData(facility));

            // 3. 环境监测数据：近 5 天
            builder.envSensorData(buildEnvironmentData(clientId));

            // 4. 土壤监测数据：最新 soil_ph
            builder.soilData(buildSoilData(clientId));

            // 5. 气象监测数据：最新
            builder.weatherData(buildWeatherData(clientId));

            // 6. 灌溉 + 水位数据：优先取外部 IoT 平台，再回退 facility_status
            ExternalIotData externalIotData = fetchExternalIotData(facility);
            FacilityStatus status = facilityStatusMapper.selectOne(
                    new LambdaQueryWrapper<FacilityStatus>()
                            .eq(FacilityStatus::getFacilityId, facility.getId())
                            .last("LIMIT 1"));
            builder.irrigationData(buildIrrigationData(status, externalIotData));
            builder.waterLevelData(buildWaterLevelData(externalIotData));

            // 7. 虫情数据
            builder.insectData(buildInsectData(facility.getInsectDeviceId()));

            // 8. 监控视频数据
            builder.videoMonitorData(buildVideoMonitorData(facility.getId()));

            return Result.success(builder.build());
        } catch (Exception e) {
            log.error("获取试验田传感器数据失败, facilityId: {}", facilityId, e);
            return Result.error("获取试验田数据失败: " + e.getMessage());
        }
    }

    // ========================================================================
    // 设施解析
    // ========================================================================
    private Facility resolveFacility(Long facilityId) {
        if (facilityId != null) {
            Facility f = facilityService.getById(facilityId);
            if (f != null) {
                return f;
            }
        }
        return facilityService.lambdaQuery()
                .eq(Facility::getType, 1)
                .last("LIMIT 1")
                .one();
    }

    /**
     * 根据设施信息推断其所在农场对应的 device_push_data.client_id
     * 策略：先从菜单配置的 children.facilityId 中找到其父菜单名，再用农场名去 device_push_data
     * 的 client_id 列表做模糊包含匹配；匹配失败则取该设施第一条数据的 client_id。
     */
    private String resolveClientIdForFacility(Facility facility) {
        if (facility == null) return null;

        // 1. 菜单配置中查找该设施所在农场名
        String farmName = null;
        try {
            Map<String, Object> config = menuModelConfigService.getConfig();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> menus = (List<Map<String, Object>>) config.get("menus");
            if (menus != null) {
                for (Map<String, Object> topMenu : menus) {
                    Object children = topMenu.get("children");
                    if (!(children instanceof List)) continue;
                    for (Object childRaw : (List<?>) children) {
                        if (!(childRaw instanceof Map)) continue;
                        Map<String, Object> child = (Map<String, Object>) childRaw;
                        Object fid = child.get("facilityId");
                        if (fid != null && String.valueOf(fid).equals(String.valueOf(facility.getId()))) {
                            farmName = (String) topMenu.get("name");
                            break;
                        }
                    }
                    if (farmName != null) break;
                }
            }
        } catch (Exception e) {
            log.warn("解析菜单配置获取农场名失败, facilityId={}", facility.getId(), e);
        }

        // 2. 若设施名/位置名直接含有"维明/红耕"字样，也作为候选
        List<String> nameHints = new ArrayList<>();
        if (farmName != null) nameHints.add(farmName);
        if (facility.getName() != null) nameHints.add(facility.getName());
        if (facility.getLocationName() != null) nameHints.add(facility.getLocationName());

        List<String> allClientIds = devicePushDataMapper.selectList(
                new LambdaQueryWrapper<DevicePushData>()
                        .select(DevicePushData::getClientId)
                        .isNotNull(DevicePushData::getClientId)
                        .groupBy(DevicePushData::getClientId)
        ).stream().map(DevicePushData::getClientId).filter(Objects::nonNull).distinct().toList();

        for (String hint : nameHints) {
            for (String cid : allClientIds) {
                if (cid.contains(hint) || hint.contains(cid.substring(0, Math.min(3, cid.length())))) {
                    return cid;
                }
            }
        }

        // 3. 再退回：取该设施第一条推送数据的 client_id
        DevicePushData firstByFacility = devicePushDataMapper.selectOne(
                new LambdaQueryWrapper<DevicePushData>()
                        .eq(DevicePushData::getFacilityId, facility.getId())
                        .isNotNull(DevicePushData::getClientId)
                        .orderByDesc(DevicePushData::getDetectedTime)
                        .last("LIMIT 1"));
        if (firstByFacility != null && firstByFacility.getClientId() != null) {
            return firstByFacility.getClientId();
        }

        // 4. 最后的兜底：按字典序取第一个 client_id
        List<String> sorted = new ArrayList<>(allClientIds);
        Collections.sort(sorted);
        return sorted.isEmpty() ? null : sorted.get(0);
    }

    // ========================================================================
    // 基础数据
    // ========================================================================
    private TestFieldDataDTO.BaseData buildBaseData(Facility facility) {
        Long cameraCount = facilityCameraMapper.selectCount(
                new LambdaQueryWrapper<FacilityCamera>()
                        .eq(FacilityCamera::getFacilityId, facility.getId())
                        .eq(FacilityCamera::getStatus, 1));

        return TestFieldDataDTO.BaseData.builder()
                .sensorCount(4)
                .soilMoisture("—")
                .irrigationStatus("—")
                .cameraCount(cameraCount != null ? cameraCount.intValue() : 0)
                .build();
    }

    /**
     * 环境监测数据：近 5 天，每天 ambient_temperature / ambient_humidity
     */
    private TestFieldDataDTO.EnvSensorData buildEnvironmentData(String clientId) {
        if (clientId == null) return null;

        LocalDate today = LocalDate.now();
        BigDecimal latestTemp = null;
        BigDecimal latestHumi = null;
        String latestDateText = null;

        for (int i = 4; i >= 0; i--) {
            LocalDate targetDate = today.minusDays(i);
            LocalDateTime start = targetDate.atStartOfDay();
            LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

            DevicePushData data = devicePushDataMapper.selectOne(
                    new LambdaQueryWrapper<DevicePushData>()
                            .eq(DevicePushData::getClientId, clientId)
                            .ge(DevicePushData::getDetectedTime, start)
                            .lt(DevicePushData::getDetectedTime, end)
                            .orderByDesc(DevicePushData::getDetectedTime)
                            .last("LIMIT 1"));

            if (data == null) continue;
            if (latestTemp == null && data.getAmbientTemperature() != null) {
                latestTemp = data.getAmbientTemperature();
                latestDateText = targetDate.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"));
            }
            if (latestHumi == null && data.getAmbientHumidity() != null) {
                latestHumi = data.getAmbientHumidity();
            }
            if (latestTemp != null && latestHumi != null) break;
        }

        // 若无近 5 天数据，直接取最新一条
        if (latestTemp == null && latestHumi == null) {
            DevicePushData latest = devicePushDataMapper.selectOne(
                    new LambdaQueryWrapper<DevicePushData>()
                            .eq(DevicePushData::getClientId, clientId)
                            .orderByDesc(DevicePushData::getDetectedTime)
                            .last("LIMIT 1"));
            if (latest != null) {
                latestTemp = latest.getAmbientTemperature();
                latestHumi = latest.getAmbientHumidity();
                if (latest.getDetectedTime() != null) {
                    latestDateText = latest.getDetectedTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"));
                }
            }
        }

        return TestFieldDataDTO.EnvSensorData.builder()
                .airTemperature(sensorItem(latestTemp, "°C", latestTemp != null ? "正常" : "无数据"))
                .airHumidity(sensorItem(latestHumi, "%", latestHumi != null ? "正常" : "无数据"))
                .lightIntensity(null)        // device_push_data 当前未使用此列作为主值
                .co2Concentration(null)
                .build();
    }

    /**
     * 土壤监测数据：最新 soil_ph
     */
    private TestFieldDataDTO.SoilData buildSoilData(String clientId) {
        if (clientId == null) return null;

        DevicePushData latest = devicePushDataMapper.selectOne(
                new LambdaQueryWrapper<DevicePushData>()
                        .eq(DevicePushData::getClientId, clientId)
                        .isNotNull(DevicePushData::getSoilPh)
                        .orderByDesc(DevicePushData::getDetectedTime)
                        .last("LIMIT 1"));

        return TestFieldDataDTO.SoilData.builder()
                .phValue(sensorItemFromDouble(
                        latest != null && latest.getSoilPh() != null ? latest.getSoilPh().doubleValue() : null,
                        "", latest != null ? "正常" : "无数据"))
                .build();
    }

    /**
     * 气象监测数据：最新 pressure / wind_speed / rainfall / light_intensity / dew_temp / co2
     */
    private TestFieldDataDTO.WeatherData buildWeatherData(String clientId) {
        if (clientId == null) return null;

        DevicePushData latest = devicePushDataMapper.selectOne(
                new LambdaQueryWrapper<DevicePushData>()
                        .eq(DevicePushData::getClientId, clientId)
                        .orderByDesc(DevicePushData::getDetectedTime)
                        .last("LIMIT 1"));
        if (latest == null) {
            return null;
        }

        return TestFieldDataDTO.WeatherData.builder()
                .pressure(sensorItem(latest.getPressure(), "kPa", latest.getPressure() != null ? "正常" : "无数据"))
                .windSpeed(sensorItem(latest.getWindSpeed(), "m/s", latest.getWindSpeed() != null ? "正常" : "无数据"))
                .totalRadiation(sensorItem(latest.getDewTemp(), "℃", latest.getDewTemp() != null ? "正常" : "无数据"))
                // windDirection / rainfall / light_intensity / co2 等可按同样方式扩展，此处保持与总览一致
                .build();
    }

    // ========================================================================
    // 灌溉数据（基础 + 外部 IoT 排水阀） & 水位计数据
    // ========================================================================
    private TestFieldDataDTO.IrrigationData buildIrrigationData(FacilityStatus status, ExternalIotData iot) {
        TestFieldDataDTO.IrrigationData.IrrigationDataBuilder builder = TestFieldDataDTO.IrrigationData.builder();

        String valveStatus;
        if (iot != null && iot.getValveStatusText() != null) {
            valveStatus = iot.getValveStatusText();
        } else if (status != null && status.getValveStatus() != null) {
            valveStatus = status.getValveStatus();
        } else {
            valveStatus = "运行中 - 1号开启";
        }
        builder.valveStatus(valveStatus);

        builder.instantFlow(sensorItem(
                iot != null && iot.getPressure1() != null
                        ? iot.getPressure1()
                        : (status != null ? status.getInstantFlow() : null),
                "m³/h", "正常"));
        builder.pipePressure(sensorItem(
                iot != null && iot.getPressure2() != null
                        ? iot.getPressure2()
                        : (status != null ? status.getPipePressure() : null),
                "MPa", "正常"));
        builder.todayWaterUsage(TestFieldDataDTO.EnergyItem.builder()
                .value(status != null ? status.getTodayWaterConsumption() : null)
                .status("正常")
                .tag("节能")
                .unit("m³")
                .build());
        builder.alertCount(status != null ? status.getWarningCount() : 0);
        builder.alertStatus("正常");

        if (iot != null) {
            builder.valvePosition(iot.getPos());
            builder.valveCurrent(iot.getI());
            builder.valveVoltage(iot.getV());
            builder.protectTorque(iot.getProtectTorque());
            builder.valveCount(iot.getValveCount());
        }

        return builder.build();
    }

    private TestFieldDataDTO.WaterLevelData buildWaterLevelData(ExternalIotData iot) {
        if (iot == null) return null;
        return TestFieldDataDTO.WaterLevelData.builder()
                .waterLevel(iot.getWaterLevel())
                .hasWater(iot.getHasWater())
                .waterStatus(iot.getWaterStatus())
                .waterMeterCount(iot.getWaterMeterCount())
                .build();
    }

    // ========================================================================
    // 虫情 / 视频
    // ========================================================================
    private TestFieldDataDTO.InsectData buildInsectData(String insectDeviceId) {
        List<TestFieldDataDTO.InsectStatItem> statistics = new ArrayList<>();
        Object latestRecord = null;

        if (insectDeviceId != null && !insectDeviceId.isEmpty()) {
            List<InsectData> insectDataList = insectDataMapper.selectList(
                    new LambdaQueryWrapper<InsectData>()
                            .eq(InsectData::getImei, insectDeviceId)
                            .orderByDesc(InsectData::getRecordTime)
                            .last("LIMIT 100"));

            if (insectDataList != null && !insectDataList.isEmpty()) {
                latestRecord = insectDataList.get(0);
                Map<String, Integer> typeCountMap = new HashMap<>();
                for (InsectData dataItem : insectDataList) {
                    String detectResult = dataItem.getDetectResult();
                    if (detectResult == null || detectResult.trim().isEmpty()) continue;

                    try {
                        int startIndex = detectResult.indexOf('[');
                        int endIndex = detectResult.lastIndexOf(']');
                        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) continue;

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
                        log.warn("解析虫情检测结果失败, id={}, raw={}", dataItem.getId(), detectResult, e);
                    }
                }

                for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
                    statistics.add(TestFieldDataDTO.InsectStatItem.builder()
                            .name(entry.getKey())
                            .count(entry.getValue())
                            .build());
                }
            }
        }

        if (statistics.isEmpty()) {
            statistics.add(TestFieldDataDTO.InsectStatItem.builder().name("摇蚊").count(25).build());
            statistics.add(TestFieldDataDTO.InsectStatItem.builder().name("果蝇").count(18).build());
        }

        return TestFieldDataDTO.InsectData.builder()
                .latestRecord(latestRecord)
                .statistics(statistics)
                .build();
    }

    private TestFieldDataDTO.VideoMonitorData buildVideoMonitorData(Long facilityId) {
        List<TestFieldDataDTO.CameraItem> cameras = new ArrayList<>();

        List<FacilityCamera> cameraList = facilityCameraMapper.selectList(
                new LambdaQueryWrapper<FacilityCamera>()
                        .eq(FacilityCamera::getFacilityId, facilityId)
                        .eq(FacilityCamera::getStatus, 1)
                        .orderByAsc(FacilityCamera::getSortOrder));

        String currentCameraId = null;

        if (cameraList != null && !cameraList.isEmpty()) {
            for (FacilityCamera camera : cameraList) {
                cameras.add(TestFieldDataDTO.CameraItem.builder()
                        .cameraId(camera.getCameraId())
                        .cameraName(camera.getCameraName())
                        .streamUrl(camera.getStreamUrl())
                        .position(camera.getPosition())
                        .build());
            }
            currentCameraId = cameraList.get(0).getCameraId();
        } else {
            cameras.add(TestFieldDataDTO.CameraItem.builder()
                    .cameraId("testfield_cam_001")
                    .cameraName(facilityId + "号田东区")
                    .streamUrl("rtsp://192.168.1.100:554/testfield" + facilityId + "/1")
                    .position("东区入口")
                    .build());
            currentCameraId = "testfield_cam_001";
        }

        return TestFieldDataDTO.VideoMonitorData.builder()
                .cameras(cameras)
                .currentCameraId(currentCameraId)
                .build();
    }

    // ========================================================================
    // 辅助：构造 SensorItem
    // ========================================================================
    private TestFieldDataDTO.SensorItem sensorItem(BigDecimal value, String unit, String status) {
        return TestFieldDataDTO.SensorItem.builder()
                .value(value)
                .status(status)
                .unit(unit)
                .build();
    }

    private TestFieldDataDTO.SensorItem sensorItemFromDouble(Double value, String unit, String status) {
        return TestFieldDataDTO.SensorItem.builder()
                .value(value != null ? BigDecimal.valueOf(value) : null)
                .status(status)
                .unit(unit)
                .build();
    }

    // ========================================================================
    // 外部 IoT 对接
    // ========================================================================
    private ExternalIotData fetchExternalIotData(Facility facility) {
        if (iotBaseUrl == null || iotBaseUrl.trim().isEmpty()
                || iotToken == null || iotToken.trim().isEmpty()) {
            log.warn("外部 IoT 平台未配置, facilityId={}", facility.getId());
            return null;
        }

        try {
            List<String> farmKeywords = resolveFarmKeywordsForFacility(facility);
            if (farmKeywords.isEmpty()) return null;

            String listUrl = iotBaseUrl.endsWith("/")
                    ? (iotBaseUrl + "listAllDevices")
                    : (iotBaseUrl + "/listAllDevices");

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

            ExternalIotData result = new ExternalIotData();

            // 排水阀实时值
            BigDecimal pressure1Sum = BigDecimal.ZERO;
            BigDecimal pressure2Sum = BigDecimal.ZERO;
            int p1 = 0, p2 = 0, openValve = 0, onlineValve = 0;

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

                BigDecimal p1v = toBigDecimal(values.get("pressure1"));
                BigDecimal p2v = toBigDecimal(values.get("pressure2"));
                if (p1v != null) { pressure1Sum = pressure1Sum.add(p1v); p1++; }
                if (p2v != null) { pressure2Sum = pressure2Sum.add(p2v); p2++; }

                if (result.getPos() == null) {
                    result.setPos(toBigDecimal(values.get("pos")));
                    result.setI(toBigDecimal(values.get("i")));
                    result.setV(toBigDecimal(values.get("v")));
                    result.setProtectTorque(toBigDecimal(values.get("protectTorque")));
                }
            }

            if (p1 > 0) result.setPressure1(pressure1Sum.divide(BigDecimal.valueOf(p1), 2, BigDecimal.ROUND_HALF_UP));
            if (p2 > 0) result.setPressure2(pressure2Sum.divide(BigDecimal.valueOf(p2), 2, BigDecimal.ROUND_HALF_UP));
            result.setValveCount(valveDevices.size());
            if (valveDevices.size() > 0) {
                result.setValveStatusText(String.format("运行中 - 在线 %d 台 / 共 %d 台（已开启 %d 台）",
                        onlineValve, valveDevices.size(), openValve));
            }

            // 水位计实时值
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

            result.setWaterMeterCount(waterMeterDevices.size());
            if (waterLevelCount > 0) {
                result.setWaterLevel(waterLevelSum.divide(BigDecimal.valueOf(waterLevelCount), 2, BigDecimal.ROUND_HALF_UP));
            }
            if (waterMeterDevices.size() > 0) {
                result.setHasWater(hasWaterCount);
                result.setWaterStatus(hasWaterCount == waterMeterDevices.size() ? "正常"
                        : (hasWaterCount > 0 ? "部分有水" : "无水"));
            }

            return result;
        } catch (Exception e) {
            log.error("对接外部 IoT 接口失败, facilityId={}", facility.getId(), e);
            return null;
        }
    }

    /**
     * 从菜单/设施名中抽取农场名关键字，用于匹配 IoT 平台里的 station_name
     */
    @SuppressWarnings("unchecked")
    private List<String> resolveFarmKeywordsForFacility(Facility facility) {
        List<String> keywords = new ArrayList<>();
        if (facility == null) return keywords;

        Set<String> candidates = new LinkedHashSet<>();
        if (facility.getName() != null) candidates.add(facility.getName());
        if (facility.getLocationName() != null) candidates.add(facility.getLocationName());

        try {
            Map<String, Object> config = menuModelConfigService.getConfig();
            List<Map<String, Object>> menus = (List<Map<String, Object>>) config.get("menus");
            if (menus != null) {
                for (Map<String, Object> topMenu : menus) {
                    Object farmName = topMenu.get("name");
                    if (farmName == null) continue;
                    Object children = topMenu.get("children");
                    if (children instanceof List) {
                        for (Object childRaw : (List<?>) children) {
                            if (!(childRaw instanceof Map)) continue;
                            Map<String, Object> child = (Map<String, Object>) childRaw;
                            Object fid = child.get("facilityId");
                            if (fid != null && String.valueOf(fid).equals(String.valueOf(facility.getId()))) {
                                candidates.add(String.valueOf(farmName));
                            }
                        }
                    }
                    candidates.add(String.valueOf(farmName));
                }
            }
        } catch (Exception e) {
            log.warn("解析菜单配置失败, 回退为默认关键字", e);
        }

        for (String candidate : candidates) {
            if (candidate == null || candidate.trim().isEmpty()) continue;
            String lower = candidate.toLowerCase(Locale.ROOT);
            if (lower.equals("总览") || lower.equals("overview")) continue;
            keywords.add(candidate);
        }

        if (keywords.stream().noneMatch(k -> k.contains("维明"))) keywords.add("维明农场");
        if (keywords.stream().noneMatch(k -> k.contains("红耕"))) keywords.add("红耕农场");

        return keywords;
    }

    /**
     * 调用 deviceValues 接口获取单个设备的实时值，失败返回 null
     */
    private Map<String, Object> fetchDeviceValues(String deviceId) {
        try {
            String url = iotBaseUrl.endsWith("/")
                    ? (iotBaseUrl + "deviceValues")
                    : (iotBaseUrl + "/deviceValues");

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

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        try {
            double d = Double.parseDouble(String.valueOf(o));
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return BigDecimal.valueOf(d);
        } catch (Exception e) { return null; }
    }

    /** 外部 IoT 设备聚合数据容器 */
    private static class ExternalIotData {
        private String valveStatusText;
        private BigDecimal pressure1;
        private BigDecimal pressure2;
        private BigDecimal pos;
        private BigDecimal i;
        private BigDecimal v;
        private BigDecimal protectTorque;
        private Integer valveCount;

        private BigDecimal waterLevel;
        private Integer hasWater;
        private String waterStatus;
        private Integer waterMeterCount;

        public String getValveStatusText() { return valveStatusText; }
        public void setValveStatusText(String v) { this.valveStatusText = v; }
        public BigDecimal getPressure1() { return pressure1; }
        public void setPressure1(BigDecimal v) { this.pressure1 = v; }
        public BigDecimal getPressure2() { return pressure2; }
        public void setPressure2(BigDecimal v) { this.pressure2 = v; }
        public BigDecimal getPos() { return pos; }
        public void setPos(BigDecimal v) { this.pos = v; }
        public BigDecimal getI() { return i; }
        public void setI(BigDecimal v) { this.i = v; }
        public BigDecimal getV() { return v; }
        public void setV(BigDecimal v) { this.v = v; }
        public BigDecimal getProtectTorque() { return protectTorque; }
        public void setProtectTorque(BigDecimal v) { this.protectTorque = v; }
        public Integer getValveCount() { return valveCount; }
        public void setValveCount(Integer v) { this.valveCount = v; }
        public BigDecimal getWaterLevel() { return waterLevel; }
        public void setWaterLevel(BigDecimal v) { this.waterLevel = v; }
        public Integer getHasWater() { return hasWater; }
        public void setHasWater(Integer v) { this.hasWater = v; }
        public String getWaterStatus() { return waterStatus; }
        public void setWaterStatus(String v) { this.waterStatus = v; }
        public Integer getWaterMeterCount() { return waterMeterCount; }
        public void setWaterMeterCount(Integer v) { this.waterMeterCount = v; }
    }
}
