package com.analoggpixel.nowinwiki.service.impl;

import com.analoggpixel.nowinwiki.common.exception.BusinessException;
import com.analoggpixel.nowinwiki.common.id.SnowflakeIdGenerator;
import com.analoggpixel.nowinwiki.common.util.SyncTimeUtils;
import com.analoggpixel.nowinwiki.entity.Bookmark;
import com.analoggpixel.nowinwiki.entity.BookmarkFolder;
import com.analoggpixel.nowinwiki.mapper.BookmarkFolderMapper;
import com.analoggpixel.nowinwiki.mapper.BookmarkMapper;
import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.sync.BookmarkFolderSyncDTO;
import com.analoggpixel.nowinwiki.parameter.sync.BookmarkPushRequest;
import com.analoggpixel.nowinwiki.parameter.sync.BookmarkPushResponse;
import com.analoggpixel.nowinwiki.parameter.sync.BookmarkSnapshotVO;
import com.analoggpixel.nowinwiki.parameter.sync.BookmarkSyncDTO;
import com.analoggpixel.nowinwiki.service.BookmarkSyncService;
import com.analoggpixel.nowinwiki.service.PushIdempotencyService;
import com.analoggpixel.nowinwiki.support.AuthContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookmarkSyncServiceImpl implements BookmarkSyncService {

    private final BookmarkFolderMapper bookmarkFolderMapper;
    private final BookmarkMapper bookmarkMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PushIdempotencyService pushIdempotencyService;

    @Override
    public Result<BookmarkSnapshotVO> snapshot() {
        Long userId = AuthContext.requireUserId();
        return Result.ok(buildSnapshot(userId, null));
    }

    @Override
    public Result<BookmarkSnapshotVO> pull(String since) {
        Long userId = AuthContext.requireUserId();
        LocalDateTime sinceTime = parseSince(since);
        return Result.ok(buildSnapshot(userId, sinceTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BookmarkPushResponse> push(BookmarkPushRequest request, String requestIdHeader) {
        Long userId = AuthContext.requireUserId();
        String requestId = StringUtils.hasText(request.getRequestId())
                ? request.getRequestId()
                : requestIdHeader;
        pushIdempotencyService.ensureFirstSeen(userId, "bookmarks", requestId);

        Map<Long, Long> folderMappings = new HashMap<>();
        int foldersApplied = 0;
        int bookmarksApplied = 0;
        int skipped = 0;

        for (BookmarkFolderSyncDTO folderDto : request.getFolders()) {
            Long serverId = upsertFolder(userId, folderDto, folderMappings);
            if (serverId != null) {
                foldersApplied++;
            } else {
                skipped++;
            }
        }

        for (BookmarkSyncDTO bookmarkDto : request.getBookmarks()) {
            if (upsertBookmark(userId, bookmarkDto, folderMappings)) {
                bookmarksApplied++;
            } else {
                skipped++;
            }
        }

        BookmarkPushResponse response = new BookmarkPushResponse();
        response.setServerTime(SyncTimeUtils.nowString());
        folderMappings.forEach((clientId, serverId) ->
                response.getFolderIdMappings().put(String.valueOf(clientId), serverId)
        );
        response.setFoldersApplied(foldersApplied);
        response.setBookmarksApplied(bookmarksApplied);
        response.setSkipped(skipped);
        return Result.ok(response);
    }

    private BookmarkSnapshotVO buildSnapshot(Long userId, LocalDateTime since) {
        List<BookmarkFolder> folders = bookmarkFolderMapper.selectList(
                new LambdaQueryWrapper<BookmarkFolder>()
                        .eq(BookmarkFolder::getUserId, userId)
                        .and(since != null, wrapper -> wrapper
                                .gt(BookmarkFolder::getUpdatedAt, since)
                                .or()
                                .gt(BookmarkFolder::getDeletedAt, since)
                        )
                        .orderByAsc(BookmarkFolder::getSortOrder)
        );

        List<Bookmark> bookmarks = bookmarkMapper.selectList(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .and(since != null, wrapper -> wrapper
                                .gt(Bookmark::getUpdatedAt, since)
                                .or()
                                .gt(Bookmark::getDeletedAt, since)
                        )
                        .orderByDesc(Bookmark::getBookmarkedAt)
        );

        BookmarkSnapshotVO snapshot = new BookmarkSnapshotVO();
        snapshot.setServerTime(SyncTimeUtils.nowString());
        folders.forEach(folder -> snapshot.getFolders().add(toFolderDto(folder)));
        bookmarks.forEach(bookmark -> snapshot.getBookmarks().add(toBookmarkDto(bookmark)));
        return snapshot;
    }

    private Long upsertFolder(
            Long userId,
            BookmarkFolderSyncDTO dto,
            Map<Long, Long> folderMappings
    ) {
        LocalDateTime incomingUpdatedAt = SyncTimeUtils.parse(dto.getUpdatedAt());
        BookmarkFolder existing = null;
        if (dto.getId() != null) {
            existing = bookmarkFolderMapper.selectOne(
                    new LambdaQueryWrapper<BookmarkFolder>()
                            .eq(BookmarkFolder::getId, dto.getId())
                            .eq(BookmarkFolder::getUserId, userId)
            );
        }

        if (existing != null && Boolean.TRUE.equals(existing.getIsDefault()) && dto.isDeleted()) {
            throw new BusinessException("默认收藏夹不可删除");
        }

        if (existing != null && !isIncomingNewer(incomingUpdatedAt, existing.getUpdatedAt())) {
            if (dto.getClientId() != null) {
                folderMappings.put(dto.getClientId(), existing.getId());
            }
            return null;
        }

        BookmarkFolder folder = existing != null ? existing : new BookmarkFolder();
        if (existing == null) {
            folder.setId(snowflakeIdGenerator.nextId());
            folder.setUserId(userId);
            // Only the server creates the default folder at registration.
            folder.setIsDefault(false);
        }
        folder.setName(dto.getName());
        folder.setDescription(dto.getDescription());
        folder.setSortOrder(dto.getSortOrder());
        folder.setUpdatedAt(incomingUpdatedAt);
        folder.setDeletedAt(dto.isDeleted() ? incomingUpdatedAt : null);

        if (existing == null) {
            bookmarkFolderMapper.insert(folder);
        } else {
            bookmarkFolderMapper.updateById(folder);
        }

        if (dto.getClientId() != null) {
            folderMappings.put(dto.getClientId(), folder.getId());
        }
        return folder.getId();
    }

    private boolean upsertBookmark(
            Long userId,
            BookmarkSyncDTO dto,
            Map<Long, Long> folderMappings
    ) {
        Long folderId = resolveFolderId(dto, folderMappings);
        if (folderId == null) {
            throw new BusinessException("书签缺少有效的 folderId");
        }

        BookmarkFolder folder = bookmarkFolderMapper.selectOne(
                new LambdaQueryWrapper<BookmarkFolder>()
                        .eq(BookmarkFolder::getId, folderId)
                        .eq(BookmarkFolder::getUserId, userId)
        );
        if (folder == null) {
            throw new BusinessException("收藏夹不存在: " + folderId);
        }

        LocalDateTime incomingUpdatedAt = SyncTimeUtils.parse(dto.getUpdatedAt());
        Bookmark existing = bookmarkMapper.selectOne(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .eq(Bookmark::getFolderId, folderId)
                        .eq(Bookmark::getLanguage, dto.getLanguage())
                        .eq(Bookmark::getTitle, dto.getTitle())
        );

        if (existing != null && !isIncomingNewer(incomingUpdatedAt, existing.getUpdatedAt())) {
            return false;
        }

        Bookmark bookmark = existing != null ? existing : new Bookmark();
        if (existing == null) {
            bookmark.setId(snowflakeIdGenerator.nextId());
            bookmark.setUserId(userId);
        }
        bookmark.setFolderId(folderId);
        bookmark.setTitle(dto.getTitle());
        bookmark.setLanguage(dto.getLanguage());
        bookmark.setBookmarkedAt(dto.getBookmarkedAt());
        bookmark.setDescription(dto.getDescription());
        bookmark.setThumbnailUrl(dto.getThumbnailUrl());
        bookmark.setUpdatedAt(incomingUpdatedAt);
        bookmark.setDeletedAt(dto.isDeleted() ? incomingUpdatedAt : null);

        if (existing == null) {
            bookmarkMapper.insert(bookmark);
        } else {
            bookmarkMapper.updateById(bookmark);
        }
        return true;
    }

    private Long resolveFolderId(BookmarkSyncDTO dto, Map<Long, Long> folderMappings) {
        if (dto.getFolderId() != null) {
            return dto.getFolderId();
        }
        if (dto.getClientFolderId() != null) {
            return folderMappings.get(dto.getClientFolderId());
        }
        return null;
    }

    private boolean isIncomingNewer(LocalDateTime incoming, LocalDateTime existing) {
        return existing == null || incoming.isAfter(existing);
    }

    private LocalDateTime parseSince(String since) {
        if (!StringUtils.hasText(since)) {
            throw new BusinessException("since 参数不能为空");
        }
        return SyncTimeUtils.parse(since);
    }

    private BookmarkFolderSyncDTO toFolderDto(BookmarkFolder folder) {
        BookmarkFolderSyncDTO dto = new BookmarkFolderSyncDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        dto.setDescription(folder.getDescription());
        dto.setSortOrder(folder.getSortOrder());
        dto.setIsDefault(Boolean.TRUE.equals(folder.getIsDefault()));
        dto.setUpdatedAt(SyncTimeUtils.format(folder.getUpdatedAt()));
        dto.setDeleted(folder.getDeletedAt() != null);
        return dto;
    }

    private BookmarkSyncDTO toBookmarkDto(Bookmark bookmark) {
        BookmarkSyncDTO dto = new BookmarkSyncDTO();
        dto.setId(bookmark.getId());
        dto.setFolderId(bookmark.getFolderId());
        dto.setTitle(bookmark.getTitle());
        dto.setLanguage(bookmark.getLanguage());
        dto.setBookmarkedAt(bookmark.getBookmarkedAt());
        dto.setDescription(bookmark.getDescription());
        dto.setThumbnailUrl(bookmark.getThumbnailUrl());
        dto.setUpdatedAt(SyncTimeUtils.format(bookmark.getUpdatedAt()));
        dto.setDeleted(bookmark.getDeletedAt() != null);
        return dto;
    }
}
