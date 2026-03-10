package com.example.shorturl.dao;

import com.example.shorturl.model.entity.User;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问对象
 * <p>
 * 模块职责：
 * - 提供用户表的数据库操作接口
 * - 支持用户认证和管理相关操作
 * - 提供安全的用户数据访问
 * <p>
 * 安全考虑：
 * - 密码字段加密存储
 * - 支持账户状态管理
 * - 记录最后登录时间
 * <p>
 * 使用场景：
 * - 用户注册和登录验证
 * - 用户信息管理
 * - 权限控制
 * <p>
 * MyBatis-Flex优势：
 * - 类型安全的Lambda查询
 * - 自动防止SQL注入
 * - 简化数据库操作
 * <p>
 * 依赖关系：
 * - 被UserService和AuthService使用
 * - 操作User实体
 */
@Mapper
public interface UserDao extends BaseMapper<User> {

}