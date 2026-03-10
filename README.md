# 短网址管理系统

## 项目概述

短网址管理系统是一个用于生成、管理和统计短网址的Web应用系统。用户通过前端页面输入长网址，系统生成对应的短网址，用户可以通过短网址快速访问原始的长网址内容。

### 使用场景
- 部署在已有Nginx服务器的环境中
- 用户通过Web界面生成短网址
- 支持短网址访问统计和分析
- 提供管理后台进行数据管理

## 技术栈

### 前端技术栈
- **核心框架**: Vue 3 + Vite
- **UI组件库**: ant-design-vue 4.x
- **状态管理**: Pinia
- **路由管理**: Vue Router
- **HTTP客户端**: Axios
- **构建工具**: Vite
- **额外技术选择理由**:
  - Pinia：Vue 3官方推荐的状态管理方案，比Vuex更轻量、类型安全
  - Vite：现代化的构建工具，开发体验更好，构建速度更快

### 后端技术栈
- **核心框架**: Java 17 + Spring Boot 3.x
- **ORM框架**: MyBatis-Flex
- **数据库**: MySQL 8.x
- **缓存**: Redis 6.x
- **构建工具**: Maven
- **其他依赖**:
  - Lombok：简化Java代码
  - Spring Security：安全认证
  - Spring Data Redis：Redis集成
  - Logback：日志记录
  - Hutool：Java工具类库（选择理由：功能全面，减少重复造轮子）

## 功能特性

### 核心功能
- ✅ 长网址转短网址生成
- ✅ 短网址访问重定向
- ✅ 访问统计分析
- ✅ 用户友好的管理界面
- ✅ 响应式设计
- ✅ 用户认证系统（登录/注册/权限管理）
- ✅ 管理后台（短网址管理、用户管理、操作日志）
- ✅ 操作审计日志（完整的管理操作追踪）

### 高级特性
- 🔒 安全保护（JWT Token认证、BCrypt密码加密）
- 📊 访问统计（点击次数、访问时间、IP记录、来源分析）
- 📋 用户操作审计（完整的管理操作日志）
- ⚡ 高性能缓存（Redis缓存热点数据，7天过期策略）
- 🛡️ 安全防护（SQL注入防护、XSS防护、CSRF防护）
- 📝 完整日志记录（访问日志、操作日志、错误日志）
- 🔄 高并发支持（异步处理、连接池优化）

## 系统要求

### 功能要求
- 使用合理的短网址生成算法（Base62编码）
- 实现完整的访问统计分析
- 支持Redis缓存优化性能
- 实现Nginx访问限流
- 完整的日志记录系统
- 安全认证机制

### 技术要求
- 响应式布局，支持移动端访问
- 后端接口支持高并发场景
- 前端输入合法性验证
- 使用JDK 17新特性（Lambda、Stream API、var声明变量等）
- 完整的API文档和数据库设计

### 代码规范要求
- 所有方法必须有详细注释
- 重要文件需包含模块职责说明
- 遵循命名规范和代码格式
- 接口文档与代码实现保持一致
- 每次修改需更新CHANGELOG

## 项目结构

