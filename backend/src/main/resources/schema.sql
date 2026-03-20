-- 短网址管理系统数据库建表脚本
-- H2 Database

-- =============================================
-- 1. 短网址映射表 (short_url_mapping)
-- =============================================
CREATE TABLE IF NOT EXISTS "short_url_mapping" (
  "id" BIGINT NOT NULL AUTO_INCREMENT,
  "short_key" VARCHAR(20) NOT NULL,
  "original_url" VARCHAR(2048) NOT NULL,
  "title" VARCHAR(255),
  "click_count" BIGINT NOT NULL DEFAULT 0,
  "status" TINYINT NOT NULL DEFAULT 1,
  "created_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "expired_time" TIMESTAMP,
  "updated_time" TIMESTAMP,
  PRIMARY KEY ("id"),
  UNIQUE ("short_key")
);

CREATE INDEX IF NOT EXISTS "idx_short_url_mapping_created_time" ON "short_url_mapping"("created_time");
CREATE INDEX IF NOT EXISTS "idx_short_url_mapping_status" ON "short_url_mapping"("status");

-- =============================================
-- 2. 访问日志表 (url_access_log)
-- =============================================
CREATE TABLE IF NOT EXISTS "url_access_log" (
  "id" BIGINT NOT NULL AUTO_INCREMENT,
  "short_key" VARCHAR(20) NOT NULL,
  "user_agent" TEXT,
  "ip_address" VARCHAR(45),
  "access_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE INDEX IF NOT EXISTS "idx_url_access_log_short_key" ON "url_access_log"("short_key");
CREATE INDEX IF NOT EXISTS "idx_url_access_log_access_time" ON "url_access_log"("access_time");
CREATE INDEX IF NOT EXISTS "idx_url_access_log_ip_address" ON "url_access_log"("ip_address");

-- =============================================
-- 3. 用户表 (user)
-- =============================================
CREATE TABLE IF NOT EXISTS "user" (
  "id" BIGINT NOT NULL AUTO_INCREMENT,
  "username" VARCHAR(50) NOT NULL,
  "password" VARCHAR(255) NOT NULL,
  "email" VARCHAR(100),
  "role" VARCHAR(20) NOT NULL DEFAULT 'USER',
  "status" TINYINT NOT NULL DEFAULT 1,
  "last_login_time" TIMESTAMP,
  "created_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  UNIQUE ("username")
);

CREATE INDEX IF NOT EXISTS "idx_user_role" ON "user"("role");
CREATE INDEX IF NOT EXISTS "idx_user_status" ON "user"("status");
CREATE INDEX IF NOT EXISTS "idx_user_created_time" ON "user"("created_time");

-- =============================================
-- 4. 用户操作日志表 (user_operation_log)
-- =============================================
CREATE TABLE IF NOT EXISTS "user_operation_log" (
  "id" BIGINT NOT NULL AUTO_INCREMENT,
  "user_id" BIGINT NOT NULL,
  "operation_type" VARCHAR(50) NOT NULL,
  "operation_desc" VARCHAR(500),
  "module" VARCHAR(50) NOT NULL,
  "ip_address" VARCHAR(45),
  "user_agent" TEXT,
  "request_path" VARCHAR(500),
  "request_method" VARCHAR(10),
  "request_params" TEXT,
  "response_result" TEXT,
  "status" TINYINT NOT NULL DEFAULT 1,
  "error_message" VARCHAR(1000),
  "operation_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);

CREATE INDEX IF NOT EXISTS "idx_user_operation_log_user_id" ON "user_operation_log"("user_id");
CREATE INDEX IF NOT EXISTS "idx_user_operation_log_operation_time" ON "user_operation_log"("operation_time");
CREATE INDEX IF NOT EXISTS "idx_user_operation_log_operation_type" ON "user_operation_log"("operation_type");
CREATE INDEX IF NOT EXISTS "idx_user_operation_log_module" ON "user_operation_log"("module");

-- =============================================
-- 5. 初始化数据
-- =============================================

-- 创建默认管理员用户（密码：admin123，使用 BCrypt 加密）
INSERT INTO "user" ("username", "password", "email", "role", "status")
SELECT 'admin', '$2a$12$VkcbkzVi1EepDMsQwOxBm.8dAn2/zRwJYDJY0LarGn.cbSPrXXbBK', 'admin@example.com', 'ADMIN', 1
WHERE NOT EXISTS (SELECT 1 FROM "user" WHERE "username" = 'admin');
