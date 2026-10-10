# 社区诊所门诊诊疗管理系统 — 项目记忆

> 只留**跨会话铁律**。细节一律看 `.workbuddy/memory/YYYY-MM-DD.md`（9-30 / 10-02 / 10-03 / 10-05 / 10-06 等）。

## 基本盘
- 栈：Spring Boot 3.2.5 + MyBatis-Plus 3.5.5 + Spring Security + JWT + MySQL 8.4(`clinic_db`) + Redis 7 + RabbitMQ 3；Vue 3.4 + Element Plus 2.5 + Vite 5 + Pinia + ECharts 5
- 端口 8080 / 5173；工作目录 `D:\newproject\wk6real`；作者 袁水康 2023102114
- JDK 18（`/d/work/jdk/openjdk-18+36_windows-x64_bin/jdk-18`）；Node 22.22.2 managed；MySQL 客户端 `C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe`
- 角色 ADMIN / DOCTOR / CASHIER；账号 `admin|doctor01|doctor03~08|cashier01`，密码 `admin123`
- 10 模块：建档/医生/排班/挂号/接诊/处方/收费/病历/复诊提醒/仪表盘；另 H5 免登录（手机号即身份）
- 约定：统一返回 `Result<T>{code,message,data}`；实体基类 `BaseEntity(createTime/updateTime/deleted)`

## 数据库（最易踩）
- **重置一律根目录 `reset.bat`**（= `mysql < sql/init.sql` + `docker exec clinic-redis redis-cli FLUSHDB`，密码/路径/容器名已配好）。不要手敲第二套，也不要另造重置脚本
- **`sql/` 下只有 `init.sql`**（13 表 + 全量种子，144KB）。源头 = `tools/init_template.sql`（模板，勿直接执行）+ `gen_init_full.py`（原地覆盖生成，`random.seed(20260921)`）。**不要另造补丁/第二份 init**
- 生成器依赖 `pycryptodome` → 只能用 `~/.workbuddy/binaries/python/envs/default/Scripts/python.exe`
- 种子：8 医生 / 30 患者 / 224 排班 / 109 挂号 / 86 病历 / 70 处方 / 109 账单 / 12 药品 / 10 用户
- 挂号费 = `doctor.registration_fee`：张50/陈45/赵35/孙30/李25/王20/刘15/周10
- `remain_num` 由 SQL 统一重算（`total_num − status<>0 的挂号数`），**禁止手算**；过去日期排班 `deleted=1` 归档
- 库名 `clinic_db`；**密码不硬记明文** → **Read** `backend/src/main/resources/application.yml` 的 `datasource.password`（Read 返回未被脱敏，可直接用）
- 🔴 **`reset.bat:9` 里写的 `-p789456` 与 `application.yml` 实际口令不一致 → reset.bat 现在跑不通**（MySQL 已拒绝该口令）。修法：把 reset.bat 的 `-p` 改掉即可，**不要另造第二套重置流程**（仍然是 `mysql < sql/init.sql` + `docker exec clinic-redis redis-cli FLUSHDB`）
- `init.sql` 密文 = AES-256-CBC(key=`clinic-aes-secret-key-for-256bit`, IV=`ClinicSystem-IV-`, PKCS7, Base64)；**换密钥必须重加密种子**（AES 历史：`clinic-aes-key-32bytes-secret!!` 是旧 AES-128 版）

## 药品效期两级管控（两条线互不干扰）
- 下架/禁开线 `NEAR_EXPIRY_DAYS = 30`：≤30 天 → 自动下架 + 禁止开具
- 预警线 `EXPIRY_WARN_MONTHS = 3`：31~90 天 → **可开具** + 前端黄标 + 提交二次确认
- 阈值只在 `DrugExpiryScheduler` 一处；`DrugService.fillExpiryInfo()` 算 `daysToExpire`/`expiryLevel`；**未加数据库列**（`@TableField(exist=false)`）
- 种子分布：**7 NORMAL / 2 WARN(阿莫西林+45、布洛芬+60) / 2 NEAR(蒙脱石散+20、奥美拉唑+25) / 1 EXPIRED(氯雷他定−46)**
- ⚠️ **`reset.bat` 后 `drug.status` 全 =1**，须**重启后端**（`ApplicationReadyEvent` 补偿扫描）才下架那 3 个 → 演示顺序：**reset → 启动后端 → 演示**
- 药品列表默认查全部（不传 status 查全部）；`DrugController.checkExpiryDates()` 接口层必填

