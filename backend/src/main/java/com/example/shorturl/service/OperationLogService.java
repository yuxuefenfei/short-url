package com.example.shorturl.service;

import com.example.shorturl.common.response.PageResult;
import com.example.shorturl.dao.OperationLogDao;
import com.example.shorturl.model.entity.UserOperationLog;
import com.example.shorturl.model.entity.table.UserOperationLogTableDef;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;


    private final OperationLogDao operationLogDao;

    @Transactional(readOnly = true)
    public PageResult<UserOperationLog> getOperationLogs(Integer page, Integer size, String keyword,
                                                         String module, String operationType, Integer status,
                                                         LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper queryWrapper = buildQuery(keyword, module, operationType, status, startTime, endTime);
        queryWrapper.orderBy(UserOperationLogTableDef.USER_OPERATION_LOG.OPERATION_TIME, false);

        List<UserOperationLog> records = operationLogDao.selectListByQuery(queryWrapper);
        long total = operationLogDao.selectCountByQuery(
                buildQuery(keyword, module, operationType, status, startTime, endTime)
        );

        return PageResult.of(paginate(records, page, size), total, safePage(page), safeSize(size));
    }

    @Transactional(readOnly = true)
    public OperationLogStats getOperationStats() {
        long totalOperations = operationLogDao.selectCountByQuery(QueryWrapper.create());
        long successOperations = operationLogDao.selectCountByQuery(
                QueryWrapper.create().where(UserOperationLogTableDef.USER_OPERATION_LOG.STATUS.eq(1))
        );
        long failedOperations = operationLogDao.selectCountByQuery(
                QueryWrapper.create().where(UserOperationLogTableDef.USER_OPERATION_LOG.STATUS.eq(0))
        );
        long todayOperations = operationLogDao.selectCountByQuery(
                QueryWrapper.create().where(UserOperationLogTableDef.USER_OPERATION_LOG.OPERATION_TIME.ge(LocalDate.now().atStartOfDay()))
        );

        List<UserOperationLog> allLogs = operationLogDao.selectListByQuery(QueryWrapper.create());
        long activeUsers = allLogs.stream()
                .map(UserOperationLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        OperationLogStats stats = new OperationLogStats();
        stats.setTotalOperations(totalOperations);
        stats.setSuccessOperations(successOperations);
        stats.setFailedOperations(failedOperations);
        stats.setTodayOperations(todayOperations);
        stats.setActiveUsers(activeUsers);
        return stats;
    }

    private QueryWrapper buildQuery(String keyword, String module, String operationType, Integer status,
                                    LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper queryWrapper = QueryWrapper.create();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.where(UserOperationLogTableDef.USER_OPERATION_LOG.OPERATION_DESC.like(keyword))
                    .or(UserOperationLogTableDef.USER_OPERATION_LOG.MODULE.like(keyword))
                    .or(UserOperationLogTableDef.USER_OPERATION_LOG.OPERATION_TYPE.like(keyword))
                    .or(UserOperationLogTableDef.USER_OPERATION_LOG.IP_ADDRESS.like(keyword));
        }

        if (StringUtils.hasText(module)) {
            queryWrapper.and(UserOperationLogTableDef.USER_OPERATION_LOG.MODULE.eq(module));
        }

        if (StringUtils.hasText(operationType)) {
            queryWrapper.and(UserOperationLogTableDef.USER_OPERATION_LOG.OPERATION_TYPE.eq(operationType));
        }

        if (status != null) {
            queryWrapper.and(UserOperationLogTableDef.USER_OPERATION_LOG.STATUS.eq(status));
        }

        if (startTime != null) {
            queryWrapper.and(UserOperationLogTableDef.USER_OPERATION_LOG.OPERATION_TIME.ge(startTime));
        }

        if (endTime != null) {
            queryWrapper.and(UserOperationLogTableDef.USER_OPERATION_LOG.OPERATION_TIME.le(endTime));
        }

        return queryWrapper;
    }

    private List<UserOperationLog> paginate(List<UserOperationLog> records, Integer page, Integer size) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        int safePage = safePage(page);
        int safeSize = safeSize(size);
        int fromIndex = Math.max((safePage - 1) * safeSize, 0);
        if (fromIndex >= records.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + safeSize, records.size());
        return records.subList(fromIndex, toIndex);
    }

    private int safePage(Integer page) {
        return Optional.ofNullable(page)
                .filter(value -> value > 0)
                .orElse(DEFAULT_PAGE);
    }

    private int safeSize(Integer size) {
        return Optional.ofNullable(size)
                .filter(value -> value > 0)
                .orElse(DEFAULT_PAGE_SIZE);
    }

    @Data
    public static class OperationLogStats {
        private long totalOperations;
        private long successOperations;
        private long failedOperations;
        private long activeUsers;
        private long todayOperations;
    }
}
