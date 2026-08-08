-- R2DBC 测试表结构（启动时自动创建）
DROP TABLE IF EXISTS aryee_test_user_r2dbc;
CREATE TABLE aryee_test_user_r2dbc (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    email VARCHAR(128) COMMENT '邮箱',
    age INT COMMENT '年龄',
    status INT COMMENT '状态',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    version BIGINT COMMENT '乐观锁版本号',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Aryee 测试用户表';
