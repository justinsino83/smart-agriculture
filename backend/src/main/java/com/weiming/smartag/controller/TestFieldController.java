package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.Facility;
import com.weiming.smartag.entity.FacilityRealtimeData;
import com.weiming.smartag.entity.FacilityStatus;
import com.weiming.smartag.entity.InsectData;
import com.weiming.smartag.entity.SoilData;
import com.weiming.smartag.entity.WeatherData;
import com.weiming.smartag.mapper.FacilityRealtimeDataMapper;
import com.weiming.smartag.mapper.FacilityStatusMapper;
import com.weiming.smartag.mapper.InsectDataMapper;
import com.weiming.smartag.mapper.SoilDataMapper;
import com.weiming.smartag.mapper.WeatherDataMapper;
import com.weiming.smartag.service.DevicePushService;
import com.weiming.smartag.service.FacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/testfield")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "试验田管理", description = "试验田数据管理接口")
public class TestFieldController {

    private final DevicePushService devicePushService;
    private final WeatherDataMapper weatherDataMapper;
    private final SoilDataMapper soilDataMapper;
    private final InsectDataMapper insectDataMapper;
    private final FacilityService facilityService;
    private final FacilityRealtimeDataMapper facilityRealtimeDataMapper;
    private final FacilityStatusMapper facilityStatusMapper;

    @GetMapping("/sensors")
    @Operation(summary = "获取试验田传感器数据", description = "获取试验田的环境监测、气象数据、灌溉控制等综合数据")
    public Result<Map<String, Object>> getTestFieldSensors(
            @Parameter(description = "设施ID")
            @RequestParam(required = false) Long facilityId) {
        try {
            Map<String, Object> result = new HashMap<>();

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
            result.put("facility", facility);

            if (facility != null) {
                // 环境数据
                Map<String, Object> envData = devicePushService.getDashboardOverview(null);
                result.put("environment", envData.get("environment"));

                // 实时传感器数据 - 从数据库读取
                FacilityRealtimeData realtimeData = facilityRealtimeDataMapper.selectOne(
                        new LambdaQueryWrapper<FacilityRealtimeData>()
                                .eq(FacilityRealtimeData::getFacilityId, facility.getId())
                                .orderByDesc(FacilityRealtimeData::getCollectTime)
                                .last("LIMIT 1")
                );
                if (realtimeData != null) {
                    Map<String, Object> realtimeSensor = new HashMap<>();
                    realtimeSensor.put("airTemperature", realtimeData.getAirTemperature());
                    realtimeSensor.put("airHumidity", realtimeData.getAirHumidity());
                    realtimeSensor.put("lightIntensity", realtimeData.getLightIntensity());
                    realtimeSensor.put("co2Concentration", realtimeData.getCo2Concentration());
                    realtimeSensor.put("soilTemperature", realtimeData.getSoilTemperature());
                    realtimeSensor.put("soilHumidity", realtimeData.getSoilHumidity());
                    realtimeSensor.put("soilPh", realtimeData.getSoilPh());
                    result.put("realtimeSensor", realtimeSensor);
                }

                // 设施状态 - 从数据库读取
                FacilityStatus status = facilityStatusMapper.selectOne(
                        new LambdaQueryWrapper<FacilityStatus>()
                                .eq(FacilityStatus::getFacilityId, facility.getId())
                                .last("LIMIT 1")
                );
                if (status != null) {
                    // 设施状态卡片
                    Map<String, Object> facilityStatusMap = new HashMap<>();
                    facilityStatusMap.put("transmissionProbability", status.getTransmissionProbability());
                    facilityStatusMap.put("irrigationStatus", status.getIrrigationStatus());
                    facilityStatusMap.put("warningCount", status.getWarningCount());
                    result.put("facilityStatus", facilityStatusMap);

                    // 气象信息
                    Map<String, Object> weatherInfo = new HashMap<>();
                    weatherInfo.put("windSpeed", status.getWindSpeed());
                    weatherInfo.put("windDirection", status.getWindDirection());
                    weatherInfo.put("totalIrrigation", status.getTotalIrrigation());
                    weatherInfo.put("airPressure", status.getAirPressure());
                    weatherInfo.put("totalRadiation", status.getTotalRadiation());
                    result.put("weatherInfo", weatherInfo);

                    // 灌溉控制
                    Map<String, Object> irrigationControl = new HashMap<>();
                    irrigationControl.put("valveStatus", status.getValveStatus());
                    irrigationControl.put("instantFlow", status.getInstantFlow());
                    irrigationControl.put("pipePressure", status.getPipePressure());
                    irrigationControl.put("todayWaterConsumption", status.getTodayWaterConsumption());
                    result.put("irrigationControl", irrigationControl);
                }
            }

            // 该设施的气象数据
            WeatherData latestWeather = null;
            if (facility != null) {
                latestWeather = weatherDataMapper.selectOne(
                        new LambdaQueryWrapper<WeatherData>()
                                .eq(WeatherData::getFacilityId, facility.getId())
                                .orderByDesc(WeatherData::getCollectTime)
                                .last("LIMIT 1")
                );
            }
            result.put("weather", latestWeather);

            // 该设施的土壤数据
            SoilData latestSoil = null;
            if (facility != null) {
                latestSoil = soilDataMapper.selectOne(
                        new LambdaQueryWrapper<SoilData>()
                                .eq(SoilData::getFacilityId, facility.getId())
                                .orderByDesc(SoilData::getCollectTime)
                                .last("LIMIT 1")
                );
            }
            result.put("soil", latestSoil);

            // 该设施的虫情数据
            List<InsectData> insectList = new ArrayList<>();
            if (facility != null) {
                insectList = insectDataMapper.selectList(
                        new LambdaQueryWrapper<InsectData>()
                                .eq(InsectData::getFacilityId, facility.getId())
                                .orderByDesc(InsectData::getRecordTime)
                                .last("LIMIT 5")
                );
            }
            result.put("insectData", insectList);
            result.put("insectCount", insectList.size());

            // 虫情检测
            Map<String, Object> insectDetection = new HashMap<>();
            insectDetection.put("channel1", "insect_camera_001");
            insectDetection.put("channel2", "insect_camera_002");
            result.put("insectDetection", insectDetection);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取试验田传感器数据失败, facilityId: {}", facilityId, e);
            return Result.fail("获取试验田数据失败: " + e.getMessage());
        }
    }
}