CREATE TABLE IF NOT EXISTS energy_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device VARCHAR(100) NOT NULL,
    type VARCHAR(10) NOT NULL,
    `usage` DOUBLE,
    unit VARCHAR(20),
    cost DOUBLE,
    record_time DATETIME,
    efficiency INT,
    remark VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO energy_record (device, type, `usage`, unit, cost, record_time, efficiency, remark) VALUES
('热泵烘干机-01', '电', 45.6, 'kWh', 27.4, NOW(), 4, '正常运行'),
('热泵烘干机-02', '电', 52.3, 'kWh', 31.4, DATE_SUB(NOW(), INTERVAL 1 HOUR), 3, '高负荷运行'),
('灌溉设备-01', '水', 12.5, 'm³', 25.0, DATE_SUB(NOW(), INTERVAL 2 HOUR), 5, '节水模式'),
('1号仓空调', '电', 28.7, 'kWh', 17.2, DATE_SUB(NOW(), INTERVAL 3 HOUR), 4, '温控运行'),
('照明系统', '电', 8.2, 'kWh', 4.9, DATE_SUB(NOW(), INTERVAL 4 HOUR), 5, '节能模式');