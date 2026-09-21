SET NAMES utf8mb4;
-- ================================================================
-- 社区诊所门诊诊疗管理系统 数据库初始化脚本（完整演示版）
-- ----------------------------------------------------------------
-- 与 init.sql 的关系：
--   init.sql       最小可用数据集（8 患者 / 2 医生 / 5 挂号 / 3 账单）
--   init_full.sql  本文件 = init.sql 全部内容 + 扩充演示数据
--   两者择一执行即可，执行本文件会重建全部表结构。
--
-- 扩充后的数据规模：
--   医生 8 位（覆盖内科/儿科/外科/皮肤科/中医科/耳鼻喉科/妇科）
--   患者 30 位，挂号 111 条，就诊与病历 86 份，处方 68 张，账单 111 条
--   挂号日期覆盖近 7 天，仪表盘趋势图与科室占比图均有真实分布
--
-- 执行方式：
--   mysql -uroot -p < sql/init_full.sql
--   或 Docker：docker exec -i clinic-mysql mysql -uroot -p*** < sql/init_full.sql
-- ================================================================
-- ============================================
-- 社区诊所门诊诊疗管理系统 数据库初始化脚本
-- ============================================
--
-- 重要：运行此脚本后，务必清空 Redis 缓存！
-- Linux/Mac: docker exec clinic-redis redis-cli FLUSHDB
-- Windows:  docker exec clinic-redis redis-cli FLUSHDB
-- 或直接运行项目根目录的 reset.bat
--
-- ============================================

CREATE DATABASE IF NOT EXISTS clinic_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE clinic_db;

-- ============================================
-- 1. 系统用户表
-- ============================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                            `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
                            `password`    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
                            `real_name`   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
                            `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
                            `role`        VARCHAR(20)  NOT NULL COMMENT '角色: ADMIN/DOCTOR/CASHIER',
                            `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
                            `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 1删除 0正常',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ============================================
-- 2. 患者表
-- ============================================
DROP TABLE IF EXISTS `patient`;
CREATE TABLE `patient` (
                           `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                           `name`            VARCHAR(50)  NOT NULL COMMENT '姓名',
                           `id_card`         VARCHAR(18)  DEFAULT NULL COMMENT '身份证号',
                           `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
                           `age`             INT          DEFAULT NULL COMMENT '年龄',
                           `gender`          CHAR(1)      DEFAULT NULL COMMENT '性别: M/F',
                           `allergy_history` VARCHAR(500) DEFAULT NULL COMMENT '过敏史',
                           `address`         VARCHAR(200) DEFAULT NULL COMMENT '住址',
                           `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                           PRIMARY KEY (`id`),
                           KEY `idx_phone` (`phone`),
                           KEY `idx_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者表';

-- ============================================
-- 3. 医生表
-- ============================================
DROP TABLE IF EXISTS `doctor`;
CREATE TABLE `doctor` (
                          `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                          `name`         VARCHAR(50)  NOT NULL COMMENT '姓名',
                          `department`   VARCHAR(50)  NOT NULL COMMENT '科室',
                          `title`        VARCHAR(50)  DEFAULT NULL COMMENT '职称',
                          `phone`        VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
                          `introduction` VARCHAR(500) DEFAULT NULL COMMENT '简介',
                          `user_id`      BIGINT       DEFAULT NULL COMMENT '关联系统用户ID',
                          `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                          PRIMARY KEY (`id`),
                          KEY `idx_department` (`department`),
                          KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生表';

-- ============================================
-- 4. 排班表
-- ============================================
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule` (
                            `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
                            `doctor_id`   BIGINT   NOT NULL COMMENT '医生ID',
                            `work_date`   DATE     NOT NULL COMMENT '工作日期',
                            `period`      VARCHAR(10) NOT NULL COMMENT '时段: AM=上午 PM=下午',
                            `total_num`   INT      NOT NULL DEFAULT 30 COMMENT '总号源',
                            `remain_num`  INT      NOT NULL DEFAULT 30 COMMENT '剩余号源',
                            `status`      TINYINT  NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0停诊',
                            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                            PRIMARY KEY (`id`),
                            KEY `idx_doctor_date` (`doctor_id`, `work_date`),
                            KEY `idx_work_date` (`work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班表';

-- ============================================
-- 5. 挂号表
-- ============================================
DROP TABLE IF EXISTS `registration`;
CREATE TABLE `registration` (
                                `id`                BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `patient_id`        BIGINT   NOT NULL COMMENT '患者ID',
                                `schedule_id`       BIGINT   NOT NULL COMMENT '排班ID',
                                `queue_num`         INT      DEFAULT NULL COMMENT '排队序号',
                                `status`            TINYINT  NOT NULL DEFAULT 1 COMMENT '状态: 0已取消 1已挂号 2已就诊 3爽约',
                                `registration_date` DATE     NOT NULL COMMENT '挂号日期',
                                `remark`            VARCHAR(200) DEFAULT NULL COMMENT '备注',
                                `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `deleted`           TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                                PRIMARY KEY (`id`),
                                KEY `idx_patient_id` (`patient_id`),
                                KEY `idx_schedule_id` (`schedule_id`),
                                KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挂号表';

-- ============================================
-- 6. 就诊记录表
-- ============================================
DROP TABLE IF EXISTS `consultation`;
CREATE TABLE `consultation` (
                                `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `registration_id` BIGINT       NOT NULL COMMENT '挂号ID',
                                `doctor_id`       BIGINT       NOT NULL COMMENT '医生ID',
                                `complaint`       VARCHAR(500) DEFAULT NULL COMMENT '主诉',
                                `diagnosis`       VARCHAR(500) DEFAULT NULL COMMENT '诊断',
                                `advice`          VARCHAR(500) DEFAULT NULL COMMENT '医嘱',
                                `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0待诊 1就诊中 2已完成',
                                `start_time`      DATETIME     DEFAULT NULL COMMENT '开始时间',
                                `end_time`        DATETIME     DEFAULT NULL COMMENT '结束时间',
                                `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                                PRIMARY KEY (`id`),
                                KEY `idx_registration_id` (`registration_id`),
                                KEY `idx_doctor_id` (`doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='就诊记录表';

-- ============================================
-- 7. 处方表
-- ============================================
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription` (
                                `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `consultation_id`     BIGINT       NOT NULL COMMENT '就诊ID',
                                `diagnosis`           VARCHAR(500) DEFAULT NULL COMMENT '诊断',
                                `prescription_content` TEXT         DEFAULT NULL COMMENT '用药说明',
                                `status`              TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0待收费 1已收费',
                                `doctor_id`           BIGINT       NOT NULL COMMENT '医生ID',
                                `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `deleted`             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                                PRIMARY KEY (`id`),
                                KEY `idx_consultation_id` (`consultation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方表';

-- ============================================
-- 8. 处方药品明细表
-- ============================================
DROP TABLE IF EXISTS `prescription_drug`;
CREATE TABLE `prescription_drug` (
                                     `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `prescription_id` BIGINT      NOT NULL COMMENT '处方ID',
                                     `drug_id`         BIGINT      NOT NULL COMMENT '药品ID',
                                     `quantity`        INT         NOT NULL COMMENT '数量',
                                     `usage_method`    VARCHAR(50) DEFAULT NULL COMMENT '用法: 口服/外用/注射',
                                     `dosage`          VARCHAR(50) DEFAULT NULL COMMENT '用量',
                                     `frequency`       VARCHAR(50) DEFAULT NULL COMMENT '频次',
                                     `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `deleted`         TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                                     PRIMARY KEY (`id`),
                                     KEY `idx_prescription_id` (`prescription_id`),
                                     KEY `idx_drug_id` (`drug_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方药品明细表';

-- ============================================
-- 9. 药品表
-- ============================================
DROP TABLE IF EXISTS `drug`;
CREATE TABLE `drug` (
                        `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
                        `name`          VARCHAR(100)   NOT NULL COMMENT '药品名称',
                        `specification` VARCHAR(100)   DEFAULT NULL COMMENT '规格',
                        `unit`          VARCHAR(20)    NOT NULL COMMENT '单位',
                        `unit_price`    DECIMAL(10,2)  NOT NULL COMMENT '单价',
                        `category`      VARCHAR(50)    DEFAULT NULL COMMENT '分类',
                        `status`        TINYINT        NOT NULL DEFAULT 1 COMMENT '状态: 1在用 0停用',
                        `create_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `deleted`       TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                        PRIMARY KEY (`id`),
                        KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品表';

-- ============================================
-- 10. 药品库存表
-- ============================================
DROP TABLE IF EXISTS `drug_stock`;
CREATE TABLE `drug_stock` (
                              `id`                BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键',
                              `drug_id`           BIGINT  NOT NULL COMMENT '药品ID',
                              `current_stock`     INT     NOT NULL DEFAULT 0 COMMENT '当前库存',
                              `warning_threshold` INT     NOT NULL DEFAULT 10 COMMENT '预警阈值',
                              `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `deleted`           TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_drug_id` (`drug_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品库存表';

-- ============================================
-- 11. 账单表
-- ============================================
DROP TABLE IF EXISTS `bill`;
CREATE TABLE `bill` (
                        `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
                        `registration_id`  BIGINT         NOT NULL COMMENT '挂号ID',
                        `prescription_id`  BIGINT         DEFAULT NULL COMMENT '处方ID',
                        `registration_fee` DECIMAL(10,2)  NOT NULL DEFAULT 0 COMMENT '挂号费',
                        `drug_fee`         DECIMAL(10,2)  NOT NULL DEFAULT 0 COMMENT '药费',
                        `total_amount`     DECIMAL(10,2)  NOT NULL DEFAULT 0 COMMENT '总金额',
                        `payment_method`   VARCHAR(20)    DEFAULT NULL COMMENT '支付方式: CASH/WECHAT/ALIPAY/CARD',
                        `status`           TINYINT        NOT NULL DEFAULT 0 COMMENT '状态: 0未支付 1已支付 2已退款',
                        `create_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `deleted`          TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                        PRIMARY KEY (`id`),
                        KEY `idx_registration_id` (`registration_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单表';

-- ============================================
-- 12. 电子病历表
-- ============================================
DROP TABLE IF EXISTS `medical_record`;
CREATE TABLE `medical_record` (
                                  `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
                                  `patient_id`      BIGINT   NOT NULL COMMENT '患者ID',
                                  `consultation_id` BIGINT   NOT NULL COMMENT '就诊ID',
                                  `content`         TEXT     DEFAULT NULL COMMENT '病历内容(AES加密)',
                                  `diagnosis`       VARCHAR(500) DEFAULT NULL COMMENT '诊断',
                                  `treatment_plan`  TEXT     DEFAULT NULL COMMENT '治疗方案',
                                  `doctor_id`       BIGINT   NOT NULL COMMENT '医生ID',
                                  `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  `deleted`         TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_patient_id` (`patient_id`),
                                  KEY `idx_consultation_id` (`consultation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子病历表';

-- ============================================
-- 13. 复诊提醒表
-- ============================================
DROP TABLE IF EXISTS `reminder`;
CREATE TABLE `reminder` (
                            `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                            `patient_id`      BIGINT       NOT NULL COMMENT '患者ID',
                            `consultation_id` BIGINT       NOT NULL COMMENT '就诊ID',
                            `doctor_id`       BIGINT       NOT NULL COMMENT '医生ID',
                            `remind_date`     DATE         NOT NULL COMMENT '应复诊日期',
                            `content`         VARCHAR(500) NOT NULL COMMENT '提醒内容',
                            `status`          TINYINT      DEFAULT 0 COMMENT '状态: 0待提醒 1已提醒 2已复诊',
                            `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `deleted`         TINYINT      DEFAULT 0 COMMENT '逻辑删除',
                            PRIMARY KEY (`id`),
                            KEY `idx_remind_date` (`remind_date`),
                            KEY `idx_patient_id` (`patient_id`),
                            KEY `idx_doctor_id` (`doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复诊提醒表';

-- ============================================
-- 初始数据: 管理员账号 admin/admin123
-- ============================================
-- 初始密码均为 admin123 (BCrypt加密)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) VALUES
                                                                                   ('admin',    '$2b$12$S4QQR9XurZKEzE8VLUDO9.slEa6qfILye9A3QUYKPULjc2E6k9A9e', '系统管理员', 'ADMIN', 1),
                                                                                   ('doctor01', '$2b$12$S4QQR9XurZKEzE8VLUDO9.slEa6qfILye9A3QUYKPULjc2E6k9A9e', '张医生',   'DOCTOR', 1),
                                                                                   ('doctor02', '$2b$12$S4QQR9XurZKEzE8VLUDO9.slEa6qfILye9A3QUYKPULjc2E6k9A9e', '李医生',   'DOCTOR', 1),
                                                                                   ('cashier01','$2b$12$S4QQR9XurZKEzE8VLUDO9.slEa6qfILye9A3QUYKPULjc2E6k9A9e', '王收银',   'CASHIER', 1);

INSERT INTO `doctor` (`name`, `department`, `title`, `phone`, `introduction`, `user_id`) VALUES
                                                                                             ('张医生', '内科',   '主任医师', '13800001111', '从事内科临床工作20年，擅长心血管疾病诊治', 2),
                                                                                             ('李医生', '儿科',   '副主任医师', '13800002222', '从事儿科临床工作15年，擅长儿童呼吸系统疾病', 3);

-- ============================================
-- 患者种子数据
-- ============================================
INSERT INTO `patient` (`name`, `phone`, `age`, `gender`, `allergy_history`, `address`) VALUES
                                                                                           ('张三',   '13800001101', 35, 'M', '无',                '朝阳区建国路100号'),
                                                                                           ('李四',   '13800001102', 28, 'F', '青霉素过敏',         '海淀区中关村大街1号'),
                                                                                           ('王五',   '13800001103', 45, 'M', '无',                '西城区金融街10号'),
                                                                                           ('赵六',   '13800001104', 52, 'F', '磺胺类过敏',         '东城区王府井大街20号'),
                                                                                           ('孙七',   '13800001105', 8,  'M', '无',                '朝阳区望京街道5号'),
                                                                                           ('周八',   '13800001106', 19, 'F', '花粉过敏',           '丰台区方庄路15号'),
                                                                                           ('吴九',   '13800001107', 62, 'M', '无',                '通州区梨园镇8号'),
                                                                                           ('郑十',   '13800001108', 31, 'F', '头孢类过敏',         '大兴区亦庄经济开发区3号');

-- ============================================
-- 排班种子数据（一周，两位医生，每天上午+下午）
-- ============================================
INSERT INTO `schedule` (`doctor_id`, `work_date`, `period`, `total_num`, `remain_num`, `status`) VALUES
-- 张医生(doctor_id=1) 今起7天
(1, CURDATE(),                          'AM', 30, 30, 1),
(1, CURDATE(),                          'PM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  'AM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  'PM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 2 DAY),  'AM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 2 DAY),  'PM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 3 DAY),  'AM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 3 DAY),  'PM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 4 DAY),  'AM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 4 DAY),  'PM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 5 DAY),  'AM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 5 DAY),  'PM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 6 DAY),  'AM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 6 DAY),  'PM', 30, 30, 1),
-- 李医生(doctor_id=2) 今起7天
(2, CURDATE(),                          'AM', 30, 30, 1),
(2, CURDATE(),                          'PM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  'AM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  'PM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 2 DAY),  'AM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 2 DAY),  'PM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 3 DAY),  'AM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 3 DAY),  'PM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 4 DAY),  'AM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 4 DAY),  'PM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 5 DAY),  'AM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 5 DAY),  'PM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 6 DAY),  'AM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 6 DAY),  'PM', 30, 30, 1);

-- ============================================
-- 药品种子数据
-- ============================================
INSERT INTO `drug` (`name`, `specification`, `unit`, `unit_price`, `category`) VALUES
                                                                                   ('阿莫西林胶囊',     '0.5g×24粒',    '盒', 18.50, '抗生素'),
                                                                                   ('头孢克洛胶囊',     '0.25g×12粒',   '盒', 35.00, '抗生素'),
                                                                                   ('布洛芬缓释胶囊',   '0.3g×20粒',    '盒', 15.00, '解热镇痛'),
                                                                                   ('对乙酰氨基酚片',   '0.5g×10片',    '盒', 5.80,  '解热镇痛'),
                                                                                   ('蒙脱石散',         '3g×10袋',      '盒', 22.00, '消化系统'),
                                                                                   ('奥美拉唑肠溶胶囊', '20mg×14粒',    '盒', 28.50, '消化系统'),
                                                                                   ('复方氨酚烷胺片',   '12片',         '盒', 8.50,  '感冒用药'),
                                                                                   ('连花清瘟胶囊',     '0.35g×36粒',   '盒', 14.80, '感冒用药'),
                                                                                   ('氯雷他定片',       '10mg×6片',     '盒', 12.00, '抗过敏'),
                                                                                   ('维生素C片',        '100mg×100片',  '瓶', 9.90,  '维生素类'),
                                                                                   ('硝苯地平控释片',   '30mg×7片',     '盒', 32.00, '心血管'),
                                                                                   ('盐酸二甲双胍片',   '0.5g×20片',    '盒', 16.00, '降糖药');

INSERT INTO `drug_stock` (`drug_id`, `current_stock`, `warning_threshold`) VALUES
                                                                               (1,  200, 20),
                                                                               (2,  120, 15),
                                                                               (3,  150, 15),
                                                                               (4,  80,  10),
                                                                               (5,  80,  10),
                                                                               (6,  100, 15),
                                                                               (7,  300, 25),
                                                                               (8,  250, 20),
                                                                               (9,  100, 10),
                                                                               (10, 500, 30),
                                                                               (11, 60,  10),
                                                                               (12, 90,  15);

-- ============================================
-- 演示用挂号数据（今日，已挂号待诊）
-- 让仪表盘"今日挂号""待就诊"显示真实数据
-- ============================================
INSERT INTO `registration` (`patient_id`, `schedule_id`, `queue_num`, `status`, `registration_date`) VALUES
                                                                                                         (1, 1, 1, 1, CURDATE()),
                                                                                                         (2, 1, 2, 1, CURDATE()),
                                                                                                         (3, 1, 3, 1, CURDATE()),
                                                                                                         (4, 2, 1, 1, CURDATE()),
                                                                                                         (5, 2, 2, 1, CURDATE());

