-- 智慧农业管理系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS smart_agriculture DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_agriculture;

-- 农田地块表
CREATE TABLE IF NOT EXISTS farm_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '地块名称',
    code VARCHAR(20) NOT NULL COMMENT '地块编号',
    area DOUBLE COMMENT '面积(亩)',
    crop VARCHAR(20) COMMENT '种植作物',
    growth_stage VARCHAR(20) COMMENT '作物生长期',
    manager VARCHAR(20) COMMENT '负责人',
    location VARCHAR(100) COMMENT '位置坐标',
    status TINYINT DEFAULT 1 COMMENT '状态:0-闲置 1-种植中 2-休耕',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='农田地块表';

-- 土壤传感器表
CREATE TABLE IF NOT EXISTS soil_sensor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_code VARCHAR(30) NOT NULL COMMENT '传感器编号',
    device_name VARCHAR(50) COMMENT '传感器名称',
    field_id BIGINT COMMENT '所属地块ID',
    status TINYINT DEFAULT 1 COMMENT '状态:0-离线 1-在线',
    location VARCHAR(100) COMMENT '安装位置',
    last_report_time DATETIME COMMENT '最后上报时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='土壤传感器表';

-- 土壤数据表
CREATE TABLE IF NOT EXISTS soil_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sensor_id BIGINT NOT NULL COMMENT '传感器ID',
    moisture DOUBLE COMMENT '土壤湿度(%)',
    temperature DOUBLE COMMENT '土壤温度(°C)',
    ph DOUBLE COMMENT 'pH值',
    ec DOUBLE COMMENT 'EC值(mS/cm)',
    nitrogen DOUBLE COMMENT '氮含量(mg/kg)',
    phosphorus DOUBLE COMMENT '磷含量(mg/kg)',
    potassium DOUBLE COMMENT '钾含量(mg/kg)',
    collect_time DATETIME NOT NULL COMMENT '采集时间'
) ENGINE=InnoDB COMMENT='土壤监测数据表';

-- 灌溉设备表
CREATE TABLE IF NOT EXISTS irrigation_device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_code VARCHAR(30) NOT NULL COMMENT '设备编号',
    device_name VARCHAR(50) COMMENT '设备名称',
    field_id BIGINT COMMENT '所属地块ID',
    device_type TINYINT COMMENT '类型:1-喷灌 2-滴灌 3-微灌',
    status TINYINT DEFAULT 1 COMMENT '状态:0-离线 1-在线 2-运行中',
    flow_rate DOUBLE COMMENT '流量(m³/h)',
    current_task_id BIGINT COMMENT '当前任务ID',
    location VARCHAR(100) COMMENT '安装位置',
    last_start_time DATETIME COMMENT '上次启动时间',
    total_run_time INT COMMENT '累计运行时长(分钟)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='灌溉设备表';

-- 灌溉任务表
CREATE TABLE IF NOT EXISTS irrigation_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(50) COMMENT '任务名称',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    status TINYINT DEFAULT 0 COMMENT '状态:0-待执行 1-执行中 2-已完成 3-已取消',
    trigger_type TINYINT COMMENT '触发方式:1-手动 2-自动',
    plan_start_time DATETIME COMMENT '计划开始时间',
    actual_start_time DATETIME COMMENT '实际开始时间',
    actual_end_time DATETIME COMMENT '实际结束时间',
    duration INT COMMENT '计划灌溉时长(分钟)',
    water_usage DOUBLE COMMENT '实际用水量(m³)',
    create_by VARCHAR(20) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='灌溉任务表';

-- 烘干批次表
CREATE TABLE IF NOT EXISTS drying_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no VARCHAR(30) NOT NULL COMMENT '批次号',
    grain_type VARCHAR(20) COMMENT '粮食品种',
    initial_moisture DOUBLE COMMENT '初始含水率(%)',
    target_moisture DOUBLE COMMENT '目标含水率(%)',
    current_moisture DOUBLE COMMENT '当前含水率(%)',
    weight DOUBLE COMMENT '重量(kg)',
    status TINYINT DEFAULT 0 COMMENT '状态:0-待开始 1-预热中 2-烘干中 3-冷却中 4-已完成',
    device_id BIGINT COMMENT '烘干设备ID',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    power_usage DOUBLE COMMENT '用电量(kWh)',
    create_by VARCHAR(20) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='烘干批次表';

