package com.weiming.smartag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weiming.smartag.entity.DevicePushData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * DevicePushData Mapper
 */
@Mapper
public interface DevicePushDataMapper extends BaseMapper<DevicePushData> {
    
    /**
     * 删除重复记录（暂不实现，需确认具体去重规则）
     * 注意：当前方法暂不执行任何操作
     */
    @Update("SELECT 1")
    int deleteDuplicateRecords();
}