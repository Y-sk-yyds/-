# 社区诊所门诊诊疗管理系统 - 项目记忆

## 项目基本信息
- 项目名称: 社区诊所门诊诊疗管理系统 (Clinic Management System)
- 技术栈: Spring Boot 3.2 + Vue3 + Element Plus + MyBatis-Plus + MySQL + Redis + RabbitMQ
- 端口: 后端 8080, 前端 5173
- 工作目录: D:\newproject\wk6real
- 作者: 袁水康（学号 2023102114；桌面模板原属张怡，系借用同学模板）

## 技术选型
- 后端: Java 17, Spring Boot 3.2, Spring Security + JWT, MyBatis-Plus 3.5.5
- 中间件: Redis 7 (分布式锁/缓存/限流), RabbitMQ 3.x (异步消息)
- 加密: AES-256-CBC（病历加密存储）
- 前端: Vue 3.4 + Vite 5 + Element Plus 2.5 + ECharts 5 + Pinia

## 核心模块
1. 患者建档 - 基本信息+过敏史
2. 医生管理 - 科室+职称+简介
3. 排班管理 - 按日期+时段设置号源
4. 线上挂号 - Redis原子扣减号源，防超卖
5. 医生接诊 - 主诉→诊断→医嘱
6. 处方开具 - 多药品+用法用量+库存扣减
7. 收费结算 - 挂号费+药费，多种支付方式
8. 电子病历 - AES加密存储
9. 复诊提醒 - RabbitMQ异步推送
10. 仪表盘 - ECharts统计图表

## 中间件应用
- Redis: 挂号号源(Lua原子扣减)、排班缓存(Hash)、药品库存锁(SETNX)、限流(ZSET滑动窗口)
- RabbitMQ: 复诊提醒异步推送(Direct Exchange)
- AES: 病历内容加密(CBC模式)

## 技术约定
- 统一返回: Result<T> { code, message, data }
- 实体基类: BaseEntity (createTime/updateTime/deleted)
- 权限: 单角色 ADMIN/DOCTOR/CASHIER
- MySQL: 本机 8.4, 3306；**库名 `clinic_db`**（不是 `clinic`，误用会报 1049）
- **MySQL 密码不要硬记明文**：一律优先用根目录 `reset.bat`（脚本内已配好），
  或 **Read** 读 `backend/src/main/resources/application.yml`（grep 同一文件可能返回脱敏值，不可信）
- JDK: Java 18, /d/work/jdk/openjdk-18+36_windows-x64_bin/jdk-18
- Node: 22.22.2 managed
- MySQL 客户端: C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe

## 权限设计
- 三层防护: 前端菜单 v-if → 前端路由 meta.roles → 后端 requireRole()
- 统一模式: private void requireRole(String... roles) → SecurityUtils.getCurrentRole()
- 权限异常: ForbiddenException → HTTP 403（"权限不足，当前角色：XXX"）
- 医生: SecurityUtils.getCurrentUserId() → DoctorMapper → doctorId，统一在 Controller 层过滤
- ADMIN/收银员: 无 Doctor 实体，查询不过滤，看全部数据
- 前端: userStore 存 doctorId（登录返回），用于前端过滤

### 权限矩阵
| 模块 | 读(list) | 写(CUD) |
|------|:---:|:---:|
| 工作台 | 所有人 | — |
| 患者管理 | 所有人 | ADM+DOC (create全角色) |
| 医生管理 | 所有人 | 仅 ADM |
| 排班管理 | 所有人 | 仅 ADM |
| 挂号管理 | 所有人 | 所有人 |
| 就诊管理 | ADM+DOC | ADM+DOC |
| 处方管理 | 所有人 | ADM+DOC (create) |
| 药品管理 | ADM+DOC | 仅 ADM |
| 收费管理 | ADM+CASHIER | ADM+CASHIER |
| 病历管理 | ADM+DOC | ADM+DOC |
| 用户管理 | 仅 ADM | 仅 ADM |

## 关键工作流
1. 挂号 → 排班号源(Redis Lua原子扣减) → 排队号(同排班计数)
2. 接诊 → startConsultation(校验挂号属当前医生) → finishConsultation(自动生成病历)
3. 处方 → 写处方+药品明细 → Controller层自动生成待收账单
4. 收费 → 选支付方式 → 更新账单+处方状态

