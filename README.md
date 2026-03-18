# 短网址管理系统

## 项目简介

这是一个完整的短网址管理系统，包含前端、后端和配套文档，支持：

- 长链接转短链接
- 短链跳转与访问统计
- 用户认证与后台管理
- 短链管理、用户管理、操作日志
- 统一接口响应格式与分页结构

当前仓库已经统一采用 `ApiResponse` 作为后端业务接口返回包裹，前端统一按 `response.code` 和 `response.data` 读取数据。

## 技术栈

### 前端

- Vue 3
- Vite
- Ant Design Vue 4
- Pinia
- Vue Router
- Axios
- ECharts

### 后端

- Java 17
- Spring Boot 3
- MyBatis-Flex
- MySQL 8
- Redis 6+
- Maven
- Spring Security

## 当前功能

### 公开端

- 创建短链：`POST /api/shorten`
- 查询短链公开统计：`GET /api/stats/{shortKey}`
- 短链跳转：`GET /{shortKey}`

### 认证

- 登录：`POST /api/auth/login`
- 注册：`POST /api/auth/register`
- 刷新 Token：`POST /api/auth/refresh-token`

### 管理后台

- 用户分页、创建、详情、更新、状态修改、密码重置
- 短链分页、详情、编辑、状态修改、删除、批量操作
- 系统统计概览
- 数据看板概览
- 操作日志分页与统计

## 统一响应格式

业务接口统一返回：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-03-16T10:00:00Z",
  "requestId": "req_a1b2c3d4"
}
```

分页接口统一放在：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [],
    "total": 0,
    "page": 1,
    "size": 20,
    "totalPages": 0
  }
}
```

更详细的说明见：

- [统一响应格式](C:/Users/13080/Workspace/short-url/docs/api/response-format.md)
- [OpenAPI 规范](C:/Users/13080/Workspace/short-url/docs/api/api-spec.yaml)

## 项目结构

```text
short-url/
├─ frontend/   前端项目
├─ backend/    后端项目
├─ docs/       接口、数据库、开发文档
└─ README.md
```

## 本地启动

### 后端

```bash
cd backend
mvn spring-boot:run
```

或：

```bash
cd backend
mvn -DskipTests package
java -jar target/short-url-*.jar
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

### 生产构建

```bash
cd frontend
npm run build
```

```bash
cd backend
mvn test
mvn -DskipTests package
```

## 文档索引

- [接口规范](C:/Users/13080/Workspace/short-url/docs/api/api-spec.yaml)
- [统一响应格式](C:/Users/13080/Workspace/short-url/docs/api/response-format.md)
- [数据库数据字典](C:/Users/13080/Workspace/short-url/docs/database/data-dictionary.md)
- [数据库建表脚本](C:/Users/13080/Workspace/short-url/docs/database/schema.sql)

## 当前状态

当前前后端已完成以下统一：

- 前端统一消费 `response.code / response.data`
- 后端统一返回 `ApiResponse`
- 分页统一返回 `PageResult`
- 管理后台看板、操作日志、公开统计已接入真实数据
- 文档已同步到当前接口契约
