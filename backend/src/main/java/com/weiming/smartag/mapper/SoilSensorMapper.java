package com.weiming.smartag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weiming.smartag.entity.SoilSensor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * SoilSensor Mapper
 */
@Mapper
public interface SoilSensorMapper extends BaseMapper<SoilSensor> {

    @Select("SELECT * FROM soil_sensor WHERE status = 1")
    List<SoilSensor> selectOnlineSensors();

    @Select("SELECT * FROM soil_sensor WHERE device_code = #{deviceCode} LIMIT 1")
    SoilSensor selectByCode(String deviceCode);
}