-- 同步扣减排班剩余号源
UPDATE `schedule` SET `remain_num` = `remain_num` - 3 WHERE `id` = 1;
UPDATE `schedule` SET `remain_num` = `remain_num` - 2 WHERE `id` = 2;

-- ============================================
-- 演示用就诊记录（模拟完整就诊流程）
-- ============================================
INSERT INTO `consultation` (`registration_id`, `doctor_id`, `complaint`, `diagnosis`, `advice`, `status`, `start_time`, `end_time`) VALUES
                                                                                                                                        (1, 1, '反复头晕1月余，呈阵发性，伴胸闷', '高血压病1级', '低盐低脂饮食，硝苯地平控释片30mg qd，建议心电图检查，2周后复诊', 2, NOW(), NOW()),
                                                                                                                                        (2, 1, '发热咳嗽3天，体温最高38.5度', '上呼吸道感染', '多饮水，注意休息，3天后复诊', 2, NOW(), NOW()),
                                                                                                                                        (3, 1, '腹痛腹泻2天，每日3-4次水样便', '急性胃肠炎', '清淡饮食，口服补液盐，如症状加重及时就诊', 2, NOW(), NOW());

-- 同步更新挂号状态为"已就诊"
UPDATE `registration` SET `status` = 2 WHERE `id` IN (1, 2, 3);

-- ============================================
-- 演示用电子病历（AES-CBC 加密）
-- 密钥: clinic-aes-key-32bytes-secret!! (前16字节)
-- IV:   ClinicSystem-IV-
-- ============================================
INSERT INTO `medical_record` (`patient_id`, `consultation_id`, `content`, `diagnosis`, `treatment_plan`, `doctor_id`) VALUES
                                                                                                                          (1, 1, 'LEos8RWn7R3tx8yslgQ97ZZWqMf7GSuwmi9EMc+TGWuPcHla8K3JOLX2cDycuP4zuosrCBSK39sqc9KPRSNYeKPa3HlEkMeJokK7emule6yAp8RLbKWpXgJNYKf/NjJiNyM3GoUnoFIRmh8SlIEoWHLm2w26ThhLF6KGI2DkFF0AxD/6L+xwWEPY3BAOJoy3tJ1+0wFlXg0TE6G0MdVrynVodPlm2kDEus/fDlvFmvvtQm3KP3GTS64JH0LB+ZZ5NjKfTjgAyiJkV/SYTEdYBKq+XYgeOGv9y2DKBbgssR1exFA6GUewDp5qJ1OJPlfUnPO8EXaa6Vy71iGm36hCwTEFK7ICFUE7nei2omPnSJclFp3D9/OFsO43gxDon5FT', '高血压病1级', '1.低盐低脂饮食 2.硝苯地平控释片30mg qd 3.建议心电图检查 4.2周后复诊', 1),
                                                                                                                          (2, 2, '7imQPWX37hjADEdsp6We38mLHt4gdQBPsCzE6gByyceKwDZGhWuD7deCNNK5mjIZVf2tKpiE1+CGVUbqe0+VQkg48lIEsBrKWY5iLoaywCPlGtkyC3+Py3n38QX4cP3Jmff3BGNlcBqnVw9DMxzhw2ay/Gk0u+7qH8cL8+kVhFekBALuKzG4BBN97lSUHW/pZnNQ1XNxOIGhHZVIWvus3tQTDcQFsejS8R+8tRbo/DZoyAdO7iXyzFsHI5lGdjaNSJjd4qOAAZJh3ZN3jmAsnypS0BfY8yWvlt/xZM4WPeYZlB/FBby3tSIuebCXMdpSjZoDeVNtudn8u+yfRNsR2A==', '上呼吸道感染', '1.多饮水 2.复方氨酚烷胺片1片tid 3.布洛芬缓释胶囊0.3g prn 4.3天后复诊', 1),
                                                                                                                          (3, 3, '9eqBOA7ErPkDZY469vaXgauPfNnI5BeTYq+NEFn7XKY48oOPdpdkU6HCKNWcQELOakSlKzbzTOVYHnVdwgTfHHdxHs9yIwMYaitHG3AMPmji/HdlBOKf3eBl2+lJ5UIv11cgL99EPxpiy7+1XvDmelTo5NqWXBKhCdtDdXb/qCkRxbnQ7t1YMmPq0fPXx4AcDYkZKWDxVSLTHiagV23b87LCkTOHnfN8110PqR6mXoZxWLETI6yLQZiA5Q42LEnzOSQjw4v831qIx9CVF6O6iihKckyOe1/KSMAnKUnZyrPJSj/ZxNxRp/pKmDjMEtbb6XR8gUZK2HVHVU+JIQE3oe2VhpBni0BvnNPlqR7bPClW5GapSRoA+QFmOtOS6YxX', '急性胃肠炎', '1.清淡饮食 2.蒙脱石散3g tid 3.口服补液盐 4.如症状加重及时就诊', 1);

-- ============================================
-- 演示用处方案
-- ============================================
INSERT INTO `prescription` (`consultation_id`, `diagnosis`, `prescription_content`, `status`, `doctor_id`) VALUES
                                                                                                               (1, '高血压病1级', '硝苯地平控释片30mg每天1次', 1, 1),
                                                                                                               (2, '上呼吸道感染', '复方氨酚烷胺片1片tid + 布洛芬缓释胶囊0.3g prn', 0, 1),
                                                                                                               (3, '急性胃肠炎', '蒙脱石散3g tid', 1, 1);

INSERT INTO `prescription_drug` (`prescription_id`, `drug_id`, `quantity`, `usage_method`, `dosage`, `frequency`) VALUES
                                                                                                                      (1, 11, 1, '口服', '30mg', '每日1次'),
                                                                                                                      (2, 7, 1, '口服', '1片', '每日3次'),
                                                                                                                      (2, 3, 1, '口服', '0.3g', '必要时'),
                                                                                                                      (3, 5, 2, '口服', '3g', '每日3次');

-- ============================================
-- 演示用账单数据（明细见文末扩充段落）
-- ============================================
/* 账单数据在文末扩充段落统一生成（挂号费 10.00 元） */

-- ============================================
-- 演示用复诊提醒
-- ============================================
INSERT INTO `reminder` (`patient_id`, `consultation_id`, `doctor_id`, `remind_date`, `content`, `status`) VALUES
                                                                                                              (1, 1, 1, DATE_ADD(CURDATE(), INTERVAL 14 DAY), '高血压复诊：复查血压，评估硝苯地平控释片疗效', 0),
                                                                                                              (2, 2, 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '上呼吸道感染复诊：复查体温及咽部情况', 0);

-- ============================================================
-- 【扩充演示数据】
-- ------------------------------------------------------------
-- 说明：
--   1. 新增 6 位医生（外科/皮肤科/中医科/耳鼻喉科/内科/妇科）
--      对应登录账号 doctor03 ~ doctor08，密码同为 admin123
--   2. 新增 22 位患者，总患者数 30
--   3. 排班扩展至过去 7 天 ~ 未来 6 天；过去日期的排班
--      以 deleted=1 标记归档，不出现在排班管理列表中，
--      保证挂号记录仍能关联到对应的医生与科室
--   4. 挂号覆盖近 7 天，仪表盘趋势图与科室占比图均有真实分布
--   5. 挂号费统一为 10.00 元（与后端 BillService 一致）
--   6. 病历内容使用 AES-256-CBC 加密（与 application.yml 密钥匹配）
-- ============================================================

USE clinic_db;

-- ---- 1. 新增系统用户（医生账号，密码 admin123）----
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `status`) VALUES
(5, 'doctor03', '$2b$12$S4QQR9XurZKEzE8VLUDO9.slEa6qfILye9A3QUYKPULjc2E6k9A9e', '王医生', 'DOCTOR', 1),
(6, 'doctor04', '$2b$12$S4QQR9XurZKEzE8VLUDO9.slEa6qfILye9A3QUYKPULjc2E6k9A9e', '赵医生', 'DOCTOR', 1),
(7, 'doctor05', '$2b$12$S4QQR9XurZKEzE8VLUDO9.slEa6qfILye9A3QUYKPULjc2E6k9A9e', '陈医生', 'DOCTOR', 1),
(8, 'doctor06', '$2b$12$S4QQR9XurZKEzE8VLUDO9.slEa6qfILye9A3QUYKPULjc2E6k9A9e', '刘医生', 'DOCTOR', 1),
(9, 'doctor07', '$2b$12$S4QQR9XurZKEzE8VLUDO9.slEa6qfILye9A3QUYKPULjc2E6k9A9e', '周医生', 'DOCTOR', 1),
(10, 'doctor08', '$2b$12$S4QQR9XurZKEzE8VLUDO9.slEa6qfILye9A3QUYKPULjc2E6k9A9e', '孙医生', 'DOCTOR', 1);

-- ---- 2. 新增医生 ----
INSERT INTO `doctor` (`id`, `name`, `department`, `title`, `phone`, `introduction`, `user_id`) VALUES
(3, '王医生', '外科', '主治医师', '13800003333', '从事普外科临床工作12年，擅长体表肿物、软组织损伤诊治', 5),
(4, '赵医生', '皮肤科', '副主任医师', '13800004444', '从事皮肤科临床工作16年，擅长过敏性及感染性皮肤病诊治', 6),
(5, '陈医生', '中医科', '主任医师', '13800005555', '从事中医内科临床工作25年，擅长脾胃病与失眠调理', 7),
(6, '刘医生', '耳鼻喉科', '主治医师', '13800006666', '从事耳鼻喉科临床工作10年，擅长咽喉及鼻部疾病诊治', 8),
(7, '周医生', '内科', '主治医师', '13800007777', '从事内科临床工作9年，擅长高血压、糖尿病等慢病管理', 9),
(8, '孙医生', '妇科', '副主任医师', '13800008888', '从事妇科临床工作18年，擅长月经不调及妇科炎症诊治', 10);

-- ---- 3. 新增患者 ----
INSERT INTO `patient` (`id`, `name`, `phone`, `age`, `gender`, `allergy_history`, `address`) VALUES
(9, '陈晓东', '13800001201', 34, 'M', '无', '朝阳区朝阳北路88号'),
(10, '林书仪', '13800001202', 27, 'F', '青霉素过敏', '海淀区上地七街6号'),
(11, '黄志强', '13800001203', 41, 'M', '无', '西城区德胜门外大街12号'),
(12, '徐雅琴', '13800001204', 30, 'F', '无', '东城区东直门南大街9号'),
(13, '何俊杰', '13800001205', 23, 'M', '磺胺类过敏', '丰台区丰台南路22号'),
(14, '高文博', '13800001206', 56, 'M', '无', '朝阳区安贞西里3号'),
(15, '马丽娜', '13800001207', 38, 'F', '无', '海淀区学院路30号'),
(16, '罗宇轩', '13800001208', 11, 'M', '无', '石景山区鲁谷路18号'),
(17, '谢婉如', '13800001209', 45, 'F', '头孢类过敏', '西城区月坛北街5号'),
(18, '韩雪梅', '13800001210', 33, 'F', '无', '大兴区黄村西大街7号'),
(19, '沈嘉豪', '13800001211', 19, 'M', '无', '昌平区回龙观东大街15号'),
(20, '曹慧敏', '13800001212', 49, 'F', '无', '通州区新华大街40号'),
(21, '邓子昂', '13800001213', 6, 'M', '青霉素过敏', '朝阳区双井桥东2号'),
(22, '冯雨欣', '13800001214', 26, 'F', '无', '海淀区西二旗大街9号'),
(23, '蒋一鸣', '13800001215', 61, 'M', '无', '东城区和平里中街6号'),
(24, '卢佳琪', '13800001216', 15, 'F', '无', '丰台区方庄芳城园1号'),
(25, '崔浩宇', '13800001217', 29, 'M', '无', '朝阳区酒仙桥路11号'),
(26, '范思远', '13800001218', 52, 'M', '花粉过敏', '海淀区清河小营西路4号'),
(27, '汪雨桐', '13800001219', 9, 'F', '无', '西城区广安门内大街8号'),
(28, '唐若彤', '13800001220', 36, 'F', '无', '石景山区八角西街3号'),
(29, '石建国', '13800001221', 68, 'M', '无', '房山区良乡西路20号'),
(30, '秦梦洁', '13800001222', 24, 'F', '无', '朝阳区望京西园四区5号');

