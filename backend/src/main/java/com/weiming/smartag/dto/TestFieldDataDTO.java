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

    @Schema(description = "排水阀数据（来自外部IoT平台）")
    private ValveData valve;

    @Schema(description = "水位计数据（来自外部IoT平台）")
    private WaterMeterData waterMeter;

    @Schema(description = "虫情数据")
    private InsectData insectData;

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
    @Schema(description = "排水阀数据（来自IoT平台实时值）")
    public static class ValveData {
        @Schema(description = "阀门状态文本（在线/离线/已开启数等）")
        private String status;
        @Schema(description = "平均压力1")
        private BigDecimal pressure1;
        @Schema(description = "平均压力2")
        private BigDecimal pressure2;
        @Schema(description = "阀门开度")
        private BigDecimal pos;
        @Schema(description = "阀门电流")
        private BigDecimal current;
        @Schema(description = "阀门电压")
        private BigDecimal voltage;
        @Schema(description = "保护扭矩")
        private BigDecimal protectTorque;
        @Schema(description = "农场下的排水阀数量")
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "水位计数据（来自IoT平台实时值）")
    public static class WaterMeterData {
        @Schema(description = "平均水位")
        private BigDecimal waterLevel;
        @Schema(description = "有水的水位计数量")
        private Integer hasWater;
        @Schema(description = "整体有水状态")
        private String status;
        @Schema(description = "农场下的水位计数量")
        private Integer count;
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
}
