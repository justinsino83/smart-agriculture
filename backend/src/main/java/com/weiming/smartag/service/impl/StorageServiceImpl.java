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
        
        // 库存统计 - 使用 quantity 字段（单位：吨）
        double totalStock = records.stream()
                .filter(r -> r.getStatus() == 0)
                .mapToDouble(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                .sum();
        
        long stockCount = records.stream().filter(r -> r.getStatus() == 0).count();
        
        // 今日出入库
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        
        double todayIn = records.stream()
                .filter(r -> r.getEntryDate() != null && r.getEntryDate().isAfter(today))
                .mapToDouble(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                .sum();
        
        double todayOut = records.stream()
                .filter(r -> r.getStatus() == 1 && r.getExitDate() != null && r.getExitDate().isAfter(today))
                .mapToDouble(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                .sum();
        
        // 按粮食品种统计
        Map<String, Double> grainTypeStats = records.stream()
                .filter(r -> r.getStatus() == 0)
                .collect(Collectors.groupingBy(
                        StorageRecord::getGrainType,
                        Collectors.summingDouble(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                ));
        
        // 库存预警（超过90天 或 数量少于10吨）
        LocalDateTime warningDate = LocalDateTime.now().minusDays(90);
        long warningCount = records.stream()
                .filter(r -> r.getStatus() == 0)
                .filter(r -> (r.getEntryDate() != null && r.getEntryDate().isBefore(warningDate)) || 
                        (r.getQuantity() != null && r.getQuantity() < 10))
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
                    .mapToDouble(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                    .sum();
            
            double exitWeight = list().stream()
                    .filter(r -> r.getExitDate() != null)
                    .filter(r -> !r.getExitDate().isBefore(dayStart) && r.getExitDate().isBefore(dayEnd))
                    .mapToDouble(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                    .sum();
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr);
            dayData.put("entry", Math.round(entryWeight * 10) / 10.0);
            dayData.put("exit", Math.round(exitWeight * 10) / 10.0);
            trend.add(dayData);
        }
        
        return trend;
    }
    
    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        List<StorageRecord> records = list();
        
        // 总库存 - 使用 quantity 字段（单位：吨）
        double totalStock = records.stream()
                .filter(r -> r.getStatus() == 0)
                .mapToDouble(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                .sum();
        
        // 今日出入库
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        
        double todayIn = records.stream()
                .filter(r -> r.getEntryDate() != null && r.getEntryDate().isAfter(today))
                .mapToDouble(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                .sum();
        
        double todayOut = records.stream()
                .filter(r -> r.getStatus() == 1 && r.getExitDate() != null && r.getExitDate().isAfter(today))
                .mapToDouble(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                .sum();
        
        // 库存预警（超过90天 或 数量少于10吨）
        LocalDateTime warningDate = LocalDateTime.now().minusDays(90);
        long warningCount = records.stream()
                .filter(r -> r.getStatus() == 0)
                .filter(r -> (r.getEntryDate() != null && r.getEntryDate().isBefore(warningDate)) || 
                        (r.getQuantity() != null && r.getQuantity() < 10))
                .count();
        
        overview.put("totalStock", Math.round(totalStock * 10) / 10.0);
        overview.put("todayIn", Math.round(todayIn * 10) / 10.0);
        overview.put("todayOut", Math.round(todayOut * 10) / 10.0);
        overview.put("warningCount", warningCount);
        
        return overview;
    }
    
    @Override
    public Map<String, Object> getStockListPage(int page, int size, String keyword) {
        Map<String, Object> result = new HashMap<>();
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<StorageRecord> pageObj = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        
        var queryWrapper = lambdaQuery();
        
        // 如果有关键词，添加搜索条件
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(StorageRecord::getGrainType, keyword)
                    .or()
                    .like(StorageRecord::getBatchNo, keyword)
            );
        }
        
        com.baomidou.mybatisplus.core.metadata.IPage<StorageRecord> pageResult = 
                queryWrapper.orderByDesc(StorageRecord::getEntryDate).page(pageObj);
        
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        LocalDateTime warningDate = LocalDateTime.now().minusDays(90);
        
        List<StorageRecord> warningRecords = lambdaQuery()
                .eq(StorageRecord::getStatus, 0)
                .list()
                .stream()
                .filter(r -> (r.getEntryDate() != null && r.getEntryDate().isBefore(warningDate)) || 
                        (r.getQuantity() != null && r.getQuantity() < 10))
                .collect(Collectors.toList());
        
        for (StorageRecord record : warningRecords) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("id", record.getId());
            alert.put("batchNo", record.getBatchNo());
            alert.put("grainType", record.getGrainType());
            alert.put("warehouse", record.getWarehouse());
            alert.put("quantity", record.getQuantity());
            
            List<String> reasons = new ArrayList<>();
            List<String> suggestions = new ArrayList<>();
            
            if (record.getEntryDate() != null && record.getEntryDate().isBefore(warningDate)) {
                reasons.add("库存时间超过90天");
                suggestions.add("建议尽快出库或轮换");
            }
            
            if (record.getQuantity() != null && record.getQuantity() < 10) {
                reasons.add("库存数量不足10吨");
                suggestions.add("建议及时补充库存");
            }
            
            alert.put("reasons", reasons);
            alert.put("suggestions", suggestions);
            alert.put("entryDate", record.getEntryDate());
            
            alerts.add(alert);
        }
        
        return alerts;
    }
    
    @Override
    public Map<String, Object> getTrace(Long stockId) {
        Map<String, Object> trace = new HashMap<>();
        StorageRecord record = getById(stockId);
        
        if (record != null) {
            trace.put("basicInfo", record);
            
            List<Map<String, Object>> timeline = new ArrayList<>();
            
            Map<String, Object> entryEvent = new HashMap<>();
            entryEvent.put("time", record.getEntryDate());
            entryEvent.put("type", "入库");
            entryEvent.put("description", String.format("%s入库%s%.2f吨", 
                    record.getWarehouse(), record.getGrainType(), record.getQuantity()));
            timeline.add(entryEvent);
            
            if (record.getStatus() == 1 && record.getExitDate() != null) {
                Map<String, Object> exitEvent = new HashMap<>();
                exitEvent.put("time", record.getExitDate());
                exitEvent.put("type", "出库");
                exitEvent.put("description", String.format("%s出库%s%.2f吨", 
                        record.getWarehouse(), record.getGrainType(), record.getQuantity()));
                timeline.add(exitEvent);
            }
            
            trace.put("timeline", timeline);
        }
        
        return trace;
    }
    
    @Override
    public StorageRecord getStockDetail(Long stockId) {
        return getById(stockId);
    }
    
    @Override
    public boolean deleteStock(Long id) {
        return removeById(id);
    }
}
