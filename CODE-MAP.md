# CODE-MAP.md — 社区诊所门诊诊疗管理系统

> 最后更新: 2026-09-30（补录 7-30 之后的进展：种子数据扩充/工具脚本/文档索引；代码侧 P1-P7 全完成，卡点转为论文收尾）

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
├── tools/                             # 开发辅助（不参与部署，可整体删除）
│   ├── init_template.sql              #   生成器的输入模板（13 表 DDL + 最少种子）——勿直接执行
│   └── gen_init_full_log.txt          #   最近一次生成的统计
└── sql/
    └── init.sql                       # ★ **唯一的数据库脚本**（13 张表 + 全量演示数据）
                                       #   docker-compose.yml 与 reset.bat 都指向它，不要再新增别的 sql
                                       #   规模：8医生/30患者/224排班/109挂号/86病历/70处方/109账单
                                       #   约定①过去排班 deleted=1 归档 ②挂号费按医生分别定价（10~50 梯度）
                                       #   ③病历密文须匹配当前 AES 密钥 ④号源 remain_num 由 SQL 重算
                                       #   ⑤★排班2是集成测试夹具，禁止追加挂号（详见第十二节）
```

## 二、数据库表 (13张)

| # | 表名 | 说明 | 关键字段 |
|---|------|------|---------|
| 1 | sys_user | 系统用户 | username, password(BCrypt), role(ADMIN/DOCTOR/CASHIER) |
| 2 | patient | 患者 | name, id_card, phone, allergy_history |
| 3 | doctor | 医生 | name, department, title, registration_fee(挂号费), user_id |
| 4 | schedule | 排班 | doctor_id, work_date, period(AM/PM), total_num, remain_num |
| 5 | registration | 挂号 | patient_id, schedule_id, queue_num, status(0-3) |
| 6 | consultation | 就诊 | registration_id, doctor_id, complaint, diagnosis, status(0-2) |
| 7 | prescription | 处方 | consultation_id, diagnosis, prescription_content, status(0-1) |
| 8 | prescription_drug | 处方药品 | prescription_id, drug_id, quantity, usage_method |
| 9 | drug | 药品 | name, specification, unit, unit_price, category, status(1在用/0下架), produce_date, expire_date |
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
| GET | /api/drugs | 药品列表（可选 `status` 筛选：1在用 / 0已下架；不传查全部） |

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

### 定时任务 / 启动补偿
| 组件 | 触发 | 作用 |
|------|------|------|
| `ReminderScheduler` | `@Scheduled` cron | 扫描到期复诊提醒 → 投递 RabbitMQ |
| `DrugExpiryScheduler` | `@Scheduled(cron = "0 30 0 * * ?")` + `ApplicationReadyEvent` | 过期药品自动下架（批量 `status→0`），启动时补偿扫描一次 |
| `DashboardService.buildTodos()` | 实时聚合 | 药品近效期(30天内)与已过期（不按 status 过滤，下架后持续提醒）/ 库存预警 / 待收费 / 复诊提醒 |

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
| P8 | 挂号费按医生分级定价 + 药品效期(生产/过期日期)与过期自动下架 | ✅ 完成并实测通过 |

## 八、移动端患者服务

| 文件 | 用途 |
|------|------|
| `backend/src/main/java/.../MobileController.java` | 3个免登录 API：排班、挂号、我的数据+支付 |
| `frontend/src/views/mobile/MobileHome.vue` | 排班挂号页（日期选择+医生卡片+手机挂号） |
| `frontend/src/views/mobile/MobileRecords.vue` | 我的就诊页（挂号列表+排队看板+病历+手机支付） |

访问：`http://localhost/mobile`，无需登录，输入患者手机号

## 九、工具脚本（根目录 + tools/，按用途区分）

| 脚本 | 类型 | 说明 |
|------|------|------|
| `gen_init_full.py` | **生成器（可重跑）** | 读 `tools/init_template.sql` → 追加演示数据 → **覆盖** `sql/init.sql`（原地）。调整数据量改这里再重跑。日志：`tools/gen_init_full_log.txt` |
| `add_aes_sec.py` | 一次性改造 | AES-128 → AES-256 升级脚本（已完成，保留备查） |
| `add_ch1_cites.py` / `add_perf_test.py` | 一次性文档 | 论文第一章引用补录 / 性能测试章节补录（已完成） |
| `concurrent_test.py` / `demo_test.py` | 测试脚本 | 并发/演示用途（非正式测试，正式性能数据见 docs/测试报告.md 的 JMeter 结果） |
| `fix_55.py` 等 | 一次性修正 | 历史数据/代码修正脚本，保留备查 |

**约定**：一次性脚本已完成使命，勿重跑（会重复改文件）；只有 `gen_init_full.py` 是设计为可重跑的。

## 十、文档索引（docs/）