```
short-url/
├── frontend/                           # 前端项目(Vue 3 + Vite)
│   ├── src/
│   │   ├── api/                       # API接口定义
│   │   │   ├── auth.js                 # 认证相关接口
│   │   │   ├── url.js                  # 短网址相关接口
│   │   │   └── admin.js                # 管理后台接口
│   │   ├── assets/                    # 静态资源
│   │   │   ├── images/                 # 图片资源
│   │   │   └── styles/                 # 样式文件
│   │   │       └── global.css        # 全局样式
│   │   ├── components/                 # 通用组件
│   │   │   └── common/                # 基础通用组件
│   │   │       ├── LoadingMask.vue   # 加载遮罩组件
│   │   │       └── NotificationContainer.vue # 通知容器组件
│   │   ├── layouts/                    # 布局组件
│   │   │   └── AdminLayout.vue      # 管理后台布局
│   │   ├── pages/                     # 页面组件
│   │   │   ├── Home.vue              # 短网址生成页面
│   │   │   ├── Login.vue             # 登录页面
│   │   │   ├── Register.vue          # 注册页面
│   │   │   └── admin/                # 管理后台页面
│   │   │       ├── Dashboard.vue    # 数据统计面板
│   │   │       ├── UrlManage.vue    # 短网址管理
│   │   │       ├── UserManage.vue   # 用户管理
│   │   │       └── OperationLogs.vue # 操作日志
│   │   ├── router/                    # 路由配置
│   │   │   └── index.js             # 路由定义
│   │   ├── stores/                    # Pinia状态管理
│   │   │   ├── userStore.js         # 用户状态
│   │   │   ├── urlStore.js          # 短网址状态
│   │   │   └── adminStore.js        # 管理后台状态
│   │   ├── utils/                     # 工具函数
│   │   │   ├── request.js            # HTTP请求封装
│   │   │   ├── validators.js         # 验证工具
│   │   │   ├── date.js               # 日期处理
│   │   │   └── storage.js            # 本地存储工具
│   │   ├── App.vue                    # 根组件
│   │   └── main.js                    # 入口文件
│   ├── public/                       # 静态资源
│   │   └── favicon.ico              # 网站图标
│   ├── index.html                    # HTML模板
│   ├── package.json                  # 依赖配置
│   ├── vite.config.js               # Vite配置
│   └── README.md                    # 前端说明文档
│
├── backend/                           # 后端项目(Spring Boot)
│   ├── src/main/java/com/example/shorturl/
│   │   ├── config/                     # 配置类
│   │   │   ├── RedisConfig.java        # Redis配置
│   │   │   ├── SecurityConfig.java     # 安全配置
│   │   │   └── WebConfig.java         # Web配置
│   │   ├── controller/                # 控制器
│   │   │   ├── UrlController.java      # 短网址控制器
│   │   │   ├── AuthController.java     # 认证控制器
│   │   │   ├── AdminController.java    # 管理控制器
│   │   │   ├── AdminUrlController.java # 管理员URL控制器
│   │   │   ├── RedirectController.java # 重定向控制器
│   │   │   └── HealthController.java   # 健康检查控制器
│   │   ├── service/                   # 业务逻辑
│   │   │   ├── UrlService.java        # 短网址服务
│   │   │   ├── UserService.java       # 用户服务
│   │   │   ├── AuthService.java       # 认证服务
│   │   │   ├── UserDetailsServiceImpl.java # Spring Security用户详情
│   │   │   └── AsyncLogService.java   # 异步日志服务
│   │   ├── dao/                       # 数据访问层
│   │   │   ├── UrlMappingDao.java     # 短网址DAO
│   │   │   ├── AccessLogDao.java      # 访问日志DAO
│   │   │   ├── UserDao.java           # 用户DAO
│   │   │   └── OperationLogDao.java   # 操作日志DAO
│   │   ├── model/                     # 数据模型
│   │   │   ├── entity/                 # 实体类
│   │   │   │   ├── ShortUrlMapping.java
│   │   │   │   ├── UrlAccessLog.java
│   │   │   │   ├── User.java
│   │   │   │   └── UserOperationLog.java
│   │   │   └── dto/                   # 数据传输对象
│   │   │       ├── LoginRequest.java
│   │   │       └── RegisterRequest.java
│   │   ├── common/                   # 公共组件
│   │   │   ├── annotation/            # 自定义注解
│   │   │   │   ├── RequiresLog.java
│   │   │   │   └── RequiresPermission.java
│   │   │   ├── aspect/                # AOP切面
│   │   │   │   ├── OperationLogAspect.java
│   │   │   │   └── PermissionAspect.java
│   │   │   ├── exception/             # 异常处理
│   │   │   │   ├── BusinessException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── response/              # 统一响应封装
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── PageResult.java
│   │   │   │   └── ResponseStatus.java
│   │   │   ├── utils/                 # 工具类
│   │   │   │   ├── ShortUrlGenerator.java # 短网址生成器
│   │   │   │   ├── JsonUtils.java
│   │   │   │   └── DateUtils.java
│   │   │   └── constants/            # 常量定义
│   │   │       ├── ApiConstants.java
│   │   │       └── SystemConstants.java
│   │   └── ShortUrlApplication.java   # 应用启动类
│   │
│   ├── src/main/resources/
│   │   ├── mapper/                     # MyBatis映射文件（可选）
│   │   ├── application.yml            # 应用配置
│   │   ├── application-dev.yml        # 开发环境配置
│   │   └── application-prod.yml       # 生产环境配置
│   │
│   ├── pom.xml                         # Maven依赖配置
│   └── README.md                       # 后端说明文档
│
├── docs/                            # 技术文档
│   ├── api/                         # API文档
│   │   └── api-spec.yaml            # OpenAPI规范
│   ├── database/                    # 数据库文档
│   │   ├── schema.sql              # 数据库建表脚本
│   │   └── data-dictionary.md      # 数据字典
│   └── deployment/                 # 部署文档
│       ├── deploy-guide.md        # 部署指南
│       └── nginx-config.conf      # Nginx配置示例
│
├── CHANGELOG                        # 变更日志
├── README.md                        # 项目主文档
└── LICENSE                          # 开源协议
```