## 启动坑
- 命令行启动必须 `unset SERVER__PORT SERVER__HOST`（沙箱注入 50447 会被宽松绑定覆盖 8080 → `Port 50447 already in use`）
- 改 Java/SQL/XML 后必须**重启**（IDE 热更新不覆盖）

## 权限（三层：菜单 v-if → 路由 meta.roles → 后端 `requireRole()`）
| 模块 | 读 | 写 |
|---|:--:|:--:|
| 工作台 | 全 | — |
| 患者 | 全 | ADM+DOC |
| 医生 / 排班 | 全 | 仅 ADM |
| 挂号 | 全 | 全 |
| 就诊 / 病历 | ADM+DOC | ADM+DOC |
| 处方 | 全 | ADM+DOC |
| 药品 | ADM+DOC | 仅 ADM |
| 收费 | ADM+CASHIER | ADM+CASHIER |
| 用户 | 仅 ADM | 仅 ADM |

- 模式：`private void requireRole(String... roles)` → `SecurityUtils.getCurrentRole()`；异常 `ForbiddenException` → 403。⚠️ 该方法**在 7 个 Controller 里各复制一份**（改逻辑要改 7 处）；前端 `meta.roles` **只配了 7 个页面**（patient/registration/prescription 未配）
- 🔑 **RBAC 三层**：菜单 `v-if`（Pinia）→ 路由 `meta.roles`（localStorage）→ 后端 `requireRole`（token 签名 role）。**前两层数据源都在 localStorage、客户端可改 → 只是 UX；第三层才是安全防线**（改 localStorage 能看菜单但接口仍 403）
- 医生数据过滤统一在 Controller 层：`SecurityUtils.getCurrentUserId()` → DoctorMapper → doctorId；ADM/CASHIER 无 Doctor 实体，看全部

## 关键工作流与坑
1. 挂号 → Redis Lua 原子扣号源 → 排队号
2. 接诊 → 校验挂号属当前医生 → 完成时自动生成病历
3. 处方 → 明细 + 库存扣减 → Controller 层生成药费单
4. **账单分单**：挂号费单(`prescription_id IS NULL`) / 药费单。**禁用 `getOne(registrationId)`**（一挂号多账单 → `TooManyResultsException`），必须 `list(in(regIds))` 内存分组；`resolveFeeType()` 由金额推导类型，未加列。⚠️ `createRegistrationBill`/`createDrugBill` 用 `getOne(wrapper, false)` 是防历史脏数据抛异常的**静默取第一条**（会掩盖异常）
   - ⚠️ **`init.sql` 种子仍是旧模型**（2026-10-07 实测）：109 账单 = 39 纯挂号费单 + **70 混合单**（`prescription_id` 非空且 `registration_fee>0`），纯药费单 **0**，每挂号仅 **1** 张 → **种子未体现 P11 分单**，这就是 `resolveFeeType()` 保留 MIXED 分支的原因
   - `bill` 表**无唯一约束**（仅 PRIMARY + 非唯一 `idx_registration_id`），防重全靠代码；且 MySQL 唯一索引允许多 NULL → `UNIQUE(registration_id, prescription_id)` 挡不住挂号费单重复
