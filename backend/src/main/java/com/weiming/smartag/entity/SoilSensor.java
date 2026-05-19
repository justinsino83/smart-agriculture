package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 土壤传感器实体
 */
@Data
@TableName("soil_sensor")
public class SoilSensor {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 传感器编号 */
    private String deviceCode;
    
    /** 传感器名称 */
    private String deviceName;
    
    /** 所属地块ID */
    private Long fieldId;
    
    /** 设备状态：0-离线 1-在线 */
    private Integer status;
    
    /** 安装位置 */
    private String location;
    
    /** 最后上报时间 */
    private LocalDateTime lastReportTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}