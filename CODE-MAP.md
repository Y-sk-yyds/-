# CODE-MAP.md — 社区诊所门诊诊疗管理系统

> 最后更新: 2026-07-30 | Phase: Docker 部署配置完成

## 一、项目结构

```
wk6real/
├── backend/                          # Spring Boot 3 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/clinic/
│       │   ├── ClinicApplication.java
│       │   ├── common/               # 公共类
│       │   │   ├── Result.java       # 统一返回
│       │   │   ├── BaseEntity.java   # 基础实体
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── config/               # 配置
│       │   │   ├── SecurityConfig.java
│       │   │   ├── CorsConfig.java
│       │   │   ├── RedisConfig.java
│       │   │   ├── RabbitMQConfig.java
│       │   │   └── MyMetaObjectHandler.java
│       │   ├── security/             # 安全
│       │   │   ├── JwtTokenProvider.java
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   └── SecurityUtils.java
│       │   ├── controller/           # 控制器 (13个全部完成)
│       │   │   ├── AuthController.java
│       │   │   ├── PatientController.java
│       │   │   ├── DoctorController.java
│       │   │   ├── ScheduleController.java
│       │   │   ├── RegistrationController.java
│       │   │   ├── ConsultationController.java
│       │   │   ├── PrescriptionController.java
│       │   │   ├── DrugController.java
│       │   │   ├── BillController.java
│       │   │   ├── MedicalRecordController.java
│       │   │   ├── UserController.java
│       │   │   └── DashboardController.java
│       │   ├── service/              # 服务层 (13个全部完成)
│       │   │   ├── DashboardService.java
│       │   │   └── impl/
│       │   ├── mq/                    # 消息队列消费者
│       │   │   └── ReminderConsumer.java
│       │   ├── mapper/               # MyBatis-Plus Mapper (13个)
│       │   ├── entity/               # 实体类 (12个)
│       │   ├── dto/                  # 数据传输对象
│       │   └── utils/                # 工具类
│       │       ├── AesUtils.java     # AES加解密
│       │       └── RedisUtils.java   # Redis工具
│       └── resources/
│           └── application.yml
├── frontend/                         # Vue3 前端
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── api/index.js              # API封装(所有接口)
│       ├── router/index.js           # 路由(12个页面)
│       ├── stores/user.js            # Pinia用户状态
│       ├── utils/request.js          # Axios封装
│       └── views/
│           ├── layout/Login.vue      # 登录页
│           ├── layout/Layout.vue     # 主布局
│           ├── dashboard/Dashboard.vue # 工作台(ECharts)
│           ├── patient/PatientList.vue
│           ├── doctor/DoctorList.vue
│           ├── schedule/ScheduleList.vue
│           ├── registration/RegistrationList.vue
│           ├── consultation/ConsultationList.vue
│           ├── prescription/PrescriptionList.vue
│           ├── drug/DrugList.vue
│           ├── bill/BillList.vue
│           ├── record/RecordList.vue
│           └── user/UserList.vue
└── sql/
    └── init.sql                       # 数据库初始化(12张表+种子数据)
```

## 二、数据库表 (13张)

| # | 表名 | 说明 | 关键字段 |
|---|------|------|---------|
| 1 | sys_user | 系统用户 | username, password(BCrypt), role(ADMIN/DOCTOR/CASHIER) |
| 2 | patient | 患者 | name, id_card, phone, allergy_history |
| 3 | doctor | 医生 | name, department, title, user_id |
| 4 | schedule | 排班 | doctor_id, work_date, period(AM/PM), total_num, remain_num |
| 5 | registration | 挂号 | patient_id, schedule_id, queue_num, status(0-3) |
| 6 | consultation | 就诊 | registration_id, doctor_id, complaint, diagnosis, status(0-2) |
| 7 | prescription | 处方 | consultation_id, diagnosis, prescription_content, status(0-1) |
| 8 | prescription_drug | 处方药品 | prescription_id, drug_id, quantity, usage_method |
| 9 | drug | 药品 | name, specification, unit, unit_price, category |
| 10 | drug_stock | 药品库存 | drug_id, current_stock, warning_threshold |
| 11 | bill | 账单 | registration_id, prescription_id, total_amount, payment_method |
| 12 | medical_record | 病历 | patient_id, consultation_id, content(AES加密) |
| 13 | reminder | 复诊提醒 | patient_id, consultation_id, remind_date, status(0-2) |

## 三、API 端点规划 (17个)

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 用户登录 |

### 患者管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/patients | 患者列表(分页+搜索) |
| POST | /api/patients | 新增患者 |
| PUT | /api/patients | 更新患者 |
| DELETE | /api/patients/{id} | 删除患者 |

