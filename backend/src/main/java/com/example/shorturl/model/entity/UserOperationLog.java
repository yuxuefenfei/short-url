package com.example.shorturl.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户操作日志实体类
 * <p>
 * 模块职责：
 * - 对应数据库user_operation_log表
 * - 记录用户的所有管理操作
 * - 支持安全审计和故障排查
 * <p>
 * 表结构说明：
 * - user_id: 关联用户ID
 * - operation_type: 操作类型
 * - module: 操作模块
 * - 记录完整的请求响应信息
 * - 支持成功/失败状态追踪
 * <p>
 * 依赖关系：
 * - 被OperationLogService使用
 * - 与User实体关联
 * - 支持AOP切面自动记录
 */
@Data
@Table("user_operation_log")
public class UserOperationLog {

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户ID
     */
    @Column("user_id")
    private Long userId;

    /**
     * 操作类型（CREATE/UPDATE/DELETE/LOGIN/LOGOUT等）
     */
    @Column("operation_type")
    private String operationType;

    /**
     * 操作描述
     */
    @Column("operation_desc")
    private String operationDesc;

    /**
     * 操作模块（URL_MANAGEMENT/USER_MANAGEMENT等）
     */
    private String module;

    /**
     * 操作IP地址
     */
    @Column("ip_address")
    private String ipAddress;

    /**
     * 用户浏览器信息
     */
    @Column("user_agent")
    private String userAgent;

    /**
     * 请求路径
     */
    @Column("request_path")
    private String requestPath;

    /**
     * 请求方法（GET/POST/PUT/DELETE）
     */
    @Column("request_method")
    private String requestMethod;

    /**
     * 请求参数（JSON格式）
     */
    @Column("request_params")
    private String requestParams;

    /**
     * 响应结果（JSON格式）
     */
    @Column("response_result")
    private String responseResult;

    /**
     * 操作状态：1成功，0失败
     */
    private Integer status;

    /**
     * 错误信息
     */
    @Column("error_message")
    private String errorMessage;

    /**
     * 操作时间
     */
    @Column("operation_time")
    private LocalDateTime operationTime;

    // ==================== 构造函数 ====================

    public UserOperationLog() {
        this.status = 1; // 默认成功
        this.operationTime = LocalDateTime.now();
    }

    public UserOperationLog(Long userId, String operationType, String module, String operationDesc) {
        this();
        this.userId = userId;
        this.operationType = operationType;
        this.module = module;
        this.operationDesc = operationDesc;
    }

    // ==================== 业务方法 ====================

    /**
     * 标记操作成功
     */
    public void markSuccess(String responseResult) {
        this.status = 1;
        this.responseResult = responseResult;
        this.errorMessage = null;
    }

    /**
     * 标记操作失败
     */
    public void markFailure(String errorMessage) {
        this.status = 0;
        this.errorMessage = errorMessage;
        this.responseResult = null;
    }

    /**
     * 判断操作是否成功
     */
    public boolean isSuccess() {
        return status != null && status == 1;
    }

    /**
     * 判断操作是否失败
     */
    public boolean isFailure() {
        return !isSuccess();
    }

    /**
     * 获取操作类型显示名称
     */
    public String getOperationTypeDisplayName() {
        return switch (operationType) {
            case "CREATE" -> "创建";
            case "UPDATE" -> "更新";
            case "DELETE" -> "删除";
            case "LOGIN" -> "登录";
            case "LOGOUT" -> "登出";
            case "VIEW" -> "查看";
            case "EXPORT" -> "导出";
            case "IMPORT" -> "导入";
            default -> operationType;
        };
    }

    /**
     * 获取模块显示名称
     */
    public String getModuleDisplayName() {
        return switch (module) {
            case "URL_MANAGEMENT" -> "短网址管理";
            case "USER_MANAGEMENT" -> "用户管理";
            case "SYSTEM_CONFIG" -> "系统配置";
            case "STATISTICS" -> "数据统计";
            case "AUTH" -> "认证授权";
            default -> module;
        };
    }

    /**
     * 获取风险等级
     */
    public String getRiskLevel() {
        if ("DELETE".equals(operationType) || "UPDATE".equals(operationType)) {
            return "HIGH";
        } else if ("CREATE".equals(operationType)) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}