5. **移动端挂号即缴费**（一次调用，失败整体回退）；**PC 端保持"挂号 → 待收挂号费单 → 收银台"**（PC 必须登录，患者进不去）
6. 移动端就诊页按**就诊日**分组必须用 `schedule.work_date`（`registration.registration_date` 是挂号操作那天）
7. 历史挂号的排班已逻辑删除 → **必须 `ScheduleMapper.selectByIdsIncludeDeleted()`**（原生 `@Select` 绕过拦截器；PC 靠裸 SQL `LEFT JOIN`）
8. ⚠️ **H5 bundle 过大（2026-10-10 build 实测）**：`main.js` 用 `app.use(ElementPlus)` **全量注册 + 全量注册 300+ 图标** → tree-shake 失效，`index` 入口 chunk **1192KB（gzip 385KB）**，H5 两页自身仅 8~10KB 却都要下它；`Dashboard` chunk 另有 1042KB。build 报 `chunks larger than 500 kB`。解法 = `unplugin-vue-components` 改按需引入（**独立一件事，未做**）
9. ⚠️ **H5 病历页曾渲染 AES 密文**：`MobileController.my-data` 直接 `list()` 不解密，前端把 Base64 当正文显示，旁边还挂着"AES-256 加密"标签（像安全措施、实为 bug）。**2026-10-10 已修**：只显示明文 `diagnosis`；用户决定**不做病历详情**（免登录 + 手机号认身份 → 正文泄露风险）
10. ❌ **H5 不换 Vant**（用户已确认）：PC 端功能不受影响，但全量引入的 EP 已 1.2MB，再加 Vant 雪上加霜；弹窗改底部抽屉用 CSS 覆盖
11. **H5 优化已全部做完（2026-10-10，分支 `add--app--H5-to-H5pro`，4 次 build 全零错误）**：
    - P0 真 bug：病历页去掉 AES 密文渲染（只留明文 `diagnosis`）· `fetchSchedules` 加 `loadError` 区分"断网"与"没排班" · 挂号费去掉前端 `?? 10` 硬编码兜底 · 支付错误细分（已结清/网络/其他）
    - P1 体验：底部导航 `env(safe-area-inset-bottom)` 安全区 · **手写下拉刷新（MobileHome + MobileRecords 两页都有）** · 手机号 `type="tel" inputmode="numeric"` · 日期 7→14 天
    - P2 收尾：**底部导航抽成 `views/mobile/MobileNav.vue`**（props `current`，两页共用，抽组件后 Home CSS 5.57→4.35KB）· **手机号不再进 URL**，改 `sessionStorage['clinic:patientPhone']` 中转
    - ⚠️ **sessionStorage 一次性清除 → 刷新 Records 要重输手机号**。选安全是因为诊所有公用平板，残留会让下一个患者看到上一个的数据；配套加了顶部患者条（姓名+手机号+「切换患者」）
12. ❌ **弹窗底部抽屉已回退**（2026-10-10）：用户实测后否掉——"卡片弹出不好看，最好还是居中"。**教训：移动端 UI 别凭理论推，"更符合习惯"≠"更好看"，改样式前先给用户看**
13. ✅ **弹窗打开抖动已修**（`App.vue`，全局）：Element Plus 弹窗锁 body 滚动 → 滚动条消失（占宽度）→ 可视区变宽 → 整页重排。用 `MutationObserver` 监听 body class，锁定期间补等宽 `padding-right`；`gap<=0` 时不启用（iOS 滚动条不占宽度）。**PC 端 11 个页面的 el-dialog 之前也在抖，一并修好**。没选 `html{scrollbar-gutter:stable}`——它会始终预留空间，PC 端内容不满屏时多 15px 空白
- `PrescriptionService ↔ BillService` 有循环依赖风险，涉及账单的操作放 Controller 层
- 排班 2（张医生今日下午）是**集成测试夹具**：`total_num=30`、2 条有效挂号 → `remain_num=28`，**禁止再追加挂号**

## 演示要点（多角色串号）
- localStorage 同源全标签页共享 token → 多标签页登不同角色会串号（表现"医生点接诊报权限不足"，后端其实没错）
- 根因在 `utils/request.js` L59：**axios 拦截器每请求实时读**共享值 → 覆盖立刻生效
- 前端 localStorage **0 处 sessionStorage**，集中 4 文件：`stores/user.js` / `router/index.js` / `utils/request.js` / `PrescriptionList.vue`（真要改就是这 4 处）
- **最稳：单标签页串行**（演示完一个角色，地址栏敲 `/login` 换账号）；多标签页法只有"最后登录那个"能点、且不能刷新
- ❌ **决定不改 sessionStorage**（2026-10-07）：多标签页是**演示技巧**非产品需求；sessionStorage 关标签页即掉登录，且 Chrome 无痕窗口间仍共享隐私会话
- 演示主路径：admin（药品待办 → 挂号费分级 → 效期 → 建档/挂号/收费）→ doctor01（接诊→开方）→ cashier01（收费）

## 已定决策（勿重提）
- ❌ 医保报销：**不做**，按自费（话术：属院端 HIS / 医保局接口范畴）
- ❌ PC 端挂号改"先缴费"、挂号队列号并发重复、localStorage→sessionStorage：**不修**（话术绕开）
- ⚠️ 分单结算 / 挂号即缴费**未加任何数据库列**