| 文件 | 内容 | 状态 |
|------|------|------|
| `毕业论文.docx` | 七章 + 参考文献 + 致谢（约 1.2 万字） | 🟡 待收尾：封面信息/参考文献格式/字号行距/贴图 |
| `开题报告.docx` / `开题报告_真实文献版.docx` | 开题（真实文献版为定稿） | ✅ |
| `技术文档.md` | 架构/模块/接口说明 | ✅（集成测试数字已对齐代码 28/7） |
| `测试报告.md` | 19 测试用例 + 2 组 JMeter 压测数据（零超卖） | ✅（同上） |
| `答辩PPT.pptx` | 12 页（已插系统截图） | ✅ |
| `ER图.html` / `ER图_纯净版.html` | 数据库 ER 图 | ✅ |
| `screenshots/` | 系统截图集（PC 页面 6 张等） | ✅ |
| `部署说明.docx` | Docker 部署步骤 | ✅ |

## 十一、论文与代码的耦合点（写论文/改代码前必看）

- **AES 密钥**：论文描述的 AES-256-CBC（key 32 字节、IV `ClinicSystem-IV-`）必须与 `application.yml` 的 `aes.secret-key` 一致；换密钥后 `sql/init.sql` 里的病历密文**必须重新加密**，否则病历解密全挂。密文由 `gen_init_full.py`（读 `tools/init_template.sql`）在生成时加密，密钥常量在同一文件顶部
- **挂号费**：已由固定 10 元改为**按医生定价**（`doctor.registration_fee`，梯度 10.00~50.00）。`BillService.resolveRegistrationFee(registrationId)` 走「挂号→排班→医生」取价，取不到时兜底 `DEFAULT_REGISTRATION_FEE = 10.00`。论文若写"统一 10 元"需同步改为分级收费；`BillService` 的 `drugFee` 仍按处方实价
- **药品效期**：`expire_date <= CURDATE() + 30 天` 即视为**近效期**并自动下架、禁止开具（"有效期至 X"当天仍完整有效）。`DrugExpiryScheduler.NEAR_EXPIRY_DAYS = 30` 被**调度器 / 工作台 / 处方校验三处共用**，改阈值须三处联动
- **药品录入必填**：`produce_date` 与 `expire_date` 为**接口层必填**（`DrugController.checkExpiryDates()`，新增/编辑都校验，不允许清空）。但 `drug` 表两列仍是 NULL 可空 —— 因为 `DrugStockLockTest.setUp()` 用 `drugMapper.insert()` 直插不带日期的药品，若改成 NOT NULL 会使该单元测试变红
- **药品状态语义**：`status=1` 在用 / `0` 已下架。药品列表**默认查全部**（不做默认过滤），否则自动下架后管理员看不到、无法处理
- **性能数据（★两套数字别混）**：论文第六章同时有两组并发数据，来源不同、都对，切勿互相"对齐"：
  - **JUnit 集成测试** `testRedisLuaAtomic`：35 并发抢**剩余 28** 号源 → **成功 28 / 拒绝 7**（断言写死，见下）
  - **JMeter 压测**：35 并发抢**全空 30** 号源 → 成功 30 / 拒绝 5，平均 137ms、QPS 137.8（论文 5.1 与 6.4 引用的是这一组）
  - 库存 SETNX：30 并发抢 5 盒 → 成功 5 / 失败 25
- **页面数量**：11 PC 页 + 登录页 + 2 H5 页 = 14 页（论文与 CODE-MAP 口径一致）

## 十二、集成测试夹具与可重复性（跑测试前必看）

`MiddlewareIntegrationTest.testRedisLuaAtomic()` 断言 `succ == 28` / `fail == 7`，
这个数字**完全由排班 2 的剩余号源决定**，链路：

```
schedule.remain_num ──setIfAbsent("schedule:stock:2")──▶ Redis ──Lua GET/DECR──▶ 成功次数 ──▶ 断言
```

因此有三条硬约束：

1. **排班 2 = 张医生 今日下午 是测试夹具**：`total_num=30`、init.sql 种入 2 条有效挂号 → `remain_num=28`。
   `gen_init_full.py` 的 `TODAY_PLAN` **不得**再向排班 2 追加挂号，否则变 26，断言必红。
2. **该用例有状态、不可连跑**：跑完一次会把 `remain_num` 消耗到 0，并在 patients 100~134 留下
   `status=1` 的挂号。第二次运行会因"您已挂过该号源"+ 号源为 0 而全败。
   → **每次跑测试前必须重建库 + `FLUSHDB`**。
3. **Redis 必须先清**：`FLUSHDB` 后号源才会从 MySQL 的 `remain_num` 重新初始化。

标准流程：

**优先用项目根目录的一键脚本 `reset.bat`**（= 重建库 + 清 Redis，密码路径都已配好）：

```bash
./reset.bat                                       # ①+② 重建库 + FLUSHDB
cd backend && mvn test -Dtest=MiddlewareIntegrationTest   # ③ 跑测试
./reset.bat                                       # ④ 跑完再重置一次，恢复干净演示数据
```

等价的手工命令（仅在后台无人值守、`pause` 会卡住时使用）：

```bash
mysql -uroot -p789456 < sql/init.sql               # ① 重建库（脚本内含 DROP TABLE，可反复执行）
docker exec clinic-redis redis-cli FLUSHDB          # ② 清号源缓存
```

> ⚠️ 不要为了"重置数据库"在 `sql/` 下新增脚本或备份——已有 `reset.bat` + 唯一的 `sql/init.sql` 足够。

实测结果（2026-09-30）：`并发35请求: 成功28个, 被拒7个` → `Tests run: 2, Failures: 0, Errors: 0` → `BUILD SUCCESS`

