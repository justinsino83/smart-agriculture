package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.dto.TestFieldDataDTO;
import com.weiming.smartag.entity.*;
import com.weiming.smartag.mapper.*;
import com.weiming.smartag.service.FacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/testfield")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "试验田管理", description = "试验田综合数据接口")
public class TestFieldController {

    private final FacilityService facilityService;
    private final FacilityRealtimeDataMapper facilityRealtimeDataMapper;
    private final FacilityStatusMapper facilityStatusMapper;
    private final SoilDataMapper soilDataMapper;
    private final WeatherDataMapper weatherDataMapper;
    private final InsectDataMapper insectDataMapper;
    private final FacilityCameraMapper facilityCameraMapper;
    private final ObjectMapper objectMapper;

    @GetMapping("/sensors")
    @Operation(summary = "获取试验田传感器数据", description = "获取试验田的环境监测、土壤数据、气象数据、灌溉数据、虫情数据、监控视频等综合数据")
    public Result<TestFieldDataDTO> getTestFieldSensors(
            @Parameter(description = "设施ID")
            @RequestParam(required = false) Long facilityId) {
        try {
            // 获取设施信息
            Facility facility = null;
            if (facilityId != null) {
                facility = facilityService.getById(facilityId);
            }
            if (facility == null) {
                facility = facilityService.lambdaQuery()
                        .eq(Facility::getType, 1)
                        .last("LIMIT 1")
                        .one();
            }

            if (facility == null) {
                return Result.error("未找到试验田设施");
            }

            // 构建返回数据
            TestFieldDataDTO.TestFieldDataDTOBuilder builder = TestFieldDataDTO.builder();

            // 1. 基本数据
            builder.baseData(buildBaseData(facility));

            // 2. 获取设施实时传感器数据
            FacilityRealtimeData realtimeData = facilityRealtimeDataMapper.selectOne(
                    new LambdaQueryWrapper<FacilityRealtimeData>()
                            .eq(FacilityRealtimeData::getFacilityId, facility.getId())
                            .orderByDesc(FacilityRealtimeData::getCollectTime)
                            .last("LIMIT 1"));

            builder.envSensorData(buildEnvSensorData(realtimeData));

            // 3. 获取土壤数据
            SoilData soilData = soilDataMapper.selectOne(
                    new LambdaQueryWrapper<SoilData>()
                            .eq(SoilData::getFacilityId, facility.getId())
                            .orderByDesc(SoilData::getCollectTime)
                            .last("LIMIT 1"));

            builder.soilData(buildSoilData(soilData));

            // 4. 获取气象数据
            WeatherData weatherData = weatherDataMapper.selectOne(
                    new LambdaQueryWrapper<WeatherData>()
                            .eq(WeatherData::getFacilityId, facility.getId())
                            .orderByDesc(WeatherData::getCollectTime)
                            .last("LIMIT 1"));

            builder.weatherData(buildWeatherData(weatherData));

            // 5. 获取设施状态（灌溉数据）
            FacilityStatus status = facilityStatusMapper.selectOne(
                    new LambdaQueryWrapper<FacilityStatus>()
                            .eq(FacilityStatus::getFacilityId, facility.getId())
                            .last("LIMIT 1"));

            builder.irrigationData(buildIrrigationData(status));

            // 6. 获取虫情数据
            builder.insectData(buildInsectData(facility.getInsectDeviceId()));

            // 7. 获取监控视频数据
            builder.videoMonitorData(buildVideoMonitorData(facility.getId()));

            return Result.success(builder.build());

        } catch (Exception e) {
            log.error("获取试验田传感器数据失败, facilityId: {}", facilityId, e);
            return Result.error("获取试验田数据失败: " + e.getMessage());
        }
    }

    private TestFieldDataDTO.BaseData buildBaseData(Facility facility) {
        // 获取摄像头数量
        Long cameraCount = facilityCameraMapper.selectCount(
                new LambdaQueryWrapper<FacilityCamera>()
                        .eq(FacilityCamera::getFacilityId, facility.getId())
                        .eq(FacilityCamera::getStatus, 1));

        // 获取土壤数据
        SoilData soilData = soilDataMapper.selectOne(
                new LambdaQueryWrapper<SoilData>()
                        .eq(SoilData::getFacilityId, facility.getId())
                        .orderByDesc(SoilData::getCollectTime)
                        .last("LIMIT 1"));

        return TestFieldDataDTO.BaseData.builder()
                .sensorCount(4) // 默认4个传感器
                .soilMoisture((soilData != null && soilData.getMoisture() != null
                        ? soilData.getMoisture() : 26.57) + "%")
                .irrigationStatus("1号开启") // 默认状态
                .cameraCount(cameraCount != null ? cameraCount.intValue() : 0)
                .build();
    }

