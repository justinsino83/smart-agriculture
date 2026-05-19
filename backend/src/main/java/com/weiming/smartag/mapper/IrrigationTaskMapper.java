package com.weiming.smartag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weiming.smartag.entity.IrrigationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * IrrigationTask Mapper
 */
@Mapper
public interface IrrigationTaskMapper extends BaseMapper<IrrigationTask> {
    
    @Select("SELECT * FROM irrigation_task WHERE device_id = #{deviceId} ORDER BY create_time DESC")
    List<IrrigationTask> selectByDeviceId(Long deviceId);
}