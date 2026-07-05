-- ============================================
-- 社区诊所门诊诊疗管理系统 数据库初始化脚本
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