-- 仓储记录表
CREATE TABLE IF NOT EXISTS storage_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no VARCHAR(30) NOT NULL COMMENT '批次号',
    grain_type VARCHAR(20) COMMENT '粮食品种',
    warehouse VARCHAR(50) COMMENT '仓库位置',
    quantity DOUBLE COMMENT '数量(吨)',
    moisture DOUBLE COMMENT '含水率(%)',
    quality TINYINT COMMENT '质量等级:1-一等 2-二等 3-三等',
    entry_date DATETIME COMMENT '入库时间',
    expire_date DATETIME COMMENT '保质期限',
    status TINYINT DEFAULT 0 COMMENT '状态:0-在库 1-出库',
    operator VARCHAR(20) COMMENT '操作人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='仓储记录表';

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(30) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(20) COMMENT '真实姓名',
    phone VARCHAR(15) COMMENT '手机号',
    email VARCHAR(50) COMMENT '邮箱',
    role TINYINT COMMENT '角色:1-超级管理员 2-管理员 3-操作员 4-查看员',
    department VARCHAR(50) COMMENT '部门',
    status TINYINT DEFAULT 1 COMMENT '状态:0-禁用 1-启用',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='系统用户表';

-- 插入测试数据
INSERT INTO sys_user (username, password, real_name, phone, email, role, department, status) VALUES
('admin', '123456', '系统管理员', '13800138000', 'admin@weiming.com', 1, '管理中心', 1),
('operator', '123456', '操作员张三', '13800138001', 'operator@weiming.com', 3, '种植部', 1),
('operator2', '123456', '操作员李四', '13800138002', 'operator2@weiming.com', 3, '仓储部', 1),
('viewer', '123456', '查看员王五', '13800138003', 'viewer@weiming.com', 4, '财务部', 1),
('manager', '123456', '部门经理赵六', '13800138004', 'manager@weiming.com', 2, '种植部', 1),
('tech1', '123456', '技术员孙琪', '13800138005', 'tech1@weiming.com', 3, '技术部', 1),
('tech2', '123456', '技术员周八', '13800138006', 'tech2@weiming.com', 3, '技术部', 1),
('warehouse', '123456', '仓管员郑九', '13800138007', 'warehouse@weiming.com', 3, '仓储部', 1),
('qa', '123456', '质检员吴十', '13800138008', 'qa@weiming.com', 3, '质量部', 1),
('director', '123456', '生产总监钱一', '13800138009', 'director@weiming.com', 2, '生产部', 1),
('hr', '123456', '人事专员陈二', '13800138010', 'hr@weiming.com', 4, '人力资源部', 1),
('finance', '123456', '财务专员刘三', '13800138011', 'finance@weiming.com', 4, '财务部', 1);

-- 插入农田地块数据
INSERT INTO farm_field (name, code, area, crop, growth_stage, manager, location, status) VALUES
('1号田-水稻区', 'FD001', 50.5, '水稻', '抽穗期', '张三', '东经120°15′30″ 北纬31°45′20″', 1),
('2号田-小麦区', 'FD002', 35.2, '小麦', '灌浆期', '李四', '东经120°15′45″ 北纬31°45′35″', 1),
('3号田-玉米区', 'FD003', 42.8, '玉米', '大喇叭口期', '王五', '东经120°16′00″ 北纬31°45′50″', 1),
('4号田-大豆区', 'FD004', 28.6, '大豆', '开花期', '赵六', '东经120°16′15″ 北纬31°46′05″', 1),
('5号田-试验田', 'FD005', 15.0, '水稻', '分蘖期', '孙琪', '东经120°16′30″ 北纬31°46′20″', 1),
('6号田-蔬菜区', 'FD006', 20.3, '西红柿', '结果期', '周八', '东经120°16′45″ 北纬31°46′35″', 1),
('7号田-棉花区', 'FD007', 38.5, '棉花', '吐絮期', '郑九', '东经120°17′00″ 北纬31°46′50″', 1),
('8号田-油菜区', 'FD008', 32.1, '油菜', '成熟期', '吴十', '东经120°17′15″ 北纬31°47′05″', 1),
('9号田-空闲区', 'FD009', 25.0, NULL, NULL, '钱一', '东经120°17′30″ 北纬31°47′20″', 0),
('10号田-水稻区', 'FD010', 48.2, '水稻', '苗期', '陈二', '东经120°17′45″ 北纬31°47′35″', 1),
('11号田-休耕区', 'FD011', 30.0, NULL, NULL, '刘三', '东经120°18′00″ 北纬31°47′50″', 2),
('12号田-水稻区', 'FD012', 52.0, '水稻', '分蘖期', '张三', '东经120°18′15″ 北纬31°48′05″', 1);

