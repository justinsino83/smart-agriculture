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
@Schema(description = "试验田完整数据")
public class TestFieldDataDTO {

    @Schema(description = "基本数据")
    private BaseData baseData;

    @Schema(description = "环境传感器数据")
    private EnvSensorData envSensorData;

    @Schema(description = "土壤数据")
    private SoilData soilData;

    @Schema(description = "地块气象数据")
    private WeatherData weatherData;

    @Schema(description = "灌溉数据")
    private IrrigationData irrigationData;

    @Schema(description = "水位计数据")
    private WaterLevelData waterLevelData;

    @Schema(description = "虫情数据")
    private InsectData insectData;

    @Schema(description = "监控视频数据")
    private VideoMonitorData videoMonitorData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "基本数据")
    public static class BaseData {
        @Schema(description = "传感器数量")
        private Integer sensorCount;
        @Schema(description = "土壤湿度")
        private String soilMoisture;
        @Schema(description = "灌溉状态描述")
        private String irrigationStatus;
        @Schema(description = "视频点位数量")
        private Integer cameraCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "环境传感器数据")
    public static class EnvSensorData {
        @Schema(description = "空气温度")
        private SensorItem airTemperature;
        @Schema(description = "空气湿度")
        private SensorItem airHumidity;
        @Schema(description = "光照强度")
        private SensorItem lightIntensity;
        @Schema(description = "CO2浓度")
        private SensorItem co2Concentration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "土壤数据")
    public static class SoilData {
        @Schema(description = "土壤温度")
        private SensorItem soilTemperature;
        @Schema(description = "土壤湿度")
        private SensorItem soilMoisture;
        @Schema(description = "pH值")
        private SensorItem phValue;
        @Schema(description = "土壤电导率")
        private SensorItem soilConductivity;
        @Schema(description = "氮含量")
        private SensorItem nitrogen;
        @Schema(description = "磷含量")
        private SensorItem phosphorus;
        @Schema(description = "钾含量")
        private SensorItem potassium;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "地块气象数据")
    public static class WeatherData {
        @Schema(description = "气温")
        private SensorItem temperature;
        @Schema(description = "湿度")
        private SensorItem humidity;
        @Schema(description = "风速")
        private SensorItem windSpeed;
        @Schema(description = "风向文本")
        private String windDirection;
        @Schema(description = "气压")
        private SensorItem pressure;
        @Schema(description = "总辐射")
        private SensorItem totalRadiation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "灌溉数据")
    public static class IrrigationData {
        @Schema(description = "灌溉阀门状态")
        private String valveStatus;
        @Schema(description = "瞬时流量")
        private SensorItem instantFlow;
        @Schema(description = "管网压力")
        private SensorItem pipePressure;
        @Schema(description = "今日用水")
        private EnergyItem todayWaterUsage;
        @Schema(description = "预警数量")
        private Integer alertCount;
        @Schema(description = "预警状态")
        private String alertStatus;
        @Schema(description = "阀门开度(来自排水阀实时值)")
        private BigDecimal valvePosition;
        @Schema(description = "阀门电流(来自排水阀实时值)")
        private BigDecimal valveCurrent;
        @Schema(description = "阀门电压(来自排水阀实时值)")
        private BigDecimal valveVoltage;
        @Schema(description = "保护扭矩(来自排水阀实时值)")
        private BigDecimal protectTorque;
        @Schema(description = "当前农场下的排水阀设备数量")
        private Integer valveCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "水位计数据(来自外部IoT平台)")
    public static class WaterLevelData {
        @Schema(description = "平均水位")
        private BigDecimal waterLevel;
        @Schema(description = "有水的水位计数量")
        private Integer hasWater;
        @Schema(description = "整体有水状态(正常/部分有水/无水)")
        private String waterStatus;
        @Schema(description = "当前农场下的水位计设备数量")
        private Integer waterMeterCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "虫情数据")
    public static class InsectData {
        @Schema(description = "最新虫情记录")
        private Object latestRecord;
        @Schema(description = "虫情统计")
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
    @Schema(description = "监控视频数据")
    public static class VideoMonitorData {
        @Schema(description = "摄像头列表")
        private List<CameraItem> cameras;
        @Schema(description = "当前摄像头ID")
        private String currentCameraId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "摄像头项")
    public static class CameraItem {
        @Schema(description = "摄像头ID")
        private String cameraId;
        @Schema(description = "摄像头名称")
        private String cameraName;
        @Schema(description = "推流地址")
        private String streamUrl;
        @Schema(description = "安装位置")
        private String position;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "传感器项")
    public static class SensorItem {
        @Schema(description = "值")
        private BigDecimal value;
        @Schema(description = "状态")
        private String status;
        @Schema(description = "单位")
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "能耗项(带标签)")
    public static class EnergyItem {
        @Schema(description = "值")
        private BigDecimal value;
        @Schema(description = "状态")
        private String status;
        @Schema(description = "标签")
        private String tag;
        @Schema(description = "单位")
        private String unit;
    }
}
