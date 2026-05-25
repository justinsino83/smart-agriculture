package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备数据推送记录表
 * 用于接收来自各种传感器的推送数据
 */
@Data
@TableName("device_push_data")
public class DevicePushData {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 设施ID */
    private Long facilityId;
    
    /** 设备编号/客户端ID */
    private String clientId;
    
    /** 推送时间 */
    private LocalDateTime detectedTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    // ==================== 环境数据 ====================
    /** 环境温度 (℃) */
    private BigDecimal ambientTemperature;
    
    /** 环境湿度 (%) */
    private BigDecimal ambientHumidity;
    
    /** 气压 (KPa) */
    private BigDecimal pressure;
    
    /** 风速 (m/s) */
    private BigDecimal windSpeed;
    
    /** 风向 (°) */
    private Integer windDirection;
    
    /** 风力 (级) */
    private Integer windScale;
    
    /** 累计雨量 (mm) */
    private BigDecimal rainfall;
    
    /** 蒸发量 (mm) */
    private BigDecimal evaporation;
    
    /** 总辐射 */
    private BigDecimal totalRadiation;
    
    /** 信号强度 (dBm) */
    private Integer rssi;
    
    // ==================== 土壤数据 ====================
    /** 土壤温度 (℃) */
    private BigDecimal soilTemp;
    
    /** 土壤湿度 (%) */
    private BigDecimal soilHumi;
    
    /** 土壤电导率 (uS/cm) */
    private BigDecimal soilCond;
    
    /** 土壤PH */
    private BigDecimal soilPh;
    
    /** 土壤温度1-12 (℃) */
    private BigDecimal soilTemp1;
    private BigDecimal soilTemp2;
    private BigDecimal soilTemp3;
    private BigDecimal soilTemp4;
    private BigDecimal soilTemp5;
    private BigDecimal soilTemp6;
    private BigDecimal soilTemp7;
    private BigDecimal soilTemp8;
    private BigDecimal soilTemp9;
    private BigDecimal soilTemp10;
    private BigDecimal soilTemp11;
    private BigDecimal soilTemp12;
    private BigDecimal soilTemp13;
    private BigDecimal soilTemp14;
    private BigDecimal soilTemp15;
    
    /** 土壤湿度1-12 (%) */
    private BigDecimal soilHumi1;
    private BigDecimal soilHumi2;
    private BigDecimal soilHumi3;
    private BigDecimal soilHumi4;
    private BigDecimal soilHumi5;
    private BigDecimal soilHumi6;
    private BigDecimal soilHumi7;
    private BigDecimal soilHumi8;
    private BigDecimal soilHumi9;
    private BigDecimal soilHumi10;
    private BigDecimal soilHumi11;
    private BigDecimal soilHumi12;
    
    /** 土壤电导率1-12 (uS/cm) */
    private BigDecimal soilCond1;
    private BigDecimal soilCond2;
    private BigDecimal soilCond3;
    private BigDecimal soilCond4;
    private BigDecimal soilCond5;
    private BigDecimal soilCond6;
    private BigDecimal soilCond7;
    private BigDecimal soilCond8;
    private BigDecimal soilCond9;
    private BigDecimal soilCond10;
    private BigDecimal soilCond11;
    private BigDecimal soilCond12;
    
    // ==================== 水质数据 ====================
    /** 水位 (m) */
    private BigDecimal level;
    
    /** 水温 (℃) */
    private BigDecimal waterTemperature;
    
    /** 水质PH */
    private BigDecimal waterPh;
    
    /** 水质电导率 (uS/cm) */
    private BigDecimal waterConductivity;
    
    /** 水质ORP (mv) */
    private BigDecimal waterOrp;
    
    /** 浊度 (NTU) */
    private BigDecimal turbidity;
    
    /** 溶解氧 (mg/L) */
    private BigDecimal dissolvedOxygen;
    
    /** 氨氮 (mg/L) */
    private BigDecimal ammonia;
    
    // ==================== 气象数据 ====================
    /** 光照强度 (lux) */
    private BigDecimal lightIntensity;
    
    /** 日照时数 (h) */
    private BigDecimal sunHours;
    
    /** 紫外线 (W/m²) */
    private BigDecimal ultravioletRays;
    
    /** 净辐射 (HW/m²) */
    private BigDecimal netRadiation;
    
    /** 露点温度 (℃) */
    private BigDecimal dewTemp;
    
    /** 能见度 (m) */
    private BigDecimal visibility;
    
    // ==================== 空气质量 ====================
    /** PM2.5 (ug/m³) */
    private BigDecimal pm25;
    
    /** PM10 (ug/m³) */
    private BigDecimal pm10;
    
    /** TSP (ug/m³) */
    private BigDecimal tsp;
    
    /** 二氧化碳 (ppm) */
    private BigDecimal co2;
    
    /** 一氧化碳 (ppm) */
    private BigDecimal co;
    
    /** 二氧化硫 (ppm) */
    private BigDecimal so2;
    
    /** 二氧化氮 (ppm) */
    private BigDecimal no2;
    
    /** 臭氧 (ppm) */
    private BigDecimal o3;
    
    /** 氧浓度 (ppm) */
    private BigDecimal oxygenContent;
    
    /** TVOC (ppm) */
    private BigDecimal tvoc;
    
    /** 噪声 (db) */
    private BigDecimal noise;
    
    /** 负氧离子 (个/cm³) */
    private BigDecimal negativeOxygen;
    
    // ==================== 其他数据 ====================
    /** 雨雪状态 */
    private String rainOrSnow;
    
    /** 海拔高度 (m) */
    private BigDecimal altitude;
    
    /** 经度 (°) */
    private BigDecimal longitude;
    
    /** 纬度 (°) */
    private BigDecimal latitude;
    
    /** 电池电压 (V) */
    private BigDecimal voltage;
    
    /** 太阳能电压 (V) */
    private BigDecimal solarVoltage;
    
    /** 流速 (m/s) */
    private BigDecimal flow;
    
    /** 流量 (m³/s) */
    private BigDecimal flows;
    
    /** 氮含量 (mg/kg) */
    private BigDecimal nitrogen;
    
    /** 磷含量 (mg/kg) */
    private BigDecimal phosphorus;
    
    /** 钾含量 (mg/kg) */
    private BigDecimal potassium;
    
    /** 光合 (μmol/m²) */
    private BigDecimal photosynthesis;
    
    /** 叶面湿度 (%) */
    private BigDecimal leafHumidity;
    
    /** 叶面温度 (℃) */
    private BigDecimal leafTemperature;
    
    /** 热通量 (W/㎡) */
    private BigDecimal heatFlux;
}