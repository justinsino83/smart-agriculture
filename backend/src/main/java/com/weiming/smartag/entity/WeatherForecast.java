package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 气象预报实体
 */
@Data
@TableName("weather_forecast")
public class WeatherForecast {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate forecastDate;

    private String weatherCode;

    private Double tempHigh;

    private Double tempLow;

    private Double humidity;

    private Double windSpeed;

    private LocalDateTime createTime;
}