DROP TABLE IF EXISTS reminder;
CREATE TABLE reminder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    consultation_id BIGINT NOT NULL COMMENT '就诊ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    remind_date DATE NOT NULL COMMENT '应复诊日期',
    content VARCHAR(500) NOT NULL COMMENT '提醒内容',
    status TINYINT DEFAULT 0 COMMENT '0待提醒 1已提醒 2已复诊',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_remind_date (remind_date),
    INDEX idx_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复诊提醒';
