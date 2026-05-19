package com.weiming.smartag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weiming.smartag.entity.EnergyRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 能耗记录 Mapper
 */
@Mapper
public interface EnergyRecordMapper extends BaseMapper<EnergyRecord> {
}