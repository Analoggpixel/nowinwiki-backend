package com.analoggpixel.nowinwiki.service.impl;

import com.analoggpixel.nowinwiki.common.exception.BusinessException;
import com.analoggpixel.nowinwiki.common.util.SyncTimeUtils;
import com.analoggpixel.nowinwiki.config.SyncProperties;
import com.analoggpixel.nowinwiki.entity.ReadHistory;
import com.analoggpixel.nowinwiki.mapper.ReadHistoryMapper;
import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.sync.HistoryPushRequest;
import com.analoggpixel.nowinwiki.parameter.sync.HistoryPushResponse;
import com.analoggpixel.nowinwiki.parameter.sync.HistorySnapshotVO;
import com.analoggpixel.nowinwiki.parameter.sync.ReadHistorySyncDTO;
import com.analoggpixel.nowinwiki.service.HistorySyncService;
import com.analoggpixel.nowinwiki.service.PushIdempotencyService;
import com.analoggpixel.nowinwiki.support.AuthContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistorySyncServiceImpl implements HistorySyncService {

    private final ReadHistoryMapper readHistoryMapper;
    private final PushIdempotencyService pushIdempotencyService;
    private final SyncProperties syncProperties;

    @Override
    public Result<HistorySnapshotVO> snapshot() {
        Long userId = AuthContext.requireUserId();
        return Result.ok(buildSnapshot(userId, null));
    }

    @Override
    public Result<HistorySnapshotVO> pull(String since) {
        Long userId = AuthContext.requireUserId();
        if (!StringUtils.hasText(since)) {
            throw new BusinessException("since 参数不能为空");
        }
        return Result.ok(buildSnapshot(userId, SyncTimeUtils.parse(since)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<HistoryPushResponse> push(HistoryPushRequest request, String requestIdHeader) {
        Long userId = AuthContext.requireUserId();
        String requestId = StringUtils.hasText(request.getRequestId())
                ? request.getRequestId()
                : requestIdHeader;
        pushIdempotencyService.ensureFirstSeen(userId, "history", requestId);

        int applied = 0;
        int skipped = 0;
        for (ReadHistorySyncDTO item : request.getItems()) {
            if (upsertHistory(userId, item)) {
                applied++;
            } else {
                skipped++;
            }
        }
        int trimmed = trimHistory(userId);

        HistoryPushResponse response = new HistoryPushResponse();
        response.setServerTime(SyncTimeUtils.nowString());
        response.setItemsApplied(applied);
        response.setSkipped(skipped);
        response.setTrimmed(trimmed);
        return Result.ok(response);
    }

    private HistorySnapshotVO buildSnapshot(Long userId, LocalDateTime since) {
        List<ReadHistory> rows = readHistoryMapper.selectList(
                new LambdaQueryWrapper<ReadHistory>()
                        .eq(ReadHistory::getUserId, userId)
                        .and(since != null, wrapper -> wrapper
                                .gt(ReadHistory::getUpdatedAt, since)
                                .or()
                                .gt(ReadHistory::getDeletedAt, since)
                        )
                        .orderByDesc(ReadHistory::getViewedAt)
                        .last("LIMIT " + syncProperties.getHistoryMaxRows())
        );

        HistorySnapshotVO snapshot = new HistorySnapshotVO();
        snapshot.setServerTime(SyncTimeUtils.nowString());
        rows.forEach(row -> snapshot.getItems().add(toDto(row)));
        return snapshot;
    }

    private boolean upsertHistory(Long userId, ReadHistorySyncDTO dto) {
        LocalDateTime incomingUpdatedAt = SyncTimeUtils.parse(dto.getUpdatedAt());
        ReadHistory existing = readHistoryMapper.selectOne(
                new LambdaQueryWrapper<ReadHistory>()
                        .eq(ReadHistory::getUserId, userId)
                        .eq(ReadHistory::getLanguage, dto.getLanguage())
                        .eq(ReadHistory::getTitle, dto.getTitle())
        );

        if (existing != null && existing.getUpdatedAt() != null
                && !incomingUpdatedAt.isAfter(existing.getUpdatedAt())) {
            return false;
        }

        ReadHistory row = existing != null ? existing : new ReadHistory();
        row.setUserId(userId);
        row.setLanguage(dto.getLanguage());
        row.setTitle(dto.getTitle());
        row.setViewedAt(dto.getViewedAt());
        row.setDescription(dto.getDescription());
        row.setThumbnailUrl(dto.getThumbnailUrl());
        row.setUpdatedAt(incomingUpdatedAt);
        row.setDeletedAt(dto.isDeleted() ? incomingUpdatedAt : null);

        if (existing == null) {
            readHistoryMapper.insert(row);
        } else {
            readHistoryMapper.update(
                    row,
                    new LambdaQueryWrapper<ReadHistory>()
                            .eq(ReadHistory::getUserId, userId)
                            .eq(ReadHistory::getLanguage, dto.getLanguage())
                            .eq(ReadHistory::getTitle, dto.getTitle())
            );
        }
        return true;
    }

    private int trimHistory(Long userId) {
        Long count = readHistoryMapper.selectCount(
                new LambdaQueryWrapper<ReadHistory>()
                        .eq(ReadHistory::getUserId, userId)
                        .isNull(ReadHistory::getDeletedAt)
        );
        int maxRows = syncProperties.getHistoryMaxRows();
        if (count == null || count <= maxRows) {
            return 0;
        }
        int toDelete = (int) (count - maxRows);
        List<ReadHistory> oldest = readHistoryMapper.selectList(
                new LambdaQueryWrapper<ReadHistory>()
                        .eq(ReadHistory::getUserId, userId)
                        .isNull(ReadHistory::getDeletedAt)
                        .orderByAsc(ReadHistory::getViewedAt)
                        .last("LIMIT " + toDelete)
        );
        for (ReadHistory row : oldest) {
            readHistoryMapper.delete(
                    new LambdaQueryWrapper<ReadHistory>()
                            .eq(ReadHistory::getUserId, userId)
                            .eq(ReadHistory::getLanguage, row.getLanguage())
                            .eq(ReadHistory::getTitle, row.getTitle())
            );
        }
        return oldest.size();
    }

    private ReadHistorySyncDTO toDto(ReadHistory row) {
        ReadHistorySyncDTO dto = new ReadHistorySyncDTO();
        dto.setTitle(row.getTitle());
        dto.setLanguage(row.getLanguage());
        dto.setViewedAt(row.getViewedAt());
        dto.setDescription(row.getDescription());
        dto.setThumbnailUrl(row.getThumbnailUrl());
        dto.setUpdatedAt(SyncTimeUtils.format(row.getUpdatedAt()));
        dto.setDeleted(row.getDeletedAt() != null);
        return dto;
    }
}
