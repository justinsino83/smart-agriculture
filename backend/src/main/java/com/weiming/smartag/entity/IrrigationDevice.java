package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 灌溉设备实体
 */
@Data
@TableName("irrigation_device")
public class IrrigationDevice {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 设备编号 */
    private String deviceCode;
    
    /** 设备名称 */
    private String deviceName;
    
    /** 所属地块ID */
    private Long fieldId;
    
    /** 设备类型：1-喷灌 2-滴灌 3-微灌 */
    private Integer deviceType;
    
    /** 状态：0-离线 1-在线 2-运行中 */
    private Integer status;
    
    /** 流量(m³/h) */
    private Double flowRate;
    
    /** 当前任务ID */
    private Long currentTaskId;
    
    /** 安装位置 */
    private String location;
    
    /** 上次启动时间 */
    private LocalDateTime lastStartTime;
    
    /** 累计运行时长(分钟) */
    private Integer totalRunTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
