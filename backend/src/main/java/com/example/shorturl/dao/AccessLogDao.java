package com.example.shorturl.dao;

import com.example.shorturl.model.entity.UrlAccessLog;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 访问日志数据访问对象
 * <p>
 * 模块职责：
 * - 提供访问日志表的数据库操作接口
 * - 支持访问日志的记录和查询
 * - 支持统计分析相关查询
 * <p>
 * 使用场景：
 * - 记录短网址访问日志
 * - 查询特定短网址的访问记录
 * - 统计分析访问数据
 * <p>
 * MyBatis-Flex特性：
 * - 零XML配置，自动CRUD
 * - Lambda表达式支持
 * - 高性能批量操作
 * <p>
 * 依赖关系：
 * - 被StatsService和UrlService使用
 * - 操作UrlAccessLog实体
 */
@Mapper
public interface AccessLogDao extends BaseMapper<UrlAccessLog> {

}