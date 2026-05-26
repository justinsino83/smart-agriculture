package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.Facility;
import com.weiming.smartag.entity.InsectData;
import com.weiming.smartag.entity.WeatherData;
import com.weiming.smartag.mapper.InsectDataMapper;
import com.weiming.smartag.mapper.WeatherDataMapper;
import com.weiming.smartag.service.DevicePushService;
import com.weiming.smartag.service.FacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @Operation(summary = "获取系统总览数据", description = "获取所有设施统计、列表及整体环境数据")
    public Result<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 1. 设施数据 - 从数据库读取
            Map<String, Object> facilityOverview = facilityService.getOverview();
            result.putAll(facilityOverview);
            
            // 2. 平台位置信息 - 从数据库读取第一个设施的位置作为平台位置
            Map<String, Object> platformLocation = new HashMap<>();
            // 获取第一个设施作为参考
            Facility firstFacility = facilityService.getOne(
                    new LambdaQueryWrapper<Facility>().last("LIMIT 1")
            );
            
            if (firstFacility != null && firstFacility.getLocationName() != null) {
                // 从设施的locationName中提取乡镇信息
                String locationName = firstFacility.getLocationName();
                // 如果locationName包含乡镇信息，提取出来
                String townName = locationName;
                if (locationName.contains("园区") || locationName.contains("仓储") || locationName.contains("烘干")) {
                    // 提取"泰兴市根思乡"部分
                    if (locationName.contains("泰兴市根思乡")) {
                        townName = "泰兴市根思乡";
                    } else {
                        // 默认使用完整的locationName
                        townName = locationName;
                    }
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
            result.put("platformLocation", platformLocation);
            
            // 3. 环境数据 - 从DevicePushService获取
            Map<String, Object> deviceData = devicePushService.getDashboardOverview(null);
            result.put("environment", deviceData.get("environment"));
            
            // 4. 气象数据 - 从数据库读取最新气象数据
            WeatherData latestWeather = weatherDataMapper.selectOne(
                    new LambdaQueryWrapper<WeatherData>()
                            .orderByDesc(WeatherData::getCollectTime)
                            .last("LIMIT 1")
            );
            if (latestWeather != null) {
                Map<String, Object> weather = new HashMap<>();
                weather.put("temperature", latestWeather.getTemperature());
                weather.put("humidity", latestWeather.getHumidity());
                weather.put("windSpeed", latestWeather.getWindSpeed());
                weather.put("windDirection", latestWeather.getWindDirection());
                weather.put("pressure", latestWeather.getPressure());
                weather.put("weatherCode", latestWeather.getWeatherCode());
                weather.put("collectTime", latestWeather.getCollectTime());
                result.put("weather", weather);
            } else {
                // 备用数据
                Map<String, Object> weather = new HashMap<>();
                weather.put("temperature", 25.5);
                weather.put("humidity", 65.0);
                weather.put("windSpeed", 2.5);
                weather.put("windDirection", 90.0);
                weather.put("pressure", 1013.2);
                weather.put("weatherCode", "sunny");
                result.put("weather", weather);
            }
            
            // 5. 虫情数据 - 从数据库读取最新虫情数据
            InsectData latestInsect = insectDataMapper.selectOne(
                    new LambdaQueryWrapper<InsectData>()
                            .orderByDesc(InsectData::getRecordTime)
                            .last("LIMIT 1")
            );
            if (latestInsect != null) {
                Map<String, Object> insect = new HashMap<>();
                insect.put("totalCount", latestInsect.getObjectCount());
                insect.put("detectResult", latestInsect.getDetectResult());
                insect.put("recordTime", latestInsect.getRecordTime());
                insect.put("imageUrl", latestInsect.getImageUrl());
                result.put("insect", insect);
            } else {
                // 备用数据
                Map<String, Object> insect = new HashMap<>();
                insect.put("totalCount", 15);
                insect.put("warningLevel", "正常");
                result.put("insect", insect);
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取总览数据失败", e);
            return Result.fail("获取总览数据失败: " + e.getMessage());
        }
    }
}