## 注意
- **重置数据库一律优先用根目录 `reset.bat`**（2026-09-30 实测跑通：= `mysql < sql/init.sql` + `docker exec clinic-redis redis-cli FLUSHDB`，密码/路径/容器名均已配好）。**不要手工另敲一套命令**，也不要在 `sql/` 下另造重置脚本。仅当后台无人值守（`pause` 会卡住）时才拆成命令执行
- **`sql/` 下只有一个脚本：`sql/init.sql`**（13 张表 + 全量演示数据，144KB）——`docker-compose.yml` 与 `reset.bat` 都指向它。生成器 `gen_init_full.py` 的输入模板在 `tools/init_template.sql`（13 表 DDL + 最少种子，**勿直接执行**），生成器输出**原地覆盖** `sql/init.sql`。不要再新增补丁 / 备份 / 第二份 init
- 2026-09-30 收敛前曾并存 `init.sql`(基础版) + `init_full.sql`(完整版) 两个文件，**已合并为一个**；`patch_v2_doctor_fee_drug_expiry.sql` 与 `sql/backup/` 已删除
- **init.sql 结构已核验与实体/线上库逐列一致**（2026-10-02）：13 表 / 133 列，DDL↔`information_schema` 13/13 完全一致，13 个实体零缺列。P8 的 3 个新列（`doctor.registration_fee`、`drug.produce_date`、`drug.expire_date`）**已在 init.sql 内**，分级定价与效期分布都用 `UPDATE ... WHERE` 补（相对日期，任何时候初始化分布相同）；**分单结算 / 挂号即缴费未加任何列**（靠 `bill.prescription_id` 判单据类型）。init.sql 由「基础种子（无 id 自增）+ 扩展种子（显式 id 从 N+1 接编）」两段组成，**无 DELETE**
- **演示/验收前先跑 `reset.bat`**：线上库会因手动测试变脏（2026-10-02 曾多出 7 条张三的 10-02 挂号 + 1 个测试药品 `wqeq`，而 init.sql 是 109 挂号 / 12 药品）
- **药品效期种子分布（2026-10-03 起，含 2 个 WARN 档供演示）**：`tools/init_template.sql` 用相对日期 `UPDATE` 保证任何时候初始化分布一致 —— **7 NORMAL / 2 WARN（阿莫西林 +45、布洛芬 +60 天，开方下拉带黄标）/ 2 NEAR（蒙脱石散 +20、奥美拉唑 +25）/ 1 EXPIRED（氯雷他定 −46）**。⚠️ `reset.bat` 重建后 `drug.status` **全为 1**，必须**重启后端**（`ApplicationReadyEvent` 补偿扫描）才会把 3 个 NEAR/EXPIRED 下架 —— **重建库 ≠ 已下架**。生成器需 `pycryptodome`，系统 python 没有，用 `~/.workbuddy/binaries/python/envs/default/Scripts/python.exe`
- 每次改后端代码需重启(IDE热更新不覆盖SQL/XML等)
- **药品临期提醒（2026-10-03）**：两级效期管控 —— 下架/禁开线 `NEAR_EXPIRY_DAYS=30`、预警线 `EXPIRY_WARN_MONTHS=3`（31~90 天可开+提示），阈值只在 `DrugExpiryScheduler` 一处；`DrugService.fillExpiryInfo()` 换算 `daysToExpire`/`expiryLevel`（EXPIRED/NEAR/WARN/NORMAL/UNKNOWN）下发，前端 `PrescriptionList.vue` 纯展示（下拉文案 + 行内黄 tag + 提交二次确认）。**零数据库列**
- PrescriptionService ↔ BillService 存在循环依赖风险，涉及账单的操作放 Controller 层
- AES 加密: key=32字节(AES-256), IV="ClinicSystem-IV-", CBC模式
- AES 密钥变更历史: 初始 `clinic-aes-key-32bytes-secret!!`(AES-128) → 当前 `clinic-aes-secret-key-for-256bit`(AES-256)
- init.sql 密文必须与当前 application.yml 的密钥匹配，换密钥后必须重新加密种子数据
- **命令行启动后端必须 `unset SERVER__PORT SERVER__HOST`**：执行环境注入的 `SERVER__PORT=50447`
  会被 Spring 宽松绑定成 `server.port`，覆盖 yml 里的 8080 并撞上沙箱已占端口（报 `Port 50447 already in use`）。
  这与项目配置无关，application.yml / target 里都是 8080
