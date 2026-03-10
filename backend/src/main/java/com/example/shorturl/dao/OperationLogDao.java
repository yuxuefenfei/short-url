package com.example.shorturl.dao;

import com.example.shorturl.model.entity.UserOperationLog;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户操作日志数据访问对象
 * <p>
 * 模块职责：
 * - 提供操作日志表的数据库操作接口
 * - 支持操作审计和安全监控
 * - 提供日志查询和分析功能
 * <p>
 * 设计考虑：
 * - 支持大数据量表操作
 * - 异步写入提高性能
 * - 按时间分区优化查询
 * <p>
 * 使用场景：
 * - 记录用户管理操作
 * - 安全审计和合规检查
 * - 故障排查和问题追踪
 * <p>
 * 性能优化：
 * - 批量插入支持
 * - 索引优化查询
 * - 数据归档机制
 * <p>
 * 依赖关系：
 * - 被OperationLogService使用
 * - 操作UserOperationLog实体
 * - AOP切面自动调用
 */
@Mapper
public interface OperationLogDao extends BaseMapper<UserOperationLog> {

}