package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("facility_realtime_data")
public class FacilityRealtimeData implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long facilityId;

    // 通用
    private BigDecimal temperature;
    private BigDecimal humidity;

    // 试验田专属
    private BigDecimal airTemperature;
    private BigDecimal airHumidity;
    private BigDecimal lightIntensity;
    private BigDecimal co2Concentration;
    private BigDecimal soilTemperature;
    private BigDecimal soilHumidity;
    private BigDecimal soilPh;

    // 仓库专属
    private BigDecimal innerTemperature;
    private BigDecimal innerHumidity;
    private BigDecimal grainTemperature;
    private BigDecimal ammoniaConcentration;

    // 烘干车间专属
    private BigDecimal hotAirTemperature;
    private BigDecimal outletMoisture;
    private BigDecimal grainLayerThickness;

    private LocalDateTime collectTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}