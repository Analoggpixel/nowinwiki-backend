package com.analoggpixel.nowinwiki.service.impl;

import com.analoggpixel.nowinwiki.common.exception.BusinessException;
import com.analoggpixel.nowinwiki.common.util.SyncTimeUtils;
import com.analoggpixel.nowinwiki.entity.UserPreference;
import com.analoggpixel.nowinwiki.mapper.UserPreferenceMapper;
import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.sync.PreferencePushRequest;
import com.analoggpixel.nowinwiki.parameter.sync.PreferencePushResponse;
import com.analoggpixel.nowinwiki.parameter.sync.PreferenceSnapshotVO;
import com.analoggpixel.nowinwiki.parameter.sync.UserPreferenceSyncDTO;
import com.analoggpixel.nowinwiki.service.PreferenceSyncService;
import com.analoggpixel.nowinwiki.service.PushIdempotencyService;
import com.analoggpixel.nowinwiki.support.AuthContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PreferenceSyncServiceImpl implements PreferenceSyncService {

    private final UserPreferenceMapper userPreferenceMapper;
    private final PushIdempotencyService pushIdempotencyService;

    @Override
    public Result<PreferenceSnapshotVO> snapshot() {
        return Result.ok(buildSnapshot(AuthContext.requireUserId(), null));
    }

    @Override
    public Result<PreferenceSnapshotVO> pull(String since) {
        if (!StringUtils.hasText(since)) {
            throw new BusinessException("since 参数不能为空");
        }
        return Result.ok(buildSnapshot(AuthContext.requireUserId(), SyncTimeUtils.parse(since)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<PreferencePushResponse> push(PreferencePushRequest request, String requestIdHeader) {
        Long userId = AuthContext.requireUserId();
        String requestId = StringUtils.hasText(request.getRequestId())
                ? request.getRequestId()
                : requestIdHeader;
        pushIdempotencyService.ensureFirstSeen(userId, "preferences", requestId);

        UserPreferenceSyncDTO dto = request.getPreference();
        if (dto == null) {
            throw new BusinessException("preference 不能为空");
        }

        LocalDateTime incomingUpdatedAt = SyncTimeUtils.parse(dto.getUpdatedAt());
        UserPreference existing = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId)
        );

        boolean applied = false;
        boolean skipped = false;
        if (existing != null && existing.getUpdatedAt() != null
                && !incomingUpdatedAt.isAfter(existing.getUpdatedAt())) {
            skipped = true;
        } else {
            UserPreference row = existing != null ? existing : new UserPreference();
            row.setUserId(userId);
            row.setPayload(dto.getPayloadJson());
            row.setUpdatedAt(incomingUpdatedAt);
            row.setDeletedAt(dto.isDeleted() ? incomingUpdatedAt : null);
            if (existing == null) {
                userPreferenceMapper.insert(row);
            } else {
                userPreferenceMapper.update(
                        row,
                        new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId)
                );
            }
            applied = true;
        }

        PreferencePushResponse response = new PreferencePushResponse();
        response.setServerTime(SyncTimeUtils.nowString());
        response.setApplied(applied);
        response.setSkipped(skipped);
        return Result.ok(response);
    }

    private PreferenceSnapshotVO buildSnapshot(Long userId, LocalDateTime since) {
        UserPreference row = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId)
                        .and(since != null, wrapper -> wrapper
                                .gt(UserPreference::getUpdatedAt, since)
                                .or()
                                .gt(UserPreference::getDeletedAt, since)
                        )
        );
        PreferenceSnapshotVO snapshot = new PreferenceSnapshotVO();
        snapshot.setServerTime(SyncTimeUtils.nowString());
        if (row != null) {
            snapshot.setPreference(toDto(row));
        }
        return snapshot;
    }

    private UserPreferenceSyncDTO toDto(UserPreference row) {
        UserPreferenceSyncDTO dto = new UserPreferenceSyncDTO();
        dto.setPayloadJson(row.getPayload());
        dto.setUpdatedAt(SyncTimeUtils.format(row.getUpdatedAt()));
        dto.setDeleted(row.getDeletedAt() != null);
        return dto;
    }
}
