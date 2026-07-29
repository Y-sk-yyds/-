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
- MySQL: 本机 8.4, root/123456, 3306
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
- 每次改后端代码需重启(IDE热更新不覆盖SQL/XML等)
- PrescriptionService ↔ BillService 存在循环依赖风险，涉及账单的操作放 Controller 层
- AES 加密: key=32字节(AES-256), IV="ClinicSystem-IV-", CBC模式
- AES 密钥变更历史: 初始 `clinic-aes-key-32bytes-secret!!`(AES-128) → 当前 `clinic-aes-secret-key-for-256bit`(AES-256)
- init.sql 密文必须与当前 application.yml 的密钥匹配，换密钥后必须重新加密种子数据

## 项目状态（截至 2026-07-23）
### 已完成
- 全部 10 个业务模块开发
- RBAC 三层权限（前端菜单 + 路由 + 后端 Controller）
- ForbiddenException (HTTP 403)
- Redis Lua 防超卖 + SETNX 分布式锁 + RabbitMQ 异步
- AES-256 病历加密（已从 AES-128 升级，代码/文档一致）
- 13 张数据库表 + 完整种子数据
- 19 个测试用例（15 单元 + 4 集成）100% 通过 + IDEA 全绿截图
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
- 论文正文：第一章（引用+可行性分析）✅ 第三章（核心用例）✅ 第六章（性能测试节+第七章修正）✅ 第二章/第四章/第五章待审查
- 论文配图：用例图/架构图/ER图/流程图 已全部完成 ✅
- 6.1 补 JMeter 层 + 6.3 数据对齐代码（28/7）+ 6.4/6.5 去重合并

### 待处理
- 🟡 论文收尾：封面信息（学号2023102114/姓名袁水康/导师/日期）、参考文献格式、字号行距
- 🟡 截图贴入（第五章页面截图 + 第六章19测试+2组JMeter截图）
- 🟡 GitHub 切换默认分支 main → master
- ✅ 论文七章全部审查完成（引用/数据/编号对齐）
- ✅ 论文配图全部完成（用例/架构/ER/流程/挂号Lua）
- ✅ JMeter 性能压测 2场景（均零超卖）
- ✅ 19 测试截图 / AES-256 / init.sql
