package com.weiming.smartag.mapper;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weiming.smartag.entity.WeatherForecast;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 气象预报Mapper
 */
@Mapper
public interface WeatherForecastMapper extends IService<WeatherForecast> {

    /**
     * 查询指定日期范围的预报
     */
    List<WeatherForecast> selectForecastList(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}