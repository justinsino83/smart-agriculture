package com.weiming.smartag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weiming.smartag.entity.SoilData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SoilData Mapper
 */
@Mapper
public interface SoilDataMapper extends BaseMapper<SoilData> {
    
    @Select("SELECT * FROM soil_data WHERE sensor_id = #{sensorId} ORDER BY create_time DESC LIMIT 1")
    SoilData selectLatestBySensorId(Long sensorId);
    
    @Select("SELECT * FROM soil_data WHERE sensor_id = #{sensorId} AND create_time BETWEEN #{start} AND #{end} ORDER BY create_time ASC")
    List<SoilData> selectHistoryData(Long sensorId, LocalDateTime start, LocalDateTime end);
}