## RabbitMQ · 复诊提醒线现状（2026-10-08 读源码核实）
- 拓扑：Direct exchange `clinic.reminder.exchange` → routing key `clinic.reminder` → durable queue `clinic.reminder.queue` → `@RabbitListener ReminderConsumer` → 写 `reminder` 表（status=0）；`@Scheduled(cron="0 0 8 * * ?")` 每天 8 点把当天到期改 status=1
- ✅ `@EnableScheduling` 在 **`ClinicApplication:45`**（定时任务正常，不会不跑）
- ✅ 设计对：MQ 发送放 Controller 层 = 事务提交之后；Direct + durable queue；定时任务 `where status=0` 天然幂等（多实例无害）
- 🔴 **三处可靠性缺口**（已给用户完整落地方案，**未改代码，等拍板**）：① 未开 `publisher-confirm-type` / `publisher-returns` / `template.mandatory` → 发送失败静默丢 ② 消费者无幂等（`reminder.consultation_id` 无唯一索引，重投会插重复）③ 无死信队列 + 默认 `default-requeue-rejected=true` → 坏消息无限重投、消费者死循环
- ⚠️ 改 DLX 的**致命坑**：队列的 `x-dead-letter-exchange` 参数**队列已存在时改不生效**，必须先 `rabbitmqctl delete_queue` 或换队列名重建
- 📄 **MQ 知识已归档到 `perf-lab/mq/MQ学习笔记.md`**（2026-10-08，十节：一页纸结论 / 术语与 Exchange 选型 / 消息生命周期与两个确认点 / ACK-NACK 语义 / **幂等** / 死信队列 / publisher confirm / 本项目实战与缺口 / 面试问答 / 踩坑速查）—— **下次聊 MQ 先读这份，别重讲**
- 🔑 **MQ 心智模型（已确认用户掌握）**：Broker = 服务器进程；**① confirm 是 Broker→生产者（收到了吗），② ACK 是消费者→Broker（处理好了吗），方向相反**；**ACK=删掉 / NACK=塞回重发**；at-least-once 的根源是"处理成功但 ACK 丢失也要重投"→ **幂等是必备条件**；消息丢失的后果是**漏**不是重；DLQ 由 **Broker 自动路由**、作用是**消灭循环**
- 🔍 RabbitMQ 容器是 `rabbitmq:3-management-alpine`，**管理台 http://localhost:15672**（docker-compose 已暴露），可直接查看队列积压 / 交换机 / 死信 / 消费者
- ⚠️ `reminder` 表**没有唯一索引**、**没有 ReminderController**（PC 端无提醒页）、`findPendingToday()` 是死代码、`status=2（已复诊）` 是死值
- ⚠️ `ReminderMessage.remindDate` 注释写"由消费者计算" → **实际生产者算好**，消费者只搬运（文档债）

## 测试
- 19 用例：`AesUtilsTest` 5 / `JwtTokenTest` 5 / `SecurityUtilsTest` 5 / `MiddlewareIntegrationTest` 2 / `DrugStockLockTest` 2
- `MiddlewareIntegrationTest` **有状态、不可连跑**：跑前重建库 + FLUSHDB，跑后重建恢复演示数据
- **两套并发数字别混**：集成测试 35 抢 28 → **28/7**；JMeter 35 抢 30 → **30/5**（137ms，QPS 137.8）
- `AesUtilsTest` 报告里的 `ERROR ... AES解密失败` 是**预期的**，判绿只看 `Failures=0`
- `DrugStockLockTest` 绕过 `PrescriptionService`（直调 mapper + SETNX），其 `setUp()` 直插 `expireDate=null` → 效期两列必须允许 NULL

## 论文待办
- 5.1 第 464 行：「**集成测试**表明」→「**JMeter 压测**表明」（30/5 属 JMeter）
- 封面（学号/姓名/导师/日期）、参考文献格式、字号行距；截图贴入（第五章页面 + 第六章 19 测试 + 2 组 JMeter）
- ✅ 七章正文 + 全部配图已完成
- ⚠️ **实测（2026-10-07）**：`docs/毕业论文.docx` 最后修改 **2026-08-29 08:40**；根目录改论文的脚本（`fix_ch4_ch5.py` / `add_ch1_cites.py` / `add_aes_sec.py` / `add_perf_test.py`）全是 **7/17~7/23** 的 → **8/29 之后论文一次没动**，上述待办从 10 月初挂到现在
- ⚠️ **`docs/` 整个目录不在 git 里**（`git log --all -- docs/毕业论文.docx` 返回空；论文 / 开题报告 / 答辩PPT 全无版本控制）→ 改坏了没法回滚，建议至少留备份
- 其他文档：`技术文档.md` / `测试报告.md` / `部署说明.md` 均 10-03；`答辩PPT.pptx` 7/13

