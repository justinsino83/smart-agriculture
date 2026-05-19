package com.weiming.smartag.task;

import com.weiming.smartag.service.InsectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 虫情数据定时同步任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsectSyncTask {
    
    private final InsectService insectService;
    
    /**
     * 每30分钟同步一次所有设备数据
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void syncInsectData() {
        log.info("========== 虫情数据定时同步任务开始 ==========");
        try {
            insectService.syncAllDevicesData();
            log.info("========== 虫情数据定时同步任务完成 ==========");
        } catch (Exception e) {
            log.error("========== 虫情数据定时同步任务失败 ==========", e);
        }
    }
    
    /**
     * 每天早上8点同步一次数据（备用）
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void syncInsectDataMorning() {
        log.info("========== 早上虫情数据同步任务开始 ==========");
        try {
            insectService.syncAllDevicesData();
            log.info("========== 早上虫情数据同步任务完成 ==========");
        } catch (Exception e) {
            log.error("========== 早上虫情数据同步任务失败 ==========", e);
        }
    }
}