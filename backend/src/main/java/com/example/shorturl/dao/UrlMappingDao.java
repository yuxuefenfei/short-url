package com.example.shorturl.dao;

import com.example.shorturl.model.entity.ShortUrlMapping;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短网址映射数据访问对象
 * <p>
 * 模块职责：
 * - 提供短网址映射表的数据库操作接口
 * - 继承BaseMapper获得基础CRUD功能
 * - 支持自定义查询方法
 * <p>
 * MyBatis-Flex特性：
 * - 零XML配置，纯Java代码操作
 * - 支持Lambda表达式查询
 * - 自动生成基础CRUD方法
 * <p>
 * 依赖关系：
 * - 被UrlService注入使用
 * - 操作ShortUrlMapping实体
 */
@Mapper
public interface UrlMappingDao extends BaseMapper<ShortUrlMapping> {

}