-- 能耗记录表
CREATE TABLE IF NOT EXISTS energy_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device VARCHAR(100) NOT NULL COMMENT '设备名称',
    type VARCHAR(10) NOT NULL COMMENT '能耗类型：电/水',
    `usage` DOUBLE COMMENT '用量',
    unit VARCHAR(20) COMMENT '单位：kWh 或 m³',
    cost DOUBLE COMMENT '费用(元)',
    record_time DATETIME COMMENT '记录时间',
    efficiency INT COMMENT '能效等级 1-5',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='能耗记录表';

-- 插入测试数据
INSERT INTO energy_record (device, type, `usage`, unit, cost, record_time, efficiency, remark) VALUES
('热泵烘干机-01', '电', 45.6, 'kWh', 27.4, NOW(), 4, '正常运行'),
('热泵烘干机-02', '电', 52.3, 'kWh', 31.4, DATE_SUB(NOW(), INTERVAL 1 HOUR), 3, '高负荷运行'),
('灌溉设备-01', '水', 12.5, 'm³', 25.0, DATE_SUB(NOW(), INTERVAL 2 HOUR), 5, '节水模式'),
('1号仓空调', '电', 28.7, 'kWh', 17.2, DATE_SUB(NOW(), INTERVAL 3 HOUR), 4, '温控运行'),
('照明系统', '电', 8.2, 'kWh', 4.9, DATE_SUB(NOW(), INTERVAL 4 HOUR), 5, '节能模式'),
('热泵烘干机-01', '电', 38.5, 'kWh', 23.1, DATE_SUB(NOW(), INTERVAL 5 HOUR), 4, '正常运行'),
('灌溉设备-02', '水', 15.0, 'm³', 30.0, DATE_SUB(NOW(), INTERVAL 6 HOUR), 4, '正常灌溉'),
('2号仓空调', '电', 32.1, 'kWh', 19.3, DATE_SUB(NOW(), INTERVAL 7 HOUR), 3, '高负荷运行'),
('照明系统', '电', 6.8, 'kWh', 4.1, DATE_SUB(NOW(), INTERVAL 8 HOUR), 5, '节能模式'),
('热泵烘干机-03', '电', 41.2, 'kWh', 24.7, DATE_SUB(NOW(), INTERVAL 9 HOUR), 4, '正常运行');