-- 插入土壤传感器数据
INSERT INTO soil_sensor (device_code, device_name, field_id, status, location, last_report_time) VALUES
('SS20240001', '1号田土壤传感器', 1, 1, '1号田中心位置', NOW()),
('SS20240002', '2号田土壤传感器', 2, 1, '2号田中心位置', NOW()),
('SS20240003', '3号田土壤传感器', 3, 1, '3号田中心位置', NOW()),
('SS20240004', '4号田土壤传感器', 4, 0, '4号田中心位置', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('SS20240005', '5号田土壤传感器', 5, 1, '5号田中心位置', NOW()),
('SS20240006', '6号田土壤传感器', 6, 1, '6号田中心位置', NOW()),
('SS20240007', '7号田土壤传感器', 7, 1, '7号田中心位置', NOW()),
('SS20240008', '8号田土壤传感器', 8, 0, '8号田中心位置', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
('SS20240009', '9号田土壤传感器', 9, 1, '9号田中心位置', NOW()),
('SS20240010', '10号田土壤传感器', 10, 1, '10号田中心位置', NOW()),
('SS20240011', '11号田土壤传感器', 11, 0, '11号田中心位置', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('SS20240012', '12号田土壤传感器', 12, 1, '12号田中心位置', NOW());

-- 插入土壤数据（每个传感器10条，覆盖最近24小时每2小时一条）
INSERT INTO soil_data (sensor_id, moisture, temperature, ph, ec, nitrogen, phosphorus, potassium, collect_time) VALUES
-- 1号田传感器数据
(1, 45.2, 22.5, 6.8, 1.2, 85.3, 35.2, 37.1, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(1, 44.8, 22.0, 6.7, 1.1, 84.8, 34.8, 36.8, DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(1, 44.5, 21.5, 6.7, 1.1, 84.5, 34.5, 36.5, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(1, 44.0, 21.0, 6.6, 1.0, 83.8, 33.8, 35.8, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(1, 43.5, 20.5, 6.6, 1.0, 83.2, 33.2, 35.2, DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(1, 43.0, 20.0, 6.5, 0.9, 82.5, 32.5, 34.5, DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(1, 42.8, 19.5, 6.5, 0.9, 81.8, 31.8, 33.8, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(1, 43.2, 20.0, 6.6, 1.0, 82.2, 32.2, 34.2, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(1, 44.0, 20.5, 6.6, 1.0, 83.0, 33.0, 35.0, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(1, 44.5, 21.0, 6.7, 1.1, 83.8, 33.8, 35.8, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(1, 45.0, 21.5, 6.7, 1.1, 84.5, 34.5, 36.5, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(1, 45.5, 22.0, 6.8, 1.2, 85.2, 35.2, 37.2, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
-- 2号田传感器数据
(2, 38.5, 23.0, 6.5, 1.5, 92.0, 42.0, 40.0, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(2, 38.0, 22.5, 6.4, 1.4, 91.5, 41.5, 39.5, DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(2, 37.5, 22.0, 6.4, 1.4, 91.0, 41.0, 39.0, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(2, 37.0, 21.5, 6.3, 1.3, 90.2, 40.2, 38.2, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(2, 36.5, 21.0, 6.3, 1.3, 89.5, 39.5, 37.5, DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(2, 36.0, 20.5, 6.2, 1.2, 88.8, 38.8, 36.8, DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(2, 35.8, 20.0, 6.2, 1.2, 88.0, 38.0, 36.0, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(2, 36.2, 20.5, 6.3, 1.2, 88.5, 38.5, 36.5, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(2, 37.0, 21.0, 6.3, 1.3, 89.2, 39.2, 37.2, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(2, 37.5, 21.5, 6.4, 1.3, 90.0, 40.0, 38.0, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(2, 38.0, 22.0, 6.4, 1.4, 90.8, 40.8, 38.8, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(2, 38.5, 22.5, 6.5, 1.5, 91.5, 41.5, 39.5, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
-- 3号田传感器数据
(3, 52.0, 24.5, 7.0, 0.8, 75.0, 28.0, 32.0, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(3, 51.5, 24.0, 6.9, 0.8, 74.5, 27.5, 31.5, DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(3, 51.0, 23.5, 6.9, 0.8, 74.0, 27.0, 31.0, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(3, 50.5, 23.0, 6.8, 0.7, 73.2, 26.2, 30.2, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(3, 50.0, 22.5, 6.8, 0.7, 72.5, 25.5, 29.5, DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(3, 49.5, 22.0, 6.7, 0.7, 71.8, 24.8, 28.8, DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(3, 49.2, 21.5, 6.7, 0.7, 71.0, 24.0, 28.0, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(3, 49.5, 22.0, 6.7, 0.7, 71.5, 24.5, 28.5, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(3, 50.0, 22.5, 6.8, 0.7, 72.2, 25.2, 29.2, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(3, 50.5, 23.0, 6.8, 0.8, 73.0, 26.0, 30.0, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(3, 51.0, 23.5, 6.9, 0.8, 73.8, 26.8, 30.8, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(3, 51.5, 24.0, 6.9, 0.8, 74.5, 27.5, 31.5, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
-- 4号田传感器数据
(4, 33.5, 21.0, 5.8, 2.0, 98.0, 48.0, 45.0, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(4, 33.0, 20.5, 5.7, 1.9, 97.5, 47.5, 44.5, DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(4, 32.5, 20.0, 5.7, 1.9, 97.0, 47.0, 44.0, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(4, 32.0, 19.5, 5.6, 1.8, 96.2, 46.2, 43.2, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(4, 31.5, 19.0, 5.6, 1.8, 95.5, 45.5, 42.5, DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(4, 31.0, 18.5, 5.5, 1.7, 94.8, 44.8, 41.8, DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(4, 30.8, 18.0, 5.5, 1.7, 94.0, 44.0, 41.0, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(4, 31.2, 18.5, 5.5, 1.7, 94.5, 44.5, 41.5, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(4, 32.0, 19.0, 5.6, 1.8, 95.2, 45.2, 42.2, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(4, 32.5, 19.5, 5.6, 1.8, 96.0, 46.0, 43.0, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(4, 33.0, 20.0, 5.7, 1.9, 96.8, 46.8, 43.8, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(4, 33.5, 20.5, 5.8, 2.0, 97.5, 47.5, 44.5, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
-- 5号田传感器数据
(5, 48.0, 23.5, 6.6, 1.3, 88.0, 36.0, 38.0, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(5, 47.5, 23.0, 6.5, 1.3, 87.5, 35.5, 37.5, DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(5, 47.0, 22.5, 6.5, 1.2, 87.0, 35.0, 37.0, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(5, 46.5, 22.0, 6.4, 1.2, 86.2, 34.2, 36.2, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(5, 46.0, 21.5, 6.4, 1.2, 85.5, 33.5, 35.5, DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(5, 45.5, 21.0, 6.3, 1.1, 84.8, 32.8, 34.8, DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(5, 45.2, 20.5, 6.3, 1.1, 84.0, 32.0, 34.0, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(5, 45.5, 21.0, 6.3, 1.1, 84.5, 32.5, 34.5, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(5, 46.0, 21.5, 6.4, 1.2, 85.2, 33.2, 35.2, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(5, 46.5, 22.0, 6.4, 1.2, 86.0, 34.0, 36.0, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(5, 47.0, 22.5, 6.5, 1.2, 86.8, 34.8, 36.8, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(5, 47.5, 23.0, 6.5, 1.3, 87.5, 35.5, 37.5, DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- 插入灌溉设备数据
INSERT INTO irrigation_device (device_code, device_name, field_id, device_type, status, flow_rate, current_task_id, location, last_start_time, total_run_time) VALUES
('IRR20240001', '灌溉设备-01', 1, 1, 2, 2.5, 1, '1号田东区', DATE_SUB(NOW(), INTERVAL 30 MINUTE), 1560),
('IRR20240002', '灌溉设备-02', 2, 2, 2, 2.3, 2, '2号田西区', DATE_SUB(NOW(), INTERVAL 15 MINUTE), 1280),
('IRR20240003', '灌溉设备-03', 3, 1, 1, 2.5, NULL, '3号田南区', NULL, 980),
('IRR20240004', '灌溉设备-04', 4, 3, 1, 2.4, NULL, '4号田北区', NULL, 720),
('IRR20240005', '灌溉设备-05', 5, 2, 1, 2.5, NULL, '5号田中心', NULL, 650),
('IRR20240006', '灌溉设备-06', 1, 1, 0, 2.3, NULL, '1号田西区', NULL, 420),
('IRR20240007', '灌溉设备-07', 6, 3, 1, 2.6, NULL, '6号田东区', DATE_SUB(NOW(), INTERVAL 2 HOUR), 890),
('IRR20240008', '灌溉设备-08', 7, 2, 1, 2.2, NULL, '7号田西区', NULL, 560),
('IRR20240009', '灌溉设备-09', 8, 1, 0, 2.4, NULL, '8号田南区', NULL, 380),
('IRR20240010', '灌溉设备-10', 10, 2, 1, 2.5, NULL, '10号田北区', DATE_SUB(NOW(), INTERVAL 1 HOUR), 1150),
('IRR20240011', '灌溉设备-11', 12, 1, 1, 2.6, NULL, '12号田中心', NULL, 780),
('IRR20240012', '灌溉设备-12', 5, 3, 1, 2.3, NULL, '5号田东区', DATE_SUB(NOW(), INTERVAL 45 MINUTE), 920);

-- 插入灌溉任务数据
INSERT INTO irrigation_task (task_name, device_id, status, trigger_type, plan_start_time, actual_start_time, actual_end_time, duration, water_usage, create_by) VALUES
('早灌任务-1号田', 1, 2, 1, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), 60, 4.5, 'admin'),
('午灌任务-2号田', 2, 2, 2, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 30 MINUTE), 45, 3.2, 'admin'),
('晚灌任务-1号田', 1, 0, 1, DATE_ADD(NOW(), INTERVAL 2 HOUR), NULL, NULL, 60, NULL, 'operator'),
('计划任务-3号田', 3, 0, 1, DATE_ADD(NOW(), INTERVAL 4 HOUR), NULL, NULL, 50, NULL, 'operator2'),
('自动灌溉-4号田', 4, 3, 2, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), 40, 2.8, 'system'),
('补灌任务-5号田', 5, 3, 1, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR), 30, 2.0, 'admin'),
('紧急灌溉-7号田', 7, 3, 1, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR), 45, 3.5, 'manager'),
('轮灌任务-10号田', 10, 2, 2, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), 55, 4.0, 'system'),
('定期维护-8号田', 8, 0, 1, DATE_ADD(NOW(), INTERVAL 6 HOUR), NULL, NULL, 20, NULL, 'operator'),
('苗期灌溉-12号田', 11, 0, 1, DATE_ADD(NOW(), INTERVAL 8 HOUR), NULL, NULL, 40, NULL, 'tech1'),
('分蘖期灌溉-1号田', 1, 2, 2, DATE_SUB(NOW(), INTERVAL 24 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), DATE_SUB(NOW(), INTERVAL 23 HOUR), 50, 3.8, 'system'),
('抽穗期灌溉-2号田', 2, 2, 2, DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR), 60, 4.5, 'system'),
('日常灌溉-6号田', 6, 2, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 45, 3.5, 'admin'),
('灌溉任务-9号田', 9, 2, 2, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 40, 3.0, 'system');

-- 插入烘干批次数据
INSERT INTO drying_batch (batch_no, grain_type, initial_moisture, target_moisture, current_moisture, weight, status, device_id, start_time, end_time, power_usage, create_by) VALUES
('DRY20240001', '水稻', 25.5, 14.0, 14.2, 5000.0, 4, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 450.5, 'admin'),
('DRY20240002', '小麦', 22.0, 13.0, 13.5, 3500.0, 4, 2, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 320.0, 'admin'),
('DRY20240003', '玉米', 28.0, 15.0, 18.5, 4200.0, 2, 1, DATE_SUB(NOW(), INTERVAL 12 HOUR), NULL, 180.0, 'operator'),
('DRY20240004', '水稻', 24.0, 14.0, 14.0, 4800.0, 4, 3, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 420.0, 'operator2'),
('DRY20240005', '大豆', 20.0, 12.0, 12.0, 2800.0, 4, 2, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 250.0, 'admin'),
('DRY20240006', '小麦', 23.5, 13.0, 13.2, 3800.0, 4, 3, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 350.0, 'manager'),
('DRY20240007', '玉米', 27.0, 15.0, 21.0, 4500.0, 1, 1, DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL, 85.0, 'operator'),
('DRY20240008', '水稻', 26.0, 14.0, 16.5, 5200.0, 2, 2, DATE_SUB(NOW(), INTERVAL 18 HOUR), NULL, 200.0, 'operator2'),
('DRY20240009', '油菜', 18.0, 10.0, 10.0, 1500.0, 4, 3, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 120.0, 'admin'),
('DRY20240010', '水稻', 25.0, 14.0, 14.5, 4600.0, 4, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), 410.0, 'tech1'),
('DRY20240011', '小麦', 22.5, 13.0, 13.0, 3600.0, 4, 2, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), 330.0, 'tech2'),
('DRY20240012', '玉米', 28.5, 15.0, 15.0, 4000.0, 4, 3, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), 380.0, 'manager');

-- 插入仓储记录数据
INSERT INTO storage_record (batch_no, grain_type, warehouse, quantity, moisture, quality, entry_date, expire_date, status, operator) VALUES
('DRY20240001', '水稻', 'A库-01号仓', 48.5, 14.2, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 180 DAY), 0, 'warehouse'),
('DRY20240002', '小麦', 'A库-02号仓', 34.2, 13.5, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 365 DAY), 0, 'warehouse'),
('DRY20240004', '水稻', 'B库-01号仓', 45.8, 14.0, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 180 DAY), 0, 'warehouse'),
('DRY20240005', '大豆', 'B库-02号仓', 26.5, 12.0, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 365 DAY), 0, 'warehouse'),
('DRY20240006', '小麦', 'C库-01号仓', 36.5, 13.2, 2, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 365 DAY), 0, 'warehouse'),
('DRY20240009', '油菜', 'C库-02号仓', 14.2, 10.0, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 180 DAY), 0, 'warehouse'),
('DRY20240010', '水稻', 'A库-03号仓', 44.2, 14.5, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 180 DAY), 0, 'warehouse'),
('DRY20240011', '小麦', 'A库-04号仓', 35.0, 13.0, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_ADD(NOW(), INTERVAL 365 DAY), 0, 'warehouse'),
('DRY20240012', '玉米', 'B库-03号仓', 38.5, 15.0, 2, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_ADD(NOW(), INTERVAL 365 DAY), 0, 'warehouse'),
('STO20240001', '水稻', 'A库-01号仓', -5.0, 14.0, 1, DATE_SUB(NOW(), INTERVAL 15 DAY), NULL, 1, 'warehouse'),
('STO20240002', '小麦', 'A库-02号仓', -3.5, 13.5, 2, DATE_SUB(NOW(), INTERVAL 20 DAY), NULL, 1, 'warehouse'),
('STO20240003', '玉米', 'B库-01号仓', -4.2, 15.0, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), NULL, 1, 'warehouse');

-- 气象数据表
CREATE TABLE IF NOT EXISTS weather_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    soil_id BIGINT COMMENT '关联土壤传感器ID',
    temperature DOUBLE COMMENT '温度(°C)',
    humidity DOUBLE COMMENT '湿度(%)',
    wind_speed DOUBLE COMMENT '风速(m/s)',
    wind_direction DOUBLE COMMENT '风向(°)',
    pressure DOUBLE COMMENT '气压(hPa)',
    weather_code VARCHAR(20) COMMENT '天气代码: sunny/cloudy/rainy/stormy/foggy',
    collect_time DATETIME NOT NULL COMMENT '采集时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='气象数据表';

-- 气象预报表
CREATE TABLE IF NOT EXISTS weather_forecast (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    forecast_date DATE NOT NULL COMMENT '预报日期',
    weather_code VARCHAR(20) COMMENT '天气代码',
    temp_high DOUBLE COMMENT '最高温度(°C)',
    temp_low DOUBLE COMMENT '最低温度(°C)',
    humidity DOUBLE COMMENT '湿度(%)',
    wind_speed DOUBLE COMMENT '风速(m/s)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='气象预报表';

-- 插入24小时气象数据（最近24小时每小时一条）
INSERT INTO weather_data (soil_id, temperature, humidity, wind_speed, wind_direction, pressure, weather_code, collect_time) VALUES
(1, 18.0, 72, 2.5, 90, 1013, 'sunny', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(1, 17.5, 74, 2.3, 95, 1013, 'sunny', DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(1, 17.2, 75, 2.2, 100, 1012, 'sunny', DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(1, 16.8, 76, 2.0, 105, 1012, 'sunny', DATE_SUB(NOW(), INTERVAL 21 HOUR)),
(1, 16.5, 78, 1.8, 110, 1012, 'sunny', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(1, 17.0, 76, 1.5, 115, 1013, 'sunny', DATE_SUB(NOW(), INTERVAL 19 HOUR)),
(1, 18.5, 72, 1.8, 120, 1013, 'sunny', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(1, 20.0, 68, 2.0, 130, 1014, 'sunny', DATE_SUB(NOW(), INTERVAL 17 HOUR)),
(1, 21.5, 65, 2.3, 140, 1014, 'sunny', DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(1, 23.0, 62, 2.5, 150, 1014, 'sunny', DATE_SUB(NOW(), INTERVAL 15 HOUR)),
(1, 24.0, 60, 2.8, 160, 1014, 'sunny', DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(1, 25.0, 58, 3.0, 170, 1013, 'sunny', DATE_SUB(NOW(), INTERVAL 13 HOUR)),
(1, 25.5, 57, 3.2, 180, 1013, 'sunny', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(1, 26.0, 55, 3.5, 185, 1012, 'sunny', DATE_SUB(NOW(), INTERVAL 11 HOUR)),
(1, 25.8, 56, 3.3, 190, 1012, 'sunny', DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(1, 24.5, 58, 3.0, 195, 1012, 'sunny', DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(1, 23.5, 60, 2.8, 200, 1013, 'sunny', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(1, 22.5, 62, 2.5, 210, 1013, 'sunny', DATE_SUB(NOW(), INTERVAL 7 HOUR)),
(1, 21.0, 65, 2.3, 220, 1013, 'sunny', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(1, 20.0, 67, 2.0, 230, 1014, 'sunny', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(1, 19.5, 68, 1.8, 240, 1014, 'sunny', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(1, 18.8, 70, 2.0, 250, 1014, 'cloudy', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(1, 18.2, 71, 2.2, 260, 1014, 'cloudy', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(1, 17.5, 72, 2.4, 270, 1013, 'cloudy', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1, 17.0, 73, 2.5, 280, 1013, 'cloudy', NOW());

-- 插入3天预报数据
INSERT INTO weather_forecast (forecast_date, weather_code, temp_high, temp_low, humidity, wind_speed) VALUES
(CURDATE(), 'sunny', 26, 18, 65, 2.5),
(CURDATE() + INTERVAL 1 DAY, 'cloudy', 24, 17, 70, 3.0),
(CURDATE() + INTERVAL 2 DAY, 'rainy', 22, 16, 80, 4.0);

-- 虫情设备表
CREATE TABLE IF NOT EXISTS insect_device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    remote_id INT COMMENT '外部平台设备ID',
    dev_num VARCHAR(30) COMMENT '设备编号',
    imei VARCHAR(50) COMMENT '设备IMEI',
    dev_name VARCHAR(50) COMMENT '设备名称',
    dev_type VARCHAR(20) COMMENT '设备类型',
    dev_type_name VARCHAR(50) COMMENT '设备类型名称',
    lng DECIMAL(10,6) COMMENT '经度',
    lat DECIMAL(10,6) COMMENT '纬度',
    dev_state TINYINT COMMENT '设备状态:0-离线 1-在线',
    online_status VARCHAR(20) COMMENT '在线状态',
    last_data_time DATETIME COMMENT '最后数据时间',
    address VARCHAR(100) COMMENT '地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='虫情设备表';

-- 虫情数据表
CREATE TABLE IF NOT EXISTS insect_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    imei VARCHAR(50) COMMENT '设备IMEI',
    dev_name VARCHAR(50) COMMENT '设备名称',
    record_time DATETIME COMMENT '图片记录时间',
    image_url VARCHAR(255) COMMENT '图片URL',
    scale_image_url VARCHAR(255) COMMENT '缩略图URL',
    plot_image_url VARCHAR(255) COMMENT '分析结果图URL',
    object_count INT COMMENT '识别数量',
    detect_result TEXT COMMENT '识别结果JSON',
    store_path VARCHAR(255) COMMENT '图片存储路径',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='虫情数据表';

-- 插入虫情设备数据
INSERT INTO insect_device (remote_id, dev_num, imei, dev_name, dev_type, dev_type_name, lng, lat, dev_state, online_status, last_data_time, address) VALUES
(2, '83addcdf249', 'f249', '虫情设备-01', '0', '虫情设备', 119.164180, 36.743003, 1, '在线', '2025-07-02 20:43:04', '山东省潍坊市寒亭区开元街道停车场'),
(3, '83addcdf250', 'f250', '虫情设备-02', '0', '虫情设备', 119.165000, 36.744000, 1, '在线', '2025-07-02 19:30:00', '山东省潍坊市寒亭区农业园区'),
(4, '83addcdf251', 'f251', '虫情设备-03', '0', '虫情设备', 119.166000, 36.745000, 0, '离线', '2025-07-01 18:00:00', '山东省潍坊市寒亭区小麦基地');

-- 插入虫情数据（模拟数据）
INSERT INTO insect_data (imei, dev_name, record_time, image_url, scale_image_url, plot_image_url, object_count, detect_result, store_path) VALUES
('f249', '虫情设备-01', DATE_SUB(NOW(), INTERVAL 2 HOUR), 'uploads/insect/f249/20250702_1.jpg', 'uploads/insect/f249/thumb_20250702_1.jpg', 'uploads/insect/f249/plot_20250702_1.jpg', 12, '[{"name":"稻飞虱","count":8},{"name":"蚜虫","count":4}]', 'f249/20250702_1.jpg'),
('f249', '虫情设备-01', DATE_SUB(NOW(), INTERVAL 5 HOUR), 'uploads/insect/f249/20250702_2.jpg', 'uploads/insect/f249/thumb_20250702_2.jpg', 'uploads/insect/f249/plot_20250702_2.jpg', 5, '[{"name":"稻飞虱","count":3},{"name":"蚜虫","count":2}]', 'f249/20250702_2.jpg'),
('f249', '虫情设备-01', DATE_SUB(NOW(), INTERVAL 8 HOUR), 'uploads/insect/f249/20250702_3.jpg', 'uploads/insect/f249/thumb_20250702_3.jpg', 'uploads/insect/f249/plot_20250702_3.jpg', 0, '[]', 'f249/20250702_3.jpg'),
('f250', '虫情设备-02', DATE_SUB(NOW(), INTERVAL 3 HOUR), 'uploads/insect/f250/20250702_1.jpg', 'uploads/insect/f250/thumb_20250702_1.jpg', 'uploads/insect/f250/plot_20250702_1.jpg', 7, '[{"name":"粘虫","count":5},{"name":"蚜虫","count":2}]', 'f250/20250702_1.jpg'),
('f250', '虫情设备-02', DATE_SUB(NOW(), INTERVAL 6 HOUR), 'uploads/insect/f250/20250702_2.jpg', 'uploads/insect/f250/thumb_20250702_2.jpg', 'uploads/insect/f250/plot_20250702_2.jpg', 15, '[{"name":"粘虫","count":10},{"name":"稻飞虱","count":5}]', 'f250/20250702_2.jpg'),
('f250', '虫情设备-02', DATE_SUB(NOW(), INTERVAL 9 HOUR), 'uploads/insect/f250/20250702_3.jpg', 'uploads/insect/f250/thumb_20250702_3.jpg', 'uploads/insect/f250/plot_20250702_3.jpg', 3, '[{"name":"蚜虫","count":3}]', 'f250/20250702_3.jpg'),
('f251', '虫情设备-03', DATE_SUB(NOW(), INTERVAL 24 HOUR), 'uploads/insect/f251/20250701_1.jpg', 'uploads/insect/f251/thumb_20250701_1.jpg', 'uploads/insect/f251/plot_20250701_1.jpg', 20, '[{"name":"稻纵卷叶螟","count":12},{"name":"稻飞虱","count":8}]', 'f251/20250701_1.jpg'),
('f251', '虫情设备-03', DATE_SUB(NOW(), INTERVAL 48 HOUR), 'uploads/insect/f251/20250701_2.jpg', 'uploads/insect/f251/thumb_20250701_2.jpg', 'uploads/insect/f251/plot_20250701_2.jpg', 8, '[{"name":"稻纵卷叶螟","count":5},{"name":"粘虫","count":3}]', 'f251/20250701_2.jpg');