## MyBatis-Flex 配置说明

### ORM框架特性
- **零XML配置**：MyBatis-Flex 支持纯Java代码配置，大部分CRUD操作无需XML文件
- **智能代码生成**：提供代码生成器，自动生成Entity、Mapper、Service等代码
- **Lambda表达式**：支持Lambda表达式编写查询条件，类型安全
- **XML可选**：仅在需要编写复杂SQL时才需要创建对应的XML映射文件

### 配置方式
1. **基础配置**：在 `application.yml` 中配置数据源和MyBatis-Flex
2. **Mapper接口**：创建Mapper接口继承BaseMapper，无需XML即可实现基础CRUD
3. **复杂SQL**：仅在需要复杂查询时，才在 `resources/mapper/` 目录下创建对应的XML文件

## 开发规范

### 文档规范
- 数据库表设计需包含表名、字段名、字段类型、约束说明
- 接口文档需包含路径、方法、参数、响应、错误码
- 功能说明需包含用途、输入输出、调用示例、注意事项

### 文档创建指南
详细的Markdown文档创建方法和最佳实践，请参考 [docs/CREATION_GUIDE.md](docs/CREATION_GUIDE.md)

### 代码注释规范
重要文件开头需包含：
- 模块职责和设计思路
- 主要函数/类的用途
- 与其他模块的依赖关系
- 重要的历史决策原因

### 版本管理
每次修改需生成CHANGELOG，包含：
- 修改时间
- 新增/修改内容
- 改动原因
- 影响文件

## 部署环境

- 服务器：已部署Nginx
- 数据库：MySQL 8.x
- 缓存：Redis 6.x
- Java：JDK 17
- Node.js：20.x+

---

## 数据库设计

### 核心表结构

#### 1. 短网址映射表 (short_url_mapping)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| short_key | VARCHAR(20) | 短网址key | UNIQUE KEY |
| original_url | VARCHAR(2048) | 原始长网址 | NOT NULL |
| title | VARCHAR(255) | 网址标题 | 可选 |
| click_count | BIGINT | 点击次数 | DEFAULT 0 |
| status | TINYINT | 状态(1正常/0禁用) | DEFAULT 1 |
| created_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| expired_time | DATETIME | 过期时间 | 可选，NULL表示永不过期 |

#### 2. 访问日志表 (url_access_log)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| short_key | VARCHAR(20) | 短网址key | INDEX |
| user_agent | TEXT | 用户浏览器信息 | 可选 |
| ip_address | VARCHAR(45) | 访问者IP | INDEX |
| access_time | DATETIME | 访问时间 | DEFAULT CURRENT_TIMESTAMP, INDEX |

#### 3. 用户表 (user)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| username | VARCHAR(50) | 用户名 | UNIQUE KEY |
| password | VARCHAR(255) | 加密密码 | NOT NULL |
| email | VARCHAR(100) | 邮箱 | 可选 |
| role | VARCHAR(20) | 角色(USER/ADMIN) | DEFAULT 'USER' |
| status | TINYINT | 状态(1正常/0禁用) | DEFAULT 1 |
| last_login_time | DATETIME | 最后登录时间 | 可选 |
| created_time | DATETIME | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| updated_time | DATETIME | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |

#### 4. 用户操作日志表 (user_operation_log)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY KEY, AUTO_INCREMENT |
| user_id | BIGINT | 用户ID | INDEX, 外键关联user表 |
| operation_type | VARCHAR(50) | 操作类型 | NOT NULL |
| operation_desc | VARCHAR(500) | 操作描述 | 可选 |
| module | VARCHAR(50) | 操作模块 | NOT NULL |
| ip_address | VARCHAR(45) | 操作IP地址 | 可选 |
| user_agent | TEXT | 用户浏览器信息 | 可选 |
| request_path | VARCHAR(500) | 请求路径 | 可选 |
| request_method | VARCHAR(10) | 请求方法 | 可选 |
| request_params | TEXT | 请求参数 | 可选 |
| response_result | TEXT | 响应结果 | 可选 |
| status | TINYINT | 操作状态(1成功/0失败) | DEFAULT 1 |
| error_message | VARCHAR(1000) | 错误信息 | 可选 |
| operation_time | DATETIME | 操作时间 | DEFAULT CURRENT_TIMESTAMP, INDEX |

## 统一响应和异常处理

### 📦 统一响应封装

#### ApiResponse 类设计
```java
public class ApiResponse<T> {
    private int code;           // 响应状态码
    private String message;     // 响应消息
    private T data;            // 响应数据
    private String timestamp;   // 响应时间(ISO8601)
    private String requestId;   // 请求ID（用于链路追踪）
}
```

#### 响应状态码规范
- **200**: 成功
- **400**: 请求参数错误
- **401**: 未授权/Token失效
- **403**: 禁止访问/权限不足
- **404**: 资源不存在
- **409**: 资源冲突
- **429**: 请求频率超限
- **500**: 服务器内部错误
- **503**: 服务暂时不可用

#### 业务错误码（4001-4999）
- **4001**: URL格式不正确
- **4002**: URL长度超限
- **4003**: 短网址已存在
- **4004**: 短网址不存在
- **4005**: 短网址已过期
- **4006**: 短网址已被禁用
- **4007**: 用户不存在
- **4008**: 密码错误
- **4009**: 用户名已存在
- **4010**: 操作频率超限

### 🛡️ 全局异常处理