### 医生管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/doctors | 医生列表 |
| POST | /api/doctors | 新增医生 |
| PUT | /api/doctors | 更新医生 |
| DELETE | /api/doctors/{id} | 删除医生 |

### 排班管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/schedules | 排班列表 |
| POST | /api/schedules | 新增排班 |

### 挂号管理 (Redis并发优化)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/registrations | 挂号列表 |
| POST | /api/registrations | 创建挂号(Redis原子扣减) |
| PUT | /api/registrations/{id}/cancel | 取消挂号(回退号源) |

### 就诊管理
| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | /api/consultations/{id}/start | 开始就诊 |
| PUT | /api/consultations/{id}/complete | 完成就诊 |

### 处方管理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/prescriptions | 开具处方(Redis锁库存) |

### 药品管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/drugs | 药品列表 |

### 账单管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/bills | 账单列表 |
| PUT | /api/bills/{id}/pay | 收费结算 |

### 病历管理 (AES加密)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/records | 病历列表 |
| GET | /api/records/{id} | 病历详情(解密) |

## 四、前端页面依赖 (11页面)

| 页面 | 路由 | 角色 | 状态 |
|------|------|------|------|
| 登录 | /login | 所有 | ✅ 完成 |
| 工作台 | /dashboard | 所有 | ✅ 完成 |
| 患者管理 | /patient | 所有 | ✅ 完成 |
| 医生管理 | /doctor | ADMIN | ✅ 完成 |
| 排班管理 | /schedule | ADMIN | ✅ 完成 |
| 挂号管理 | /registration | 所有 | ✅ 完成 |
| 就诊管理 | /consultation | DOCTOR | ✅ 完成 |
| 处方管理 | /prescription | DOCTOR | ✅ 完成 |
| 药品管理 | /drug | ADMIN | ✅ 完成 |
| 收费管理 | /bill | CASHIER | ✅ 完成 |
| 病历管理 | /record | DOCTOR | ✅ 完成 |
| 用户管理 | /user | ADMIN | ✅ 完成 |

## 五、中间件应用点

| 中间件 | 场景 | 实现方式 |
|--------|------|---------|
| Redis | 挂号号源扣减 | String + Lua脚本原子操作 |
| Redis | 医生排班缓存 | Hash缓存，定时刷新 |
| Redis | 药品库存扣减 | 分布式锁(SETNX) |
| Redis | 早高峰限流 | ZSET滑动窗口计数 |
| RabbitMQ | 复诊提醒 | Direct Exchange + Queue，异步发送短信/站内提醒 |
| AES-256 | 病历加密 | AES/CBC/PKCS5Padding，密钥配置化 |

## 六、Docker 部署

| 文件 | 用途 |
|------|------|
| `.env` | 所有密码/密钥集中管理 |
| `docker-compose.yml` | 5 服务编排：MySQL + Redis + RabbitMQ + 后端 + 前端 |
| `backend/Dockerfile` | Maven 多阶段构建 → JRE 运行 |
| `backend/src/main/resources/application-docker.yml` | 环境变量注入密码 |
| `frontend/Dockerfile` | Node 构建 → Nginx 托管 |
| `frontend/nginx.conf` | SPA fallback + /api 反向代理 |

部署命令：
```bash
docker-compose up -d                # 首次启动（自动构建镜像）
docker-compose down                 # 停止
docker-compose up -d --build        # 代码更新后重新构建
docker-compose logs -f backend      # 查看后端日志
```

## 七、开发阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| P1 | 项目骨架搭建 | ✅ 完成 |
| P2 | 登录认证联调 | ✅ 完成 |
| P3 | 患者+医生+排班 CRUD | ✅ 完成 |
| P4 | 挂号+Redis并发 | ✅ 完成 |
| P5 | 就诊+处方+药品+收费 | ✅ 完成 |
| P6 | 病历AES+复诊提醒+仪表盘+用户管理 | ✅ 完成 |
| P7 | 移动端患者服务（H5 手机网页，3页面，免登录） | ✅ 完成 |

## 八、移动端患者服务

| 文件 | 用途 |
|------|------|
| `backend/src/main/java/.../MobileController.java` | 3个免登录 API：排班、挂号、我的数据+支付 |
| `frontend/src/views/mobile/MobileHome.vue` | 排班挂号页（日期选择+医生卡片+手机挂号） |
| `frontend/src/views/mobile/MobileRecords.vue` | 我的就诊页（挂号列表+排队看板+病历+手机支付） |

访问：`http://localhost/mobile`，无需登录，输入患者手机号
