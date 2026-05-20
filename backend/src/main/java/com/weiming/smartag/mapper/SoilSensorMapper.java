package com.weiming.smartag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weiming.smartag.entity.SoilSensor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SoilSensor Mapper
 */
@Mapper
public interface SoilSensorMapper extends BaseMapper<SoilSensor> {

    List<SoilSensor> selectOnlineSensors();

    SoilSensor selectByCode(String deviceCode);
}