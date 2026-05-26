package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("facility_status")
public class FacilityStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long facilityId;

    // 试验田专属
    private BigDecimal transmissionProbability;
    private String irrigationStatus;
    private Integer warningCount;
    private String valveStatus;
    private BigDecimal instantFlow;
    private BigDecimal pipePressure;
    private BigDecimal todayWaterConsumption;
    private BigDecimal windSpeed;
    private String windDirection;
    private BigDecimal totalIrrigation;
    private BigDecimal airPressure;
    private BigDecimal totalRadiation;

    // 仓库专属
    private BigDecimal currentCapacity;
    private BigDecimal availableCapacity;
    private String stockGrainType;
    private BigDecimal stockWeight;
    private Integer entryBatchCount;
    private Integer abnormalAlertCount;
    private String ventilationStatus;
    private String humidityControlStatus;
    private String doorStatus;
    private BigDecimal fireWaterPressure;
    private String securityInspection;

    // 烘干车间专属
    private String runStatus;
    private String grainTypeProcessed;
    private BigDecimal processingCapacity;
    private BigDecimal targetMoistureProcess;
    private String elevatorStatus;
    private String circulatingFanStatus;
    private String burnerStatus;
    private String exhaustValveStatus;
    private String verticalDryingFanStatus;
    private BigDecimal instantPower;
    private BigDecimal todayPowerConsumption;
    private BigDecimal gasFlowRate;
    private BigDecimal outletGrainCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}