- 验证接口速查：登录 `POST /api/auth/login`（admin/admin123 → data.token，短 token 直接作 Bearer）；
  工作台 `GET /api/dashboard`（`data.todos[]` 含 color/text/time）；药品 `GET /api/drugs?page=1&pageSize=20`
- MySQL 库名 **`clinic_db`**；密码取值原则见上方"技术约定"（不硬记明文，用 reset.bat 或 Read application.yml）
- 移动端排班接口：`GET /api/mobile/schedules?workDate=YYYY-MM-DD`（免登录，返回当日全部排班 16 条，
  含 `department`/`title`/`registrationFee`/`remainNum`）

## 项目状态（截至 2026-09-30）
### 已完成
- 全部 10 个业务模块开发
- RBAC 三层权限（前端菜单 + 路由 + 后端 Controller）
- ForbiddenException (HTTP 403)
- Redis Lua 防超卖 + SETNX 分布式锁 + RabbitMQ 异步
- AES-256 病历加密（已从 AES-128 升级，代码/文档一致）
- 13 张数据库表 + 完整种子数据
- 19 个测试用例（15 单元 + 4 集成）100% 通过 + IDEA 全绿截图
  - 分布：`AesUtilsTest` 5 / `JwtTokenTest` 5 / `SecurityUtilsTest` 5 / `MiddlewareIntegrationTest` 2 / `DrugStockLockTest` 2
  - ⚠️ `AesUtilsTest` 的 surefire 报告里有 1 处 `ERROR ... AES解密失败`——**预期内**（用例「篡改密文后解密应抛异常」先打日志再抛异常）。判绿看 `Failures=0`，别看 ERROR 计数
  - `DrugStockLockTest` 完全绕过 `PrescriptionService`（直调 mapper + Redis SETNX），故药品效期/状态校验改动拦不到它；其 `setUp()` 直插药品 `expireDate=null`
  - 跑完全量测试污染量：挂号 +28 条测试残留、药品 +2 个「测试药品-时间戳」、排班2 remain→0、Redis 多 2 个 stock key → **必须重建库 + FLUSHDB**
