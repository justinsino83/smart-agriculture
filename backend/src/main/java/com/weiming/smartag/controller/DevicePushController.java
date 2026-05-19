package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.DevicePushData;
import com.weiming.smartag.mapper.DevicePushDataMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 设备数据推送接收接口
 * 用于接收来自各种传感器的HTTP POST推送数据和TCP数据
 */
@Slf4j
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "设备数据推送", description = "接收传感器推送数据")
public class DevicePushController {
    
    private final DevicePushDataMapper devicePushDataMapper;
    
    /**
     * HTTP POST 接收设备推送数据
     * POST /api/device/push
     */
    @PostMapping("/push")
    @Operation(summary = "接收设备推送数据", description = "接收HTTP POST推送的传感器数据")
    public Result<String> receivePushData(@RequestBody Map<String, Object> payload) {
        try {
            log.info("收到设备推送数据: {}", payload);
            
            DevicePushData data = new DevicePushData();
            data.setClientId(getStringValue(payload, "clientId"));
            data.setDetectedTime(parseDateTime(payload.get("detectedTime")));
            data.setCreateTime(LocalDateTime.now());
            
            // 环境数据
            data.setAmbientTemperature(getBigDecimal(payload, "ambientTemperature"));
            data.setAmbientHumidity(getBigDecimal(payload, "ambientHumidity"));
            data.setPressure(getBigDecimal(payload, "pressure"));
            data.setWindSpeed(getBigDecimal(payload, "windSpeed"));
            data.setWindDirection(getInteger(payload, "windDirection"));
            data.setWindScale(getInteger(payload, "windScale"));
            data.setRainfall(getBigDecimal(payload, "rainfall"));
            data.setEvaporation(getBigDecimal(payload, "Evaporation"));
            data.setTotalRadiation(getBigDecimal(payload, "TotalRadiation"));
            data.setRssi(getInteger(payload, "RSSI"));
            
            // 土壤数据
            data.setSoilTemp(getBigDecimal(payload, "soilTemp"));
            data.setSoilHumi(getBigDecimal(payload, "soilHumi"));
            data.setSoilCond(getBigDecimal(payload, "soilCond"));
            data.setSoilPh(getBigDecimal(payload, "soilPH"));
            
            // 土壤温度1-12
            data.setSoilTemp1(getBigDecimal(payload, "soilTemp1"));
            data.setSoilTemp2(getBigDecimal(payload, "soilTemp2"));
            data.setSoilTemp3(getBigDecimal(payload, "soilTemp3"));
            data.setSoilTemp4(getBigDecimal(payload, "soilTemp4"));
            data.setSoilTemp5(getBigDecimal(payload, "soilTemp5"));
            data.setSoilTemp6(getBigDecimal(payload, "soilTemp6"));
            data.setSoilTemp7(getBigDecimal(payload, "soilTemp7"));
            data.setSoilTemp8(getBigDecimal(payload, "soilTemp8"));
            data.setSoilTemp9(getBigDecimal(payload, "soilTemp9"));
            data.setSoilTemp10(getBigDecimal(payload, "soilTemp10"));
            data.setSoilTemp11(getBigDecimal(payload, "soilTemp11"));
            data.setSoilTemp12(getBigDecimal(payload, "soilTemp12"));
            data.setSoilTemp13(getBigDecimal(payload, "soilTemp13"));
            data.setSoilTemp14(getBigDecimal(payload, "soilTemp14"));
            data.setSoilTemp15(getBigDecimal(payload, "soilTemp15"));
            
            // 土壤湿度1-12
            data.setSoilHumi1(getBigDecimal(payload, "soilHumi1"));
            data.setSoilHumi2(getBigDecimal(payload, "soilHumi2"));
            data.setSoilHumi3(getBigDecimal(payload, "soilHumi3"));
            data.setSoilHumi4(getBigDecimal(payload, "soilHumi4"));
            data.setSoilHumi5(getBigDecimal(payload, "soilHumi5"));
            data.setSoilHumi6(getBigDecimal(payload, "soilHumi6"));
            data.setSoilHumi7(getBigDecimal(payload, "soilHumi7"));
            data.setSoilHumi8(getBigDecimal(payload, "soilHumi8"));
            data.setSoilHumi9(getBigDecimal(payload, "soilHumi9"));
            data.setSoilHumi10(getBigDecimal(payload, "soilHumi10"));
            data.setSoilHumi11(getBigDecimal(payload, "soilHumi11"));
            data.setSoilHumi12(getBigDecimal(payload, "soilHumi12"));
            
            // 土壤电导率1-12
            data.setSoilCond1(getBigDecimal(payload, "soilCond1"));
            data.setSoilCond2(getBigDecimal(payload, "soilCond2"));
            data.setSoilCond3(getBigDecimal(payload, "soilCond3"));
            data.setSoilCond4(getBigDecimal(payload, "soilCond4"));
            data.setSoilCond5(getBigDecimal(payload, "soilCond5"));
            data.setSoilCond6(getBigDecimal(payload, "soilCond6"));
            data.setSoilCond7(getBigDecimal(payload, "soilCond7"));
            data.setSoilCond8(getBigDecimal(payload, "soilCond8"));
            data.setSoilCond9(getBigDecimal(payload, "soilCond9"));
            data.setSoilCond10(getBigDecimal(payload, "soilCond10"));
            data.setSoilCond11(getBigDecimal(payload, "soilCond11"));
            data.setSoilCond12(getBigDecimal(payload, "soilCond12"));
            
            // 水质数据
            data.setLevel(getBigDecimal(payload, "level"));
            data.setWaterTemperature(getBigDecimal(payload, "watertemperature"));
            data.setWaterPh(getBigDecimal(payload, "waterPH"));
            data.setWaterConductivity(getBigDecimal(payload, "Waterconductivity"));
            data.setWaterOrp(getBigDecimal(payload, "WaterORP"));
            data.setTurbidity(getBigDecimal(payload, "turbidity"));
            data.setDissolvedOxygen(getBigDecimal(payload, "dissolvedoxygen"));
            data.setAmmonia(getBigDecimal(payload, "ammonia"));
            
            // 气象数据
            data.setLightIntensity(getBigDecimal(payload, "lightIntensity"));
            data.setSunHours(getBigDecimal(payload, "SunHours"));
            data.setUltravioletRays(getBigDecimal(payload, "ultravioletRays"));
            data.setNetRadiation(getBigDecimal(payload, "NetRadiation"));
            data.setDewTemp(getBigDecimal(payload, "dewTemp"));
            data.setVisibility(getBigDecimal(payload, "visibility"));
            
            // 空气质量
            data.setPm25(getBigDecimal(payload, "PM2.5"));
            data.setPm10(getBigDecimal(payload, "PM10"));
            data.setTsp(getBigDecimal(payload, "TSP"));
            data.setCo2(getBigDecimal(payload, "CO2"));
            data.setCo(getBigDecimal(payload, "CO"));
            data.setSo2(getBigDecimal(payload, "SO2"));
            data.setNo2(getBigDecimal(payload, "NO2"));
            data.setO3(getBigDecimal(payload, "O3"));
            data.setOxygenContent(getBigDecimal(payload, "oxygenContent"));
            data.setTvoc(getBigDecimal(payload, "TVOC"));
            data.setNoise(getBigDecimal(payload, "noise"));
            data.setNegativeOxygen(getBigDecimal(payload, "negativeOxygen"));
            
            // 其他
            data.setRainOrSnow(getStringValue(payload, "RainOrSnow"));
            data.setAltitude(getBigDecimal(payload, "altitude"));
            data.setLongitude(getBigDecimal(payload, "longitude"));
            data.setLatitude(getBigDecimal(payload, "latitude"));
            data.setVoltage(getBigDecimal(payload, "Voltage"));
            data.setSolarVoltage(getBigDecimal(payload, "SolarVoltage"));
            data.setFlow(getBigDecimal(payload, "flow"));
            data.setFlows(getBigDecimal(payload, "flows"));
            data.setNitrogen(getBigDecimal(payload, "nitrogen"));
            data.setPhosphorus(getBigDecimal(payload, "Phosphorus"));
            data.setPotassium(getBigDecimal(payload, "Potassium"));
            data.setPhotosynthesis(getBigDecimal(payload, "Photosynthesis"));
            data.setLeafHumidity(getBigDecimal(payload, "Leafhumidity"));
            data.setLeafTemperature(getBigDecimal(payload, "Leaftemperature"));
            data.setHeatFlux(getBigDecimal(payload, "Heatflux"));
            
            devicePushDataMapper.insert(data);
            
            log.info("数据保存成功, ID: {}, clientId: {}", data.getId(), data.getClientId());
            return Result.success("数据接收成功");
            
        } catch (Exception e) {
            log.error("数据保存失败", e);
            return Result.fail("数据保存失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取推送数据历史记录
     */
    @GetMapping("/history")
    @Operation(summary = "获取推送数据历史")
    public Result<?> getHistory(
            @RequestParam(required = false) String clientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var lambdaQuery = devicePushDataMapper.selectList(null);
            // TODO: 添加分页和条件查询
            return Result.success(lambdaQuery);
        } catch (Exception e) {
            log.error("查询失败", e);
            return Result.fail("查询失败");
        }
    }
    
    /**
     * 获取最新一条数据
     */
    @GetMapping("/latest")
    @Operation(summary = "获取最新推送数据")
    public Result<DevicePushData> getLatest(@RequestParam String clientId) {
        try {
            var data = devicePushDataMapper.selectList(null).stream()
                    .filter(d -> clientId.equals(d.getClientId()))
                    .reduce((first, second) -> second);
            return data.map(Result::success).orElse(Result.fail("无数据"));
        } catch (Exception e) {
            log.error("查询失败", e);
            return Result.fail("查询失败");
        }
    }
    
    // ========== 辅助方法 ==========
    
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        try {
            if (value instanceof Number) {
                return new BigDecimal(value.toString());
            }
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return LocalDateTime.now();
        String str = value.toString().trim();
        try {
            // 尝试多种日期格式
            String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss.SSS"
            };
            for (String pattern : patterns) {
                try {
                    return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(pattern));
                } catch (DateTimeParseException ignored) {}
            }
            // 如果都失败，返回当前时间
            log.warn("无法解析日期: {}", str);
            return LocalDateTime.now();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}