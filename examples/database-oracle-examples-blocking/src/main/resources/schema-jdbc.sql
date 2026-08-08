-- Aryee Database JDBC 模式示例表结构 - Oracle
-- 使用 PL/SQL 块处理 DROP TABLE（Oracle 不支持 IF EXISTS 语法）

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE aryee_test_user';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

CREATE TABLE aryee_test_user (
    id NUMBER(20) NOT NULL,
    tenant_id VARCHAR2(64) DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR2(64) DEFAULT NULL,
    created_by_name VARCHAR2(64) DEFAULT NULL,
    updater VARCHAR2(64) DEFAULT NULL,
    updated_by_name VARCHAR2(64) DEFAULT NULL,
    deleted NUMBER(1) DEFAULT 0,
    version NUMBER(10) DEFAULT 0,
    remark VARCHAR2(512) DEFAULT NULL,
    ext CLOB DEFAULT NULL,
    username VARCHAR2(64) NOT NULL,
    email VARCHAR2(128) DEFAULT NULL,
    age NUMBER(10) DEFAULT NULL,
    status NUMBER(10) DEFAULT 1,
    PRIMARY KEY (id)
)
/

COMMENT ON TABLE aryee_test_user IS '示例用户表'
/

COMMENT ON COLUMN aryee_test_user.id IS '主键ID（雪花算法生成）'
/

COMMENT ON COLUMN aryee_test_user.username IS '用户名'
/

COMMENT ON COLUMN aryee_test_user.email IS '邮箱'
/

COMMENT ON COLUMN aryee_test_user.age IS '年龄'
/

COMMENT ON COLUMN aryee_test_user.status IS '状态（1启用 0禁用）'
/
