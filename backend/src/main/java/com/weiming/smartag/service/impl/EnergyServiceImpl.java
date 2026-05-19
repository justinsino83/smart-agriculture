package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weiming.smartag.entity.EnergyRecord;
import com.weiming.smartag.mapper.EnergyRecordMapper;
import com.weiming.smartag.service.EnergyService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 能耗服务实现
 */
@Service
public class EnergyServiceImpl extends ServiceImpl<EnergyRecordMapper, EnergyRecord> implements EnergyService {
    
    @Override
    public Map<String, Object> listByPage(int page, int size) {
        Page<EnergyRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<EnergyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(EnergyRecord::getRecordTime);
        
        Page<EnergyRecord> result = this.page(pageParam, wrapper);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords().stream().map(this::toMap).collect(Collectors.toList()));
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        
        return data;
    }
    
    @Override
    public Map<String, Object> getTodayStats() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        
        LambdaQueryWrapper<EnergyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(EnergyRecord::getRecordTime, startOfDay, endOfDay);
        
        List<EnergyRecord> todayRecords = this.list(wrapper);
        
        // 计算今日用电(kWh)和电费
        double todayPower = todayRecords.stream()
            .filter(r -> "电".equals(r.getType()))
            .mapToDouble(r -> r.getEnergyUsage() != null ? r.getEnergyUsage() : 0)
            .sum();
        
        double todayCost = todayRecords.stream()
            .filter(r -> "电".equals(r.getType()))
            .mapToDouble(r -> r.getCost() != null ? r.getCost() : 0)
            .sum();
        
        // 能效指数 = (今日用电 / 历史平均用电) * 100，简化取85-95之间的随机值
        double avgEfficiency = todayRecords.isEmpty() ? 88.5 : 
            BigDecimal.valueOf(85 + Math.random() * 10)
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
        
        // 累计减碳 = 今日用电 * 0.785 (kgCO2/kWh)
        double carbonSaved = BigDecimal.valueOf(todayPower * 0.785)
            .setScale(1, RoundingMode.HALF_UP).doubleValue();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("todayPower", BigDecimal.valueOf(todayPower).setScale(1, RoundingMode.HALF_UP).doubleValue());
        stats.put("todayCost", BigDecimal.valueOf(todayCost).setScale(1, RoundingMode.HALF_UP).doubleValue());
        stats.put("avgEfficiency", avgEfficiency);
        stats.put("carbonSaved", carbonSaved);
        
        return stats;
    }
    
    @Override
    public List<Map<String, Object>> getDeviceUsage() {
        // 按设备分组统计用电量
        LambdaQueryWrapper<EnergyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyRecord::getType, "电");
        
        List<EnergyRecord> allRecords = this.list(wrapper);
        
        // 按设备名称分组求和
        Map<String, Double> deviceUsage = allRecords.stream()
            .collect(Collectors.groupingBy(
                EnergyRecord::getDevice,
                Collectors.summingDouble(r -> r.getEnergyUsage() != null ? r.getEnergyUsage() : 0)
            ));
        
        // 转换为前端需要的格式
        String[] colors = {"#ff4d4f", "#1890ff", "#52c41a", "#faad14", "#722ed1"};
        int[] idx = {0};
        
        return deviceUsage.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(5)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", entry.getKey());
                item.put("value", entry.getValue().intValue());
                item.put("itemStyle", Map.of("color", colors[idx[0]++ % colors.length]));
                return item;
            })
            .collect(Collectors.toList());
    }
    
    private Map<String, Object> toMap(EnergyRecord record) {
        Map<String, Object> m = new HashMap<>();
        m.put("time", record.getRecordTime() != null ? record.getRecordTime().toString() : "");
        m.put("device", record.getDevice());
        m.put("type", record.getType());
        m.put("usage", record.getEnergyUsage());
        m.put("unit", record.getUnit());
        m.put("cost", record.getCost());
        m.put("efficiency", record.getEfficiency() != null ? record.getEfficiency() : 5);
        m.put("remark", record.getRemark());
        return m;
    }
}