- 技术文档 docs/技术文档.md
- 测试报告 docs/测试报告.md
- 答辩 PPT docs/答辩PPT.pptx (12 页)
- 系统截图（已插入 PPT）
- 毕业论文 docs/毕业论文.docx（约1.2万字，七章+参考文献+致谢）
- 开题报告（用户已自定稿，docx 收尾完成）
- JMeter 挂号压测：35并发→30成功/0超卖，平均137ms/QPS 137.8
- JMeter 库存压测：30并发→5成功/0超卖（发现并修复 SETNX 锁超卖 bug）
- 库存超卖 bug 修复：PrescriptionService.deductStock() 增加 UPDATE 影响行数检查
- AES-256 升级：AesUtils.java byte[16]→[32]，application.yml 密钥 31→32 字符
- init.sql 恢复：sql/init.sql 477行（从 live MySQL dump）
- **sql/init.sql（2026-09-30 合并后）**：唯一的数据库脚本 = 13 张表 DDL + 全量演示数据。规模：8 医生/30 患者/224 排班/**109** 挂号/86 就诊+病历/70 处方/**109** 账单。由 `gen_init_full.py` 读 `tools/init_template.sql` 生成（可重跑调数据量，`random.seed(20260921)` 固定；重跑会覆盖 sql/init.sql，验证过与手改前逐字节一致）。
  - 约定①：过去日期排班 `deleted=1` 归档——避开排班列表按日期升序导致首页全是过期数据，同时挂号列表裸 SQL 联表仍能取到医生/科室
  - 约定②：挂号费**按医生分别定价**（`doctor.registration_fee`，梯度 10.00~50.00）——2026-09-30 起由"统一 10.00"升级。张50/陈45(主任) 赵35/孙30/李25(副主任) 王20/刘15/周10(主治)
  - 约定③：病历 content 密文 = AES-256-CBC(key=`clinic-aes-secret-key-for-256bit`, IV=`ClinicSystem-IV-`, PKCS7, Base64)
  - 约定④：号源 remain_num 由 SQL 统一重算 `total_num - 有效挂号数(status<>0)`，禁止手算
  - 约定⑤：**药品效期两级管控（2026-10-03 起，两条线互不干扰）**
    * **下架/禁开线 30 天**：`expire_date <= CURDATE() + 30 天` 视为近效期 → 自动下架 + 禁止开具（"有效期至 X"当天仍有效）。
      `DrugExpiryScheduler.NEAR_EXPIRY_DAYS = 30` 被**调度器 / 工作台 / 处方校验三处共用**，改阈值须三处联动
    * **预警线 3 个月（2026-10-03 新增）**：`EXPIRY_WARN_MONTHS = 3`，剩余不足 3 个月但已超 30 天的药品
      **仍可开具**，只在医生开方界面黄色提示「还有 X 天过期」+ 提交前二次确认
    * 阈值统一由 `DrugExpiryScheduler` 持有，`DrugService.fillExpiryInfo()` 换算成 `daysToExpire` / `expiryLevel`
      （EXPIRED / NEAR / WARN / NORMAL / UNKNOWN）下发，**前端不自行判断阈值**、**未加数据库列**（均为 `@TableField(exist=false)` 瞬态字段）
    * 两列**必须允许 NULL**（DrugStockLockTest 运行时 insert 不带日期）
  - 约定⑥：药品 `status` 1 在用 / 0 已下架；药品列表 **默认查全部**（不传 status 查全部），否则自动下架后管理员看不见无法处理
  - 约定⑦：**排班 2（张医生今日下午）是集成测试夹具**——`total_num=30`、2 条有效挂号 → `remain_num=28`。`TODAY_PLAN` 禁止再向排班 2 追加挂号（2026-09-30 已移除 `(1,"PM",[1,1])`，挂号 111→109）
  - 新增登录账号 doctor03~doctor08，密码同 admin123
- 论文正文：第一章（引用+可行性分析）✅ 第三章（核心用例）✅ 第六章（性能测试节+第七章修正）✅ 第二章/第四章/第五章待审查
- 论文配图：用例图/架构图/ER图/流程图 已全部完成 ✅
- 6.1 补 JMeter 层 + 6.3 数据对齐代码（28/7）+ 6.4/6.5 去重合并
- **P8 挂号费分级 + 药品效期自动下架（2026-09-30 完成并实测通过）**
  - 新增 `DrugExpiryScheduler`（`@Scheduled(cron="0 30 0 * * ?")` + `ApplicationReadyEvent` 启动补偿扫描）——下架线为**剩余 ≤30 天（近效期）**，不再是"已过期"；实测启动后一次下架 3 种（蒙脱石散/奥美拉唑/额哇）
  - `BillService.resolveRegistrationFee(registrationId)` 走「挂号→排班→医生」取价，兜底 10.00；实测账单按 50.00（张医生）生成
  - `PrescriptionService` 开方前校验药品状态与**近效期（剩余 ≤30 天即禁止开具）**；`DashboardService.buildTodos()` 药品段合并为一条：已过期(红) / 近效期(黄)
  - **推送方式（用户问过）**：下架是 `@Scheduled` 定时任务改库状态；工作台提醒是 `GET /api/dashboard` 时**实时聚合计算**（拉取式），**不是主动推送**（无 WebSocket / MQ 通知）
  - 药品效期在 `DrugController.checkExpiryDates()` 为**接口层必填**（前端 DrugList.vue 同步必填+日期互斥校验）
  - 后端 8 文件 + 前端 5 文件已改；`mvn compile` / `npm run build` 均 exit=0
  - **执行方式已从"打补丁"改为"重建库"**：直接跑根目录 `reset.bat`（= `sql/init.sql` 重建 + FLUSHDB；脚本内含 DROP TABLE，可反复执行）。
    `sql/patch_v2_doctor_fee_drug_expiry.sql` 仅保留给"不想重建"的旧结构库，本项目已不需要执行

### ✅ 已解决：集成测试 28/7 断言
2026-09-30 实跑通过。排班 2 恢复 `total_num=30 / remain_num=28`（init.sql 的 2 条占用），
`gen_init_full.py` 不再向排班 2 追加挂号。
实测输出：`并发35请求: 成功28个, 被拒7个` → `Tests run: 2, Failures: 0, Errors: 0` → `BUILD SUCCESS`。

**★ 跑测试的硬规则（该用例有状态、不可连跑）**：
每次跑之前必须 ① 重建库 ② `docker exec clinic-redis redis-cli FLUSHDB`，跑完后再重建一次恢复演示数据。
否则第二轮会因"您已挂过该号源"+号源耗尽而全败。

**★ 两套并发数字别混**（曾因此在文档里出错）：
JUnit 集成测试 = 35 并发抢剩余 28 → **28/7**；JMeter 压测 = 35 并发抢全空 30 → **30/5**（平均 137ms、QPS 137.8）。
2026-09-30 已修正 `docs/技术文档.md` 与 `docs/测试报告.md` 中被误记成 30/5 的集成测试数据。

### ⚠️ 已知缺陷：挂号模块（2026-09-30 代码审查发现，尚未修复）
**① `createRegistration()` 不校验患者是否存在**（`RegistrationService` 第116-181行）
`patient` 表 `id` 仅 1~30，但测试跑出的挂号含 `patient_id=100~134`；方法全程未查 patient 表，
且 `registration` 表**无外键**（FK=0）。挂号列表 `LEFT JOIN patient`（`RegistrationMapper.xml:29`）
→ 界面患者姓名显示空白但仍占号源。
**⚠️ 修复它会打红集成测试**（测试用 `100+i` 假患者），必须同步改测试。

**② 排队号并发重复**（`RegistrationService` 第165-175行）
`queueCount = count(...) + 1` 是「先查后写」非原子。实测排班2 上 `queue_num=3` 出现 5 次。
**修复（1 行）**：Lua 已 `return stock - 1`（扣减后剩余），改为
`queue_num = schedule.getTotalNum() - result` 即原子唯一，无需额外查询。

**③ 测试代码**：`assertTrue(succ <= 30)` 是死断言（被 `succ == 28` 蕴含）；`fail == 7` 把异常也计入，脆弱。

### ✅ 已决定不修：挂号模块两缺陷 + 存储层改造（2026-09-30 21:30）
用户明确表态"算了不用改了，到时候按照一定的逻辑演示应该也可以"。以下两项**保持现状**，
改为在演示话术上绕开（缺陷②的 1 行修复方案仍记录在上面，答辩后如需可随时改回）：
1. ❌ 排队号并发重复（缺陷②）——不修
2. ❌ localStorage → sessionStorage 改造（4 文件：`stores/user.js`、`utils/request.js:59/:114`
   、`router/index.js:197/:211`、⚠️`views/prescription/PrescriptionList.vue:191/:215`）——不改

### 演示方案（2026-09-30 定稿）
**根因**：localStorage 同源全标签页共享 token → 多标签页登不同角色会串号，
表现为"医生点接诊报权限不足"（实际带出去的是收银员 token，后端权限逻辑本身没错）。

**三条代码级验证（答辩答疑可直接引用）**：
1. `views/layout/Login.vue:74` 表单是 `reactive()` **内存态、不落任何存储** → 多标签页各填各的账号互不干扰；
   且**登录页已默认预填 admin / admin123**，admin 那格不用手输
2. `router/index.js:204` 守卫只拦"非登录页 **且** 无 token"→ **已登录状态下仍可打开 `/login` 换账号**（不会被踢回工作台）
3. 全项目 grep `setInterval|setTimeout|polling|EventSource|WebSocket` = **0 匹配** → 后台闲置的旧标签页不会自己发请求

**推荐操作法（最稳）：单标签页串行**
演示完 admin → 地址栏直接敲 `/login` → 填下一个账号 → 登录。
Layout 组件重新挂载 → 所有页面数据按新 token 重拉 → 零串号。

**用户偏好的多标签页预填法（已验证成立）**：开 N 个标签页各停在登录页填好账号，轮流点登录。
| 组合 | 可并存身份数 |
|---|---|
| 同浏览器多个普通标签页（未改造前） | 1 ❌ |
| 同浏览器多个无痕窗口 | 1 ❌（共享同一隐私会话） |
| 普通窗口 + 无痕窗口 / Chrome + Edge | 2 |
| Chrome普通 + Chrome无痕 + Edge普通 | 3 |
两条铁律：① **只在"最后登录的那个"标签页里操作**，旧标签页只能看不能点
（`utils/request.js:59` 每个请求**实时读** localStorage 取 token）；② **不要刷新填好账号的标签页**
（组件重建 → 输入框恢复默认 admin/admin123）。

**演示主路径**：admin（工作台药品待办 → 医生挂号费分级 → 药品效期 → 建档/挂号/收费）
→ doctor01（接诊 → 开处方）→ cashier01（收费结算）。

### ✅ P9 移动端体验优化（2026-10-02 完成并实测）

**a) 排班页「科室分类 + 医生搜索」**（`MobileHome.vue`）
- 搜索框（医生姓名/科室/职称）+ 科室 chips（由当日排班**动态推导**）+ 结果计数 + 空态双提示
- **纯前端过滤，后端零改动**（每天仅 16 条排班，接口已返回全部字段）→ **论文 API 表无需改**

**b) 就诊页「四分区 + 分天排队看板」**（`MobileRecords.vue` + `MobileController.myData()`）
- 挂号列表四分区（**互斥**，空分区不渲染）：💳待缴费 → 📍今日就诊 → 🗓即将就诊 → 📁历史记录
- 排队看板由"单个排班"改为 **`queueGroups` 按排班分组**（一天一组，组头含 就诊日 + 上午/下午 + 医生）
- `my-data` 的 `registrations` 补全 `workDate/period/doctorName/department/title/billId/billStatus/billAmount`，
  排序为「今天优先 → 未来升序 → 过去倒序，同日 AM 在 PM 前」；新增返回 `today`（服务端下发，防客户端时区误差）

**★ 两个关键坑（改移动端就诊页必须先看）**
1. **`registration.registration_date` 是"挂号操作那天"，不是"就诊那天"**
   （`RegistrationService:177` 写死 `LocalDate.now()`）→ 按就诊日分组**必须用 `schedule.work_date`**
2. **历史挂号的排班已归档 `deleted=1`**，`scheduleService.listByIds()` 会被 MyBatis-Plus
   逻辑删除拦截器自动过滤 → 就诊日期/医生全部丢失。**必须用新增的**
   `ScheduleMapper.selectByIdsIncludeDeleted()`（原生 `@Select`，不受拦截器影响）。
   PC 端挂号列表靠裸 SQL `LEFT JOIN` 绕过（`RegistrationMapper.xml:28-31` 不带 `s.deleted=0`）

- 验证：`mvn compile` + `npm run build` 均 exit=0；**另起 8081 后端实例做端到端实测**
  （`unset SERVER__PORT SERVER__HOST && SERVER_PORT=8081 mvn -o spring-boot:run`，不干扰 8080），
  四分区/排序/分组全部符合预期；测试数据已清理（排班 9 恢复 30/30，Redis key 已删）
- 顺手修：`MobileHome.vue` 日期 tabs 的 `toISOString()` UTC 偏移（GMT+8 凌晨 0~8 点会把"今天"算成昨天）
- 详见 `.workbuddy/memory/2026-10-02.md`

### ✅ P10 移动端「聚焦当天」（2026-10-02 完成并实测）

用户反馈："我的挂号显示了太多天的内容"、"排队等待应该显示当天的上午/下午排班"。

**a) 排队看板 → 只显示今天**
- 后端 `MobileController.myData()`：`activeScheduleIds` 增加 `.filter(workDate == today)`，
  只下发今天的排班组（上午 AM / 下午 PM 各一块，同日 AM 在前）
- ⚠️ **踩坑**：`scheduleMap` 在上面被重新赋值过，lambda 内引用会报
  "lambda 表达式引用的本地变量必须是最终变量" → 必须加 `final Map<Long, Schedule> schedMap = scheduleMap;`
- 前端表头由「就诊日+时段+医生」简化为「时段+医生+人数」，顶部加一行「今日排队 10/02」
- 其他日期预约（`status=1 && workDate != today`）不进看板，收成一行
  「还有其他日期的预约 N 个 ›」→ 点击切到 `tab = 'registrations'`

**b) 我的挂号 → 历史记录折叠**
- 新增 `HISTORY_SHOW = 3` + `historyExpanded` ref + `visibleList(sec)`
- 历史分区默认只渲染最近 3 笔，超出显示「展开全部 N 笔 ⌄」/「收起 ⌃」
- `loadData()` 里重置 `historyExpanded = false`（换患者/刷新回到折叠态）
- 四分区结构与互斥规则不变

验证（8081 实例端到端，3 场景）：
| 场景 | 排队看板 | 其他日期提示 | 历史折叠 |
|---|---|---|---|
| 张三（今天有号 + 5 笔历史） | 1 组（今天 PM） | 0 | 3/5 |
| 高文博（只有明天有号） | 0 组 → 空态 | 1（10-03 AM） | 3/3 |
| 张三 + 现造 10-03 挂号 | 仍 1 组（只今天） | 1（10-03 AM） | 3/5 |

测试数据已清理（排班 7 回 30/30、Redis key 已删）；`mvn compile` + `npm run build` 均 exit=0。

### ✅ P11 账单「分单结算」：挂号费单 + 药费单（2026-10-02 完成并实测）

**用户诉求**："挂号成功就要缴费，后面的药品支付可以慢点交" → 明确"挂号就是要先交钱"。

**⚠️ 动因是一个既有 bug**：改造前一张账单承载「挂号费+药费」，挂号时创建、开方时**原地追加**。
`BillService.createBill` 的 `setStatus(0)` 只写在新建分支里 → 患者先付挂号费（status=1）后，
开方追加药费只改金额**不重置状态** → **药费白送**，收银台也看不到待收款。

**★ 分单规则（改账单代码前必看）**
| 账单 | 生成时机 | `prescription_id` | `registration_fee` | `drug_fee` |
|---|---|---|---|---|
| 挂号费单 | 挂号时 | `NULL` | 挂号费 | 0.00 |
| 药费单 | 医生开方后 | 处方ID | 0.00 | 药费 |

- `BillService`：`createBill(regId, prescId)` 降级为**兼容入口**（按 prescId 分流）；
  新增 `createRegistrationBill(regId)` / `createDrugBill(regId, prescId)`
- `resolveFeeType(bill)` **静态方法**由金额推导类型（`REGISTRATION`/`DRUG`/`MIXED`），**未加数据库列**
- **禁用 `getOne(registrationId)` 查账单**——一条挂号有多张，会抛 `TooManyResultsException`；
  必须 `list(in(regIds))` 内存分组
- 移动端 `register` 返回 `registrationId`/`billId`/`billAmount`；`my-data` 的 regView 新增 `bills[]`
  （逐笔账单），保留 `billId`/`billStatus`/`billAmount` 兼容汇总字段
- 前端：`MobileRecords.vue` 卡片内**逐行**渲染账单（各带"去支付"）；`BillList.vue` 加「费用类型」列
- 移动端挂号缴费流程于同日进一步收紧，见 **P12**

**实测**（8081 + 完整链路：挂号→付挂号费→接诊→开方→付药费）：
| bill | 挂号 | 处方 | 挂号费 | 药费 | 总额 | 状态 |
|---|---|---|---|---|---|---|
| 114 | 114 | NULL | 50.00 | 0.00 | 50.00 | 已支付 |
| 115 | 114 | 71 | 0.00 | 52.00 | 52.00 | 待支付 |

挂号费付清后开方，药费**单独成单**、挂号费单未被污染 ✓。测试数据已清理（含 `drug_stock` 恢复）。

### ✅ P12 移动端挂号「先缴费，后挂号」一次调用（2026-10-02 完成并实测）

**用户诉求**："挂号还是有问题，一定要先缴费，不能稍后，交完钱才可以挂号。"
（P11 里留的"稍后缴费"按钮被否决）

**核心约束：库中不允许存在"已挂号但挂号费未缴"的状态。**

- 后端 `MobileController.register`：`createRegistration`（Redis Lua 扣号源）→ `createRegistrationBill`
  → `billService.pay`，**串在一次调用里**；任一步抛异常则
  `billService.removeById(bill)` + `registrationService.cancelRegistration(regId)` **整体回退**。
  请求体新增 `paymentMethod`（默认 `WECHAT`），返回新增 `paymentMethod` / `paid=true`。
- 前端 `MobileHome.vue`：
  - 「确认挂号」弹窗 → 按钮改 **"下一步：缴费"**（纯前端校验手机号格式，此时**还没调挂号接口**）
  - 缴费弹窗标题 **"确认挂号并缴费"**：金额 + 医生信息 + **微信/支付宝二选一** +
    按钮"支付 ¥xx 并挂号"，**删除 `payLater` / `enterPayFlow` / `registerBill`**
  - 手机号未建档时在支付那一步报错 → 关缴费弹窗转「建档」→ 建档成功后**回到缴费**（不再直接挂号）
  - 成功弹窗显示排队号 + "挂号费 ¥xx 已支付"
- **PC 端 `RegistrationController` 明确不改**（2026-10-02 用户确认）：仍为
  "挂号 → 生成待收挂号费单 → 收银台收款"。**理由：PC 端必须登录，患者进不去**
  （H5 免登录页面才是患者自助入口），故"先缴费才能挂号"只约束移动端。
  PC 挂出来的挂号费单若未收，会正常出现在患者移动端「待缴费」里，可自行补缴。

**实测（8081，全部通过）**
| 场景 | 结果 |
|---|---|
| 挂号（郑十 → 排班5，ALIPAY） | `{"queueNum":1,"registrationId":118,"billId":119,"billAmount":50.00,"paid":true}` |
| 库内状态 | 挂号 118 `status=1`；账单 119 `挂号费=50, 药费=0, status=1, payment_method=ALIPAY`；**该挂号未付账单数=0** |
| 号源 | 排班5 `30→29`，Redis `schedule:stock:5=29` |
| `my-data` | 挂号 118 `billStatus=1` **不在「待缴费」分区**；郑十待缴费 0 笔 |
| 重复挂号同一号源 | `400 您已挂过该号源`，无新增记录、号源不再扣 |
| 未建档手机号 | `400 未找到该手机号对应的患者`，无新增 |
| 号源已满（Redis 临时置 0） | `400 号源已满`，无新增记录 |

`mvn -q -o compile` exit=0；`npm run build` exit=0（MobileHome 9.63kB）；
Vite 编译 HTTP 200，标记 `payMethod`×7 / `payAmount`×5 / `cancelPay`×3 / `pm-item`×2，
旧标记 `payLater`/`enterPayFlow`/`registerBill`/`稍后缴费` 均 **0**。

测试数据已清理（挂号 118 + 账单 119 删除、排班5 回 `30/30`、Redis key 删除、`_md.json` 删除）。

> ⚠️ **踩坑**：用 `curl -H "Authorization: Bearer ..."` 时**手工粘贴 JWT 会被工具输出的脱敏文本
> 污染**（拿到假 token → 403 白排查）。正确姿势：登录响应重定向到文件 →
> `python` 提取 token 落 `_tk.txt` → `TK=$(cat _tk.txt)`。

### ❌ 医保报销：已决定**不做**（2026-10-03 用户拍板）
- 用户表态："不用做医保方向了，目前这样的逻辑正确就可以了。"
- 系统保持 **0 医保**：`bill` 无报销额/自付额、`payment_method` 无医保；
  `drug` 无甲乙丙类；`patient` 无医保卡号/参保类型 —— **均不加字段，保持现状**
- 曾给 3 方案（A 演示级最小闭环 / B 甲乙丙类+起付线 / C 只写论文零代码），**全部弃用**
- 答辩若被问医保，话术：本系统按**自费结算**设计，医保对接属院端 HIS / 医保局接口范畴，不在本系统范围

### 待处理
- 🟡 **论文 5.1 措辞**（第464行）：现写"**集成测试**表明，35 并发请求抢 30 号源时，成功 30 次、拒绝 5 次"
  → 数字 30/5 是 JMeter 的，集成测试是 28/7。建议改为"**JMeter 压测**表明…"，与 6.3 的 28/7 并存不矛盾
- 🟡 论文收尾：封面信息（学号2023102114/姓名袁水康/导师/日期）、参考文献格式、字号行距
- 🟡 截图贴入（第五章页面截图 + 第六章19测试+2组JMeter截图）
- 🟡 GitHub 切换默认分支 main → master
- ✅ 论文七章全部审查完成（引用/数据/编号对齐）
- ✅ 论文配图全部完成（用例/架构/ER/流程/挂号Lua）
- ✅ JMeter 性能压测 2场景（均零超卖）
- ✅ 19 测试截图 / AES-256 / init.sql
- ✅ P8 两项功能 + 集成测试 28/7 实测通过（2026-09-30）
