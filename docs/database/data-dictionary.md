# 短网址管理系统数据字典

本文档描述当前项目核心数据库表结构，并与 [schema.sql](C:/Users/13080/Workspace/short-url/docs/database/schema.sql) 保持一致。

## 1. `short_url_mapping`

短链主表。

| 字段 | 类型 | 允许空 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 ID |
| `short_key` | `VARCHAR(20)` | 否 | - | 短链 Key，唯一 |
| `original_url` | `VARCHAR(2048)` | 否 | - | 原始长链接 |
| `title` | `VARCHAR(255)` | 是 | `NULL` | 标题 |
| `click_count` | `BIGINT UNSIGNED` | 否 | `0` | 累计点击数 |
| `status` | `TINYINT` | 否 | `1` | 状态：`1` 启用，`0` 禁用 |
| `created_time` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 创建时间 |
| `updated_time` | `DATETIME` | 否 | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |
| `expired_time` | `DATETIME` | 是 | `NULL` | 过期时间，`NULL` 表示不过期 |

索引：

- `PRIMARY KEY (id)`
- `UNIQUE KEY uk_short_key (short_key)`
- `KEY idx_created_time (created_time)`
- `KEY idx_status (status)`

## 2. `url_access_log`

短链访问日志表。

| 字段 | 类型 | 允许空 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 ID |
| `short_key` | `VARCHAR(20)` | 否 | - | 被访问的短链 Key |
| `user_agent` | `TEXT` | 是 | `NULL` | 用户代理 |
| `ip_address` | `VARCHAR(45)` | 是 | `NULL` | 客户端 IP |
| `access_time` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 访问时间 |

索引：

- `PRIMARY KEY (id)`
- `KEY idx_short_key (short_key)`
- `KEY idx_access_time (access_time)`
- `KEY idx_ip_address (ip_address)`

## 3. `user`

用户表。

| 字段 | 类型 | 允许空 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 ID |
| `username` | `VARCHAR(50)` | 否 | - | 用户名，唯一 |
| `password` | `VARCHAR(255)` | 否 | - | BCrypt 密码哈希 |
| `email` | `VARCHAR(100)` | 是 | `NULL` | 邮箱 |
| `role` | `VARCHAR(20)` | 否 | `USER` | 角色：`USER` / `ADMIN` |
| `status` | `TINYINT` | 否 | `1` | 状态：`1` 启用，`0` 禁用 |
| `last_login_time` | `DATETIME` | 是 | `NULL` | 最后登录时间 |
| `created_time` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 创建时间 |
| `updated_time` | `DATETIME` | 否 | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

索引：

- `PRIMARY KEY (id)`
- `UNIQUE KEY uk_username (username)`
- `KEY idx_role (role)`
- `KEY idx_status (status)`
- `KEY idx_created_time (created_time)`

## 4. `user_operation_log`

后台操作审计日志表。

| 字段 | 类型 | 允许空 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | 自增 | 主键 ID |
| `user_id` | `BIGINT UNSIGNED` | 否 | - | 操作用户 ID |
| `operation_type` | `VARCHAR(50)` | 否 | - | 操作类型，如 `CREATE` / `UPDATE` / `DELETE` / `QUERY` |
| `operation_desc` | `VARCHAR(500)` | 是 | `NULL` | 操作描述 |
| `module` | `VARCHAR(50)` | 否 | - | 模块名，如 `USER_MANAGEMENT` |
| `ip_address` | `VARCHAR(45)` | 是 | `NULL` | 操作 IP |
| `user_agent` | `TEXT` | 是 | `NULL` | 用户代理 |
| `request_path` | `VARCHAR(500)` | 是 | `NULL` | 请求路径 |
| `request_method` | `VARCHAR(10)` | 是 | `NULL` | 请求方法 |
| `request_params` | `TEXT` | 是 | `NULL` | 请求参数 |
| `response_result` | `TEXT` | 是 | `NULL` | 响应摘要 |
| `status` | `TINYINT` | 否 | `1` | 状态：`1` 成功，`0` 失败 |
| `error_message` | `VARCHAR(1000)` | 是 | `NULL` | 错误信息 |
| `operation_time` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 操作时间 |

索引：

- `PRIMARY KEY (id)`
- `KEY idx_user_id (user_id)`
- `KEY idx_operation_time (operation_time)`
- `KEY idx_operation_type (operation_type)`
- `KEY idx_module (module)`
- `CONSTRAINT fk_user_operation_log_user_id FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE`

## 字段取值约定

### 状态字段 `status`

- `1`：成功 / 启用
- `0`：失败 / 禁用

### 角色字段 `role`

- `ADMIN`：管理员
- `USER`：普通用户

### 常见操作类型 `operation_type`

- `CREATE`
- `UPDATE`
- `DELETE`
- `QUERY`
- `LOGIN`
- `LOGOUT`

### 常见模块 `module`

- `USER_MANAGEMENT`
- `URL_MANAGEMENT`
- `SYSTEM_MONITOR`
- `AUTH`
