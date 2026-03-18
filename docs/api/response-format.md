# 统一响应格式

后端业务接口统一返回 `ApiResponse<T>`，结构如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-03-18T09:30:00Z",
  "requestId": "req_a1b2c3d4"
}
```

## 字段说明

- `code`: 业务状态码，成功固定为 `200`，失败参考 `ResponseStatus` 枚举。
- `message`: 响应消息，前端可直接展示。
- `data`: 业务数据，无数据时为 `null`。
- `timestamp`: 响应时间（UTC，ISO-8601）。
- `requestId`: 请求追踪 ID。

## 分页数据格式

分页接口统一返回 `ApiResponse<PageResult<T>>`，`data` 内部结构为：

```json
{
  "list": [],
  "total": 0,
  "page": 1,
  "size": 20,
  "totalPages": 0
}
```

## 前端读取约定

- 统一先判断 `response.code === 200`。
- 成功后读取业务数据 `response.data`。
- 分页场景读取 `response.data.list` 等分页字段，不再做多层解包兼容。

## 已覆盖接口范围

- 公开接口：短链创建、短链统计、短链重定向、前端错误日志上报
- 认证接口：登录、注册、用户名可用性检查、刷新令牌、退出登录、令牌校验、当前用户
- 管理接口：用户管理、短链管理、操作日志、仪表盘概览
