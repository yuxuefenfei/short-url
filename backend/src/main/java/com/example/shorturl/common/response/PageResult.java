package com.example.shorturl.common.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果封装类
 * <p>
 * 模块职责：
 * - 统一分页查询的响应格式
 * - 包含数据列表、总数、页码等信息
 * <p>
 * 使用场景：
 * - 管理后台列表查询
 * - 用户数据分页展示
 * - 日志记录分页查询
 * <p>
 * 依赖关系：
 * - 被ApiResponse类包装使用
 * - 与MyBatis-Flex分页插件配合使用
 */
@Data
public class PageResult<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码
     */
    private int page;

    /**
     * 每页大小
     */
    private int size;

    /**
     * 总页数
     */
    private int totalPages;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = (int) Math.ceil((double) total / size);
    }

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        return new PageResult<>(list, total, page, size);
    }

    /**
     * 创建空的分页结果
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), 0, page, size);
    }

    /**
     * 判断是否有下一页
     */
    public boolean hasNext() {
        return page < totalPages;
    }

    /**
     * 判断是否有上一页
     */
    public boolean hasPrevious() {
        return page > 1;
    }

    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return list == null || list.isEmpty();
    }

    /**
     * 判断是否不为空
     */
    public boolean isNotEmpty() {
        return !isEmpty();
    }
}