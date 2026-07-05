# 社区诊所门诊诊疗管理系统 - 项目记忆

## 项目基本信息
- 项目名称: 社区诊所门诊诊疗管理系统 (Clinic Management System)
- 技术栈: Spring Boot 3.2 + Vue3 + Element Plus + MyBatis-Plus + MySQL + Redis + RabbitMQ
- 端口: 后端 8080, 前端 5173
- 工作目录: D:\newproject\wk6real

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
- 医生: SecurityUtils.getCurrentUserId() → DoctorMapper → doctorId，统一在 Controller 层过滤
- ADMIN/收银员: 无 Doctor 实体，查询不过滤，看全部数据
- 前端: userStore 存 doctorId（登录返回），用于前端过滤

## 关键工作流
1. 挂号 → 排班号源(Redis Lua原子扣减) → 排队号(同排班计数)
2. 接诊 → startConsultation(校验挂号属当前医生) → finishConsultation(自动生成病历)
3. 处方 → 写处方+药品明细 → Controller层自动生成待收账单
4. 收费 → 选支付方式 → 更新账单+处方状态

## 注意
- 每次改后端代码需重启(IDE热更新不覆盖SQL/XML等)
- PrescriptionService ↔ BillService 存在循环依赖风险，涉及账单的操作放 Controller 层
- AES 加密: key=前16字节, IV="ClinicSystem-IV-", CBC模式
