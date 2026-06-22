package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "试验田数据（总览基础 + IoT 设备数据）")
public class TestFieldDataDTO {

    @Schema(description = "环境监测数据（近5天）")
    private List<EnvData> environment;

    @Schema(description = "土壤监测数据")
    private SoilData soil;

    @Schema(description = "气象监测数据")
    private WeatherData weather;

    @Schema(description = "排水阀数据（来自外部IoT平台 deviceValues 接口，按设备返回实时值）")
    private List<ValveItem> valves;

    @Schema(description = "水位计数据（来自外部IoT平台 deviceValues 接口）")
    private WaterMeterItem waterMeter;

    @Schema(description = "虫情数据")
    private InsectData insectData;

    @Schema(description = "摄像头列表（来自外部IoT平台）")
    private List<CameraItem> cameras;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "环境监测数据（近5天：温度、湿度）")
    public static class EnvData {
        @Schema(description = "日期，格式 MM-dd")
        private String date;
        @Schema(description = "环境温度")
        private BigDecimal ambientTemperature;
        @Schema(description = "环境湿度")
        private BigDecimal ambientHumidity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "土壤监测数据（仅pH）")
    public static class SoilData {
        @Schema(description = "土壤pH")
        private BigDecimal soilPh;
        @Schema(description = "检测时间(ISO)")
        private String detectedTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "气象监测数据")
    public static class WeatherData {
        @Schema(description = "气压")
        private BigDecimal pressure;
        @Schema(description = "风速")
        private BigDecimal windSpeed;
        @Schema(description = "累计雨量")
        private BigDecimal rainfall;
        @Schema(description = "光照强度")
        private BigDecimal lightIntensity;
        @Schema(description = "露点温度")
        private BigDecimal dewTemp;
        @Schema(description = "CO2")
        private BigDecimal co2;
        @Schema(description = "检测时间(ISO)")
        private String detectedTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "排水阀数据（来自IoT平台 deviceValues 实时值）")
    public static class ValveItem {
        @Schema(description = "设备id")
        private String deviceId;
        @Schema(description = "设备名称")
        private String name;
        @Schema(description = "站点名称")
        private String stationName;
        @Schema(description = "压力1")
        private BigDecimal pressure1;
        @Schema(description = "压力2")
        private BigDecimal pressure2;
        @Schema(description = "阀门开度")
        private BigDecimal pos;
        @Schema(description = "电流")
        private BigDecimal current;
        @Schema(description = "电压")
        private BigDecimal voltage;
        @Schema(description = "保护扭矩")
        private BigDecimal protectTorque;
        @Schema(description = "状态（原始字符串）")
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "水位计数据（来自IoT平台 deviceValues 实时值）")
    public static class WaterMeterItem {
        @Schema(description = "设备id")
        private String deviceId;
        @Schema(description = "设备名称")
        private String name;
        @Schema(description = "站点名称")
        private String stationName;
        @Schema(description = "水位")
        private BigDecimal waterLevel;
        @Schema(description = "是否有水（原始字段）")
        private Object hasWater;
        @Schema(description = "状态（原始字符串）")
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "虫情数据（来自本地虫情表 JSON 解析）")
    public static class InsectData {
        @Schema(description = "最新一条虫情记录")
        private Object latestRecord;
        @Schema(description = "虫情统计（按虫名聚合的出现次数）")
        private List<InsectStatItem> statistics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "虫情统计项")
    public static class InsectStatItem {
        @Schema(description = "虫类名称")
        private String name;
        @Schema(description = "出现次数")
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "摄像头信息（来自外部 IoT 平台 listAllDevices）")
    public static class CameraItem {
        @Schema(description = "摄像头 deviceId")
        private String deviceId;
        @Schema(description = "摄像头名称")
        private String name;
        @Schema(description = "是否启用 0/1")
        private Object enable;
        @Schema(description = "在线状态 0/1")
        private Object status;
        @Schema(description = "国标编号")
        private String gbId;
        @Schema(description = "HLS/FLV 播放地址")
        private String httpsFlvUrl;
        @Schema(description = "所属站点名称")
        private String stationName;
        @Schema(description = "设备类型名称")
        private String deviceTypeName;
    }
}
