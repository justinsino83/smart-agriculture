package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 虫情设备表
 */
@Data
@TableName("insect_device")
public class InsectDevice {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 外部平台设备ID */
    private Integer remoteId;
    
    /** 设备编号 */
    private String devNum;
    
    /** 设备IMEI */
    private String imei;
    
    /** 设备名称 */
    private String devName;
    
    /** 设备类型 */
    private String devType;
    
    /** 设备类型名称 */
    private String devTypeName;
    
    /** 经度 */
    private BigDecimal lng;
    
    /** 纬度 */
    private BigDecimal lat;
    
    /** 设备状态 (0-离线 1-在线) */
    private Integer devState;
    
    /** 在线状态 */
    private String onlineStatus;
    
    /** 最后数据时间 */
    private LocalDateTime lastDataTime;
    
    /** 地址 */
    private String address;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}