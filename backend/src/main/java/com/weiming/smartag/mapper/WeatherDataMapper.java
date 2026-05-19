package com.weiming.smartag.mapper;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weiming.smartag.entity.WeatherData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 气象数据Mapper
 */
@Mapper
public interface WeatherDataMapper extends IService<WeatherData> {

    /**
     * 获取最新气象数据
     */
    WeatherData selectLatest(@Param("soilId") Long soilId);

    /**
     * 获取24小时趋势数据
     */
    List<WeatherData> select24HourTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("soilId") Long soilId);
}