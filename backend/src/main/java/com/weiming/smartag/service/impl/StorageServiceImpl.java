package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weiming.smartag.entity.StorageRecord;
import com.weiming.smartag.mapper.StorageRecordMapper;
import com.weiming.smartag.service.StorageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仓储服务实现
 */
@Service
public class StorageServiceImpl extends ServiceImpl<StorageRecordMapper, StorageRecord> 
        implements StorageService {
    
    @Override
    public boolean storageIn(StorageRecord record) {
        // 生成入库批次号
        String batchNo = "ST" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        record.setBatchNo(batchNo);
        record.setStatus(0); // 在库
        record.setEntryDate(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        
        return save(record);
    }
    
    @Override
    public boolean storageOut(String batchNo) {
        StorageRecord record = lambdaQuery()
                .eq(StorageRecord::getBatchNo, batchNo)
                .eq(StorageRecord::getStatus, 0)
                .one();
        if (record == null) return false;
        
        record.setStatus(1); // 已出库
        record.setExitDate(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        
        return updateById(record);
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<StorageRecord> records = list();
        
        // 库存统计
        double totalStock = records.stream()
                .filter(r -> r.getStatus() == 0)
                .mapToDouble(r -> r.getWeight() != null ? r.getWeight() : 0)
                .sum();
        
        long stockCount = records.stream().filter(r -> r.getStatus() == 0).count();
        
        // 今日出入库
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        
        double todayIn = records.stream()
                .filter(r -> r.getEntryDate() != null && r.getEntryDate().isAfter(today))
                .mapToDouble(r -> r.getWeight() != null ? r.getWeight() : 0)
                .sum();
        
        double todayOut = records.stream()
                .filter(r -> r.getStatus() == 1 && r.getExitDate() != null && r.getExitDate().isAfter(today))
                .mapToDouble(r -> r.getWeight() != null ? r.getWeight() : 0)
                .sum();
        
        // 按粮食品种统计
        Map<String, Double> grainTypeStats = records.stream()
                .filter(r -> r.getStatus() == 0)
                .collect(Collectors.groupingBy(
                        StorageRecord::getGrainType,
                        Collectors.summingDouble(r -> r.getWeight() != null ? r.getWeight() : 0)
                ));
        
        // 库存预警（超过90天）
        LocalDateTime warningDate = LocalDateTime.now().minusDays(90);
        long warningCount = records.stream()
                .filter(r -> r.getStatus() == 0 && r.getEntryDate() != null)
                .filter(r -> r.getEntryDate().isBefore(warningDate))
                .count();
        
        stats.put("totalStock", Math.round(totalStock * 10) / 10.0);
        stats.put("stockCount", stockCount);
        stats.put("todayIn", Math.round(todayIn * 10) / 10.0);
        stats.put("todayOut", Math.round(todayOut * 10) / 10.0);
        stats.put("warningCount", warningCount);
        stats.put("grainTypeStats", grainTypeStats);
        
        return stats;
    }
    
    @Override
    public List<StorageRecord> getStockList() {
        return lambdaQuery()
                .eq(StorageRecord::getStatus, 0)
                .orderByDesc(StorageRecord::getEntryDate)
                .list();
    }
    
    @Override
    public List<Map<String, Object>> getEntryExitTrend(int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            String dateStr = date.format(formatter);
            
            final LocalDateTime dayStart = date.toLocalDate().atStartOfDay();
            final LocalDateTime dayEnd = dayStart.plusDays(1);
            
            double entryWeight = list().stream()
                    .filter(r -> r.getEntryDate() != null)
                    .filter(r -> !r.getEntryDate().isBefore(dayStart) && r.getEntryDate().isBefore(dayEnd))
                    .mapToDouble(r -> r.getWeight() != null ? r.getWeight() : 0)
                    .sum();
            
            double exitWeight = list().stream()
                    .filter(r -> r.getExitDate() != null)
                    .filter(r -> !r.getExitDate().isBefore(dayStart) && r.getExitDate().isBefore(dayEnd))
                    .mapToDouble(r -> r.getWeight() != null ? r.getWeight() : 0)
                    .sum();
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr);
            dayData.put("entry", Math.round(entryWeight * 10) / 10.0);
            dayData.put("exit", Math.round(exitWeight * 10) / 10.0);
            trend.add(dayData);
        }
        
        return trend;
    }
}
