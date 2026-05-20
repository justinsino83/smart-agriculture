package com.weiming.smartag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weiming.smartag.entity.SoilData;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SoilData Mapper
 */
@Mapper
public interface SoilDataMapper extends BaseMapper<SoilData> {
    
    SoilData selectLatestBySensorId(Long sensorId);
    
    List<SoilData> selectHistoryData(Long sensorId, LocalDateTime start, LocalDateTime end);
}