package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weiming.smartag.entity.DryingBatch;
import com.weiming.smartag.mapper.DryingBatchMapper;
import com.weiming.smartag.service.DryingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 烘干服务实现
 */
@Service
public class DryingServiceImpl extends ServiceImpl<DryingBatchMapper, DryingBatch> 
        implements DryingService {
    
    @Override
    public boolean startBatch(Long batchId) {
        DryingBatch batch = getById(batchId);
        if (batch == null || batch.getStatus() != 0) return false;
        
        batch.setStatus(1); // 烘干中
        batch.setStartTime(LocalDateTime.now());
        batch.setUpdateTime(LocalDateTime.now());
        
        return updateById(batch);
    }
    
    @Override
    public boolean stopBatch(Long batchId) {
        DryingBatch batch = getById(batchId);
        if (batch == null) return false;
        
        batch.setStatus(4); // 已完成
        batch.setEndTime(LocalDateTime.now());
        batch.setUpdateTime(LocalDateTime.now());
        
        // 计算烘干时长（分钟）
        if (batch.getStartTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(batch.getStartTime(), batch.getEndTime());
            batch.setDryingDuration((int) minutes);
        }
        
        return updateById(batch);
    }
    
    @Override
    public boolean pauseBatch(Long batchId) {
        DryingBatch batch = getById(batchId);
        if (batch == null || batch.getStatus() != 1) return false;
        
        batch.setStatus(2); // 暂停
        batch.setUpdateTime(LocalDateTime.now());
        return updateById(batch);
    }
    
    @Override
    public boolean resumeBatch(Long batchId) {
        DryingBatch batch = getById(batchId);
        if (batch == null || batch.getStatus() != 2) return false;
        
        batch.setStatus(1); // 恢复烘干
        batch.setUpdateTime(LocalDateTime.now());
        return updateById(batch);
    }
    
    @Override
    public Map<String, Object> getCurveData(Long batchId) {
        Map<String, Object> data = new HashMap<>();
        
        DryingBatch batch = getById(batchId);
        if (batch == null) {
            data.put("temperature", new double[]{0});
            data.put("moisture", new double[]{0});
            data.put("hours", new int[]{0});
            return data;
        }
        
        // 模拟烘干曲线数据
        double startMoisture = batch.getStartMoisture() != null ? batch.getStartMoisture() : 25.0;
        double targetMoisture = batch.getTargetMoisture() != null ? batch.getTargetMoisture() : 13.5;
        
        // 生成24小时的模拟数据
        int hours = 24;
        double[] temps = new double[hours + 1];
        double[] moistures = new double[hours + 1];
        int[] timePoints = new int[hours + 1];
        
        // 标准烘干工艺曲线
        double[] standardTemps = {25, 35, 45, 55, 55, 55, 55, 50, 45, 40, 35, 30,
                                   30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 25};
        
        for (int i = 0; i <= hours; i++) {
            timePoints[i] = i;
            temps[i] = standardTemps[Math.min(i, standardTemps.length - 1)];
            
            // 水分按指数下降
            double progress = i / (double) hours;
            moistures[i] = startMoisture - (startMoisture - targetMoisture) * 
                          (1 - Math.exp(-3 * progress));
        }
        
        data.put("temperature", temps);
        data.put("moisture", moistures);
        data.put("hours", timePoints);
        data.put("batch", batch);
        
        return data;
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<DryingBatch> batches = list();
        
        // 统计各状态批次数量
        long waiting = batches.stream().filter(b -> b.getStatus() == 0).count();
        long running = batches.stream().filter(b -> b.getStatus() == 1).count();
        long paused = batches.stream().filter(b -> b.getStatus() == 2).count();
        long completed = batches.stream().filter(b -> b.getStatus() == 4).count();
        
        stats.put("waitingCount", waiting);
        stats.put("runningCount", running);
        stats.put("pausedCount", paused);
        stats.put("completedCount", completed);
        stats.put("totalCount", batches.size());
        
        // 今日统计
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        long todayCompleted = batches.stream()
                .filter(b -> b.getStatus() == 4 && b.getEndTime() != null)
                .filter(b -> b.getEndTime().isAfter(today))
                .count();
        
        double todayWeight = batches.stream()
                .filter(b -> b.getStatus() == 4 && b.getEndTime() != null)
                .filter(b -> b.getEndTime().isAfter(today))
                .mapToDouble(b -> b.getWeight() != null ? b.getWeight() : 0)
                .sum();
        
        stats.put("todayCompleted", todayCompleted);
        stats.put("todayWeight", Math.round(todayWeight * 10) / 10.0);
        
        // 计算平均烘干时长
        double avgDuration = batches.stream()
                .filter(b -> b.getStatus() == 4 && b.getDryingDuration() != null)
                .mapToDouble(DryingBatch::getDryingDuration)
                .average().orElse(0);
        
        stats.put("avgDuration", Math.round(avgDuration));
        
        return stats;
    }
    
    @Override
    public List<Map<String, Object>> getRecentBatches(int limit) {
        return list().stream()
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .limit(limit)
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }
    
    private Map<String, Object> convertToMap(DryingBatch batch) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", batch.getId());
        map.put("batchNo", batch.getBatchNo());
        map.put("grainType", batch.getGrainType());
        map.put("weight", batch.getWeight());
        map.put("status", batch.getStatus());
        map.put("startMoisture", batch.getStartMoisture());
        map.put("currentMoisture", batch.getCurrentMoisture());
        map.put("targetMoisture", batch.getTargetMoisture());
        map.put("startTime", batch.getStartTime());
        map.put("endTime", batch.getEndTime());
        map.put("progress", calculateProgress(batch));
        
        // 状态文本
        String[] statusTexts = {"待烘干", "烘干中", "暂停", "已取消", "已完成"};
        map.put("statusText", statusTexts[Math.min(batch.getStatus(), statusTexts.length - 1)]);
        
        return map;
    }
    
    private int calculateProgress(DryingBatch batch) {
        if (batch.getStatus() == 4) return 100;
        if (batch.getStatus() == 0) return 0;
        if (batch.getStartMoisture() == null || batch.getTargetMoisture() == null) return 0;
        
        double current = batch.getCurrentMoisture() != null ? batch.getCurrentMoisture() : batch.getStartMoisture();
        double total = batch.getStartMoisture() - batch.getTargetMoisture();
        double reduced = batch.getStartMoisture() - current;
        
        if (total <= 0) return 0;
        return (int) Math.min(100, Math.round(reduced / total * 100));
    }
}
