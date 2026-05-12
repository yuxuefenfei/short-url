package com.example.shorturl.service;

import com.example.shorturl.common.response.PageResult;
import com.example.shorturl.common.utils.PageUtils;
import com.example.shorturl.config.AppConfig;
import com.example.shorturl.dao.OperationLogDao;
import com.example.shorturl.model.entity.UserOperationLog;
import com.example.shorturl.model.entity.table.UserOperationLogTableDef;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.mybatisflex.core.query.QueryMethods.count;
import static com.mybatisflex.core.query.QueryMethods.distinct;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final OperationLogDao operationLogDao;

    private final AppConfig appConfig;

    @Transactional(readOnly = true)
    public PageResult<UserOperationLog> getOperationLogs(Integer page, Integer size, String keyword,
                                                         String module, String operationType, Integer status,
                                                         LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper queryWrapper = buildQuery(keyword, module, operationType, status, startTime, endTime);
        queryWrapper.orderBy(UserOperationLogTableDef.USER_OPERATION_LOG.OPERATION_TIME, false);

        int safePage = PageUtils.safePage(page, appConfig.getPagination());
        int safeSize = PageUtils.safeSize(size, appConfig.getPagination());
        Page<UserOperationLog> records = operationLogDao.paginate(safePage, safeSize, queryWrapper);

        return PageResult.of(PageUtils.records(records), records.getTotalRow(), safePage, safeSize);
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

        Long activeUsers = operationLogDao.selectObjectByQueryAs(
                QueryWrapper.create()
                        .select(count(distinct(UserOperationLogTableDef.USER_OPERATION_LOG.USER_ID)))
                        .where(UserOperationLogTableDef.USER_OPERATION_LOG.USER_ID.isNotNull()),
                Long.class
        );

        OperationLogStats stats = new OperationLogStats();
        stats.setTotalOperations(totalOperations);
        stats.setSuccessOperations(successOperations);
        stats.setFailedOperations(failedOperations);
        stats.setTodayOperations(todayOperations);
        stats.setActiveUsers(activeUsers == null ? 0L : activeUsers);
        return stats;
    }

    private QueryWrapper buildQuery(String keyword, String module, String operationType, Integer status,
                                    LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper queryWrapper = QueryWrapper.create();

        if (StringUtils.hasText(keyword)) {
            queryWrapper.where(UserOperationLogTableDef.USER_OPERATION_LOG.OPERATION_DESC.like(keyword)
                    .or(UserOperationLogTableDef.USER_OPERATION_LOG.MODULE.like(keyword))
                    .or(UserOperationLogTableDef.USER_OPERATION_LOG.OPERATION_TYPE.like(keyword))
                    .or(UserOperationLogTableDef.USER_OPERATION_LOG.IP_ADDRESS.like(keyword)));
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

    @Data
    public static class OperationLogStats {
        private long totalOperations;
        private long successOperations;
        private long failedOperations;
        private long activeUsers;
        private long todayOperations;
    }
}