    private TestFieldDataDTO.EnvSensorData buildEnvSensorData(FacilityRealtimeData realtimeData) {
        return TestFieldDataDTO.EnvSensorData.builder()
                .airTemperature(buildSensorItem(
                        realtimeData != null && realtimeData.getAirTemperature() != null
                                ? realtimeData.getAirTemperature() : new BigDecimal("16.75"),
                        "°C",
                        "正常"))
                .airHumidity(buildSensorItem(
                        realtimeData != null && realtimeData.getAirHumidity() != null
                                ? realtimeData.getAirHumidity() : new BigDecimal("68"),
                        "%",
                        "正常"))
                .lightIntensity(buildSensorItem(
                        realtimeData != null && realtimeData.getLightIntensity() != null
                                ? realtimeData.getLightIntensity() : new BigDecimal("83909"),
                        "lux",
                        "充足"))
                .co2Concentration(buildSensorItem(
                        realtimeData != null && realtimeData.getCo2Concentration() != null
                                ? realtimeData.getCo2Concentration() : new BigDecimal("421"),
                        "ppm",
                        "正常"))
                .build();
    }

    private TestFieldDataDTO.SoilData buildSoilData(SoilData soilData) {
        return TestFieldDataDTO.SoilData.builder()
                .soilTemperature(buildSensorItemFromDouble(
                        soilData != null && soilData.getTemperature() != null
                                ? soilData.getTemperature() : 10.89,
                        "°C",
                        "正常"))
                .soilMoisture(buildSensorItemFromDouble(
                        soilData != null && soilData.getMoisture() != null
                                ? soilData.getMoisture() : 26.57,
                        "%",
                        "正常"))
                .phValue(buildSensorItemFromDouble(
                        soilData != null && soilData.getPh() != null
                                ? soilData.getPh() : 6.8,
                        "",
                        "正常"))
                .soilConductivity(buildSensorItemFromDouble(
                        soilData != null && soilData.getEc() != null
                                ? soilData.getEc() : 0.45,
                        "mS/cm",
                        "正常"))
                .nitrogen(buildSensorItemFromDouble(
                        soilData != null && soilData.getNitrogen() != null
                                ? soilData.getNitrogen() : 120.5,
                        "mg/kg",
                        "正常"))
                .phosphorus(buildSensorItemFromDouble(
                        soilData != null && soilData.getPhosphorus() != null
                                ? soilData.getPhosphorus() : 45.8,
                        "mg/kg",
                        "正常"))
                .potassium(buildSensorItemFromDouble(
                        soilData != null && soilData.getPotassium() != null
                                ? soilData.getPotassium() : 180.3,
                        "mg/kg",
                        "正常"))
                .build();
    }

    private TestFieldDataDTO.WeatherData buildWeatherData(WeatherData weatherData) {
        return TestFieldDataDTO.WeatherData.builder()
                .temperature(buildSensorItemFromDouble(
                        weatherData != null && weatherData.getTemperature() != null
                                ? weatherData.getTemperature() : 26.5,
                        "°C",
                        "正常"))
                .humidity(buildSensorItemFromDouble(
                        weatherData != null && weatherData.getHumidity() != null
                                ? weatherData.getHumidity() : 65,
                        "%",
                        "正常"))
                .windSpeed(buildSensorItemFromDouble(
                        weatherData != null && weatherData.getWindSpeed() != null
                                ? weatherData.getWindSpeed() : 0.5,
                        "m/s",
                        "正常"))
                .windDirection(weatherData != null && weatherData.getWindDirectionText() != null
                        ? weatherData.getWindDirectionText() : "东北")
                .pressure(buildSensorItemFromDouble(
                        weatherData != null && weatherData.getPressure() != null
                                ? weatherData.getPressure() : 1014.32,
                        "hPa",
                        "正常"))
                .totalRadiation(buildSensorItemFromDouble(
                        weatherData != null && weatherData.getTotalRadiation() != null
                                ? weatherData.getTotalRadiation() : 8.67,
                        "W/m²",
                        "正常"))
                .build();
    }

