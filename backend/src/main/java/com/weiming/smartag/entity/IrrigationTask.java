package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 灌溉任务实体
 */
@Data
@TableName("irrigation_task")
public class IrrigationTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务名称 */
    private String taskName;

    /** 设备ID */
    private Long deviceId;

    /** 状态：0-待执行 1-执行中 2-已完成 3-已取消 */
    private Integer status;

    /** 触发方式：1-手动 2-自动 */
    private Integer triggerType;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 实际开始时间 */
    private LocalDateTime actualStartTime;

    /** 实际结束时间 */
    private LocalDateTime actualEndTime;

    /** 计划灌溉时长(分钟) */
    private Integer duration;

    /** 实际用水量(m³) */
    private Double waterUsage;

    /** 创建人 */
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
