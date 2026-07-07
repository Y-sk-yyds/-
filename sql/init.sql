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
-- 排班种子数据（今明两天，两位医生，上午+下午）
-- ============================================
INSERT INTO `schedule` (`doctor_id`, `work_date`, `period`, `total_num`, `remain_num`, `status`) VALUES
-- 张医生(doctor_id=1) 今明两天
(1, CURDATE(),           'AM', 30, 30, 1),
(1, CURDATE(),           'PM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'AM', 30, 30, 1),
(1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PM', 30, 30, 1),
-- 李医生(doctor_id=2) 今明两天
(2, CURDATE(),           'AM', 30, 30, 1),
(2, CURDATE(),           'PM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'AM', 30, 30, 1),
(2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PM', 30, 30, 1);

-- ============================================
-- 药品种子数据
-- ============================================
INSERT INTO `drug` (`name`, `specification`, `unit`, `unit_price`, `category`) VALUES
('阿莫西林胶囊',    '0.5g×24粒',  '盒', 18.50, '抗生素'),
('布洛芬缓释胶囊',   '0.3g×20粒',  '盒', 15.00, '解热镇痛'),
('蒙脱石散',         '3g×10袋',    '盒', 22.00, '消化系统'),
('复方氨酚烷胺片',   '12片',       '盒', 8.50,  '感冒用药'),
('氯雷他定片',       '10mg×6片',   '盒', 12.00, '抗过敏'),
('维生素C片',        '100mg×100片', '瓶', 9.90,  '维生素类');

INSERT INTO `drug_stock` (`drug_id`, `current_stock`, `warning_threshold`) VALUES
(1, 200, 20),
(2, 150, 15),
(3, 80,  10),
(4, 300, 25),
(5, 100, 10),
(6, 500, 30);
