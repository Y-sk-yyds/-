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
| POST | /api/bills | 手工创建账单（兼容入口，按 prescriptionId 自动分流） |
| PUT | /api/bills/{id}/pay | 收费结算 |

#### ★ 账单「分单结算」（2026-10-02 改造，**改账单相关代码前必读**）

**一条挂号会产生两张彼此独立的账单**，各自的支付状态互不影响：

| 账单 | 生成时机 | `prescription_id` | `registration_fee` | `drug_fee` | 识别方式 |
|------|---------|:---:|:---:|:---:|------|
| **挂号费单** | 挂号时 | `NULL` | 挂号费 | 0.00 | `prescription_id IS NULL` |
| **药费单** | 医生开方后 | 处方ID | 0.00 | 药费 | `prescription_id IS NOT NULL` |

**为什么必须分开**：改造前是一张账单承载"挂号费+药费"，挂号时创建、开方时**原地追加**药费。
一旦患者挂号后先付了挂号费（status=1），开方追加药费只会改金额、**不会重置状态**
→ 药费被白送、收银台也看不到待收款。分单后此问题从根上消失。

**BillService 方法**：
- `createBill(regId, prescId)` —— 兼容入口，按 `prescId` 是否为 null 自动分流
- `createRegistrationBill(regId)` —— 挂号费单，同挂号复用已有记录（重复提交不重复计费）
- `createDrugBill(regId, prescId)` —— 药费单，已收费则不再改金额
- `resolveFeeType(bill)` —— **静态方法**，由金额字段推导费用类型（未加数据库列）：
  `REGISTRATION` / `DRUG` / `MIXED`（历史数据：两者都有）

**⚠️ 调用约定**：
- 挂号（PC `RegistrationController`）→ `createRegistrationBill(regId)`，生成**待收**账单，由收银台收款
- 挂号（移动端 `MobileController.register`）→ **「先缴费，后挂号」一次调用完成**：
  `createRegistration`（Redis Lua 扣号源）→ `createRegistrationBill` → `pay`，
  任一步抛异常则 `removeById(bill)` + `cancelRegistration(regId)` 整体回退；
  请求体可带 `paymentMethod`（默认 `WECHAT`），库中**不产生"已挂号但欠费"的中间态**
- 开方（`PrescriptionController`）→ `createDrugBill(regId, prescId)`
- **查账单禁用 `getOne(registrationId)`**：一条挂号有多张账单，`getOne` 会抛
  `TooManyResultsException`。要用 `list(...in(regIds))` 再内存分组
  （`MobileController.myData` 已按此改造）

**前端展示**：`BillList.vue` 用 `feeTypeLabel/feeTypeTag/feeDetail` 由金额推导显示
「挂号费 / 药费 / 挂号费+药费」；移动端 `MobileRecords.vue` 在一个挂号卡片下**逐行**渲染
`r.bills[]`，每行独立"去支付"。

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
| P9 | 移动端体验：排班页科室分类+搜索、就诊页按日期分区、排队看板分天分组 | ✅ 完成并实测通过 |
| P10 | 就诊页：排队看板聚焦当天 + 历史记录折叠 3 笔 | ✅ 完成并实测通过 |
| P11 | 账单分单结算（挂号费单 / 药费单，靠 `bill.prescription_id` 区分，零表结构变更） | ✅ 完成并实测通过 |
| P12 | 移动端挂号「先缴费、后挂号」一次调用闭环（PC 端不改） | ✅ 完成并实测通过 |
| P13 | 处方开方药品临期提醒（3 个月 WARN 预警线 + 种子补 2 个 WARN 药） | ✅ 完成并实测通过 |

## 八、移动端患者服务

| 文件 | 用途 |
|------|------|
| `backend/src/main/java/.../MobileController.java` | 5 个免登录 API：排班、挂号、自助建档、我的数据、账单支付 |
| `backend/src/main/java/.../mapper/ScheduleMapper.java` | `selectByIdsIncludeDeleted` —— **绕过逻辑删除**取已归档排班（历史就诊回显日期必需） |
| `frontend/src/views/mobile/MobileHome.vue` | 排班挂号页（搜索框 + 科室标签 + 日期选择 + 医生卡片 + 挂号缴费一体） |
| `frontend/src/views/mobile/MobileRecords.vue` | 我的就诊页（挂号分区列表 + 历史折叠 + 今日排队看板 + 病历 + 手机支付） |

访问：`http://localhost:5173/mobile`，无需登录，输入患者手机号

**移动端挂号流程（2026-10-02 收紧）**：确认挂号 → **缴费弹窗（金额 + 微信/支付宝，无「稍后缴费」）**
→ 支付成功才真正占号并生成挂号记录；取消付款不产生任何记录。
`POST /api/mobile/registrations` 请求体：`{ phone, scheduleId, paymentMethod }`

### `GET /api/mobile/my-data` 返回结构（2026-10-02 改造）