#### GlobalExceptionHandler 设计
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 业务异常处理
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    // 参数验证异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.error(400, "参数验证失败: " + errorMsg);
    }

    // 认证授权异常
    @ExceptionHandler(AuthenticationException.class)
    public ApiResponse<?> handleAuthException(AuthenticationException e) {
        return ApiResponse.error(401, "认证失败: " + e.getMessage());
    }

    // 系统异常兜底处理
    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleSystemException(Exception e) {
        log.error("System exception: ", e);
        return ApiResponse.error(500, "服务器内部错误");
    }
}
```

#### 异常处理策略
- **业务异常**：返回具体的业务错误码和用户友好的消息
- **参数验证异常**：返回详细的字段验证错误信息
- **认证授权异常**：返回权限相关的标准错误信息
- **系统异常**：记录详细日志，返回通用错误信息避免信息泄露

## API接口设计

### 🔗 核心接口

#### 1. 短网址生成接口
- **路径**: `POST /api/shorten`
- **请求参数**:
  ```json
  {
    "originalUrl": "https://example.com/very/long/url",
    "title": "示例网址",
    "expiredAt": "2024-12-31 23:59:59"
  }
  ```
- **成功响应**:
  ```json
  {
    "code": 200,
    "message": "短网址生成成功",
    "data": {
      "shortUrl": "https://short.ly/abc123",
      "shortKey": "abc123",
      "originalUrl": "https://example.com/very/long/url",
      "title": "示例网址",
      "createdTime": "2024-01-01 10:00:00"
    },
    "timestamp": "2024-01-01T10:00:00Z",
    "requestId": "req_abc123"
  }
  ```

#### 2. 短网址访问接口
- **路径**: `GET /{shortKey}`
- **功能**: 重定向到原始URL并记录访问日志
- **响应**: HTTP 302 重定向
- **错误响应**:
  ```json
  {
    "code": 4004,
    "message": "短网址不存在",
    "data": null,
    "timestamp": "2024-01-01T10:00:00Z",
    "requestId": "req_def456"
  }
  ```

#### 3. 统计分析接口
- **路径**: `GET /api/stats/{shortKey}`
- **成功响应**:
  ```json
  {
    "code": 200,
    "message": "查询成功",
    "data": {
      "totalClicks": 1234,
      "todayClicks": 56,
      "createdTime": "2024-01-01 10:00:00",
      "lastAccessTime": "2024-01-15 14:30:00",
      "topReferers": [
        {"referer": "google.com", "count": 500},
        {"referer": "baidu.com", "count": 300}
      ]
    },
    "timestamp": "2024-01-01T10:00:00Z",
    "requestId": "req_ghi789"
  }
  ```

### 🔐 管理接口

#### 1. 认证接口
- **用户登录** `POST /api/auth/login`
  ```json
  // 请求
  {"username": "admin", "password": "password"}

  // 响应
  {
    "code": 200,
    "message": "登录成功",
    "data": {
      "token": "jwt_token_string",
      "refreshToken": "refresh_token_string",
      "userInfo": {
        "id": 1,
        "username": "admin",
        "role": "ADMIN"
      }
    },
    "timestamp": "2024-01-01T10:00:00Z",
    "requestId": "req_jkl012"
  }
  ```

#### 2. 短网址管理
- **获取列表** `GET /api/admin/urls?page=1&size=20&status=1`
  ```json
  {
    "code": 200,
    "message": "查询成功",
    "data": {
      "list": [
        {
          "id": 1,
          "shortKey": "abc123",
          "originalUrl": "https://example.com/url",
          "clickCount": 100,
          "status": 1,
          "createdTime": "2024-01-01 10:00:00"
        }
      ],
      "total": 100,
      "page": 1,
      "size": 20
    },
    "timestamp": "2024-01-01T10:00:00Z",
    "requestId": "req_mno345"
  }
  ```

#### 3. 操作日志查询
- **日志列表** `GET /api/admin/operation-logs?page=1&size=20`
  ```json
  {
    "code": 200,
    "message": "查询成功",
    "data": {
      "list": [
        {
          "id": 1,
          "userId": 1,
          "username": "admin",
          "operationType": "CREATE",
          "operationDesc": "创建短网址",
          "module": "URL_MANAGEMENT",
          "ipAddress": "192.168.1.100",
          "status": 1,
          "operationTime": "2024-01-01 10:00:00"
        }
      ],
      "total": 50,
      "page": 1,
      "size": 20
    },
    "timestamp": "2024-01-01T10:00:00Z",
    "requestId": "req_pqr678"
  }
  ```

## 安全设计

### 1. 认证授权
- JWT Token认证
- 角色权限控制 (USER/ADMIN)
- 密码加密存储 (BCrypt)

### 2. 访问控制
- Nginx限流配置
- Redis缓存防刷
- IP黑名单机制

### 3. 数据安全
- SQL注入防护
- XSS攻击防护
- CSRF防护

## 性能优化

### 1. 缓存策略
- Redis缓存热点短网址映射
- 多级缓存架构
- 缓存预热机制

### 2. 数据库优化
- 合理的索引设计
- 操作日志表分区策略（按时间分区）
- 大表数据归档机制

### 3. 并发处理
- 异步日志记录
- 连接池配置
- 线程池优化

## 用户操作日志功能

### 🎯 设计目的
- **安全审计**：记录所有管理操作，便于追溯和审计
- **故障排查**：通过操作日志快速定位问题
- **行为分析**：分析管理员操作习惯，优化系统
- **合规要求**：满足数据安全和隐私保护的合规要求

### 📝 记录内容
- **基础信息**：用户ID、操作时间、IP地址
- **操作详情**：操作类型、模块、描述
- **请求信息**：请求路径、方法、参数
- **响应信息**：响应结果、状态、错误信息

### 🔧 技术实现
- **AOP切面**：通过Spring AOP统一记录操作日志
- **异步处理**：使用线程池异步写入，不影响主业务流程
- **数据脱敏**：敏感信息自动脱敏处理
- **日志分级**：根据操作重要性设置不同日志级别

### 📊 应用场景
1. **安全监控**：异常操作实时告警
2. **数据恢复**：误操作后的数据回溯
3. **性能分析**：慢操作识别和优化
4. **用户培训**：新管理员操作指导

## 部署架构

### 环境要求
- **服务器**: Linux + Nginx
- **Java**: JDK 17+
- **数据库**: MySQL 8.x
- **缓存**: Redis 6.x
- **构建工具**: Maven 3.x, Node.js 20.x+

### 部署流程
1. 数据库初始化和表结构创建
2. Redis缓存配置
3. Spring Boot应用部署
4. Vue前端构建和部署
5. Nginx反向代理配置

## 开发状态

### ✅ 已完成阶段

#### 第一阶段：环境准备和基础架构 ✅
- 后端：Spring Boot项目搭建，数据库配置，基础实体类创建
- 前端：Vue 3项目创建，组件库配置，基础页面结构

#### 第二阶段：核心功能开发 ✅
- 后端：短网址生成算法，映射存储，访问重定向，缓存集成
- 前端：生成页面，结果展示，表单验证，响应式布局

#### 第三阶段：高级功能开发 ✅
- 后端：统计分析，用户认证，安全防护，日志监控
- 前端：管理后台，数据图表，用户管理，系统设置

### 🚧 当前阶段：测试和优化
- 功能测试，性能优化，并发测试，安全测试

### ⏳ 待开始阶段：部署和上线
- 环境搭建，应用部署，监控配置，文档整理

## 技术难点解决方案

### 短网址生成
- 使用Base62编码确保短而唯一
- Redis原子操作避免冲突
- 结合时间戳和随机数

### 高并发处理
- Redis缓存热点数据
- 异步日志记录
- 数据库索引优化
- Nginx负载均衡

### 安全保障
- JWT认证机制
- 访问频率限制
- SQL注入防护
- XSS攻击防护

## 项目交付物
1. 完整源代码（前后端）
2. 数据库建表脚本
3. API接口文档
4. 部署运维文档
5. 用户使用手册
6. 测试报告

**重要提醒**：如有任何不确定的需求或技术选型疑问，请先提出问题，不要做强制假设。

有关项目的详细变更历史，请参考 [CHANGELOG](CHANGELOG) 文件。

## 🚀 快速开始

### 开发环境编译和启动

#### 后端编译和启动
```bash
# 进入后端目录
cd backend

