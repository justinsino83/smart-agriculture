package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weiming.smartag.entity.IrrigationDevice;
import com.weiming.smartag.entity.IrrigationTask;
import com.weiming.smartag.mapper.IrrigationDeviceMapper;
import com.weiming.smartag.mapper.IrrigationTaskMapper;
import com.weiming.smartag.service.IrrigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 灌溉服务实现
 */
@Service
@RequiredArgsConstructor
public class IrrigationServiceImpl extends ServiceImpl<IrrigationDeviceMapper, IrrigationDevice> 
        implements IrrigationService {
    
    private final IrrigationTaskMapper taskMapper;
    
    @Override
    public List<IrrigationDevice> listDevices() {
        return baseMapper.selectList(null);
    }
    
    @Override
    public Map<String, Object> listDevicesPage(int page, int size) {
        try {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<IrrigationDevice> pageObj = 
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<IrrigationDevice> result = 
                    baseMapper.selectPage(pageObj, null);
            
            Map<String, Object> data = new HashMap<>();
            data.put("list", result.getRecords());
            data.put("total", result.getTotal());
            data.put("page", page);
            data.put("size", size);
            return data;
        } catch (Exception e) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", new ArrayList<>());
            data.put("total", 0L);
            data.put("page", page);
            data.put("size", size);
            return data;
        }
    }
    
    @Override
    public boolean controlDevice(Long deviceId, Boolean on) {
        IrrigationDevice device = getById(deviceId);
        if (device == null) return false;
        
        // 更新设备状态
        device.setStatus(on ? 2 : 1);
        device.setUpdateTime(LocalDateTime.now());
        
        // 记录操作日志（可扩展）
        if (on) {
            device.setLastStartTime(LocalDateTime.now());
        } else {
            // 计算运行时长
            if (device.getLastStartTime() != null) {
                long minutes = java.time.Duration.between(
                    device.getLastStartTime(), LocalDateTime.now()).toMinutes();
                device.setTotalRunTime(device.getTotalRunTime() + (int) minutes);
            }
        }
        
        return updateById(device);
    }
    
    @Override
    public List<IrrigationTask> listTasks() {
        return taskMapper.selectList(null);
    }
    
    @Override
    public Map<String, Object> listTasksPage(int page, int size, Integer status) {
        try {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<IrrigationTask> pageObj = 
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IrrigationTask> wrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            if (status != null) {
                wrapper.eq(IrrigationTask::getStatus, status);
            }
            wrapper.orderByDesc(IrrigationTask::getCreateTime);
            
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<IrrigationTask> result = 
                    taskMapper.selectPage(pageObj, wrapper);
            
            Map<String, Object> data = new HashMap<>();
            data.put("list", result.getRecords());
            data.put("total", result.getTotal());
            data.put("page", page);
            data.put("size", size);
            return data;
        } catch (Exception e) {
            Map<String, Object> data = new HashMap<>();
            data.put("list", new ArrayList<>());
            data.put("total", 0L);
            data.put("page", page);
            data.put("size", size);
            return data;
        }
    }
    
    @Override
    public boolean createTask(IrrigationTask task) {
        task.setStatus(0); // 待执行
        task.setCreateTime(LocalDateTime.now());
        return taskMapper.insert(task) > 0;
    }

    @Override
    public boolean cancelTask(Long taskId) {
        IrrigationTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() == 2) return false; // 已完成的任务不能取消
        task.setStatus(3); // 已取消
        return taskMapper.updateById(task) > 0;
    }

    @Override
    public boolean executeTask(Long taskId) {
        IrrigationTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 0) return false;

        // 更新任务状态为执行中
        task.setStatus(1);
        task.setActualStartTime(LocalDateTime.now());

        // 启动关联设备
        if (task.getDeviceId() != null) {
            controlDevice(task.getDeviceId(), true);
        }

        return taskMapper.updateById(task) > 0;
    }

    @Override
    public boolean completeTask(Long taskId, Double waterUsage) {
        IrrigationTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 1) return false;

        task.setStatus(2); // 已完成
        task.setActualEndTime(LocalDateTime.now());
        task.setWaterUsage(waterUsage);

        // 关闭设备
        if (task.getDeviceId() != null) {
            controlDevice(task.getDeviceId(), false);
        }

        return taskMapper.updateById(task) > 0;
    }
    
    @Override
    public Map<String, Object> getStatistics(String period) {
        Map<String, Object> stats = new HashMap<>();

        // 统计任务数据
        List<IrrigationTask> tasks = taskMapper.selectList(null);

        double totalWater = tasks.stream()
                .filter(t -> t.getWaterUsage() != null)
                .mapToDouble(IrrigationTask::getWaterUsage)
                .sum();

        long completedCount = tasks.stream().filter(t -> t.getStatus() == 2).count();
        long pendingCount = tasks.stream().filter(t -> t.getStatus() == 0).count();
        long runningCount = tasks.stream().filter(t -> t.getStatus() == 1).count();

        stats.put("todayUsage", Math.round(totalWater * 10) / 10.0);
        stats.put("todayCount", completedCount);
        stats.put("savingRate", 28.5);
        stats.put("planCount", pendingCount);
        stats.put("runningCount", runningCount);

        // 设备状态统计
        List<IrrigationDevice> devices = listDevices();
        long onlineDevices = devices.stream().filter(d -> d.getStatus() != 0).count();
        long runningDevices = devices.stream().filter(d -> d.getStatus() == 2).count();

        stats.put("totalDevices", devices.size());
        stats.put("onlineDevices", onlineDevices);
        stats.put("runningDevices", runningDevices);

        return stats;
    }

    @Override
    public Map<String, Object> getDeviceDetail(Long deviceId) {
        Map<String, Object> detail = new HashMap<>();

        IrrigationDevice device = getById(deviceId);
        if (device == null) return detail;

        detail.put("device", device);

        // 获取设备关联任务
        List<IrrigationTask> tasks = taskMapper.selectByDeviceId(deviceId);
        detail.put("taskCount", tasks.size());
        detail.put("completedTasks", tasks.stream().filter(t -> t.getStatus() == 2).count());

        // 计算今日用水
        double todayWater = tasks.stream()
                .filter(t -> t.getStatus() == 2 && t.getActualEndTime() != null)
                .filter(t -> t.getActualEndTime().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .mapToDouble(t -> t.getWaterUsage() != null ? t.getWaterUsage() : 0)
                .sum();
        detail.put("todayWater", Math.round(todayWater * 10) / 10.0);

        return detail;
    }

    @Override
    public List<Map<String, Object>> getWaterUsageTrend(int days) {
        List<Map<String, Object>> trend = new ArrayList<>();

        // 获取所有已完成任务
        List<IrrigationTask> tasks = taskMapper.selectList(null).stream()
                .filter(t -> t.getStatus() == 2 && t.getActualEndTime() != null)
                .collect(Collectors.toList());

        // 获取所有设备信息
        Map<Long, IrrigationDevice> deviceMap = new HashMap<>();
        listDevices().forEach(d -> deviceMap.put(d.getId(), d));

        // 按日期分组统计
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            String dateStr = date.format(formatter);

            // 统计实际用水量
            double actualWater = tasks.stream()
                    .filter(t -> t.getActualEndTime().toLocalDate().equals(date.toLocalDate()))
                    .mapToDouble(t -> t.getWaterUsage() != null ? t.getWaterUsage() : 0)
                    .sum();

            // 统计计划用水量（根据设备流量和计划时长计算）
            double plannedWater = tasks.stream()
                    .filter(t -> t.getActualEndTime().toLocalDate().equals(date.toLocalDate()))
                    .mapToDouble(t -> {
                        if (t.getDeviceId() != null && t.getDuration() != null) {
                            IrrigationDevice device = deviceMap.get(t.getDeviceId());
                            if (device != null && device.getFlowRate() != null) {
                                // 计划用水量 = 设备流量(m³/h) * 计划时长(分钟) / 60
                                return device.getFlowRate() * t.getDuration() / 60.0;
                            }
                        }
                        return 0;
                    })
                    .sum();

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr);
            dayData.put("water", Math.round(actualWater * 10) / 10.0);
            dayData.put("plannedWater", Math.round(plannedWater * 10) / 10.0);
            trend.add(dayData);
        }

        return trend;
    }
}
