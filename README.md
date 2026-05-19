# 智慧农业管理平台 - 江苏维明

基于 Spring Boot 3 + Vue3 的智慧农业管理系统

## 技术栈

- **后端**: Java 17 + Spring Boot 3.2 + MyBatis-Plus + Sa-Token
- **前端**: Vue3 + Element Plus + ECharts
- **数据库**: MySQL 8.0 + Redis 7
- **消息**: EMQX (MQTT)

## 快速启动

### 方式一: Docker Compose (推荐)

```bash
# 克隆项目
cd smart-agriculture-v2

# 一键启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

启动后访问:
- 前端页面: http://localhost
- 后端API: http://localhost:8080
- EMQX控制台: http://localhost:18083 (admin/public)

### 方式二: 本地开发

**后端启动:**
```bash
cd backend
mvn spring-boot:run
```

**前端启动:**
```bash
cd frontend
npm install
npm run dev
```

## 项目结构

```
smart-agriculture-v2/
├── backend/          # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/weiming/smartag/
│   │       ├── entity/      # 实体类
│   │       ├── mapper/      # 数据访问层
│   │       ├── service/     # 业务逻辑层
│   │       ├── controller/  # 控制层
│   │       └── config/      # 配置类
│   └── Dockerfile
├── src/              # Vue3 前端
│   ├── views/        # 页面组件
│   ├── components/   # 公共组件
│   ├── api/          # API接口
│   └── router/       # 路由配置
├── docker-compose.yml
└── README.md
```

## 功能模块

- ✅ 智慧大屏 - 数据可视化展示
- ✅ 智慧种植 - 土壤监测、智能灌溉、气象监测、虫情预警
- ✅ 绿色烘干 - 烘干批次管理、工艺曲线
- ✅ 智慧仓储 - 库存管理、出入库登记
- ✅ 能耗管理 - 用电统计、能效分析
- ✅ 系统管理 - 设备管理、用户权限

## 默认账号

- 管理员: admin / 123456
- 操作员: operator / 123456

## 许可证

MIT License