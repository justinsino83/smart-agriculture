package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 虫情监测数据表
 */
@Data
@TableName("insect_data")
public class InsectData {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 设备IMEI编号 */
    private String imei;
    
    /** 设备名称 */
    private String devName;
    
    /** 图片记录时间 */
    private LocalDateTime recordTime;
    
    /** 图片URL */
    private String imageUrl;
    
    /** 缩略图URL */
    private String scaleImageUrl;
    
    /** 分析结果图URL */
    private String plotImageUrl;
    
    /** 识别数量 */
    private Integer objectCount;
    
    /** 识别结果JSON */
    private String detectResult;
    
    /** 图片存储路径 */
    private String storePath;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}