    private TestFieldDataDTO.IrrigationData buildIrrigationData(FacilityStatus status) {
        return TestFieldDataDTO.IrrigationData.builder()
                .valveStatus("运行中 - 1号开启")
                .instantFlow(buildSensorItem(
                        status != null && status.getInstantFlow() != null
                                ? status.getInstantFlow() : new BigDecimal("12.4"),
                        "m³/h",
                        "正常"))
                .pipePressure(buildSensorItem(
                        status != null && status.getPipePressure() != null
                                ? status.getPipePressure() : new BigDecimal("0.31"),
                        "MPa",
                        "正常"))
                .todayWaterUsage(TestFieldDataDTO.EnergyItem.builder()
                        .value(status != null && status.getTodayWaterConsumption() != null
                                ? status.getTodayWaterConsumption() : new BigDecimal("18.6"))
                        .status("正常")
                        .tag("节能")
                        .unit("m³")
                        .build())
                .alertCount(status != null && status.getWarningCount() != null
                        ? status.getWarningCount() : 3)
                .alertStatus("正常")
                .build();
    }

    private TestFieldDataDTO.InsectData buildInsectData(String insectDeviceId) {
        List<TestFieldDataDTO.InsectStatItem> statistics = new ArrayList<>();
        Object latestRecord = null;

        if (insectDeviceId != null && !insectDeviceId.isEmpty()) {
            // 获取虫情数据
            List<InsectData> insectDataList = insectDataMapper.selectList(
                    new LambdaQueryWrapper<InsectData>()
                            .eq(InsectData::getImei, insectDeviceId)
                            .orderByDesc(InsectData::getRecordTime)
                            .last("LIMIT 100"));

            if (insectDataList != null && !insectDataList.isEmpty()) {
                latestRecord = insectDataList.get(0);

                // 统计虫情
                Map<String, Integer> typeCountMap = new HashMap<>();
                for (InsectData dataItem : insectDataList) {
                    String detectResult = dataItem.getDetectResult();
                    if (detectResult != null && !detectResult.isEmpty()) {
                        try {
                            // --- 参考OverviewController的终极暴力清洗与提取方案开始 ---

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
                }

                for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
                    statistics.add(TestFieldDataDTO.InsectStatItem.builder()
                            .name(entry.getKey())
                            .count(entry.getValue())
                            .build());
                }
            }
        }

        // 如果没有数据，使用默认模拟数据
        if (statistics.isEmpty()) {
            statistics.add(TestFieldDataDTO.InsectStatItem.builder().name("摇蚊").count(25).build());
            statistics.add(TestFieldDataDTO.InsectStatItem.builder().name("果蝇").count(18).build());
            statistics.add(TestFieldDataDTO.InsectStatItem.builder().name("虎甲").count(12).build());
        }

        return TestFieldDataDTO.InsectData.builder()
                .latestRecord(latestRecord)
                .statistics(statistics)
                .build();
    }

    private TestFieldDataDTO.VideoMonitorData buildVideoMonitorData(Long facilityId) {
        List<TestFieldDataDTO.CameraItem> cameras = new ArrayList<>();

        // 获取摄像头数据
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
            // 如果没有数据，使用默认模拟数据
            cameras.add(TestFieldDataDTO.CameraItem.builder()
                    .cameraId("testfield_cam_001")
                    .cameraName(facilityId + "号田东区")
                    .streamUrl("rtsp://192.168.1.100:554/testfield" + facilityId + "/1")
                    .position("东区入口")
                    .build());
            cameras.add(TestFieldDataDTO.CameraItem.builder()
                    .cameraId("testfield_cam_002")
                    .cameraName(facilityId + "号田中心")
                    .streamUrl("rtsp://192.168.1.100:554/testfield" + facilityId + "/2")
                    .position("中心位置")
                    .build());
            cameras.add(TestFieldDataDTO.CameraItem.builder()
                    .cameraId("testfield_cam_003")
                    .cameraName(facilityId + "号田西区")
                    .streamUrl("rtsp://192.168.1.100:554/testfield" + facilityId + "/3")
                    .position("西区灌溉点")
                    .build());
            currentCameraId = "testfield_cam_001";
        }

        return TestFieldDataDTO.VideoMonitorData.builder()
                .cameras(cameras)
                .currentCameraId(currentCameraId)
                .build();
    }

    private TestFieldDataDTO.SensorItem buildSensorItem(BigDecimal value, String unit, String status) {
        return TestFieldDataDTO.SensorItem.builder()
                .value(value)
                .status(status)
                .unit(unit)
                .build();
    }

    private TestFieldDataDTO.SensorItem buildSensorItemFromDouble(Double value, String unit, String status) {
        return TestFieldDataDTO.SensorItem.builder()
                .value(value != null ? BigDecimal.valueOf(value) : null)
                .status(status)
                .unit(unit)
                .build();
    }
}