# 方法1: 使用Maven直接运行（开发模式）
mvn clean compile
mvn spring-boot:run

# 方法2: 打包后运行
mvn clean package -DskipTests
java -jar target/short-url-*.jar

# 方法3: 使用Docker运行
docker build -t short-url-backend .
docker run -p 8080:8080 short-url-backend
```

#### 前端编译和启动
```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 生产构建
npm run build

# 预览生产构建
npm run preview
```

#### 全项目Docker Compose启动
```bash
# 在项目根目录
cd short-url

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 停止服务
docker-compose down
```

### 环境要求
- **Java**: JDK 17 (必需)
- **Maven**: 3.6+
- **Node.js**: 20.x+
- **MySQL**: 8.0+
- **Redis**: 6.2+
- **操作系统**: Windows/Linux/MacOS

### 常见问题解决

#### Java版本问题
如果遇到Java版本错误，请确保使用JDK 17：
```bash
# 检查Java版本
java -version

# 需要输出类似：
# openjdk version "17.0.8" 2023-07-18
```

#### 启动异常处理
如果应用启动失败，请检查：
1. MySQL和Redis服务是否正常运行
2. 数据库连接配置是否正确
3. 端口8080是否被占用
4. 查看logs目录下的日志文件

## 🔗 相关文档

### 📖 用户使用手册
- [前端使用说明](frontend/README.md) - 详细的前端功能使用说明

### 🛠️ 部署运维文档
- [后端部署指南](backend/README.md) - 完整的后端部署和运维指南
- [Nginx配置](docs/deployment/nginx-config.conf) - 生产环境Nginx配置示例

### 📋 开发文档
- [API文档](docs/api/api-spec.yaml) - OpenAPI接口规范文档
- [数据库设计](docs/database/) - 数据库表


