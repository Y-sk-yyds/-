# 社区诊所门诊诊疗管理系统

> 面向社区诊所「前台 — 医生 — 收费」全流程的一体化管理系统。
> PC 端覆盖建档、排班、挂号、接诊、开方、收费、病历、药品效期与经营看板；H5 端免登录，患者凭手机号自助完成挂号、缴费与病历查询。

[![Vue](https://img.shields.io/badge/Vue-3.4-42b883)](https://cn.vuejs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6db33f)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479a1)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-d82c20)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-ff6600)](https://www.rabbitmq.com/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.5-409eff)](https://element-plus.org/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey)]()

---

## 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [核心功能](#核心功能)
- [技术亮点](#技术亮点)
- [系统架构](#系统架构)
- [数据库设计](#数据库设计)
- [快速开始](#快速开始)
- [默认账号](#默认账号)
- [接口概览](#接口概览)
- [项目结构](#项目结构)
- [常见问题](#常见问题)
- [说明](#说明)

---

## 项目简介

社区诊所日常运营里最琐碎、最容易出错的三件事是：**号源超卖、库存超卖、收费对不上账**。

本系统围绕这三件事做了完整闭环：

| 环节 | 痛点 | 系统的处理 |
|---|---|---|
| 挂号 | 同一号源被多个窗口抢到 | Redis **Lua 脚本原子扣减** + MySQL 条件更新兜底 |
| 开方 | 并发开药导致库存变负 | Redis **SETNX 分布式锁** 串行化 + `stock >= quantity` 条件更新 |
| 收费 | 挂号费与药费混在一张单里，账目对不上 | **账单分单**（挂号费单 / 药费单分开），收银台按单结算 |
| 复诊提醒 | 同步发短信阻塞主流程 | RabbitMQ **异步解耦** 接诊完成即投递提醒消息 |
| 病历隐私 | 诊断全文明文落库 | **AES-256-CBC** 加密正文，列表页只返回明文诊断 |
| 药品效期 | 过期/临期药品还在开 | 定时扫描：≤30 天自动下架禁开，31~90 天前端黄标 + 二次确认 |

系统内置 13 张业务表与一套完整的演示种子数据（8 位医生 / 30 位患者 / 224 条排班 / 109 条挂号 / 86 份病历 / 70 张处方 / 109 张账单 / 12 种药品），**克隆下来就能直接跑起一套有数据的工作台**。

---

## 技术栈

### 后端

| 组件 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.2.5 | 应用主框架 |
| Spring Security | 6.x | 认证过滤器链、接口放行策略 |
| JWT (jjwt) | 0.12.3 | 无状态令牌签发与校验 |
| MyBatis-Plus | 3.5.5 | 单表 CRUD、条件构造器、分页 |
| MySQL | 8.4 | 业务数据持久化 |
| Redis | 7 | 号源扣减、分布式锁、登录限流、JWT 黑名单 |
| RabbitMQ | 3 | 复诊提醒异步消息（Direct 交换器 + 持久化队列） |
| BCrypt | Spring Security 内置 | 密码单向哈希 |
| AES-256-CBC | JDK 原生 | 病历正文加密 |

### 前端

| 组件 | 版本 | 用途 |
|---|---|---|
| Vue | 3.4 | 组合式 API + `<script setup>` |
| Vite | 5 | 构建与开发服务器 |
| Element Plus | 2.5 | PC 端 UI 组件库 |
| ECharts | 5.5 | 经营看板图表 |
| Pinia | 2.1 | 登录态与用户信息 |
| Vue Router | 4.3 | 前端路由 + `meta.roles` 权限元信息 |
| Axios | 1.6 | 请求拦截器统一注入 Bearer Token |

---

## 核心功能

### PC 端（11 个业务页面 + 登录页）

| 模块 | 功能 | 写权限 |
|---|---|---|
| 工作台 | 今日挂号量 / 收入 / 待办药品效期预警 / 科室分布 ECharts | — |
| 患者管理 | 建档、档案检索、就诊历史 | ADMIN / DOCTOR |
| 医生管理 | 医生档案、挂号费、擅长科室 | 仅 ADMIN |
| 排班管理 | 排班增删改、号源总数与剩余数 | 仅 ADMIN |
| 挂号管理 | 按医生/日期挂号、取消、生成挂号费单 | 全角色 |
| 接诊管理 | 候诊队列、开始接诊、填写诊断医嘱并生成病历 | ADMIN / DOCTOR |
| 处方管理 | 开方、库存扣减、自动生成药费单 | ADMIN / DOCTOR |
| 病历管理 | 病历列表（医生仅见本人） | ADMIN / DOCTOR |
| 药品管理 | 药品档案、效期查询、上下架 | 写仅 ADMIN |
| 收费管理 | 收银台、待缴单筛选、支付流水 | ADMIN / CASHIER |
| 用户管理 | 账号、角色、密码重置 | 仅 ADMIN |

### H5 移动端（2 个页面，免登录）

患者侧**无需账号**，凭手机号即身份：

- 挂号：查未来 14 天排班 → 选医生 → 选时段 → 挂号**即缴费**（一次调用，失败整体回退）
- 缴费：待缴账单 → 微信/现金标记支付 → 排队号与位置
- 排队看板：同排班患者的**脱敏**姓名与等待位次
- 病历查询：按就诊日分组展示医生诊断结论

---

## 技术亮点

### 1. Redis Lua 原子扣号源（防超卖）

`GET` + `DECR` 分两步会在并发下丢失更新。系统把两步塞进一个 Lua 脚本，Redis 单线程执行天然原子：

```java
// RegistrationService —— 号源扣减脚本
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil or stock <= 0 then
    return -1              // 号源已满，直接拒绝
end
redis.call('DECR', KEYS[1])
return stock - 1
```

Lua 返回值同时充当**校验结果**和**扣减后的余量**，省掉一次回查。Lua 返回 `-1` 时业务层回退到 MySQL 的条件更新（`SET remain_num = remain_num - 1 WHERE id = ? AND remain_num > 0`）作为最终防线。

### 2. Redis SETNX 分布式锁（防药品超卖）

```java
Boolean locked = stringRedisTemplate
        .opsForValue()
        .setIfAbsent(lockKey, "1", Duration.ofSeconds(3));
```

同一把药品锁把并发的开方请求串行化，锁粒度到**药品 ID**，不同药品互不阻塞。真正兜底的仍是 MySQL 条件更新 —— 锁只负责「快」，条件更新负责「准」。

### 3. RabbitMQ 异步复诊提醒

```
接诊完成 → Controller 投递消息 → clinic.reminder.exchange (Direct)
        → routing key: clinic.reminder
        → clinic.reminder.queue (durable)
        → ReminderConsumer → 写 reminder 表 (status=0)
        → @Scheduled(每天 08:00) → 当天到期记录置 status=1
```

设计上的三个要点：
- **投递放在 Controller 层**（事务提交之后），避免消息先到库还没写；
- **Direct + 持久化队列**，路由语义明确、重启不丢消息；
- **定时任务只查 `status = 0`**，天然幂等，多实例部署也不会重复推送。

### 4. AES-256-CBC 病历加密

只加密 `medical_record.content`（诊断全文），保留 `diagnosis` 明文用于列表展示与统计。密钥、IV 均从配置读取，加密后 Base64 存储。密钥一旦改动必须重新初始化种子数据 —— 这一点在配置里有明确注释，避免踩坑。

### 5. 三层 RBAC 权限模型

| 层级 | 载体 | 作用 |
|---|---|---|
| 菜单层 | Pinia + `v-if` | 决定显示哪些菜单 |
| 路由层 | `meta.roles` | 拦截非法路径跳转 |
| **后端层** | `requireRole()` + JWT 角色签名 | **真正的安全边界** |

前两层数据源都在 `localStorage`，客户端可篡改，只影响体验；即便伪造出菜单，接口侧仍会返回 **403**。角色三类：`ADMIN` / `DOCTOR` / `CASHIER`。

### 6. 账单分单模型

一笔挂号可能同时产生挂号费和药费，系统拆成两张单：`prescription_id IS NULL` 为挂号费单，非空为药费单。医生端不参与结算，`Cashier` 只看待缴单。查询时**必须 `list(in(registrationIds))` 后在内存分组** —— 同一挂号对应多张单，用 `getOne(registrationId)` 会直接抛 `TooManyResultsException`。

### 7. 药品效期两级管控

| 距效期 | 级别 | 行为 |
|---|---|---|
| ≤ 30 天（含已过期） | NEAR / EXPIRED | **自动下架 + 禁止开具** |
| 31 ~ 90 天 | WARN | 允许开具 + 前端黄标 + 提交二次确认 |
| > 90 天 | NORMAL | 正常 |

阈值只在 `DrugExpiryScheduler` 一处定义，`DrugService.fillExpiryInfo()` 负责给前端算 `daysToExpire` 与 `expiryLevel`（均为 `@TableField(exist=false)`，不落库）。

---

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│  前端 Vue 3 + Vite (5173)                               │
│  ┌──────────────┐            ┌──────────────────┐       │
│  │  PC 管理端   │            │  H5 患者端(免登录)│      │
│  │ 11 页 + 登录 │            │ 2 页            │       │
│  └──────┬───────┘            └────────┬─────────┘       │
│         │  Axios + Bearer Token        │                 │
└─────────┼──────────────────────────────┼────────────────┘
          │  /api/**                     │  /api/mobile/**
┌─────────▼──────────────────────────────▼────────────────┐
│  Spring Boot 3.2.5 (8080)                               │
│  ┌────────────────────────────────────────────┐        │
│  │ JwtAuthenticationFilter                   │        │
│  │  → Redis 黑名单校验 → 验签 → SecurityContext│       │
│  ├────────────────────────────────────────────┤        │
│  │ Controller（requireRole 权限闸门 + 医生数据过滤）│      │
│  ├────────────────────────────────────────────┤        │
│  │ Service（@Transactional）                   │       │
│  ├────────────────────────────────────────────┤        │
│  │ Mapper（MyBatis-Plus + 手写 XML 联表）      │       │
│  └────────┬───────────────┬─────────────┬────┘        │
└───────────┼───────────────┼─────────────┼──────────────┘
            │               │             │
    ┌───────▼──────┐  ┌─────▼─────┐  ┌────▼──────────────┐
    │  MySQL 8.4   │  │ Redis 7   │  │  RabbitMQ 3        │
    │  13 张业务表 │  │ 号源/锁/  │  │  Direct 交换器      │
    │              │  │ 限流/黑名单│  │  复诊提醒异步消费   │
    └──────────────┘  └───────────┘  └───────────────────┘
```

**统一返回格式**

```json
{ "code": 200, "message": "操作成功", "data": { } }
```

实体统一继承 `BaseEntity`（`createTime` / `updateTime` / `deleted` 逻辑删除字段）。

---

## 数据库设计

共 **13 张表**：

| 表名 | 说明 | 关键约束 |
|---|---|---|
| `sys_user` | 系统账号与角色 | 角色：ADMIN / DOCTOR / CASHIER |
| `patient` | 患者档案 | 手机号唯一（H5 身份依据） |
| `doctor` | 医生档案 | `registration_fee` 决定挂号费 |
| `schedule` | 排班与号源 | `total_num` / `remain_num` |
| `registration` | 挂号记录 | 关联 `schedule` + `doctor` + `patient` |
| `consultation` | 就诊记录 | 一个挂号对应一次就诊 |
| `prescription` | 处方头 | 关联 `consultation` |
| `prescription_drug` | 处方明细 | 关联 `prescription` + `drug` |
| `drug` | 药品档案 | 含生产日期 / 有效期 |
| `drug_stock` | 药品库存 | `current_stock` 条件更新防超卖 |
| `bill` | 账单 | `prescription_id IS NULL` = 挂号费单 |
| `medical_record` | 病历 | `content` 为 AES 密文，`diagnosis` 为明文 |
| `reminder` | 复诊提醒 | `status` 0 待推送 / 1 已推送 |

> 种子数据统一由 `gen_init_full.py`（`random.seed` 固定）生成 `sql/init.sql`，保证每次重置结果完全一致。挂号费的 `remain_num` 一律由 SQL 重算（`total_num − status<>0 的挂号数`），不做手工计算。

---

## 快速开始

### 方式一：Docker Compose 一键拉起全套中间件（推荐）

```bash
# 1. 按需修改根目录 .env 中的 MYSQL_ROOT_PASSWORD / MYSQL_DATABASE
docker compose up -d

# 2. 确认容器健康
docker compose ps
```

| 服务 | 端口 | 说明 |
|---|---|---|
| MySQL | 3306 | 库名 `clinic_db`，首次启动自动执行 `sql/init.sql` |
| Redis | 6379 | 无密码 |
| RabbitMQ | 5672 / **15672** | 管理台 http://localhost:15672（`guest` / `guest`） |

> ⚠️ `sql/init.sql` 会在 **数据卷首次初始化** 时自动执行。如果你之前拉起过容器，需要手动重置：
> ```bash
> mysql -h 127.0.0.1 -P 3306 -u root -p < sql/init.sql
> docker exec clinic-redis redis-cli FLUSHDB
> ```
> 项目根目录也提供了 `reset.bat`（Windows）做同样的事。

### 方式二：本机直接运行

**前置**：JDK 17+、Maven 3.8+、Node 18+

**1. 初始化数据库**

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p < sql/init.sql
```

**2. 修改后端配置**

编辑 `backend/src/main/resources/application.yml`，把 `datasource.password` / `redis` / `rabbitmq` 的账号密码改成你的本地环境。

**3. 启动后端**

```bash
cd backend
mvn spring-boot:run
```

后端启动后监听 **8080**，启动时会自动执行一次药品效期补偿扫描，把临期/过期药品下架。

**4. 启动前端**

```bash
cd frontend
npm install
npm run dev
```

前端监听 **5173**，浏览器打开 http://localhost:5173 即可。

**5. 访问 H5 端**

http://localhost:5173/mobile —— 免登录，输入患者手机号即可挂号、缴费、查病历。

---

## 默认账号

| 角色 | 账号 | 密码 | 能做什么 |
|---|---|---|---|
| 管理员 | `admin` | `admin123` | 全部功能，含医生/排班/用户/药品管理 |
| 医生 | `doctor01` ~ `doctor08` | `admin123` | 接诊、开方、写病历、本人数据 |
| 收费员 | `cashier01` | `admin123` | 收银台结算、账单查询 |

> 患者手机号（用于 H5 免登录）见 `sql/init.sql` 的 `patient` 表种子数据。
> 生产环境请务必在首次登录后修改全部默认密码。

---

## 接口概览

所有接口统一前缀 `/api`，返回 `{ code, message, data }`。

| 模块 | 前缀 | 说明 |
|---|---|---|
| 认证 | `/api/auth` | 登录、登出、当前用户信息 |
| 患者 | `/api/patients` | 建档、检索、就诊历史 |
| 医生 | `/api/doctors` | 医生档案与挂号费 |
| 排班 | `/api/schedules` | 排班维护与号源 |
| 挂号 | `/api/registrations` | 挂号、取消、队列 |
| 接诊 | `/api/consultations` | 候诊队列、开始接诊、完成接诊 |
| 处方 | `/api/prescriptions` | 开方、明细、库存扣减 |
| 药品 | `/api/drugs` | 药品档案、效期查询、上下架 |
| 收费 | `/api/bills` | 待缴单、支付、流水 |
| 病历 | `/api/records` | 病历列表与详情 |
| 看板 | `/api/dashboard` | 经营统计 |
| 用户 | `/api/users` | 账号与角色（仅 ADMIN） |
| **H5** | `/api/mobile/**` | 免登录：排班、挂号、缴费、排队、病历 |

---

## 项目结构

```
clinic-system/
├── backend/                          # Spring Boot 后端
│   └── src/main/
│       ├── java/com/clinic/
│       │   ├── common/               # 统一返回 Result、分页、全局异常
│       │   ├── config/               # Security / Redis / RabbitMQ / MyBatis-Plus / CORS
│       │   ├── controller/           # 13 个 Controller（权限闸门在这一层）
│       │   ├── dto/  vo/  entity/    # 出入参与实体（BaseEntity 统一逻辑删除）
│       │   ├── mapper/               # MyBatis-Plus Mapper + 手写 XML
│       │   ├── mq/                   # 消息生产者 / 消费者
│       │   ├── security/             # JWT 签发校验、SecurityUtils
│       │   ├── service/              # 业务层
│       │   └── utils/                # AesUtils / RedisUtils
│       └── resources/
│           ├── application.yml
│           └── mapper/*.xml
│
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── api/                      # axios 封装（按模块拆分）
│       ├── router/                   # 路由 + meta.roles 权限元信息
│       ├── stores/                   # Pinia（登录态）
│       ├── utils/                    # request.js 拦截器
│       └── views/                    # 11 个 PC 页面 + views/mobile（H5 两页）
│
├── sql/init.sql                      # 13 张表 + 全量种子数据（唯一 SQL 源）
├── gen_init_full.py                  # 种子数据生成器（random.seed 固定）
├── reset.bat                         # 一键重置数据库 + 清 Redis
├── docker-compose.yml                # MySQL + Redis + RabbitMQ + Nginx 编排
├── CODE-MAP.md                       # 项目结构 / 表 / API / 页面依赖全景图
├── docs/                             # 技术文档、测试报告、ER 图、截图
└── perf-lab/                         # 技术实验靶场
    ├── sql/                          # 慢查询定位与优化实验
    ├── jvm/                          # JVM 调优与 OOM 排查实验
    ├── mq/                           # RabbitMQ 消息可靠性专题笔记
    └── clinic-modules/               # 业务模块逐行精读与缺陷速查
```

---

## 常见问题

<details>
<summary><b>启动后端口 8080 被占用 / 报 Port already in use</b></summary>

环境变量 `SERVER__PORT` 被外部注入覆盖。先清掉再启动：

```bash
unset SERVER__PORT
unset SERVER__HOST
```
</details>

<details>
<summary><b>重置数据后药品效期预警没生效</b></summary>

`sql/init.sql` 里的药品 `status` 全为 1。效期下架依赖启动时的补偿扫描，**重置后需重启后端**，再打开药品页才会看到下架状态。
</details>

<details>
<summary><b>改了 aes.secret-key 后病历全是乱码</b></summary>

病历密文由固定密钥加密。**换密钥必须重新生成 `sql/init.sql`**，否则历史密文无法解密。项目内已固定密钥，如需改动请同步重跑 `gen_init_full.py`。
</details>

<details>
<summary><b>多标签页登录不同角色，提示权限不足</b></summary>

`localStorage` 在同源下所有标签页共享，Axios 拦截器每次请求实时读取共享的 token，所以后登录的会覆盖先登录的 —— **这是浏览器机制，不是 bug**。演示多角色最稳的做法是单标签页串行：演示完一个角色，回 `/login` 换账号。
</details>

<details>
<summary><b>前端 build 报 chunk 超过 500KB</b></summary>

Element Plus 目前是全量引入（`app.use(ElementPlus)` + 全量注册图标），入口 chunk 约 1.2MB（gzip 385KB）。可改为 `unplugin-vue-components` 按需引入进一步压缩。
</details>

<details>
<summary><b>历史挂号的排班查不到</b></summary>

过去日期的排班被逻辑删除（`deleted = 1`），MyBatis-Plus 的逻辑删除拦截器会过滤掉。查询历史数据必须走 `ScheduleMapper.selectByIdsIncludeDeleted()`（原生 `@Select` 绕过拦截器）或手写 `LEFT JOIN` SQL。
</details>

---

## 说明

- 本项目为学习型/工程实践项目，内置的种子数据（患者姓名、手机号、医生信息）**全部为虚构数据**，不含任何真实个人信息。
- 代码注释与技术笔记为中文，遵循「自底向上、逐行验证」的注解风格，便于二次阅读。
- `perf-lab/` 目录是配套的技术实验靶场，记录 SQL 优化、JVM 排查、MQ 可靠性等实验过程与结论，**不参与主工程构建**。
- 技术细节问题可参考 `docs/技术文档.md`；整体结构速查见 `CODE-MAP.md`。

## License

[MIT](https://opensource.org/licenses/MIT)
