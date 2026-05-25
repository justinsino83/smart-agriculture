package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 气象数据实体
 */
@Data
@TableName("weather_data")
public class WeatherData {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设施ID */
    private Long facilityId;

    private Long soilId;

    private Double temperature;

    private Double humidity;

    private Double windSpeed;

    private Double windDirection;

    private Double pressure;

    private String weatherCode;

    private LocalDateTime collectTime;

    private LocalDateTime createTime;
}