-- ---- 4. 新增排班（医生 3-8；过去日期 deleted=1 归档）----
INSERT INTO `schedule` (`id`, `doctor_id`, `work_date`, `period`, `total_num`, `remain_num`, `status`, `deleted`) VALUES
(29, 3, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'AM', 20, 20, 1, 1),
(30, 3, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'PM', 20, 20, 1, 1),
(31, 3, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'AM', 20, 20, 1, 1),
(32, 3, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'PM', 20, 20, 1, 1),
(33, 3, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'AM', 20, 20, 1, 1),
(34, 3, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'PM', 20, 20, 1, 1),
(35, 3, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'AM', 20, 20, 1, 1),
(36, 3, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'PM', 20, 20, 1, 1),
(37, 3, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'AM', 20, 20, 1, 1),
(38, 3, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'PM', 20, 20, 1, 1),
(39, 3, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'AM', 20, 20, 1, 1),
(40, 3, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'PM', 20, 20, 1, 1),
(41, 3, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'AM', 20, 20, 1, 1),
(42, 3, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'PM', 20, 20, 1, 1),
(43, 3, CURDATE(), 'AM', 20, 20, 1, 0),
(44, 3, CURDATE(), 'PM', 20, 20, 1, 0),
(45, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'AM', 20, 20, 1, 0),
(46, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PM', 20, 20, 1, 0),
(47, 3, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'AM', 20, 20, 1, 0),
(48, 3, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'PM', 20, 20, 1, 0),
(49, 3, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'AM', 20, 20, 1, 0),
(50, 3, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'PM', 20, 20, 1, 0),
(51, 3, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'AM', 20, 20, 1, 0),
(52, 3, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'PM', 20, 20, 1, 0),
(53, 3, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'AM', 20, 20, 1, 0),
(54, 3, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'PM', 20, 20, 1, 0),
(55, 3, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'AM', 20, 20, 1, 0),
(56, 3, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'PM', 20, 20, 1, 0),
(57, 4, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'AM', 30, 30, 1, 1),
(58, 4, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'PM', 30, 30, 1, 1),
(59, 4, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'AM', 30, 30, 1, 1),
(60, 4, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'PM', 30, 30, 1, 1),
(61, 4, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'AM', 30, 30, 1, 1),
(62, 4, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'PM', 30, 30, 1, 1),
(63, 4, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'AM', 30, 30, 1, 1),
(64, 4, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'PM', 30, 30, 1, 1),
(65, 4, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'AM', 30, 30, 1, 1),
(66, 4, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'PM', 30, 30, 1, 1),
(67, 4, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'AM', 30, 30, 1, 1),
(68, 4, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'PM', 30, 30, 1, 1),
(69, 4, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'AM', 30, 30, 1, 1),
(70, 4, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'PM', 30, 30, 1, 1),
(71, 4, CURDATE(), 'AM', 30, 30, 1, 0),
(72, 4, CURDATE(), 'PM', 30, 30, 1, 0),
(73, 4, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'AM', 30, 30, 1, 0),
(74, 4, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PM', 30, 30, 1, 0),
(75, 4, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'AM', 30, 30, 1, 0),
(76, 4, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'PM', 30, 30, 1, 0),
(77, 4, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'AM', 30, 30, 1, 0),
(78, 4, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'PM', 30, 30, 1, 0),
(79, 4, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'AM', 30, 30, 1, 0),
(80, 4, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'PM', 30, 30, 1, 0),
(81, 4, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'AM', 30, 30, 1, 0),
(82, 4, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'PM', 30, 30, 1, 0),
(83, 4, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'AM', 30, 30, 1, 0),
(84, 4, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'PM', 30, 30, 1, 0),
(85, 5, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'AM', 30, 30, 1, 1),
(86, 5, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'PM', 30, 30, 1, 1),
(87, 5, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'AM', 30, 30, 1, 1),
(88, 5, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'PM', 30, 30, 1, 1),
(89, 5, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'AM', 30, 30, 1, 1),
(90, 5, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'PM', 30, 30, 1, 1),
(91, 5, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'AM', 30, 30, 1, 1),
(92, 5, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'PM', 30, 30, 1, 1),
(93, 5, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'AM', 30, 30, 1, 1),
(94, 5, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'PM', 30, 30, 1, 1),
(95, 5, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'AM', 30, 30, 1, 1),
(96, 5, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'PM', 30, 30, 1, 1),
(97, 5, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'AM', 30, 30, 1, 1),
(98, 5, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'PM', 30, 30, 1, 1),
(99, 5, CURDATE(), 'AM', 30, 30, 1, 0),
(100, 5, CURDATE(), 'PM', 30, 30, 1, 0),
(101, 5, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'AM', 30, 30, 1, 0),
(102, 5, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PM', 30, 30, 1, 0),
(103, 5, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'AM', 30, 30, 1, 0),
(104, 5, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'PM', 30, 30, 1, 0),
(105, 5, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'AM', 30, 30, 1, 0),
(106, 5, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'PM', 30, 30, 1, 0),
(107, 5, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'AM', 30, 30, 1, 0),
(108, 5, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'PM', 30, 30, 1, 0),
(109, 5, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'AM', 30, 30, 1, 0),
(110, 5, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'PM', 30, 30, 1, 0),
(111, 5, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'AM', 30, 30, 1, 0),
(112, 5, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'PM', 30, 30, 1, 0),
(113, 6, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'AM', 20, 20, 1, 1),
(114, 6, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'PM', 20, 20, 1, 1),
(115, 6, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'AM', 20, 20, 1, 1),
(116, 6, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'PM', 20, 20, 1, 1),
(117, 6, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'AM', 20, 20, 1, 1),
(118, 6, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'PM', 20, 20, 1, 1),
(119, 6, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'AM', 20, 20, 1, 1),
(120, 6, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'PM', 20, 20, 1, 1),
(121, 6, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'AM', 20, 20, 1, 1),
(122, 6, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'PM', 20, 20, 1, 1),
(123, 6, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'AM', 20, 20, 1, 1),
(124, 6, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'PM', 20, 20, 1, 1),
(125, 6, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'AM', 20, 20, 1, 1),
(126, 6, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'PM', 20, 20, 1, 1),
(127, 6, CURDATE(), 'AM', 20, 20, 1, 0),
(128, 6, CURDATE(), 'PM', 20, 20, 1, 0),
(129, 6, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'AM', 20, 20, 1, 0),
(130, 6, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PM', 20, 20, 1, 0),
(131, 6, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'AM', 20, 20, 1, 0),
(132, 6, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'PM', 20, 20, 1, 0),
(133, 6, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'AM', 20, 20, 1, 0),
(134, 6, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'PM', 20, 20, 1, 0),
(135, 6, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'AM', 20, 20, 1, 0),
(136, 6, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'PM', 20, 20, 1, 0),
(137, 6, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'AM', 20, 20, 1, 0),
(138, 6, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'PM', 20, 20, 1, 0),
(139, 6, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'AM', 20, 20, 1, 0),
(140, 6, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'PM', 20, 20, 1, 0),
(141, 7, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'AM', 20, 20, 1, 1),
(142, 7, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'PM', 20, 20, 1, 1),
(143, 7, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'AM', 20, 20, 1, 1),
(144, 7, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'PM', 20, 20, 1, 1),
(145, 7, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'AM', 20, 20, 1, 1),
(146, 7, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'PM', 20, 20, 1, 1),
(147, 7, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'AM', 20, 20, 1, 1),
(148, 7, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'PM', 20, 20, 1, 1),
(149, 7, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'AM', 20, 20, 1, 1),
(150, 7, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'PM', 20, 20, 1, 1),
(151, 7, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'AM', 20, 20, 1, 1),
(152, 7, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'PM', 20, 20, 1, 1),
(153, 7, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'AM', 20, 20, 1, 1),
(154, 7, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'PM', 20, 20, 1, 1),
(155, 7, CURDATE(), 'AM', 20, 20, 1, 0),
(156, 7, CURDATE(), 'PM', 20, 20, 1, 0),
(157, 7, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'AM', 20, 20, 1, 0),
(158, 7, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PM', 20, 20, 1, 0),
(159, 7, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'AM', 20, 20, 1, 0),
(160, 7, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'PM', 20, 20, 1, 0),
(161, 7, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'AM', 20, 20, 1, 0),
(162, 7, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'PM', 20, 20, 1, 0),
(163, 7, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'AM', 20, 20, 1, 0),
(164, 7, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'PM', 20, 20, 1, 0),
(165, 7, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'AM', 20, 20, 1, 0),
(166, 7, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'PM', 20, 20, 1, 0),
(167, 7, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'AM', 20, 20, 1, 0),
(168, 7, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'PM', 20, 20, 1, 0),
(169, 8, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'AM', 30, 30, 1, 1),
(170, 8, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'PM', 30, 30, 1, 1),
(171, 8, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'AM', 30, 30, 1, 1),
(172, 8, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'PM', 30, 30, 1, 1),
(173, 8, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'AM', 30, 30, 1, 1),
(174, 8, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'PM', 30, 30, 1, 1),
(175, 8, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'AM', 30, 30, 1, 1),
(176, 8, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'PM', 30, 30, 1, 1),
(177, 8, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'AM', 30, 30, 1, 1),
(178, 8, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'PM', 30, 30, 1, 1),
(179, 8, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'AM', 30, 30, 1, 1),
(180, 8, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'PM', 30, 30, 1, 1),
(181, 8, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'AM', 30, 30, 1, 1),
(182, 8, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'PM', 30, 30, 1, 1),
(183, 8, CURDATE(), 'AM', 30, 30, 1, 0),
(184, 8, CURDATE(), 'PM', 30, 30, 1, 0),
(185, 8, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'AM', 30, 30, 1, 0),
(186, 8, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PM', 30, 30, 1, 0),
(187, 8, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'AM', 30, 30, 1, 0),
(188, 8, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'PM', 30, 30, 1, 0),
(189, 8, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'AM', 30, 30, 1, 0),
(190, 8, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'PM', 30, 30, 1, 0),
(191, 8, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'AM', 30, 30, 1, 0),
(192, 8, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'PM', 30, 30, 1, 0),
(193, 8, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'AM', 30, 30, 1, 0),
(194, 8, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'PM', 30, 30, 1, 0),
(195, 8, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'AM', 30, 30, 1, 0),
(196, 8, DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'PM', 30, 30, 1, 0),
(197, 1, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'AM', 30, 30, 1, 1),
(198, 1, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'PM', 30, 30, 1, 1),
(199, 1, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'AM', 30, 30, 1, 1),
(200, 1, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'PM', 30, 30, 1, 1),
(201, 1, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'AM', 30, 30, 1, 1),
(202, 1, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'PM', 30, 30, 1, 1),
(203, 1, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'AM', 30, 30, 1, 1),
(204, 1, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'PM', 30, 30, 1, 1),
(205, 1, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'AM', 30, 30, 1, 1),
(206, 1, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'PM', 30, 30, 1, 1),
(207, 1, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'AM', 30, 30, 1, 1),
(208, 1, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'PM', 30, 30, 1, 1),
(209, 1, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'AM', 30, 30, 1, 1),
(210, 1, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'PM', 30, 30, 1, 1),
(211, 2, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'AM', 30, 30, 1, 1),
(212, 2, DATE_ADD(CURDATE(), INTERVAL -7 DAY), 'PM', 30, 30, 1, 1),
(213, 2, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'AM', 30, 30, 1, 1),
(214, 2, DATE_ADD(CURDATE(), INTERVAL -6 DAY), 'PM', 30, 30, 1, 1),
(215, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'AM', 30, 30, 1, 1),
(216, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), 'PM', 30, 30, 1, 1),
(217, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'AM', 30, 30, 1, 1),
(218, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), 'PM', 30, 30, 1, 1),
(219, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'AM', 30, 30, 1, 1),
(220, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), 'PM', 30, 30, 1, 1),
(221, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'AM', 30, 30, 1, 1),
(222, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), 'PM', 30, 30, 1, 1),
(223, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'AM', 30, 30, 1, 1),
(224, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), 'PM', 30, 30, 1, 1);

-- ---- 5. 新增挂号（近 7 天 + 今日 + 未来预约）----
INSERT INTO `registration` (`id`, `patient_id`, `schedule_id`, `queue_num`, `status`, `registration_date`, `create_time`, `update_time`) VALUES
(6, 4, 143, 1, 2, DATE_ADD(CURDATE(), INTERVAL -6 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00')),
(7, 1, 143, 2, 2, DATE_ADD(CURDATE(), INTERVAL -6 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00')),
(8, 7, 199, 1, 2, DATE_ADD(CURDATE(), INTERVAL -6 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00')),
(9, 2, 199, 2, 2, DATE_ADD(CURDATE(), INTERVAL -6 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00')),
(10, 5, 213, 1, 2, DATE_ADD(CURDATE(), INTERVAL -6 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00')),
(11, 12, 31, 1, 2, DATE_ADD(CURDATE(), INTERVAL -6 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00')),
(12, 11, 31, 2, 2, DATE_ADD(CURDATE(), INTERVAL -6 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00')),
(13, 6, 59, 1, 2, DATE_ADD(CURDATE(), INTERVAL -6 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '08:30:00')),
(14, 10, 88, 1, 2, DATE_ADD(CURDATE(), INTERVAL -6 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '13:30:00')),
(15, 15, 145, 1, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00')),
(16, 13, 145, 2, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00')),
(17, 18, 145, 3, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00')),
(18, 9, 201, 1, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00')),
(19, 16, 215, 1, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00')),
(20, 21, 216, 1, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '13:30:00')),
(21, 14, 33, 1, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00')),
(22, 22, 62, 1, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '13:30:00')),
(23, 3, 62, 2, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '13:30:00')),
(24, 17, 173, 1, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00')),
(25, 28, 173, 2, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '08:30:00')),
(26, 19, 90, 1, 2, DATE_ADD(CURDATE(), INTERVAL -5 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '13:30:00')),
(27, 8, 147, 1, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00')),
(28, 25, 147, 2, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00')),
(29, 1, 147, 3, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00')),
(30, 2, 148, 1, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00')),
(31, 24, 204, 1, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00')),
(32, 29, 204, 2, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00')),
(33, 27, 217, 1, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00')),
(34, 20, 36, 1, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00')),
(35, 4, 36, 2, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00')),
(36, 3, 63, 1, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00')),
(37, 23, 63, 2, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00')),
(38, 6, 91, 1, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00')),
(39, 30, 91, 2, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '08:30:00')),
(40, 10, 120, 1, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00')),
(41, 11, 120, 2, 2, DATE_ADD(CURDATE(), INTERVAL -4 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '13:30:00')),
(42, 9, 150, 1, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '13:30:00')),
(43, 8, 150, 2, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '13:30:00')),
(44, 12, 205, 1, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00')),
(45, 5, 219, 1, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00')),
(46, 16, 220, 1, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '13:30:00')),
(47, 18, 177, 1, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00')),
(48, 17, 177, 2, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00')),
(49, 14, 37, 1, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00')),
(50, 20, 37, 2, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '08:30:00')),
(51, 15, 66, 1, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '13:30:00')),
(52, 26, 66, 2, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '13:30:00')),
(53, 22, 151, 1, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00')),
(54, 23, 151, 2, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00')),
(55, 7, 151, 3, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00')),
(56, 25, 207, 1, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00')),
(57, 29, 207, 2, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00')),
(58, 21, 221, 1, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00')),
(59, 1, 40, 1, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '13:30:00')),
(60, 26, 40, 2, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '13:30:00')),
(61, 2, 95, 1, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00')),
(62, 13, 95, 2, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00')),
(63, 3, 123, 1, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00')),
(64, 30, 123, 2, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '08:30:00')),
(65, 4, 180, 1, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '13:30:00')),
(66, 8, 180, 2, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '13:30:00')),
(67, 6, 153, 1, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(68, 11, 153, 2, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(69, 28, 153, 3, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(70, 19, 154, 1, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '13:30:00')),
(71, 7, 154, 2, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '13:30:00')),
(72, 10, 209, 1, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(73, 13, 209, 2, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(74, 27, 223, 1, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(75, 5, 224, 1, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '13:30:00')),
(76, 9, 41, 1, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(77, 15, 41, 2, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(78, 18, 69, 1, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(79, 12, 69, 2, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(80, 14, 69, 3, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(81, 22, 98, 1, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '13:30:00')),
(82, 17, 98, 2, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '13:30:00')),
(83, 20, 181, 1, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(84, 30, 181, 2, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:30:00')),
(85, 28, 1, 4, 1, CURDATE(), TIMESTAMP(CURDATE(), '08:30:00'), TIMESTAMP(CURDATE(), '08:30:00')),
(86, 23, 1, 5, 1, CURDATE(), TIMESTAMP(CURDATE(), '08:30:00'), TIMESTAMP(CURDATE(), '08:30:00')),
(87, 2, 2, 3, 1, CURDATE(), TIMESTAMP(CURDATE(), '13:30:00'), TIMESTAMP(CURDATE(), '13:30:00')),
(88, 29, 2, 4, 1, CURDATE(), TIMESTAMP(CURDATE(), '13:30:00'), TIMESTAMP(CURDATE(), '13:30:00')),
(89, 16, 16, 1, 1, CURDATE(), TIMESTAMP(CURDATE(), '13:30:00'), TIMESTAMP(CURDATE(), '13:30:00')),
(90, 26, 43, 1, 2, CURDATE(), TIMESTAMP(CURDATE(), '08:30:00'), TIMESTAMP(CURDATE(), '08:30:00')),
(91, 25, 43, 2, 2, CURDATE(), TIMESTAMP(CURDATE(), '08:30:00'), TIMESTAMP(CURDATE(), '08:30:00')),
(92, 19, 43, 3, 1, CURDATE(), TIMESTAMP(CURDATE(), '08:30:00'), TIMESTAMP(CURDATE(), '08:30:00')),
(93, 24, 44, 1, 1, CURDATE(), TIMESTAMP(CURDATE(), '13:30:00'), TIMESTAMP(CURDATE(), '13:30:00')),
(94, 3, 44, 2, 1, CURDATE(), TIMESTAMP(CURDATE(), '13:30:00'), TIMESTAMP(CURDATE(), '13:30:00')),
(95, 4, 71, 1, 1, CURDATE(), TIMESTAMP(CURDATE(), '08:30:00'), TIMESTAMP(CURDATE(), '08:30:00')),
(96, 1, 71, 2, 2, CURDATE(), TIMESTAMP(CURDATE(), '08:30:00'), TIMESTAMP(CURDATE(), '08:30:00')),
(97, 10, 72, 1, 3, CURDATE(), TIMESTAMP(CURDATE(), '13:30:00'), TIMESTAMP(CURDATE(), '13:30:00')),
(98, 7, 100, 1, 1, CURDATE(), TIMESTAMP(CURDATE(), '13:30:00'), TIMESTAMP(CURDATE(), '13:30:00')),
(99, 9, 100, 2, 1, CURDATE(), TIMESTAMP(CURDATE(), '13:30:00'), TIMESTAMP(CURDATE(), '13:30:00')),
(100, 6, 127, 1, 2, CURDATE(), TIMESTAMP(CURDATE(), '08:30:00'), TIMESTAMP(CURDATE(), '08:30:00')),
(101, 12, 127, 2, 1, CURDATE(), TIMESTAMP(CURDATE(), '08:30:00'), TIMESTAMP(CURDATE(), '08:30:00')),
(102, 13, 155, 1, 1, CURDATE(), TIMESTAMP(CURDATE(), '08:30:00'), TIMESTAMP(CURDATE(), '08:30:00')),
(103, 11, 155, 2, 1, CURDATE(), TIMESTAMP(CURDATE(), '08:30:00'), TIMESTAMP(CURDATE(), '08:30:00')),
(104, 15, 184, 1, 1, CURDATE(), TIMESTAMP(CURDATE(), '13:30:00'), TIMESTAMP(CURDATE(), '13:30:00')),
(105, 17, 184, 2, 1, CURDATE(), TIMESTAMP(CURDATE(), '13:30:00'), TIMESTAMP(CURDATE(), '13:30:00')),
(106, 14, 45, 1, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:30:00')),
(107, 8, 185, 1, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:30:00')),
(108, 18, 75, 1, 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '08:30:00')),
(109, 20, 104, 1, 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '13:30:00')),
(110, 22, 161, 1, 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '08:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '08:30:00')),
(111, 30, 134, 1, 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '13:30:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '13:30:00'));

-- ---- 6. 新增就诊记录 ----
INSERT INTO `consultation` (`id`, `registration_id`, `doctor_id`, `complaint`, `diagnosis`, `advice`, `status`, `start_time`, `end_time`) VALUES
(4, 6, 7, '体检发现血脂偏高，无明显不适', '高脂血症', '低脂饮食，加强锻炼，3个月后复查血脂', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:45:00')),
(5, 7, 7, '体检发现血脂偏高，无明显不适', '高脂血症', '低脂饮食，加强锻炼，3个月后复查血脂', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:45:00')),
(6, 8, 1, '体检发现血脂偏高，无明显不适', '高脂血症', '低脂饮食，加强锻炼，3个月后复查血脂', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:45:00')),
(7, 9, 1, '上腹隐痛反酸2周，进食后加重', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:45:00')),
(8, 10, 2, '发热2天，体温最高38.9℃，伴咽痛', '急性上呼吸道感染', '多饮水，物理降温，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:45:00')),
(9, 11, 3, '颈后皮下肿块3天，红肿疼痛', '体表脓肿', '切开引流后按时换药，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:45:00')),
(10, 12, 3, '右前臂肿胀疼痛2天，有外伤史', '前臂软组织挫伤', '患肢制动，48小时内冷敷，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:45:00')),
(11, 13, 4, '颈部皮疹1周，出汗后加重', '痱子', '保持皮肤清洁干燥，穿透气衣物', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '09:45:00')),
(12, 14, 5, '乏力倦怠2周，面色少华', '气血两虚', '加强营养，避免劳累，1个月后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '14:40:00')),
(13, 15, 7, '胸闷心悸1周，活动后明显', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:45:00')),
(14, 16, 7, '咽痛咳嗽3天，伴低热', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:45:00')),
(15, 17, 7, '胸闷心悸1周，活动后明显', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:45:00')),
(16, 18, 1, '咽痛咳嗽3天，伴低热', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:45:00')),
(17, 19, 2, '咳嗽1周，夜间加重，有痰', '急性支气管炎', '多饮水，避免受凉，5天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:45:00')),
(18, 20, 2, '发热1天，咽部可见散在疱疹', '疱疹性咽峡炎', '清淡流质饮食，注意口腔清洁，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '14:40:00')),
(19, 21, 3, '右前臂肿胀疼痛2天，有外伤史', '前臂软组织挫伤', '患肢制动，48小时内冷敷，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:45:00')),
(20, 22, 4, '双手皮疹伴瘙痒1月，反复发作', '慢性湿疹', '避免接触洗涤剂，注意保湿，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '14:40:00')),
(21, 23, 4, '全身风团瘙痒1天，时起时消', '荨麻疹', '忌食海鲜，避免受热，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '14:40:00')),
(22, 24, 8, '外阴瘙痒伴白带增多5天', '阴道炎', '保持外阴清洁，勤换内裤，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:45:00')),
(23, 25, 8, '近2月月经量明显增多', '月经过多', '注意休息，加强营养，1个月后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '09:45:00')),
(24, 26, 5, '畏寒鼻塞2天，无汗', '风寒感冒', '多饮温水，注意保暖，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '14:40:00')),
(25, 27, 7, '咽痛咳嗽3天，伴低热', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:45:00')),
(26, 28, 7, '上腹隐痛反酸2周，进食后加重', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:45:00')),
(27, 29, 7, '体检发现血脂偏高，无明显不适', '高脂血症', '低脂饮食，加强锻炼，3个月后复查血脂', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:45:00')),
(28, 30, 7, '上腹隐痛反酸2周，进食后加重', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:40:00')),
(29, 31, 1, '咽痛咳嗽3天，伴低热', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:40:00')),
(30, 32, 1, '上腹隐痛反酸2周，进食后加重', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:40:00')),
(31, 33, 2, '咽部不适2天，无发热', '急性咽炎', '多饮水，清淡饮食，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:45:00')),
(32, 34, 3, '右前臂肿胀疼痛2天，有外伤史', '前臂软组织挫伤', '患肢制动，48小时内冷敷，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:40:00')),
(33, 35, 3, '左足背皮肤红肿2天，行走时疼痛', '足背软组织感染', '抬高患肢，保持局部清洁，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:40:00')),
(34, 36, 4, '双手皮疹伴瘙痒1月，反复发作', '慢性湿疹', '避免接触洗涤剂，注意保湿，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:45:00')),
(35, 37, 4, '面部红斑瘙痒2天，近期更换化妆品', '接触性皮炎', '停用可疑化妆品，避免搔抓，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:45:00')),
(36, 38, 5, '畏寒鼻塞2天，无汗', '风寒感冒', '多饮温水，注意保暖，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:45:00')),
(37, 39, 5, '食欲不振、腹胀便溏1月', '脾胃虚弱', '饮食清淡易消化，忌生冷，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '09:45:00')),
(38, 40, 6, '鼻塞头痛3天，涕多色黄', '急性鼻窦炎', '多饮水，生理盐水冲洗鼻腔，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:40:00')),
(39, 41, 6, '鼻塞头痛3天，涕多色黄', '急性鼻窦炎', '多饮水，生理盐水冲洗鼻腔，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -4 DAY), '14:40:00')),
(40, 42, 7, '胸闷心悸1周，活动后明显', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:40:00')),
(41, 43, 7, '咽痛咳嗽3天，伴低热', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:40:00')),
(42, 44, 1, '胸闷心悸1周，活动后明显', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:45:00')),
(43, 45, 2, '发热2天，体温最高38.9℃，伴咽痛', '急性上呼吸道感染', '多饮水，物理降温，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:45:00')),
(44, 46, 2, '咳嗽1周，夜间加重，有痰', '急性支气管炎', '多饮水，避免受凉，5天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:40:00')),
(45, 47, 8, '外阴瘙痒伴白带增多5天', '阴道炎', '保持外阴清洁，勤换内裤，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:45:00')),
(46, 48, 8, '外阴瘙痒伴白带增多5天', '阴道炎', '保持外阴清洁，勤换内裤，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:45:00')),
(47, 49, 3, '右下腹疼痛12小时，麦氏点压痛', '急性阑尾炎', '建议立即转上级医院手术治疗', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:45:00')),
(48, 50, 3, '右手掌皮肤裂伤2小时', '手部软组织裂伤', '保持伤口干燥，3天后换药，1周后拆线', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '09:45:00')),
(49, 51, 4, '面部红斑瘙痒2天，近期更换化妆品', '接触性皮炎', '停用可疑化妆品，避免搔抓，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:40:00')),
(50, 52, 4, '双手皮疹伴瘙痒1月，反复发作', '慢性湿疹', '避免接触洗涤剂，注意保湿，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:40:00')),
(51, 53, 7, '上腹隐痛反酸2周，进食后加重', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:45:00')),
(52, 54, 7, '胸闷心悸1周，活动后明显', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:45:00')),
(53, 55, 7, '上腹隐痛反酸2周，进食后加重', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:45:00')),
(54, 56, 1, '咽痛咳嗽3天，伴低热', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:45:00')),
(55, 57, 1, '多饮多尿3月，空腹血糖8.6mmol/L', '2型糖尿病', '控制饮食，适量运动，监测血糖，1个月后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:45:00')),
(56, 58, 2, '发热1天，咽部可见散在疱疹', '疱疹性咽峡炎', '清淡流质饮食，注意口腔清洁，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:45:00')),
(57, 59, 3, '右前臂肿胀疼痛2天，有外伤史', '前臂软组织挫伤', '患肢制动，48小时内冷敷，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '14:40:00')),
(58, 60, 3, '右下腹疼痛12小时，麦氏点压痛', '急性阑尾炎', '建议立即转上级医院手术治疗', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '14:40:00')),
(59, 61, 5, '食欲不振、腹胀便溏1月', '脾胃虚弱', '饮食清淡易消化，忌生冷，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:45:00')),
(60, 62, 5, '乏力倦怠2周，面色少华', '气血两虚', '加强营养，避免劳累，1个月后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:45:00')),
(61, 63, 6, '咽痛2天，吞咽时加重', '急性咽炎', '多饮水，忌辛辣，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:45:00')),
(62, 64, 6, '鼻塞头痛3天，涕多色黄', '急性鼻窦炎', '多饮水，生理盐水冲洗鼻腔，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:45:00')),
(63, 65, 8, '外阴瘙痒伴白带增多5天', '阴道炎', '保持外阴清洁，勤换内裤，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '14:40:00')),
(64, 66, 8, '外阴瘙痒伴白带增多5天', '阴道炎', '保持外阴清洁，勤换内裤，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '14:40:00')),
(65, 67, 7, '咽痛咳嗽3天，伴低热', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(66, 68, 7, '上腹隐痛反酸2周，进食后加重', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(67, 69, 7, '胸闷心悸1周，活动后明显', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(68, 70, 7, '咽痛咳嗽3天，伴低热', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '14:40:00')),
(69, 71, 7, '多饮多尿3月，空腹血糖8.6mmol/L', '2型糖尿病', '控制饮食，适量运动，监测血糖，1个月后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '14:40:00')),
(70, 72, 1, '咽痛咳嗽3天，伴低热', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(71, 73, 1, '咽痛咳嗽3天，伴低热', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(72, 74, 2, '咳嗽1周，夜间加重，有痰', '急性支气管炎', '多饮水，避免受凉，5天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(73, 75, 2, '发热2天，体温最高38.9℃，伴咽痛', '急性上呼吸道感染', '多饮水，物理降温，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '14:40:00')),
(74, 76, 3, '腰部酸痛1周，久坐后加重', '腰肌劳损', '避免久坐，局部热敷，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(75, 77, 3, '左足背皮肤红肿2天，行走时疼痛', '足背软组织感染', '抬高患肢，保持局部清洁，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(76, 78, 4, '双足趾间脱屑瘙痒2周', '足癣', '保持足部干燥，勤换鞋袜，1个月后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(77, 79, 4, '全身风团瘙痒1天，时起时消', '荨麻疹', '忌食海鲜，避免受热，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(78, 80, 4, '面部红斑瘙痒2天，近期更换化妆品', '接触性皮炎', '停用可疑化妆品，避免搔抓，1周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(79, 81, 5, '畏寒鼻塞2天，无汗', '风寒感冒', '多饮温水，注意保暖，3天后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '14:40:00')),
(80, 82, 5, '入睡困难1月余，多梦易醒', '不寐（心脾两虚）', '规律作息，睡前避免饮浓茶，2周后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '14:40:00')),
(81, 83, 8, '近2月月经量明显增多', '月经过多', '注意休息，加强营养，1个月后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(82, 84, 8, '经期下腹疼痛1年，近2月加重', '痛经', '经期注意保暖，避免剧烈运动，1个月后复诊', 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:45:00')),
(83, 90, 3, '腰部酸痛1周，久坐后加重', '腰肌劳损', '避免久坐，局部热敷，2周后复诊', 2, TIMESTAMP(CURDATE(), '09:00:00'), TIMESTAMP(CURDATE(), '09:45:00')),
(84, 91, 3, '腰部酸痛1周，久坐后加重', '腰肌劳损', '避免久坐，局部热敷，2周后复诊', 2, TIMESTAMP(CURDATE(), '09:00:00'), TIMESTAMP(CURDATE(), '09:45:00')),
(85, 96, 4, '面部红斑瘙痒2天，近期更换化妆品', '接触性皮炎', '停用可疑化妆品，避免搔抓，1周后复诊', 2, TIMESTAMP(CURDATE(), '09:00:00'), TIMESTAMP(CURDATE(), '09:45:00')),
(86, 100, 6, '鼻塞头痛3天，涕多色黄', '急性鼻窦炎', '多饮水，生理盐水冲洗鼻腔，1周后复诊', 2, TIMESTAMP(CURDATE(), '09:00:00'), TIMESTAMP(CURDATE(), '09:45:00'));

-- ---- 7. 新增电子病历（content 为 AES-256-CBC 密文）----
INSERT INTO `medical_record` (`id`, `patient_id`, `consultation_id`, `content`, `diagnosis`, `treatment_plan`, `doctor_id`) VALUES
(4, 4, 4, '9+Y3EyKyO3AAHW2anaCfZ7081QqoNr7QXEayPa3sZol7b5un01B6finDIVZUoN+nBHe4Ete4vrx7eny9RJXGO3aoZPUQ8KCPLyOzDqVMhUU4jtiUJnQWdLRpKBcKzTADrOvYzmOS4m/dEZ1jvuGoI1zvXe3BKz1tl1QLcxn2kpXZFTkuYrfp+StZotOFmvfB', '高脂血症', '低脂饮食，加强锻炼，3个月后复查血脂', 7),
(5, 1, 5, '9+Y3EyKyO3AAHW2anaCfZ7081QqoNr7QXEayPa3sZol7b5un01B6finDIVZUoN+nBHe4Ete4vrx7eny9RJXGO3aoZPUQ8KCPLyOzDqVMhUU4jtiUJnQWdLRpKBcKzTADrOvYzmOS4m/dEZ1jvuGoI1zvXe3BKz1tl1QLcxn2kpXZFTkuYrfp+StZotOFmvfB', '高脂血症', '低脂饮食，加强锻炼，3个月后复查血脂', 7),
(6, 7, 6, '9+Y3EyKyO3AAHW2anaCfZ7081QqoNr7QXEayPa3sZol7b5un01B6finDIVZUoN+nBHe4Ete4vrx7eny9RJXGO3aoZPUQ8KCPLyOzDqVMhUU4jtiUJnQWdLRpKBcKzTADrOvYzmOS4m/dEZ1jvuGoI1zvXe3BKz1tl1QLcxn2kpXZFTkuYrfp+StZotOFmvfB', '高脂血症', '低脂饮食，加强锻炼，3个月后复查血脂', 1),
(7, 2, 7, 'dAuLSlcBEueK/QUwen91LOslXbABS7iVodrWYhRld9pl1eB8t9P5zqUzFbONNKYpdKKWfC748KUPMXimjDn5ftMMq59djYON8mXiMNHzZa4Y+vThQ+f0LH7BYtqeX7Q2khwVqQH1sacLle4Y+rnbeyil2vlqDvaSjpkbbORJSgw=', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 1),
(8, 5, 8, 'Nr7lb64Ewfq6hoc0LVRGusuVI4iiYt6kMm03yQlx6gopZZcNGBf4zDlLpC8JO1k2gG/UQE56lAL3AmmfxUHZJ1IJUjyJSFRty+kGECwrgpRee9GdbtC6rZqkkCIN5VnVTOXwNasl7YYeeA+13C9hQMH6avaT1iuu5Z5lHdh35yF2P1Ly4J7UVmFQGJ6t0OdE', '急性上呼吸道感染', '多饮水，物理降温，3天后复诊', 2),
(9, 12, 9, 'D6b4A4looHbPNAdSpZzBbVnfmgmo+cpIq0a+n2KXHqeOACoXweQsPkdXiRW4A+cdr/yHAy/QiEd1qTNe5o9TGXWJvCMPQjkpM1hviT1/rhO/rAc/aoD0g4gIZh22JUfgdLWtDrMVDSlwsS7dnVIC3j8cJDp2HTy5bawjQ4tIChQ=', '体表脓肿', '切开引流后按时换药，3天后复诊', 3),
(10, 11, 10, 'PLdbasFj0g5eaWZu9WHwlxiRFS+cpcTEfez7rkUY4lnzNeFGihrfDR3U0GffRTLQeEHaAVF5MX79DJpORKsXYV8BIYROqLsrkAq5ISbj7Fyg9QbvZeJQhGM2buts9X8JQ+S4eUV+TuX+19nNSWfDD/FvW0bHy4AqkfWfdW+cgCZsDadlJO5X64sWU6GUyFwU', '前臂软组织挫伤', '患肢制动，48小时内冷敷，2周后复诊', 3),
(11, 6, 11, '4bOayDGjsXuy+mPpcekiSgKEiGfLxvNXEblv5ENZBNwUOtg6ypwDRVyvYNk1tPn7nSGA9RJ3QTEPPBnYo6Oo/5N7MmIYRurID687CwAg8xgHIGBt7HBzgNtm2QX19eHQZ3CAPmYxT8Si7PqF0B4ICQ==', '痱子', '保持皮肤清洁干燥，穿透气衣物', 4),
(12, 10, 12, 'yK89pKF9PI1dHW4KnsqoeNzEJtjQW0+EQ1LFSdZzAPDqFBZYHJsZ2xhHPm18JvBSf3rd3/3+XeH2JuAj1xVPJI18tm2zJ1cKlayQtydvcK54LxMuOZ0o7UilmWBwVzU5Emgz39gzUtpxsyJIR/oHqiJqJQzNpXxZgtAgWeSqogU=', '气血两虚', '加强营养，避免劳累，1个月后复诊', 5),
(13, 15, 13, '4WYovjSuuffN30yn5OwSo0+a3YucoTbba111oXkBbgWPlqIPaov/zHYuVNoHx5BTtWxkIJhl2W7eFyyUee1MgWIjOjTxJNEXaN2xTpQLyaPrmPbPnWVIOSPs1Day/YwMKM0oulvTr/McPiYpQH3sB666it5zDoPxAmFD7aZUu98EzRGppCNlACI/gam/dCSPt0j1zGqU2T5pjUABij6OhA==', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 7),
(14, 13, 14, 'barIpiHas/4g4ICrubE435tJvuCF+Tuxm5sHXMbj6t9SIdxSKWDl+LKbevzu9hFG+e7Y2MdRzI1BL92uFMy2nJBgIfT3cLmW690P3tJuA73b+uDRbERFIKp5vf1sW+/voAQ/sEg7LeUqKhQ1+4SZWaRzTJz1Ubz97+0+kYkBq5s=', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 7),
(15, 18, 15, '4WYovjSuuffN30yn5OwSo0+a3YucoTbba111oXkBbgWPlqIPaov/zHYuVNoHx5BTtWxkIJhl2W7eFyyUee1MgWIjOjTxJNEXaN2xTpQLyaPrmPbPnWVIOSPs1Day/YwMKM0oulvTr/McPiYpQH3sB666it5zDoPxAmFD7aZUu98EzRGppCNlACI/gam/dCSPt0j1zGqU2T5pjUABij6OhA==', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 7),
(16, 9, 16, 'barIpiHas/4g4ICrubE435tJvuCF+Tuxm5sHXMbj6t9SIdxSKWDl+LKbevzu9hFG+e7Y2MdRzI1BL92uFMy2nJBgIfT3cLmW690P3tJuA73b+uDRbERFIKp5vf1sW+/voAQ/sEg7LeUqKhQ1+4SZWaRzTJz1Ubz97+0+kYkBq5s=', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 1),
(17, 16, 17, 'nfkRPIKLOfEjzHQ+KVovGoWrd8heulsCOOn8WNXIq4fsCW7da6+413pQfPfJLmB+69lLhkGvH5dW3ru1Tu3a42VT4MiTuulIgrL8Xna7CfesbBOVR8sb/QNEWtbIanJDN2+zXngGocwXiv8MhxeAgVThUErDJkxdzV0O2YdB/c8=', '急性支气管炎', '多饮水，避免受凉，5天后复诊', 2),
(18, 21, 18, 'Fz6sHwEr7vaOQ8su3p3WTxB1QdjUShzPOgISlYjara7/eJRYLZ7UsOweE/gm+tAsrd57b3vwXNUxpDyudqFe9pwsT9DHceHbWCgSU00447PbrY4oAcmXa0amkeSkFdCxxy4lrGGo4JBQZ9anBkvGLtJ+tY1mFMczvVwj78xai9CkPffQRXeQDaCHASm4X18m', '疱疹性咽峡炎', '清淡流质饮食，注意口腔清洁，3天后复诊', 2),
(19, 14, 19, 'PLdbasFj0g5eaWZu9WHwlxiRFS+cpcTEfez7rkUY4lnzNeFGihrfDR3U0GffRTLQeEHaAVF5MX79DJpORKsXYV8BIYROqLsrkAq5ISbj7Fyg9QbvZeJQhGM2buts9X8JQ+S4eUV+TuX+19nNSWfDD/FvW0bHy4AqkfWfdW+cgCZsDadlJO5X64sWU6GUyFwU', '前臂软组织挫伤', '患肢制动，48小时内冷敷，2周后复诊', 3),
(20, 22, 20, 'YS9PbY65PiRPdEBpecfFXyIHeBf8X1zIk2VYFJ6Kux5JSW+zYmKa7kxgF9GxcKRxV1Tw+8oXcYGoHL90yhH2W3MYTuu2lJjPISmQ+Tpt7PZBeizSXHV5w88cg0jK8Msq42cZkIucuwZ8VVXN/SNhVnSxjXGk0/m3schwm1tswQzru1Dn0JBPP4fSPgnSx9mv', '慢性湿疹', '避免接触洗涤剂，注意保湿，2周后复诊', 4),
(21, 3, 21, 'SCiIX5hMsmLYEQ0jWlPVhRTfqPxI/moMb3ajTPht4tqfEsJC4UE77D2o81Q9yaAN3Vhl/RGWZHx++RQMdhquX4MwP20YdaiygJIfLhuu58bJyYPoBnHnTZqejFSDRh/EWoTYfPZTOFUkpWkARSXMVZEdxlMRs734mszwr4+R+bQ=', '荨麻疹', '忌食海鲜，避免受热，1周后复诊', 4),
(22, 17, 22, 'BsEz1J5J4e6lC14qZxopLz+SPn20gBzPa8pDJX7M9KGh/vD0SjWvq1kTRAgElDyLA2NYRTQ8ANDMSGlpYpanPcmbNISds5jmuZCa5Ko5ljAjOFJ9D3p65c2B7/4n6EaZ8p2Bi7/ivRMJJvwc3uG5+8I9HscUHCK+pW1Mjn9ZRrY=', '阴道炎', '保持外阴清洁，勤换内裤，1周后复诊', 8),
(23, 28, 23, 'gD6d6RkBDD7Ba5v2ajzSQNSA+4cQqwYkNgiEgh07tIRbbbycTaCfKGAe2Z56PQWfAFWRuhMrz7vLe1G4kM8tg/752YzqEpjiNtCtfuh4qhET7ZhMmsMJYztSGOvM2S7qRmMJeGvvxz9Fs+oGpuHJlA1iJZWU9OpICeitYOlzf1k=', '月经过多', '注意休息，加强营养，1个月后复诊', 8),
(24, 19, 24, 'VEZ2l+GDKylNfYpcgrm5ag7yAs+BvGojCdbHQmmQrO3dN17KvIs48kKBrU+FsXUbhLI1kiFJnp0fbTqqph6FLinsG5QhI5iBBmxUEjYU1MD/GAEr0WT0SJGBFBf6B5OsaJ3l0KzmRfNaVXjIpoI+6g==', '风寒感冒', '多饮温水，注意保暖，3天后复诊', 5),
(25, 8, 25, 'barIpiHas/4g4ICrubE435tJvuCF+Tuxm5sHXMbj6t9SIdxSKWDl+LKbevzu9hFG+e7Y2MdRzI1BL92uFMy2nJBgIfT3cLmW690P3tJuA73b+uDRbERFIKp5vf1sW+/voAQ/sEg7LeUqKhQ1+4SZWaRzTJz1Ubz97+0+kYkBq5s=', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 7),
(26, 25, 26, 'dAuLSlcBEueK/QUwen91LOslXbABS7iVodrWYhRld9pl1eB8t9P5zqUzFbONNKYpdKKWfC748KUPMXimjDn5ftMMq59djYON8mXiMNHzZa4Y+vThQ+f0LH7BYtqeX7Q2khwVqQH1sacLle4Y+rnbeyil2vlqDvaSjpkbbORJSgw=', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 7),
(27, 1, 27, '9+Y3EyKyO3AAHW2anaCfZ7081QqoNr7QXEayPa3sZol7b5un01B6finDIVZUoN+nBHe4Ete4vrx7eny9RJXGO3aoZPUQ8KCPLyOzDqVMhUU4jtiUJnQWdLRpKBcKzTADrOvYzmOS4m/dEZ1jvuGoI1zvXe3BKz1tl1QLcxn2kpXZFTkuYrfp+StZotOFmvfB', '高脂血症', '低脂饮食，加强锻炼，3个月后复查血脂', 7),
(28, 2, 28, 'dAuLSlcBEueK/QUwen91LOslXbABS7iVodrWYhRld9pl1eB8t9P5zqUzFbONNKYpdKKWfC748KUPMXimjDn5ftMMq59djYON8mXiMNHzZa4Y+vThQ+f0LH7BYtqeX7Q2khwVqQH1sacLle4Y+rnbeyil2vlqDvaSjpkbbORJSgw=', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 7),
(29, 24, 29, 'barIpiHas/4g4ICrubE435tJvuCF+Tuxm5sHXMbj6t9SIdxSKWDl+LKbevzu9hFG+e7Y2MdRzI1BL92uFMy2nJBgIfT3cLmW690P3tJuA73b+uDRbERFIKp5vf1sW+/voAQ/sEg7LeUqKhQ1+4SZWaRzTJz1Ubz97+0+kYkBq5s=', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 1),
(30, 29, 30, 'dAuLSlcBEueK/QUwen91LOslXbABS7iVodrWYhRld9pl1eB8t9P5zqUzFbONNKYpdKKWfC748KUPMXimjDn5ftMMq59djYON8mXiMNHzZa4Y+vThQ+f0LH7BYtqeX7Q2khwVqQH1sacLle4Y+rnbeyil2vlqDvaSjpkbbORJSgw=', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 1),
(31, 27, 31, 'wdekEkCB3XoAcFyAKZCQjASMSeLzMNNi0j/NqozzTdA7mYsLZzoC/EM8Y8yZAGbgSI60TAaxD7Lk0tT6AXSjI9yfU6CpuYyGrlFgnqjBiR8QeslDT/u69Rp+tKhUOVh+9ez0atSBV6APps6pFNX1vw==', '急性咽炎', '多饮水，清淡饮食，3天后复诊', 2),
(32, 20, 32, 'PLdbasFj0g5eaWZu9WHwlxiRFS+cpcTEfez7rkUY4lnzNeFGihrfDR3U0GffRTLQeEHaAVF5MX79DJpORKsXYV8BIYROqLsrkAq5ISbj7Fyg9QbvZeJQhGM2buts9X8JQ+S4eUV+TuX+19nNSWfDD/FvW0bHy4AqkfWfdW+cgCZsDadlJO5X64sWU6GUyFwU', '前臂软组织挫伤', '患肢制动，48小时内冷敷，2周后复诊', 3),
(33, 4, 33, 'a7mBC3wmN+C8gyk8DxJQjytQ5wntaRdoVWxBlIsP7wdchIf5IVxSvmZ+xRUGdU6kwVHVstawjBm2PqTB8BDcqJpfakwx2aKVzwREd0n4C0V03DrMZ/bsq2U91c/l8l9Ba9fAZXXPa1pNlD7rKbC3aj3oMEchG6FasRbgHRnpfb+LD3Z7uVQn68y0lgzYvlAu', '足背软组织感染', '抬高患肢，保持局部清洁，3天后复诊', 3),
(34, 3, 34, 'YS9PbY65PiRPdEBpecfFXyIHeBf8X1zIk2VYFJ6Kux5JSW+zYmKa7kxgF9GxcKRxV1Tw+8oXcYGoHL90yhH2W3MYTuu2lJjPISmQ+Tpt7PZBeizSXHV5w88cg0jK8Msq42cZkIucuwZ8VVXN/SNhVnSxjXGk0/m3schwm1tswQzru1Dn0JBPP4fSPgnSx9mv', '慢性湿疹', '避免接触洗涤剂，注意保湿，2周后复诊', 4),
(35, 23, 35, 'HzPQzHn+KM1yUdTWIIuuqdjCMywTNo6h0hjmL9SNvKKNYSaSJ2Y7KtKKaFj+whg7RsPbEdYhUx44fAk/Ad/xnBrgcO1nENFDh3DaW857Ur44pjGcQy683jmy3gXhyaRNnIPXRhF/5Yz26n6iApzL7Ndi1pOgdONHvq5yRxAXp7zwJOLmZGS66ssb/8dFR9Ay', '接触性皮炎', '停用可疑化妆品，避免搔抓，1周后复诊', 4),
(36, 6, 36, 'VEZ2l+GDKylNfYpcgrm5ag7yAs+BvGojCdbHQmmQrO3dN17KvIs48kKBrU+FsXUbhLI1kiFJnp0fbTqqph6FLinsG5QhI5iBBmxUEjYU1MD/GAEr0WT0SJGBFBf6B5OsaJ3l0KzmRfNaVXjIpoI+6g==', '风寒感冒', '多饮温水，注意保暖，3天后复诊', 5),
(37, 30, 37, 'qN8QImYCf7agUNjKS7ukM//ICJ4LsgjUv5NJpgbTyA54DfV2sYrmEz6SNtDRENmamYgvxrSz7yzuD0bruuNnM61N2z2ebrQeUWz+nX+jm5Lo2Tkq75O4hPEOSIeZtoGp6z8u3UHZOroY5MratU0krr8TGcGPLznBGadji0WIQes=', '脾胃虚弱', '饮食清淡易消化，忌生冷，2周后复诊', 5),
(38, 10, 38, 'q/4WphthcCr+iBQc5jALIQr1HgPG+3x2YBB4wUTJbOPqtaqXyzEe1IgrwvYyW+8aJSlocUkuVjyypYJUxZMSJUl3oaB69MgsgtqGdVxDWKylicRrlL/pboiSFEDT8AcxMxknf0YkTB3LSRcbVVK1YIZ4nFstkg0cRSK+KLWrxuk=', '急性鼻窦炎', '多饮水，生理盐水冲洗鼻腔，1周后复诊', 6),
(39, 11, 39, 'q/4WphthcCr+iBQc5jALIQr1HgPG+3x2YBB4wUTJbOPqtaqXyzEe1IgrwvYyW+8aJSlocUkuVjyypYJUxZMSJUl3oaB69MgsgtqGdVxDWKylicRrlL/pboiSFEDT8AcxMxknf0YkTB3LSRcbVVK1YIZ4nFstkg0cRSK+KLWrxuk=', '急性鼻窦炎', '多饮水，生理盐水冲洗鼻腔，1周后复诊', 6),
(40, 9, 40, '4WYovjSuuffN30yn5OwSo0+a3YucoTbba111oXkBbgWPlqIPaov/zHYuVNoHx5BTtWxkIJhl2W7eFyyUee1MgWIjOjTxJNEXaN2xTpQLyaPrmPbPnWVIOSPs1Day/YwMKM0oulvTr/McPiYpQH3sB666it5zDoPxAmFD7aZUu98EzRGppCNlACI/gam/dCSPt0j1zGqU2T5pjUABij6OhA==', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 7),
(41, 8, 41, 'barIpiHas/4g4ICrubE435tJvuCF+Tuxm5sHXMbj6t9SIdxSKWDl+LKbevzu9hFG+e7Y2MdRzI1BL92uFMy2nJBgIfT3cLmW690P3tJuA73b+uDRbERFIKp5vf1sW+/voAQ/sEg7LeUqKhQ1+4SZWaRzTJz1Ubz97+0+kYkBq5s=', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 7),
(42, 12, 42, '4WYovjSuuffN30yn5OwSo0+a3YucoTbba111oXkBbgWPlqIPaov/zHYuVNoHx5BTtWxkIJhl2W7eFyyUee1MgWIjOjTxJNEXaN2xTpQLyaPrmPbPnWVIOSPs1Day/YwMKM0oulvTr/McPiYpQH3sB666it5zDoPxAmFD7aZUu98EzRGppCNlACI/gam/dCSPt0j1zGqU2T5pjUABij6OhA==', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 1),
(43, 5, 43, 'Nr7lb64Ewfq6hoc0LVRGusuVI4iiYt6kMm03yQlx6gopZZcNGBf4zDlLpC8JO1k2gG/UQE56lAL3AmmfxUHZJ1IJUjyJSFRty+kGECwrgpRee9GdbtC6rZqkkCIN5VnVTOXwNasl7YYeeA+13C9hQMH6avaT1iuu5Z5lHdh35yF2P1Ly4J7UVmFQGJ6t0OdE', '急性上呼吸道感染', '多饮水，物理降温，3天后复诊', 2),
(44, 16, 44, 'nfkRPIKLOfEjzHQ+KVovGoWrd8heulsCOOn8WNXIq4fsCW7da6+413pQfPfJLmB+69lLhkGvH5dW3ru1Tu3a42VT4MiTuulIgrL8Xna7CfesbBOVR8sb/QNEWtbIanJDN2+zXngGocwXiv8MhxeAgVThUErDJkxdzV0O2YdB/c8=', '急性支气管炎', '多饮水，避免受凉，5天后复诊', 2),
(45, 18, 45, 'BsEz1J5J4e6lC14qZxopLz+SPn20gBzPa8pDJX7M9KGh/vD0SjWvq1kTRAgElDyLA2NYRTQ8ANDMSGlpYpanPcmbNISds5jmuZCa5Ko5ljAjOFJ9D3p65c2B7/4n6EaZ8p2Bi7/ivRMJJvwc3uG5+8I9HscUHCK+pW1Mjn9ZRrY=', '阴道炎', '保持外阴清洁，勤换内裤，1周后复诊', 8),
(46, 17, 46, 'BsEz1J5J4e6lC14qZxopLz+SPn20gBzPa8pDJX7M9KGh/vD0SjWvq1kTRAgElDyLA2NYRTQ8ANDMSGlpYpanPcmbNISds5jmuZCa5Ko5ljAjOFJ9D3p65c2B7/4n6EaZ8p2Bi7/ivRMJJvwc3uG5+8I9HscUHCK+pW1Mjn9ZRrY=', '阴道炎', '保持外阴清洁，勤换内裤，1周后复诊', 8),
(47, 14, 47, 'kNR9kmHFAW5KtyB+hejEP6fiIYhHT5TDC8ihOV959GJYVylo99wL3b6j1avnSy0e28/A/ptaa4g0s28TJ0G5TYnHRWN63Sa4vfQSj8GKoeCiDNHkUAPlY47ndgSDo6803cHftRyqx91O3pGmD1okAySF3TbBCokxlMB3rz5gKpA=', '急性阑尾炎', '建议立即转上级医院手术治疗', 3),
(48, 20, 48, 'ylqEhHYeDRooMtJXwXUc6xW57Ia2M+kGPQ8KbcB1vSS+tRevfhTaL1D9X/e6mgw5g62iXlAs0UN1eo89OS/bMSTqmSzhLr+4JRmluX+B5H2gljDAc6ZfjCRnqeY/iqPV4TPJQgPuJ9YpKU9+CeaCh0ALR0WJpljVpgWlrNghsso=', '手部软组织裂伤', '保持伤口干燥，3天后换药，1周后拆线', 3),
(49, 15, 49, 'HzPQzHn+KM1yUdTWIIuuqdjCMywTNo6h0hjmL9SNvKKNYSaSJ2Y7KtKKaFj+whg7RsPbEdYhUx44fAk/Ad/xnBrgcO1nENFDh3DaW857Ur44pjGcQy683jmy3gXhyaRNnIPXRhF/5Yz26n6iApzL7Ndi1pOgdONHvq5yRxAXp7zwJOLmZGS66ssb/8dFR9Ay', '接触性皮炎', '停用可疑化妆品，避免搔抓，1周后复诊', 4),
(50, 26, 50, 'YS9PbY65PiRPdEBpecfFXyIHeBf8X1zIk2VYFJ6Kux5JSW+zYmKa7kxgF9GxcKRxV1Tw+8oXcYGoHL90yhH2W3MYTuu2lJjPISmQ+Tpt7PZBeizSXHV5w88cg0jK8Msq42cZkIucuwZ8VVXN/SNhVnSxjXGk0/m3schwm1tswQzru1Dn0JBPP4fSPgnSx9mv', '慢性湿疹', '避免接触洗涤剂，注意保湿，2周后复诊', 4),
(51, 22, 51, 'dAuLSlcBEueK/QUwen91LOslXbABS7iVodrWYhRld9pl1eB8t9P5zqUzFbONNKYpdKKWfC748KUPMXimjDn5ftMMq59djYON8mXiMNHzZa4Y+vThQ+f0LH7BYtqeX7Q2khwVqQH1sacLle4Y+rnbeyil2vlqDvaSjpkbbORJSgw=', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 7),
(52, 23, 52, '4WYovjSuuffN30yn5OwSo0+a3YucoTbba111oXkBbgWPlqIPaov/zHYuVNoHx5BTtWxkIJhl2W7eFyyUee1MgWIjOjTxJNEXaN2xTpQLyaPrmPbPnWVIOSPs1Day/YwMKM0oulvTr/McPiYpQH3sB666it5zDoPxAmFD7aZUu98EzRGppCNlACI/gam/dCSPt0j1zGqU2T5pjUABij6OhA==', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 7),
(53, 7, 53, 'dAuLSlcBEueK/QUwen91LOslXbABS7iVodrWYhRld9pl1eB8t9P5zqUzFbONNKYpdKKWfC748KUPMXimjDn5ftMMq59djYON8mXiMNHzZa4Y+vThQ+f0LH7BYtqeX7Q2khwVqQH1sacLle4Y+rnbeyil2vlqDvaSjpkbbORJSgw=', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 7),
(54, 25, 54, 'barIpiHas/4g4ICrubE435tJvuCF+Tuxm5sHXMbj6t9SIdxSKWDl+LKbevzu9hFG+e7Y2MdRzI1BL92uFMy2nJBgIfT3cLmW690P3tJuA73b+uDRbERFIKp5vf1sW+/voAQ/sEg7LeUqKhQ1+4SZWaRzTJz1Ubz97+0+kYkBq5s=', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 1),
(55, 29, 55, 'kmFs4H+KZCwhtNAFdUPjQSSpUxhQJG/wTP5NQIrwhW3EltUqEZ0DGqJFdrMGtHZ/WKOPW7M4XaX5+xarvrFg5UakWEIC0TsDY7bUE2QdB0xaQmK1D2ZVnzh3mHvaQfH2GJOlmrFEytG6gHohIYzZcbbQHXygdM+IrrdLMLkQb+M6aTUtShX7iDC3VLNkUkg1', '2型糖尿病', '控制饮食，适量运动，监测血糖，1个月后复诊', 1),
(56, 21, 56, 'Fz6sHwEr7vaOQ8su3p3WTxB1QdjUShzPOgISlYjara7/eJRYLZ7UsOweE/gm+tAsrd57b3vwXNUxpDyudqFe9pwsT9DHceHbWCgSU00447PbrY4oAcmXa0amkeSkFdCxxy4lrGGo4JBQZ9anBkvGLtJ+tY1mFMczvVwj78xai9CkPffQRXeQDaCHASm4X18m', '疱疹性咽峡炎', '清淡流质饮食，注意口腔清洁，3天后复诊', 2),
(57, 1, 57, 'PLdbasFj0g5eaWZu9WHwlxiRFS+cpcTEfez7rkUY4lnzNeFGihrfDR3U0GffRTLQeEHaAVF5MX79DJpORKsXYV8BIYROqLsrkAq5ISbj7Fyg9QbvZeJQhGM2buts9X8JQ+S4eUV+TuX+19nNSWfDD/FvW0bHy4AqkfWfdW+cgCZsDadlJO5X64sWU6GUyFwU', '前臂软组织挫伤', '患肢制动，48小时内冷敷，2周后复诊', 3),
(58, 26, 58, 'kNR9kmHFAW5KtyB+hejEP6fiIYhHT5TDC8ihOV959GJYVylo99wL3b6j1avnSy0e28/A/ptaa4g0s28TJ0G5TYnHRWN63Sa4vfQSj8GKoeCiDNHkUAPlY47ndgSDo6803cHftRyqx91O3pGmD1okAySF3TbBCokxlMB3rz5gKpA=', '急性阑尾炎', '建议立即转上级医院手术治疗', 3),
(59, 2, 59, 'qN8QImYCf7agUNjKS7ukM//ICJ4LsgjUv5NJpgbTyA54DfV2sYrmEz6SNtDRENmamYgvxrSz7yzuD0bruuNnM61N2z2ebrQeUWz+nX+jm5Lo2Tkq75O4hPEOSIeZtoGp6z8u3UHZOroY5MratU0krr8TGcGPLznBGadji0WIQes=', '脾胃虚弱', '饮食清淡易消化，忌生冷，2周后复诊', 5),
(60, 13, 60, 'yK89pKF9PI1dHW4KnsqoeNzEJtjQW0+EQ1LFSdZzAPDqFBZYHJsZ2xhHPm18JvBSf3rd3/3+XeH2JuAj1xVPJI18tm2zJ1cKlayQtydvcK54LxMuOZ0o7UilmWBwVzU5Emgz39gzUtpxsyJIR/oHqiJqJQzNpXxZgtAgWeSqogU=', '气血两虚', '加强营养，避免劳累，1个月后复诊', 5),
(61, 3, 61, 'cirCl9lgO5EmPOVUXaX3CGU96Xmkh8/nLgw1I2JzTas5ysRnAoLVabBZrndyRx3/bMOn1HcESi7Neh6usilgXuHNBVGqBTtx4N9ZJeunX71/EQsBldRIxeQtfZ7XaJwuq4QohOlHoi4gSjEtn2Oaqg==', '急性咽炎', '多饮水，忌辛辣，3天后复诊', 6),
(62, 30, 62, 'q/4WphthcCr+iBQc5jALIQr1HgPG+3x2YBB4wUTJbOPqtaqXyzEe1IgrwvYyW+8aJSlocUkuVjyypYJUxZMSJUl3oaB69MgsgtqGdVxDWKylicRrlL/pboiSFEDT8AcxMxknf0YkTB3LSRcbVVK1YIZ4nFstkg0cRSK+KLWrxuk=', '急性鼻窦炎', '多饮水，生理盐水冲洗鼻腔，1周后复诊', 6),
(63, 4, 63, 'BsEz1J5J4e6lC14qZxopLz+SPn20gBzPa8pDJX7M9KGh/vD0SjWvq1kTRAgElDyLA2NYRTQ8ANDMSGlpYpanPcmbNISds5jmuZCa5Ko5ljAjOFJ9D3p65c2B7/4n6EaZ8p2Bi7/ivRMJJvwc3uG5+8I9HscUHCK+pW1Mjn9ZRrY=', '阴道炎', '保持外阴清洁，勤换内裤，1周后复诊', 8),
(64, 8, 64, 'BsEz1J5J4e6lC14qZxopLz+SPn20gBzPa8pDJX7M9KGh/vD0SjWvq1kTRAgElDyLA2NYRTQ8ANDMSGlpYpanPcmbNISds5jmuZCa5Ko5ljAjOFJ9D3p65c2B7/4n6EaZ8p2Bi7/ivRMJJvwc3uG5+8I9HscUHCK+pW1Mjn9ZRrY=', '阴道炎', '保持外阴清洁，勤换内裤，1周后复诊', 8),
(65, 6, 65, 'barIpiHas/4g4ICrubE435tJvuCF+Tuxm5sHXMbj6t9SIdxSKWDl+LKbevzu9hFG+e7Y2MdRzI1BL92uFMy2nJBgIfT3cLmW690P3tJuA73b+uDRbERFIKp5vf1sW+/voAQ/sEg7LeUqKhQ1+4SZWaRzTJz1Ubz97+0+kYkBq5s=', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 7),
(66, 11, 66, 'dAuLSlcBEueK/QUwen91LOslXbABS7iVodrWYhRld9pl1eB8t9P5zqUzFbONNKYpdKKWfC748KUPMXimjDn5ftMMq59djYON8mXiMNHzZa4Y+vThQ+f0LH7BYtqeX7Q2khwVqQH1sacLle4Y+rnbeyil2vlqDvaSjpkbbORJSgw=', '慢性胃炎', '规律饮食，忌辛辣刺激，2周后复诊', 7),
(67, 28, 67, '4WYovjSuuffN30yn5OwSo0+a3YucoTbba111oXkBbgWPlqIPaov/zHYuVNoHx5BTtWxkIJhl2W7eFyyUee1MgWIjOjTxJNEXaN2xTpQLyaPrmPbPnWVIOSPs1Day/YwMKM0oulvTr/McPiYpQH3sB666it5zDoPxAmFD7aZUu98EzRGppCNlACI/gam/dCSPt0j1zGqU2T5pjUABij6OhA==', '心律失常待查', '避免劳累及情绪激动，建议行动态心电图，1周后复诊', 7),
(68, 19, 68, 'barIpiHas/4g4ICrubE435tJvuCF+Tuxm5sHXMbj6t9SIdxSKWDl+LKbevzu9hFG+e7Y2MdRzI1BL92uFMy2nJBgIfT3cLmW690P3tJuA73b+uDRbERFIKp5vf1sW+/voAQ/sEg7LeUqKhQ1+4SZWaRzTJz1Ubz97+0+kYkBq5s=', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 7),
(69, 7, 69, 'kmFs4H+KZCwhtNAFdUPjQSSpUxhQJG/wTP5NQIrwhW3EltUqEZ0DGqJFdrMGtHZ/WKOPW7M4XaX5+xarvrFg5UakWEIC0TsDY7bUE2QdB0xaQmK1D2ZVnzh3mHvaQfH2GJOlmrFEytG6gHohIYzZcbbQHXygdM+IrrdLMLkQb+M6aTUtShX7iDC3VLNkUkg1', '2型糖尿病', '控制饮食，适量运动，监测血糖，1个月后复诊', 7),
(70, 10, 70, 'barIpiHas/4g4ICrubE435tJvuCF+Tuxm5sHXMbj6t9SIdxSKWDl+LKbevzu9hFG+e7Y2MdRzI1BL92uFMy2nJBgIfT3cLmW690P3tJuA73b+uDRbERFIKp5vf1sW+/voAQ/sEg7LeUqKhQ1+4SZWaRzTJz1Ubz97+0+kYkBq5s=', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 1),
(71, 13, 71, 'barIpiHas/4g4ICrubE435tJvuCF+Tuxm5sHXMbj6t9SIdxSKWDl+LKbevzu9hFG+e7Y2MdRzI1BL92uFMy2nJBgIfT3cLmW690P3tJuA73b+uDRbERFIKp5vf1sW+/voAQ/sEg7LeUqKhQ1+4SZWaRzTJz1Ubz97+0+kYkBq5s=', '急性上呼吸道感染', '多饮水，注意休息，3天后复诊', 1),
(72, 27, 72, 'nfkRPIKLOfEjzHQ+KVovGoWrd8heulsCOOn8WNXIq4fsCW7da6+413pQfPfJLmB+69lLhkGvH5dW3ru1Tu3a42VT4MiTuulIgrL8Xna7CfesbBOVR8sb/QNEWtbIanJDN2+zXngGocwXiv8MhxeAgVThUErDJkxdzV0O2YdB/c8=', '急性支气管炎', '多饮水，避免受凉，5天后复诊', 2),
(73, 5, 73, 'Nr7lb64Ewfq6hoc0LVRGusuVI4iiYt6kMm03yQlx6gopZZcNGBf4zDlLpC8JO1k2gG/UQE56lAL3AmmfxUHZJ1IJUjyJSFRty+kGECwrgpRee9GdbtC6rZqkkCIN5VnVTOXwNasl7YYeeA+13C9hQMH6avaT1iuu5Z5lHdh35yF2P1Ly4J7UVmFQGJ6t0OdE', '急性上呼吸道感染', '多饮水，物理降温，3天后复诊', 2),
(74, 9, 74, 'fayk4lHQRv4hwIob5/2TeXu0Ynjxfil0EHJawI45DssDxCmf/NXnSs2YlaPifOSz6TH/XLBqZ9PNmr1r26OYF8OGBJn0W9FZ7mG9mfagEnr3AHIGwQLVAQl9YAsJc1Pr3ReTrmktc5xUxLgqb+Qb9RmKVO3tXZ+eKgtmOz8wkAM=', '腰肌劳损', '避免久坐，局部热敷，2周后复诊', 3),
(75, 15, 75, 'a7mBC3wmN+C8gyk8DxJQjytQ5wntaRdoVWxBlIsP7wdchIf5IVxSvmZ+xRUGdU6kwVHVstawjBm2PqTB8BDcqJpfakwx2aKVzwREd0n4C0V03DrMZ/bsq2U91c/l8l9Ba9fAZXXPa1pNlD7rKbC3aj3oMEchG6FasRbgHRnpfb+LD3Z7uVQn68y0lgzYvlAu', '足背软组织感染', '抬高患肢，保持局部清洁，3天后复诊', 3),
(76, 18, 76, '7ajZeZioOOrenQzxbIpLzbmpnENB5/S/AnKnTNARpADFvVq+YlTP/0dqVsZN1LuqyWewAjH23t17KQvtsXbamerNM+HW8CcDhSZzd/K/sNjO+Vo56mt4MeGF9wKRHNxcSxFgmEpF/lqWLC1wpVHvbBFC6K0F4LY/aPRcC21Vs+A=', '足癣', '保持足部干燥，勤换鞋袜，1个月后复诊', 4),
(77, 12, 77, 'SCiIX5hMsmLYEQ0jWlPVhRTfqPxI/moMb3ajTPht4tqfEsJC4UE77D2o81Q9yaAN3Vhl/RGWZHx++RQMdhquX4MwP20YdaiygJIfLhuu58bJyYPoBnHnTZqejFSDRh/EWoTYfPZTOFUkpWkARSXMVZEdxlMRs734mszwr4+R+bQ=', '荨麻疹', '忌食海鲜，避免受热，1周后复诊', 4),
(78, 14, 78, 'HzPQzHn+KM1yUdTWIIuuqdjCMywTNo6h0hjmL9SNvKKNYSaSJ2Y7KtKKaFj+whg7RsPbEdYhUx44fAk/Ad/xnBrgcO1nENFDh3DaW857Ur44pjGcQy683jmy3gXhyaRNnIPXRhF/5Yz26n6iApzL7Ndi1pOgdONHvq5yRxAXp7zwJOLmZGS66ssb/8dFR9Ay', '接触性皮炎', '停用可疑化妆品，避免搔抓，1周后复诊', 4),
(79, 22, 79, 'VEZ2l+GDKylNfYpcgrm5ag7yAs+BvGojCdbHQmmQrO3dN17KvIs48kKBrU+FsXUbhLI1kiFJnp0fbTqqph6FLinsG5QhI5iBBmxUEjYU1MD/GAEr0WT0SJGBFBf6B5OsaJ3l0KzmRfNaVXjIpoI+6g==', '风寒感冒', '多饮温水，注意保暖，3天后复诊', 5),
(80, 17, 80, '0NH2pb4xPYGwnYyI3Y59awnFB6reUvqI3Jammm/AqNKx52r2uNMJv+8aj6i1YPzCZk42HH0pkY5nEWc89JA9Y+Nr3TNtObMrIU0dypQtE4nG71JoEh7vc1SHSTPFzyikv88ldnn1yIPX8Ax+GENCAtWA+3x5ulmAcgIKI0wYaYeb9wAXjDG2vD5cPwx9JonV', '不寐（心脾两虚）', '规律作息，睡前避免饮浓茶，2周后复诊', 5),
(81, 20, 81, 'gD6d6RkBDD7Ba5v2ajzSQNSA+4cQqwYkNgiEgh07tIRbbbycTaCfKGAe2Z56PQWfAFWRuhMrz7vLe1G4kM8tg/752YzqEpjiNtCtfuh4qhET7ZhMmsMJYztSGOvM2S7qRmMJeGvvxz9Fs+oGpuHJlA1iJZWU9OpICeitYOlzf1k=', '月经过多', '注意休息，加强营养，1个月后复诊', 8),
(82, 30, 82, 'p1QmacEAeuC6Fawy/Y7lkIP0AqAIBT9+1pgj6+4elmJXWJI5iETSzgIQGc1U/YYlqW4XFdwMmJDm19tNMHs0j+8Px4cDOfhAlnFA9v2S4IhmoOs8zGGepfc8nQpydDgkzPvpeHS+T/JzgVsUt5k4Y/Y/m5vJ+KKgK5TNkyDEdGiAGsCTSDlDVO29PNxs8kIt', '痛经', '经期注意保暖，避免剧烈运动，1个月后复诊', 8),
(83, 26, 83, 'fayk4lHQRv4hwIob5/2TeXu0Ynjxfil0EHJawI45DssDxCmf/NXnSs2YlaPifOSz6TH/XLBqZ9PNmr1r26OYF8OGBJn0W9FZ7mG9mfagEnr3AHIGwQLVAQl9YAsJc1Pr3ReTrmktc5xUxLgqb+Qb9RmKVO3tXZ+eKgtmOz8wkAM=', '腰肌劳损', '避免久坐，局部热敷，2周后复诊', 3),
(84, 25, 84, 'fayk4lHQRv4hwIob5/2TeXu0Ynjxfil0EHJawI45DssDxCmf/NXnSs2YlaPifOSz6TH/XLBqZ9PNmr1r26OYF8OGBJn0W9FZ7mG9mfagEnr3AHIGwQLVAQl9YAsJc1Pr3ReTrmktc5xUxLgqb+Qb9RmKVO3tXZ+eKgtmOz8wkAM=', '腰肌劳损', '避免久坐，局部热敷，2周后复诊', 3),
(85, 1, 85, 'HzPQzHn+KM1yUdTWIIuuqdjCMywTNo6h0hjmL9SNvKKNYSaSJ2Y7KtKKaFj+whg7RsPbEdYhUx44fAk/Ad/xnBrgcO1nENFDh3DaW857Ur44pjGcQy683jmy3gXhyaRNnIPXRhF/5Yz26n6iApzL7Ndi1pOgdONHvq5yRxAXp7zwJOLmZGS66ssb/8dFR9Ay', '接触性皮炎', '停用可疑化妆品，避免搔抓，1周后复诊', 4),
(86, 6, 86, 'q/4WphthcCr+iBQc5jALIQr1HgPG+3x2YBB4wUTJbOPqtaqXyzEe1IgrwvYyW+8aJSlocUkuVjyypYJUxZMSJUl3oaB69MgsgtqGdVxDWKylicRrlL/pboiSFEDT8AcxMxknf0YkTB3LSRcbVVK1YIZ4nFstkg0cRSK+KLWrxuk=', '急性鼻窦炎', '多饮水，生理盐水冲洗鼻腔，1周后复诊', 6);

-- ---- 8. 新增处方 ----
INSERT INTO `prescription` (`id`, `consultation_id`, `diagnosis`, `prescription_content`, `status`, `doctor_id`) VALUES
(4, 7, '慢性胃炎', '奥美拉唑肠溶胶囊 20mg 每日1次', 1, 1),
(5, 8, '急性上呼吸道感染', '布洛芬缓释胶囊 0.15g 必要时 + 对乙酰氨基酚片 0.5片 必要时', 1, 2),
(6, 9, '体表脓肿', '阿莫西林胶囊 0.5g 每日3次', 1, 3),
(7, 10, '前臂软组织挫伤', '头孢克洛胶囊 0.25g 每日3次', 1, 3),
(8, 11, '痱子', '维生素C片 100mg 每日1次', 1, 4),
(9, 12, '气血两虚', '维生素C片 100mg 每日1次', 1, 5),
(10, 14, '急性上呼吸道感染', '复方氨酚烷胺片 1片 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 7),
(11, 16, '急性上呼吸道感染', '复方氨酚烷胺片 1片 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 1),
(12, 17, '急性支气管炎', '阿莫西林胶囊 0.25g 每日3次', 1, 2),
(13, 18, '疱疹性咽峡炎', '布洛芬缓释胶囊 0.15g 必要时', 1, 2),
(14, 19, '前臂软组织挫伤', '头孢克洛胶囊 0.25g 每日3次', 1, 3),
(15, 20, '慢性湿疹', '氯雷他定片 10mg 每日1次', 1, 4),
(16, 21, '荨麻疹', '氯雷他定片 10mg 每日1次 + 维生素C片 100mg 每日1次', 1, 4),
(17, 22, '阴道炎', '阿莫西林胶囊 0.5g 每日3次', 1, 8),
(18, 24, '风寒感冒', '连花清瘟胶囊 4粒 每日3次', 1, 5),
(19, 25, '急性上呼吸道感染', '复方氨酚烷胺片 1片 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 7),
(20, 26, '慢性胃炎', '奥美拉唑肠溶胶囊 20mg 每日1次', 1, 7),
(21, 28, '慢性胃炎', '奥美拉唑肠溶胶囊 20mg 每日1次', 1, 7),
(22, 29, '急性上呼吸道感染', '复方氨酚烷胺片 1片 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 1),
(23, 30, '慢性胃炎', '奥美拉唑肠溶胶囊 20mg 每日1次', 1, 1),
(24, 32, '前臂软组织挫伤', '头孢克洛胶囊 0.25g 每日3次', 1, 3),
(25, 33, '足背软组织感染', '头孢克洛胶囊 0.25g 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 3),
(26, 34, '慢性湿疹', '氯雷他定片 10mg 每日1次', 1, 4),
(27, 35, '接触性皮炎', '氯雷他定片 10mg 每日1次', 1, 4),
(28, 36, '风寒感冒', '连花清瘟胶囊 4粒 每日3次', 1, 5),
(29, 38, '急性鼻窦炎', '头孢克洛胶囊 0.25g 每日3次', 1, 6),
(30, 39, '急性鼻窦炎', '头孢克洛胶囊 0.25g 每日3次', 1, 6),
(31, 41, '急性上呼吸道感染', '复方氨酚烷胺片 1片 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 7),
(32, 43, '急性上呼吸道感染', '布洛芬缓释胶囊 0.15g 必要时 + 对乙酰氨基酚片 0.5片 必要时', 1, 2),
(33, 44, '急性支气管炎', '阿莫西林胶囊 0.25g 每日3次', 1, 2),
(34, 45, '阴道炎', '阿莫西林胶囊 0.5g 每日3次', 1, 8),
(35, 46, '阴道炎', '阿莫西林胶囊 0.5g 每日3次', 1, 8),
(36, 48, '手部软组织裂伤', '阿莫西林胶囊 0.5g 每日3次', 1, 3),
(37, 49, '接触性皮炎', '氯雷他定片 10mg 每日1次', 1, 4),
(38, 50, '慢性湿疹', '氯雷他定片 10mg 每日1次', 1, 4),
(39, 51, '慢性胃炎', '奥美拉唑肠溶胶囊 20mg 每日1次', 1, 7),
(40, 53, '慢性胃炎', '奥美拉唑肠溶胶囊 20mg 每日1次', 1, 7),
(41, 54, '急性上呼吸道感染', '复方氨酚烷胺片 1片 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 1),
(42, 55, '2型糖尿病', '盐酸二甲双胍片 0.5g 每日2次', 1, 1),
(43, 56, '疱疹性咽峡炎', '布洛芬缓释胶囊 0.15g 必要时', 1, 2),
(44, 57, '前臂软组织挫伤', '头孢克洛胶囊 0.25g 每日3次', 1, 3),
(45, 60, '气血两虚', '维生素C片 100mg 每日1次', 1, 5),
(46, 61, '急性咽炎', '阿莫西林胶囊 0.5g 每日3次', 1, 6),
(47, 62, '急性鼻窦炎', '头孢克洛胶囊 0.25g 每日3次', 1, 6),
(48, 63, '阴道炎', '阿莫西林胶囊 0.5g 每日3次', 1, 8),
(49, 64, '阴道炎', '阿莫西林胶囊 0.5g 每日3次', 1, 8),
(50, 65, '急性上呼吸道感染', '复方氨酚烷胺片 1片 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 7),
(51, 66, '慢性胃炎', '奥美拉唑肠溶胶囊 20mg 每日1次', 1, 7),
(52, 68, '急性上呼吸道感染', '复方氨酚烷胺片 1片 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 7),
(53, 69, '2型糖尿病', '盐酸二甲双胍片 0.5g 每日2次', 1, 7),
(54, 70, '急性上呼吸道感染', '复方氨酚烷胺片 1片 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 1),
(55, 71, '急性上呼吸道感染', '复方氨酚烷胺片 1片 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 1),
(56, 72, '急性支气管炎', '阿莫西林胶囊 0.25g 每日3次', 1, 2),
(57, 73, '急性上呼吸道感染', '布洛芬缓释胶囊 0.15g 必要时 + 对乙酰氨基酚片 0.5片 必要时', 1, 2),
(58, 74, '腰肌劳损', '布洛芬缓释胶囊 0.3g 必要时', 1, 3),
(59, 75, '足背软组织感染', '头孢克洛胶囊 0.25g 每日3次 + 布洛芬缓释胶囊 0.3g 必要时', 1, 3),
(60, 77, '荨麻疹', '氯雷他定片 10mg 每日1次 + 维生素C片 100mg 每日1次', 1, 4),
(61, 78, '接触性皮炎', '氯雷他定片 10mg 每日1次', 1, 4),
(62, 79, '风寒感冒', '连花清瘟胶囊 4粒 每日3次', 1, 5),
(63, 80, '不寐（心脾两虚）', '维生素C片 100mg 每日1次', 1, 5),
(64, 82, '痛经', '布洛芬缓释胶囊 0.3g 必要时', 1, 8),
(65, 83, '腰肌劳损', '布洛芬缓释胶囊 0.3g 必要时', 1, 3),
(66, 84, '腰肌劳损', '布洛芬缓释胶囊 0.3g 必要时', 1, 3),
(67, 85, '接触性皮炎', '氯雷他定片 10mg 每日1次', 1, 4),
(68, 86, '急性鼻窦炎', '头孢克洛胶囊 0.25g 每日3次', 1, 6);

-- ---- 9. 新增处方药品明细 ----
INSERT INTO `prescription_drug` (`id`, `prescription_id`, `drug_id`, `quantity`, `usage_method`, `dosage`, `frequency`) VALUES
(5, 4, 6, 1, '口服', '20mg', '每日1次'),
(6, 5, 3, 2, '口服', '0.15g', '必要时'),
(7, 5, 4, 1, '口服', '0.5片', '必要时'),
(8, 6, 1, 1, '口服', '0.5g', '每日3次'),
(9, 7, 2, 1, '口服', '0.25g', '每日3次'),
(10, 8, 10, 1, '口服', '100mg', '每日1次'),
(11, 9, 10, 1, '口服', '100mg', '每日1次'),
(12, 10, 7, 1, '口服', '1片', '每日3次'),
(13, 10, 3, 1, '口服', '0.3g', '必要时'),
(14, 11, 7, 1, '口服', '1片', '每日3次'),
(15, 11, 3, 1, '口服', '0.3g', '必要时'),
(16, 12, 1, 1, '口服', '0.25g', '每日3次'),
(17, 13, 3, 2, '口服', '0.15g', '必要时'),
(18, 14, 2, 1, '口服', '0.25g', '每日3次'),
(19, 15, 9, 1, '口服', '10mg', '每日1次'),
(20, 16, 9, 1, '口服', '10mg', '每日1次'),
(21, 16, 10, 1, '口服', '100mg', '每日1次'),
(22, 17, 1, 1, '口服', '0.5g', '每日3次'),
(23, 18, 8, 1, '口服', '4粒', '每日3次'),
(24, 19, 7, 1, '口服', '1片', '每日3次'),
(25, 19, 3, 1, '口服', '0.3g', '必要时'),
(26, 20, 6, 1, '口服', '20mg', '每日1次'),
(27, 21, 6, 1, '口服', '20mg', '每日1次'),
(28, 22, 7, 1, '口服', '1片', '每日3次'),
(29, 22, 3, 1, '口服', '0.3g', '必要时'),
(30, 23, 6, 1, '口服', '20mg', '每日1次'),
(31, 24, 2, 1, '口服', '0.25g', '每日3次'),
(32, 25, 2, 1, '口服', '0.25g', '每日3次'),
(33, 25, 3, 1, '口服', '0.3g', '必要时'),
(34, 26, 9, 1, '口服', '10mg', '每日1次'),
(35, 27, 9, 1, '口服', '10mg', '每日1次'),
(36, 28, 8, 1, '口服', '4粒', '每日3次'),
(37, 29, 2, 1, '口服', '0.25g', '每日3次'),
(38, 30, 2, 1, '口服', '0.25g', '每日3次'),
(39, 31, 7, 1, '口服', '1片', '每日3次'),
(40, 31, 3, 1, '口服', '0.3g', '必要时'),
(41, 32, 3, 2, '口服', '0.15g', '必要时'),
(42, 32, 4, 1, '口服', '0.5片', '必要时'),
(43, 33, 1, 1, '口服', '0.25g', '每日3次'),
(44, 34, 1, 1, '口服', '0.5g', '每日3次'),
(45, 35, 1, 1, '口服', '0.5g', '每日3次'),
(46, 36, 1, 1, '口服', '0.5g', '每日3次'),
(47, 37, 9, 1, '口服', '10mg', '每日1次'),
(48, 38, 9, 1, '口服', '10mg', '每日1次'),
(49, 39, 6, 1, '口服', '20mg', '每日1次'),
(50, 40, 6, 1, '口服', '20mg', '每日1次'),
(51, 41, 7, 1, '口服', '1片', '每日3次'),
(52, 41, 3, 1, '口服', '0.3g', '必要时'),
(53, 42, 12, 2, '口服', '0.5g', '每日2次'),
(54, 43, 3, 2, '口服', '0.15g', '必要时'),
(55, 44, 2, 1, '口服', '0.25g', '每日3次'),
(56, 45, 10, 1, '口服', '100mg', '每日1次'),
(57, 46, 1, 1, '口服', '0.5g', '每日3次'),
(58, 47, 2, 1, '口服', '0.25g', '每日3次'),
(59, 48, 1, 1, '口服', '0.5g', '每日3次'),
(60, 49, 1, 1, '口服', '0.5g', '每日3次'),
(61, 50, 7, 1, '口服', '1片', '每日3次'),
(62, 50, 3, 1, '口服', '0.3g', '必要时'),
(63, 51, 6, 1, '口服', '20mg', '每日1次'),
(64, 52, 7, 1, '口服', '1片', '每日3次'),
(65, 52, 3, 1, '口服', '0.3g', '必要时'),
(66, 53, 12, 2, '口服', '0.5g', '每日2次'),
(67, 54, 7, 1, '口服', '1片', '每日3次'),
(68, 54, 3, 1, '口服', '0.3g', '必要时'),
(69, 55, 7, 1, '口服', '1片', '每日3次'),
(70, 55, 3, 1, '口服', '0.3g', '必要时'),
(71, 56, 1, 1, '口服', '0.25g', '每日3次'),
(72, 57, 3, 2, '口服', '0.15g', '必要时'),
(73, 57, 4, 1, '口服', '0.5片', '必要时'),
(74, 58, 3, 1, '口服', '0.3g', '必要时'),
(75, 59, 2, 1, '口服', '0.25g', '每日3次'),
(76, 59, 3, 1, '口服', '0.3g', '必要时'),
(77, 60, 9, 1, '口服', '10mg', '每日1次'),
(78, 60, 10, 1, '口服', '100mg', '每日1次'),
(79, 61, 9, 1, '口服', '10mg', '每日1次'),
(80, 62, 8, 1, '口服', '4粒', '每日3次'),
(81, 63, 10, 1, '口服', '100mg', '每日1次'),
(82, 64, 3, 1, '口服', '0.3g', '必要时'),
(83, 65, 3, 1, '口服', '0.3g', '必要时'),
(84, 66, 3, 1, '口服', '0.3g', '必要时'),
(85, 67, 9, 1, '口服', '10mg', '每日1次'),
(86, 68, 2, 1, '口服', '0.25g', '每日3次');

-- ---- 10. 账单（挂号即收费 10.00 元，开处方后追加药费）----
-- 说明：init.sql 中 3 条账单的挂号费由 15.00 规范为 10.00，与后端实现一致；
--       并补齐挂号 4、5 的账单。此处为全部账单的完整重建。
INSERT INTO `bill` (`id`, `registration_id`, `prescription_id`, `registration_fee`, `drug_fee`, `total_amount`, `payment_method`, `status`) VALUES
(1, 1, 1, 10.00, 32.00, 42.00, 'WECHAT', 1),
(2, 2, 2, 10.00, 23.50, 33.50, NULL, 0),
(3, 3, 3, 10.00, 44.00, 54.00, 'CASH', 1),
(4, 4, NULL, 10.00, 0.00, 10.00, NULL, 0),
(5, 5, NULL, 10.00, 0.00, 10.00, NULL, 0),
(6, 6, NULL, 10.00, 0.00, 10.00, 'CARD', 1),
(7, 7, NULL, 10.00, 0.00, 10.00, 'CASH', 1),
(8, 8, NULL, 10.00, 0.00, 10.00, 'ALIPAY', 1),
(9, 9, 4, 10.00, 28.50, 38.50, 'CASH', 1),
(10, 10, 5, 10.00, 35.80, 45.80, 'ALIPAY', 1),
(11, 11, 6, 10.00, 18.50, 28.50, 'WECHAT', 1),
(12, 12, 7, 10.00, 35.00, 45.00, 'CASH', 1),
(13, 13, 8, 10.00, 9.90, 19.90, 'ALIPAY', 1),
(14, 14, 9, 10.00, 9.90, 19.90, 'CARD', 1),
(15, 15, NULL, 10.00, 0.00, 10.00, 'WECHAT', 1),
(16, 16, 10, 10.00, 23.50, 33.50, 'WECHAT', 1),
(17, 17, NULL, 10.00, 0.00, 10.00, 'WECHAT', 1),
(18, 18, 11, 10.00, 23.50, 33.50, 'CASH', 1),
(19, 19, 12, 10.00, 18.50, 28.50, 'ALIPAY', 1),
(20, 20, 13, 10.00, 30.00, 40.00, 'WECHAT', 1),
(21, 21, 14, 10.00, 35.00, 45.00, 'ALIPAY', 1),
(22, 22, 15, 10.00, 12.00, 22.00, 'CARD', 1),
(23, 23, 16, 10.00, 21.90, 31.90, 'WECHAT', 1),
(24, 24, 17, 10.00, 18.50, 28.50, 'WECHAT', 1),
(25, 25, NULL, 10.00, 0.00, 10.00, 'WECHAT', 1),
(26, 26, 18, 10.00, 14.80, 24.80, 'CARD', 1),
(27, 27, 19, 10.00, 23.50, 33.50, 'ALIPAY', 1),
(28, 28, 20, 10.00, 28.50, 38.50, 'ALIPAY', 1),
(29, 29, NULL, 10.00, 0.00, 10.00, 'CASH', 1),
(30, 30, 21, 10.00, 28.50, 38.50, 'CARD', 1),
(31, 31, 22, 10.00, 23.50, 33.50, 'CARD', 1),
(32, 32, 23, 10.00, 28.50, 38.50, 'WECHAT', 1),
(33, 33, NULL, 10.00, 0.00, 10.00, 'CASH', 1),
(34, 34, 24, 10.00, 35.00, 45.00, 'CARD', 1),
(35, 35, 25, 10.00, 50.00, 60.00, 'WECHAT', 1),
(36, 36, 26, 10.00, 12.00, 22.00, 'ALIPAY', 1),
(37, 37, 27, 10.00, 12.00, 22.00, 'CARD', 1),
(38, 38, 28, 10.00, 14.80, 24.80, 'ALIPAY', 1),
(39, 39, NULL, 10.00, 0.00, 10.00, 'CASH', 1),
(40, 40, 29, 10.00, 35.00, 45.00, 'CARD', 1),
(41, 41, 30, 10.00, 35.00, 45.00, 'WECHAT', 1),
(42, 42, NULL, 10.00, 0.00, 10.00, 'WECHAT', 1),
(43, 43, 31, 10.00, 23.50, 33.50, 'WECHAT', 1),
(44, 44, NULL, 10.00, 0.00, 10.00, 'WECHAT', 1),
(45, 45, 32, 10.00, 35.80, 45.80, 'ALIPAY', 1),
(46, 46, 33, 10.00, 18.50, 28.50, 'CARD', 1),
(47, 47, 34, 10.00, 18.50, 28.50, 'CARD', 1),
(48, 48, 35, 10.00, 18.50, 28.50, 'ALIPAY', 1),
(49, 49, NULL, 10.00, 0.00, 10.00, 'ALIPAY', 1),
(50, 50, 36, 10.00, 18.50, 28.50, 'CASH', 1),
(51, 51, 37, 10.00, 12.00, 22.00, 'ALIPAY', 1),
(52, 52, 38, 10.00, 12.00, 22.00, 'CARD', 1),
(53, 53, 39, 10.00, 28.50, 38.50, 'ALIPAY', 1),
(54, 54, NULL, 10.00, 0.00, 10.00, 'CARD', 1),
(55, 55, 40, 10.00, 28.50, 38.50, 'CARD', 1),
(56, 56, 41, 10.00, 23.50, 33.50, 'ALIPAY', 1),
(57, 57, 42, 10.00, 32.00, 42.00, 'ALIPAY', 1),
(58, 58, 43, 10.00, 30.00, 40.00, 'WECHAT', 1),
(59, 59, 44, 10.00, 35.00, 45.00, 'WECHAT', 1),
(60, 60, NULL, 10.00, 0.00, 10.00, 'WECHAT', 1),
(61, 61, NULL, 10.00, 0.00, 10.00, 'CASH', 1),
(62, 62, 45, 10.00, 9.90, 19.90, 'ALIPAY', 1),
(63, 63, 46, 10.00, 18.50, 28.50, 'CARD', 1),
(64, 64, 47, 10.00, 35.00, 45.00, 'WECHAT', 1),
(65, 65, 48, 10.00, 18.50, 28.50, 'CASH', 1),
(66, 66, 49, 10.00, 18.50, 28.50, 'CARD', 1),
(67, 67, 50, 10.00, 23.50, 33.50, 'CASH', 1),
(68, 68, 51, 10.00, 28.50, 38.50, 'CASH', 1),
(69, 69, NULL, 10.00, 0.00, 10.00, 'CASH', 1),
(70, 70, 52, 10.00, 23.50, 33.50, 'CASH', 1),
(71, 71, 53, 10.00, 32.00, 42.00, 'CASH', 1),
(72, 72, 54, 10.00, 23.50, 33.50, 'WECHAT', 1),
(73, 73, 55, 10.00, 23.50, 33.50, 'WECHAT', 1),
(74, 74, 56, 10.00, 18.50, 28.50, 'CARD', 1),
(75, 75, 57, 10.00, 35.80, 45.80, 'ALIPAY', 1),
(76, 76, 58, 10.00, 15.00, 25.00, 'WECHAT', 1),
(77, 77, 59, 10.00, 50.00, 60.00, 'CARD', 1),
(78, 78, NULL, 10.00, 0.00, 10.00, 'CARD', 1),
(79, 79, 60, 10.00, 21.90, 31.90, 'CASH', 1),
(80, 80, 61, 10.00, 12.00, 22.00, 'ALIPAY', 1),
(81, 81, 62, 10.00, 14.80, 24.80, 'CARD', 1),
(82, 82, 63, 10.00, 9.90, 19.90, 'WECHAT', 1),
(83, 83, NULL, 10.00, 0.00, 10.00, 'ALIPAY', 1),
(84, 84, 64, 10.00, 15.00, 25.00, 'WECHAT', 1),
(85, 85, NULL, 10.00, 0.00, 10.00, NULL, 0),
(86, 86, NULL, 10.00, 0.00, 10.00, NULL, 0),
(87, 87, NULL, 10.00, 0.00, 10.00, NULL, 0),
(88, 88, NULL, 10.00, 0.00, 10.00, NULL, 0),
(89, 89, NULL, 10.00, 0.00, 10.00, NULL, 0),
(90, 90, 65, 10.00, 15.00, 25.00, 'WECHAT', 1),
(91, 91, 66, 10.00, 15.00, 25.00, 'CARD', 1),
(92, 92, NULL, 10.00, 0.00, 10.00, NULL, 0),
(93, 93, NULL, 10.00, 0.00, 10.00, NULL, 0),
(94, 94, NULL, 10.00, 0.00, 10.00, NULL, 0),
(95, 95, NULL, 10.00, 0.00, 10.00, NULL, 0),
(96, 96, 67, 10.00, 12.00, 22.00, 'CASH', 1),
(97, 97, NULL, 10.00, 0.00, 10.00, NULL, 0),
(98, 98, NULL, 10.00, 0.00, 10.00, NULL, 0),
(99, 99, NULL, 10.00, 0.00, 10.00, NULL, 0),
(100, 100, 68, 10.00, 35.00, 45.00, 'CARD', 1),
(101, 101, NULL, 10.00, 0.00, 10.00, NULL, 0),
(102, 102, NULL, 10.00, 0.00, 10.00, NULL, 0),
(103, 103, NULL, 10.00, 0.00, 10.00, NULL, 0),
(104, 104, NULL, 10.00, 0.00, 10.00, NULL, 0),
(105, 105, NULL, 10.00, 0.00, 10.00, NULL, 0),
(106, 106, NULL, 10.00, 0.00, 10.00, NULL, 0),
(107, 107, NULL, 10.00, 0.00, 10.00, NULL, 0),
(108, 108, NULL, 10.00, 0.00, 10.00, NULL, 0),
(109, 109, NULL, 10.00, 0.00, 10.00, NULL, 0),
(110, 110, NULL, 10.00, 0.00, 10.00, NULL, 0),
(111, 111, NULL, 10.00, 0.00, 10.00, NULL, 0);

-- ---- 11. 新增复诊提醒 ----
INSERT INTO `reminder` (`id`, `patient_id`, `consultation_id`, `doctor_id`, `remind_date`, `content`, `status`) VALUES
(7, 5, 8, 2, DATE_ADD(CURDATE(), INTERVAL -3 DAY), '急性上呼吸道感染复诊：多饮水，物理降温，3天后复诊', 1),
(8, 12, 9, 3, DATE_ADD(CURDATE(), INTERVAL -3 DAY), '体表脓肿复诊：切开引流后按时换药，3天后复诊', 1),
(12, 13, 14, 7, DATE_ADD(CURDATE(), INTERVAL -2 DAY), '急性上呼吸道感染复诊：多饮水，注意休息，3天后复诊', 1),
(14, 9, 16, 1, DATE_ADD(CURDATE(), INTERVAL -2 DAY), '急性上呼吸道感染复诊：多饮水，注意休息，3天后复诊', 1),
(16, 21, 18, 2, DATE_ADD(CURDATE(), INTERVAL -2 DAY), '疱疹性咽峡炎复诊：清淡流质饮食，注意口腔清洁，3天后复诊', 1),
(22, 19, 24, 5, DATE_ADD(CURDATE(), INTERVAL -2 DAY), '风寒感冒复诊：多饮温水，注意保暖，3天后复诊', 1),
(23, 8, 25, 7, DATE_ADD(CURDATE(), INTERVAL -1 DAY), '急性上呼吸道感染复诊：多饮水，注意休息，3天后复诊', 1),
(27, 24, 29, 1, DATE_ADD(CURDATE(), INTERVAL -1 DAY), '急性上呼吸道感染复诊：多饮水，注意休息，3天后复诊', 1),
(29, 27, 31, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), '急性咽炎复诊：多饮水，清淡饮食，3天后复诊', 1),
(31, 4, 33, 3, DATE_ADD(CURDATE(), INTERVAL -1 DAY), '足背软组织感染复诊：抬高患肢，保持局部清洁，3天后复诊', 1),
(34, 6, 36, 5, DATE_ADD(CURDATE(), INTERVAL -1 DAY), '风寒感冒复诊：多饮温水，注意保暖，3天后复诊', 1),
(15, 16, 17, 2, CURDATE(), '急性支气管炎复诊：多饮水，避免受凉，5天后复诊', 0),
(39, 8, 41, 7, CURDATE(), '急性上呼吸道感染复诊：多饮水，注意休息，3天后复诊', 0),
(41, 5, 43, 2, CURDATE(), '急性上呼吸道感染复诊：多饮水，物理降温，3天后复诊', 0),
(51, 25, 54, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '急性上呼吸道感染复诊：多饮水，注意休息，3天后复诊', 0),
(53, 21, 56, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '疱疹性咽峡炎复诊：清淡流质饮食，注意口腔清洁，3天后复诊', 0),
(57, 3, 61, 6, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '急性咽炎复诊：多饮水，忌辛辣，3天后复诊', 0),
(11, 15, 13, 7, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '心律失常待查复诊：避免劳累及情绪激动，建议行动态心电图，1周后复诊', 0),
(13, 18, 15, 7, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '心律失常待查复诊：避免劳累及情绪激动，建议行动态心电图，1周后复诊', 0),
(19, 3, 21, 4, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '荨麻疹复诊：忌食海鲜，避免受热，1周后复诊', 1),
(20, 17, 22, 8, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '阴道炎复诊：保持外阴清洁，勤换内裤，1周后复诊', 1),
(42, 16, 44, 2, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '急性支气管炎复诊：多饮水，避免受凉，5天后复诊', 1),
(61, 6, 65, 7, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '急性上呼吸道感染复诊：多饮水，注意休息，3天后复诊', 1),
(64, 19, 68, 7, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '急性上呼吸道感染复诊：多饮水，注意休息，3天后复诊', 1),
(66, 10, 70, 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '急性上呼吸道感染复诊：多饮水，注意休息，3天后复诊', 1),
(67, 13, 71, 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '急性上呼吸道感染复诊：多饮水，注意休息，3天后复诊', 1),
(69, 5, 73, 2, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '急性上呼吸道感染复诊：多饮水，物理降温，3天后复诊', 1),
(71, 15, 75, 3, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '足背软组织感染复诊：抬高患肢，保持局部清洁，3天后复诊', 1),
(75, 22, 79, 5, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '风寒感冒复诊：多饮温水，注意保暖，3天后复诊', 1),
(33, 23, 35, 4, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '接触性皮炎复诊：停用可疑化妆品，避免搔抓，1周后复诊', 1),
(36, 10, 38, 6, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '急性鼻窦炎复诊：多饮水，生理盐水冲洗鼻腔，1周后复诊', 1),
(37, 11, 39, 6, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '急性鼻窦炎复诊：多饮水，生理盐水冲洗鼻腔，1周后复诊', 1),
(38, 9, 40, 7, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '心律失常待查复诊：避免劳累及情绪激动，建议行动态心电图，1周后复诊', 1),
(40, 12, 42, 1, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '心律失常待查复诊：避免劳累及情绪激动，建议行动态心电图，1周后复诊', 1),
(43, 18, 45, 8, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '阴道炎复诊：保持外阴清洁，勤换内裤，1周后复诊', 1),
(44, 17, 46, 8, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '阴道炎复诊：保持外阴清洁，勤换内裤，1周后复诊', 1),
(45, 20, 48, 3, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '手部软组织裂伤复诊：保持伤口干燥，3天后换药，1周后拆线', 1),
(46, 15, 49, 4, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '接触性皮炎复诊：停用可疑化妆品，避免搔抓，1周后复诊', 1),
(68, 27, 72, 2, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '急性支气管炎复诊：多饮水，避免受凉，5天后复诊', 1),
(49, 23, 52, 7, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '心律失常待查复诊：避免劳累及情绪激动，建议行动态心电图，1周后复诊', 1),
(58, 30, 62, 6, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '急性鼻窦炎复诊：多饮水，生理盐水冲洗鼻腔，1周后复诊', 1),
(59, 4, 63, 8, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '阴道炎复诊：保持外阴清洁，勤换内裤，1周后复诊', 1),
(60, 8, 64, 8, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '阴道炎复诊：保持外阴清洁，勤换内裤，1周后复诊', 1),
(63, 28, 67, 7, DATE_ADD(CURDATE(), INTERVAL 6 DAY), '心律失常待查复诊：避免劳累及情绪激动，建议行动态心电图，1周后复诊', 1),
(73, 12, 77, 4, DATE_ADD(CURDATE(), INTERVAL 6 DAY), '荨麻疹复诊：忌食海鲜，避免受热，1周后复诊', 1),
(74, 14, 78, 4, DATE_ADD(CURDATE(), INTERVAL 6 DAY), '接触性皮炎复诊：停用可疑化妆品，避免搔抓，1周后复诊', 1),
(81, 1, 85, 4, DATE_ADD(CURDATE(), INTERVAL 7 DAY), '接触性皮炎复诊：停用可疑化妆品，避免搔抓，1周后复诊', 1),
(82, 6, 86, 6, DATE_ADD(CURDATE(), INTERVAL 7 DAY), '急性鼻窦炎复诊：多饮水，生理盐水冲洗鼻腔，1周后复诊', 1),
(6, 2, 7, 1, DATE_ADD(CURDATE(), INTERVAL 8 DAY), '慢性胃炎复诊：规律饮食，忌辛辣刺激，2周后复诊', 1),
(9, 11, 10, 3, DATE_ADD(CURDATE(), INTERVAL 8 DAY), '前臂软组织挫伤复诊：患肢制动，48小时内冷敷，2周后复诊', 1),
(17, 14, 19, 3, DATE_ADD(CURDATE(), INTERVAL 9 DAY), '前臂软组织挫伤复诊：患肢制动，48小时内冷敷，2周后复诊', 1),
(18, 22, 20, 4, DATE_ADD(CURDATE(), INTERVAL 9 DAY), '慢性湿疹复诊：避免接触洗涤剂，注意保湿，2周后复诊', 1),
(24, 25, 26, 7, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '慢性胃炎复诊：规律饮食，忌辛辣刺激，2周后复诊', 1),
(26, 2, 28, 7, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '慢性胃炎复诊：规律饮食，忌辛辣刺激，2周后复诊', 1),
(28, 29, 30, 1, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '慢性胃炎复诊：规律饮食，忌辛辣刺激，2周后复诊', 1),
(30, 20, 32, 3, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '前臂软组织挫伤复诊：患肢制动，48小时内冷敷，2周后复诊', 1),
(32, 3, 34, 4, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '慢性湿疹复诊：避免接触洗涤剂，注意保湿，2周后复诊', 1),
(35, 30, 37, 5, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '脾胃虚弱复诊：饮食清淡易消化，忌生冷，2周后复诊', 1),
(47, 26, 50, 4, DATE_ADD(CURDATE(), INTERVAL 11 DAY), '慢性湿疹复诊：避免接触洗涤剂，注意保湿，2周后复诊', 1),
(48, 22, 51, 7, DATE_ADD(CURDATE(), INTERVAL 12 DAY), '慢性胃炎复诊：规律饮食，忌辛辣刺激，2周后复诊', 1),
(50, 7, 53, 7, DATE_ADD(CURDATE(), INTERVAL 12 DAY), '慢性胃炎复诊：规律饮食，忌辛辣刺激，2周后复诊', 1),
(54, 1, 57, 3, DATE_ADD(CURDATE(), INTERVAL 12 DAY), '前臂软组织挫伤复诊：患肢制动，48小时内冷敷，2周后复诊', 1),
(55, 2, 59, 5, DATE_ADD(CURDATE(), INTERVAL 12 DAY), '脾胃虚弱复诊：饮食清淡易消化，忌生冷，2周后复诊', 1),
(62, 11, 66, 7, DATE_ADD(CURDATE(), INTERVAL 13 DAY), '慢性胃炎复诊：规律饮食，忌辛辣刺激，2周后复诊', 1),
(70, 9, 74, 3, DATE_ADD(CURDATE(), INTERVAL 13 DAY), '腰肌劳损复诊：避免久坐，局部热敷，2周后复诊', 1),
(76, 17, 80, 5, DATE_ADD(CURDATE(), INTERVAL 13 DAY), '不寐（心脾两虚）复诊：规律作息，睡前避免饮浓茶，2周后复诊', 1),
(79, 26, 83, 3, DATE_ADD(CURDATE(), INTERVAL 14 DAY), '腰肌劳损复诊：避免久坐，局部热敷，2周后复诊', 1),
(80, 25, 84, 3, DATE_ADD(CURDATE(), INTERVAL 14 DAY), '腰肌劳损复诊：避免久坐，局部热敷，2周后复诊', 1),
(10, 10, 12, 5, DATE_ADD(CURDATE(), INTERVAL 24 DAY), '气血两虚复诊：加强营养，避免劳累，1个月后复诊', 1),
(21, 28, 23, 8, DATE_ADD(CURDATE(), INTERVAL 25 DAY), '月经过多复诊：注意休息，加强营养，1个月后复诊', 1),
(52, 29, 55, 1, DATE_ADD(CURDATE(), INTERVAL 28 DAY), '2型糖尿病复诊：控制饮食，适量运动，监测血糖，1个月后复诊', 1),
(56, 13, 60, 5, DATE_ADD(CURDATE(), INTERVAL 28 DAY), '气血两虚复诊：加强营养，避免劳累，1个月后复诊', 1),
(65, 7, 69, 7, DATE_ADD(CURDATE(), INTERVAL 29 DAY), '2型糖尿病复诊：控制饮食，适量运动，监测血糖，1个月后复诊', 1),
(72, 18, 76, 4, DATE_ADD(CURDATE(), INTERVAL 29 DAY), '足癣复诊：保持足部干燥，勤换鞋袜，1个月后复诊', 1),
(77, 20, 81, 8, DATE_ADD(CURDATE(), INTERVAL 29 DAY), '月经过多复诊：注意休息，加强营养，1个月后复诊', 1),
(78, 30, 82, 8, DATE_ADD(CURDATE(), INTERVAL 29 DAY), '痛经复诊：经期注意保暖，避免剧烈运动，1个月后复诊', 1),
(3, 4, 4, 7, DATE_ADD(CURDATE(), INTERVAL 84 DAY), '高脂血症复诊：低脂饮食，加强锻炼，3个月后复查血脂', 1),
(4, 1, 5, 7, DATE_ADD(CURDATE(), INTERVAL 84 DAY), '高脂血症复诊：低脂饮食，加强锻炼，3个月后复查血脂', 1),
(5, 7, 6, 1, DATE_ADD(CURDATE(), INTERVAL 84 DAY), '高脂血症复诊：低脂饮食，加强锻炼，3个月后复查血脂', 1),
(25, 1, 27, 7, DATE_ADD(CURDATE(), INTERVAL 86 DAY), '高脂血症复诊：低脂饮食，加强锻炼，3个月后复查血脂', 1);

-- ---- 12. 同步排班剩余号源 ----
-- remain_num = total_num - 该排班下有效挂号数（已取消 status=0 不占号）
UPDATE `schedule` s SET s.`remain_num` = s.`total_num` - (
    SELECT COUNT(*) FROM `registration` r
     WHERE r.`schedule_id` = s.`id` AND r.`status` <> 0 AND r.`deleted` = 0
);

-- ---- 13. 药品库存快照（含 2 项低于预警阈值，用于展示库存预警）----
UPDATE `drug_stock` SET `current_stock` = 186 WHERE `drug_id` = 1;
UPDATE `drug_stock` SET `current_stock` = 12  WHERE `drug_id` = 2;   -- 低于阈值 15，触发预警
UPDATE `drug_stock` SET `current_stock` = 108 WHERE `drug_id` = 3;
UPDATE `drug_stock` SET `current_stock` = 62  WHERE `drug_id` = 4;
UPDATE `drug_stock` SET `current_stock` = 71  WHERE `drug_id` = 5;
UPDATE `drug_stock` SET `current_stock` = 84  WHERE `drug_id` = 6;
UPDATE `drug_stock` SET `current_stock` = 236 WHERE `drug_id` = 7;
UPDATE `drug_stock` SET `current_stock` = 205 WHERE `drug_id` = 8;
UPDATE `drug_stock` SET `current_stock` = 77  WHERE `drug_id` = 9;
UPDATE `drug_stock` SET `current_stock` = 428 WHERE `drug_id` = 10;
UPDATE `drug_stock` SET `current_stock` = 6   WHERE `drug_id` = 11;  -- 低于阈值 10，触发预警
UPDATE `drug_stock` SET `current_stock` = 74  WHERE `drug_id` = 12;

-- ============================================================
-- 执行完成后请清空 Redis 排班号源缓存：
--   docker exec clinic-redis redis-cli FLUSHDB
-- ============================================================
