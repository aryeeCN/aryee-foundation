-- Aryee Database JDBC 模式示例表结构
-- 用于 database-examples-blocking JDBC 模式测试

DROP TABLE IF EXISTS aryee_test_user;

CREATE TABLE aryee_test_user (
    id BIGINT NOT NULL COMMENT '主键ID（雪花算法生成）',
    tenant_id VARCHAR(64) DEFAULT NULL COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator VARCHAR(64) DEFAULT NULL COMMENT '创建人ID',
    created_by_name VARCHAR(64) DEFAULT NULL COMMENT '创建人姓名',
    updater VARCHAR(64) DEFAULT NULL COMMENT '更新人ID',
    updated_by_name VARCHAR(64) DEFAULT NULL COMMENT '更新人姓名',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记（0：正常；1：已删除）',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    ext TEXT DEFAULT NULL COMMENT '扩展字段（JSON格式）',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    age INT DEFAULT NULL COMMENT '年龄',
    status INT DEFAULT 1 COMMENT '状态（1启用 0禁用）',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='示例用户表';