## 面试准备（2026-10-06 起）
- 用户方向转为**熟悉项目全貌备面试**（放弃页面优化）
- 已梳理并讲解：五层架构图 / 技术栈表 / 10 模块地图 / 四个技术亮点（Redis Lua 扣号 · SETNX 库存锁 · AES-256 病历加密 · RabbitMQ 复诊提醒）/ **登录认证链**（IP 限流 + BCrypt + JWT + 登出黑名单）
- 后端规模：Controller 13 / Service 16 / Mapper 13；写操作均有 `requireRole` 守卫
- ⚠️ 敏感拦截：`CODE-MAP.md`、`Login.vue` 因含敏感内容读取被拦 → **不绕过**，改用目录树 + 源码梳理
- 已逐模块深入：10 模块地图（第 1 轮）→ **④ 挂号全链路**（第 2 轮，2026-10-07：数据隔离 / createRegistration 六步 / 内联 Lua / 裸 SQL 分页）→ **登入模块逐行拆**（第 3 轮：限流三缺陷 / getKey 补齐隐患 / 异常处理器 HTTP 仍是 200 / 未配 EntryPoint 返 403 / logout 两个 500 / CORS 只放行 localhost:5173）→ **延伸答疑**（BCrypt 存储与比对 / RBAC 三层 / localStorage vs sessionStorage / @PreAuthorize 取舍）
- **逐行覆盖进度（2026-10-07 23:35 更新）**：
  - ✅ **已逐行**：④ 挂号 / 登入认证链 / 权限三层 / **⑤ 接诊**
  - ◐ **只讲过概要**：⑥ 处方（SETNX 锁）/ ⑦ 收费（分单模型）/ ⑧ 病历（AES-256）/ ⑨ 复诊提醒（RabbitMQ）
  - ❌ **未碰**：⑩ 仪表盘 / H5 移动端（H5 读过源码但未系统讲）
- ⚠️ **四大技术亮点里只有「Redis Lua 扣号」被逐行讲过**；**SETNX 库存锁 / AES-256 / RabbitMQ 都停在概要** → 面试要接住追问，这三个必须补逐行
- 🔴 **接诊模块的越权缺陷（2026-10-07 实测）**：`completeConsultation` 只校验 `status==1`、**不校验就诊归属** → 任何 DOCTOR 能改别人诊断/医嘱并生成病历。**同项目三种写法两对一错**：`list()` 无条件覆盖 doctorId ✅ / `start()` 校验排班归属 ✅ / `complete()` 不校验 🔴
- `consultation.status=0` 是**死值**（创建时直接 `setStatus(1)`，"待诊"实际是挂号表 status=1）；`startConsultation` 用 `scheduleService.getById()` **受逻辑删除拦截器**影响，排班归档后会误报"不属于您的排班"（应改 `selectByIdsIncludeDeleted()`）
- ✅ 设计得对：MQ 发送放 Controller 层、在事务提交**之后** → 躲开"消息先到、数据未提交"
- **推荐剩余顺序**：⑥ 处方（补 SETNX 逐行）→ ⑨ 复诊提醒（补 MQ 逐行）→ ⑧ 病历（补 AES 逐行）→ ⑩ 仪表盘（最薄，快速过）

## 环境备注
- **分支实况（2026-10-07）**：4 个本地分支 —— `add--app--H5`（**当前**，领先 master 6 提交）/ `add--app--H5-to-lab` / `add--app--H5-to-lab-real` / `master`
- **perf-lab 只在 `add--app--H5-to-lab-real` 分支**（7 个 sql 文件已 commit，**不会丢**）；**当前 `add--app--H5` 上 `perf-lab` 未被 git 跟踪、`sql/` 只剩 `lab-build.txt`** → 要看 SQL 靶场文档必须切回实验分支；master 上无 perf-lab
- ⚠️ **`.workbuddy/memory/*.md`、`docs/`、根目录脚本（`gen_init_full.py` / `make_kaibao*.py` / `concurrent_test.py` 等）全是未跟踪状态** → 换分支前先 `git status` 确认
- 靶场文档在 `perf-lab/README.md`（**不合并回 master**）
- 📚 **perf-lab 四个专题目录**：`sql/`（SQL 调优）· `jvm/`（JVM 排障）· `mq/`（RabbitMQ，含 `MQ学习笔记.md`）· **`clinic-modules/`**（本项目模块精读：`README.md` 索引 / `模块精读.md` 8 模块逐行 / `缺陷与面试速查.md` 42 条缺陷分级总表）
- 前三个是**技术专题**（换项目也用得上），`clinic-modules/` 是**具体项目**拆解 —— **下次要复习模块或准备面试，先读这两份，别重讲**
