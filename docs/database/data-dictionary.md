# 短网址管理系统数据字典

## 📋 表结构详细说明

### 1. 短网址映射表 (short_url_mapping)

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 自增 | 说明 |
|--------|----------|------|--------|--------|------|------|------|
| id | BIGINT UNSIGNED | 20 | NO | NULL | 是 | 是 | 主键ID |
| short_key | VARCHAR | 20 | NO | NULL | 否 | 否 | 短网址key，唯一索引 |
| original_url | VARCHAR | 2048 | NO | NULL | 否 | 否 | 原始长网址 |
| title | VARCHAR | 255 | YES | NULL | 否 | 否 | 网址标题，可选 |
| click_count | BIGINT UNSIGNED | 20 | NO | 0 | 否 | 否 | 点击次数统计 |
| status | TINYINT | 4 | NO | 1 | 否 | 否 | 状态：1正常，0禁用 |
| created_time | DATETIME | 19 | NO | CURRENT_TIMESTAMP | 否 | 否 | 创建时间 |
| expired_time | DATETIME | 19 | YES | NULL | 否 | 否 | 过期时间，NULL表示永不过期 |

**索引说明：**
- PRIMARY KEY (id): 主键索引
- UNIQUE KEY uk_short_key (short_key): 短网址key唯一索引
- KEY idx_created_time (created_time): 创建时间索引
- KEY idx_status (status): 状态索引

---

### 2. 访问日志表 (url_access_log)

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 自增 | 说明 |
|--------|----------|------|--------|--------|------|------|------|
| id | BIGINT UNSIGNED | 20 | NO | NULL | 是 | 是 | 主键ID |
| short_key | VARCHAR | 20 | NO | NULL | 否 | 否 | 短网址key |
| user_agent | TEXT | - | YES | NULL | 否 | 否 | 用户浏览器信息 |
| ip_address | VARCHAR | 45 | YES | NULL | 否 | 否 | 访问者IP地址 |
| access_time | DATETIME | 19 | NO | CURRENT_TIMESTAMP | 否 | 否 | 访问时间 |

---

### 3. 用户表 (user)

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 自增 | 说明 |
|--------|----------|------|--------|--------|------|------|------|
| id | BIGINT UNSIGNED | 20 | NO | NULL | 是 | 是 | 主键ID |
| username | VARCHAR | 50 | NO | NULL | 否 | 否 | 用户名，唯一索引 |
| password | VARCHAR | 255 | NO | NULL | 否 | 否 | BCrypt加密密码 |
| email | VARCHAR | 100 | YES | NULL | 否 | 否 | 邮箱地址 |
| role | VARCHAR | 20 | NO | USER | 否 | 否 | 角色：USER/ADMIN |
| status | TINYINT | 4 | NO | 1 | 否 | 否 | 状态：1正常，0禁用 |
| last_login_time | DATETIME | 19 | YES | NULL | 否 | 否 | 最后登录时间 |
| created_time | DATETIME | 19 | NO | CURRENT_TIMESTAMP | 否 | 否 | 创建时间 |
| updated_time | DATETIME | 19 | NO | CURRENT_TIMESTAMP | 否 | 否 | 更新时间 |

---

### 4. 用户操作日志表 (user_operation_log)

| 字段名 | 数据类型 | 长度 | 允许空 | 默认值 | 主键 | 自增 | 说明 |
|--------|----------|------|--------|--------|------|------|------|
| id | BIGINT UNSIGNED | 20 | NO | NULL | 是 | 是 | 主键ID |
| user_id | BIGINT UNSIGNED | 20 | NO | NULL | 否 | 否 | 用户ID，外键 |
| operation_type | VARCHAR | 50 | NO | NULL | 否 | 否 | 操作类型 |
| operation_desc | VARCHAR | 500 | YES | NULL | 否 | 否 | 操作描述 |
| module | VARCHAR | 50 | NO | NULL | 否 | 否 | 操作模块 |
| ip_address | VARCHAR | 45 | YES | NULL | 否 | 否 | 操作IP地址 |
| user_agent | TEXT | - | YES | NULL | 否 | 否 | 用户浏览器信息 |
| request_path | VARCHAR | 500 | YES | NULL | 否 | 否 | 请求路径 |
| request_method | VARCHAR | 10 | YES | NULL | 否 | 否 | 请求方法 |
| request_params | TEXT | - | YES | NULL | 否 | 否 | 请求参数 |
| response_result | TEXT | - | YES | NULL | 否 | 否 | 响应结果 |
| status | TINYINT | 4 | NO | 1 | 否 | 否 | 操作状态：1成功，0失败 |
| error_message | VARCHAR | 1000 | YES | NULL | 否 | 否 | 错误信息 |
| operation_time | DATETIME | 19 | NO | CURRENT_TIMESTAMP | 否 | 否 | 操作时间 |

## 🔧 字段枚举值说明

### 状态字段 (status)
- 0: 禁用/失败
- 1: 正常/成功

### 用户角色 (role)
- USER: 普通用户
- ADMIN: 管理员

## 📊 数据库设计特点

### 1. 性能优化
- 合理的索引设计
- 使用DATETIME类型
- 分区表设计支持大数据量
- 外键约束确保数据完整性

### 2. 安全性
- 密码BCrypt加密存储
- 操作日志完整记录
- IP地址和User-Agent记录

### 3. 扩展性
- 软删除设计
- 状态字段设计
- 预留字段支持功能扩展
- 分区和归档机制