| 字段 | 说明 |
|------|------|
| `registrations[]` | 挂号视图，**已补全** `workDate`/`period`/`doctorName`/`department`/`title`；后端已排序（今天优先 → 未来升序 → 过去倒序，同日 AM 在前） |
| `registrations[].bills[]` | **逐笔账单**（挂号费单 / 药费单），每笔含 `id`/`feeType`/`registrationFee`/`drugFee`/`totalAmount`/`status`/`paymentMethod` → 移动端分行渲染、分别支付 |
| `registrations[].billId`/`billStatus`/`billAmount` | 兼容汇总字段：`billAmount` = 该挂号账单合计；`billStatus` = 还有未付的就是 0（进「待缴费」分区）；`billId` = 优先指向**未支付**的那张 |
| `queueGroups[]` | 排队看板，**只含「今天」的排班**（`work_date == today`），按排班分组（替代旧的单个 `queue`），每组含 `workDate`/`period`/`doctorName`/`items[]`，同日 AM 在 PM 前 → 前端渲染为「上午一块 / 下午一块」 |
| `today` | 服务端下发的当天日期，移动端据此判断"今天"，避免客户端时区误差 |

⚠️ **`registration.registration_date` 是"挂号操作那天"，不是"就诊那天"**——
按就诊日分组必须用 `schedule.work_date`。
⚠️ 历史挂号的排班已归档（`deleted=1`），**普通 `listByIds` 会被逻辑删除拦截**导致日期/医生丢失，
必须走 `scheduleMapper.selectByIdsIncludeDeleted()`。

**移动端挂号列表分区规则（互斥，空分区不渲染）**：
💳 待缴费（未支付账单，跨日期置顶）→ 📍 今日就诊 → 🗓 即将就诊 → 📁 历史记录
📁 历史记录**默认只渲染最近 3 笔**（`HISTORY_SHOW = 3`），超出时显示「展开全部 N 笔 ⌄」，
避免列表随患者使用天数无限变长；换患者/刷新后回到折叠态。

**移动端排队看板规则（2026-10-02）**：
只显示**今天**的排班，上午(AM)/下午(PM)各一块 —— 排队是"当场等叫号"的场景，
历史/未来排班的队列无参考价值且会随挂号变化，后端直接不下发。
患者其他日期的待就诊预约（`status=1 && workDate != today`）不走看板，
收成一行提示「还有其他日期的预约 N 个 ›」，点击跳回「我的挂号」。

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
- **挂号费**：已由固定 10 元改为**按医生定价**（`doctor.registration_fee`，梯度 10.00~50.00）。`BillService.resolveRegistrationFee(registrationId)` 走「挂号→排班→医生」取价，取不到时兜底 `DEFAULT_REGISTRATION_FEE = 10.00`。论文若写"统一 10 元"需同步改为分级收费；药费按处方实价
- **账单分单结算 + 移动端「先缴费、后挂号」**：挂号费与药费拆成两张独立账单（详见「三、API 接口 – 账单管理 – ★ 账单分单结算」）。
  移动端挂号已改为**一次调用闭环**（`MobileHome.vue`「确认挂号并缴费」弹窗 → `POST /api/mobile/registrations` 带 `paymentMethod`）：
  `createRegistration` → `createRegistrationBill` → `billService.pay` 三步串行，**任一步失败整体回退**（删账单 + 取消挂号，Redis 号源回补），库里不留"已挂号未缴费"中间态，返回 `paid=true`。
  药费待医生开方后作为独立账单出现在「我的就诊 → 待缴费」。
  PC 端 `BillList.vue` 用「费用类型」列区分两种单子；**PC 端挂号流程保持原样**（挂号 → 待收挂号费单 → 收银台收款），因 PC 患者登不进去
- **药品效期（两级管控，2026-10-03 起）**：
  - **下架/禁开线 30 天**：`expire_date <= CURDATE() + 30 天` 即视为**近效期**并自动下架、禁止开具（"有效期至 X"当天仍完整有效）。`DrugExpiryScheduler.NEAR_EXPIRY_DAYS = 30` 被**调度器 / 工作台 / 处方校验三处共用**，改阈值须三处联动
  - **预警线 3 个月**：`DrugExpiryScheduler.EXPIRY_WARN_MONTHS = 3`，剩余不足 3 个月但已超 30 天的药品**可正常开具**，仅标记为 `WARN` 级别下发，开方界面 `PrescriptionList.vue` 黄色提示剩余天数并**在提交前二次确认**
  - 阈值只由 `DrugExpiryScheduler` 一处持有，`DrugService.fillExpiryInfo()` 换算成 `daysToExpire` / `expiryLevel`（EXPIRED/NEAR/WARN/NORMAL/UNKNOWN）；这两个字段是 `Drug` 的 `@TableField(exist=false)` 瞬态字段，**未加数据库列**，前端不自行判断阈值
  - **种子分布**（`tools/init_template.sql`，全部相对日期）：7 NORMAL / **2 WARN**（阿莫西林 +45 天、布洛芬 +60 天，供演示临期提醒）/ 2 NEAR（蒙脱石散 +20、奥美拉唑 +25）/ 1 EXPIRED（氯雷他定 −46）；⚠️ 重建库后必须**重启后端**才会自动下架 NEAR/EXPIRED
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

