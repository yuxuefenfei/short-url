package com.example.shorturl.common.utils;

import com.example.shorturl.config.AppConfig;
import com.mybatisflex.core.paginate.Page;

import java.util.List;
import java.util.Optional;

public final class PageUtils {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 200;

    private PageUtils() {
    }

    public static int safePage(Integer page) {
        return Optional.ofNullable(page)
                .filter(value -> value > 0)
                .orElse(DEFAULT_PAGE);
    }

    public static int safePage(Integer page, AppConfig.Pagination pagination) {
        return Optional.ofNullable(page)
                .filter(value -> value > 0)
                .orElse(pagination.getDefaultPage());
    }

    public static int safeSize(Integer size) {
        return Optional.ofNullable(size)
                .filter(value -> value > 0)
                .map(value -> Math.min(value, MAX_PAGE_SIZE))
                .orElse(DEFAULT_PAGE_SIZE);
    }

    public static int safeSize(Integer size, AppConfig.Pagination pagination) {
        return Optional.ofNullable(size)
                .filter(value -> value > 0)
                .map(value -> Math.min(value, pagination.getMaxSize()))
                .orElse(pagination.getDefaultSize());
    }

    public static <T> List<T> records(Page<T> page) {
        return page == null || page.getRecords() == null ? List.of() : page.getRecords();
    }
}
