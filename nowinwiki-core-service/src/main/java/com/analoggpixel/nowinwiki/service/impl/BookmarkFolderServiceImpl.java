package com.analoggpixel.nowinwiki.service.impl;

import com.analoggpixel.nowinwiki.common.exception.BusinessException;
import com.analoggpixel.nowinwiki.common.id.SnowflakeIdGenerator;
import com.analoggpixel.nowinwiki.common.util.SyncTimeUtils;
import com.analoggpixel.nowinwiki.entity.Bookmark;
import com.analoggpixel.nowinwiki.entity.BookmarkFolder;
import com.analoggpixel.nowinwiki.mapper.BookmarkFolderMapper;
import com.analoggpixel.nowinwiki.mapper.BookmarkMapper;
import com.analoggpixel.nowinwiki.parameter.dto.Result;
import com.analoggpixel.nowinwiki.parameter.folder.FolderCreateRequest;
import com.analoggpixel.nowinwiki.parameter.folder.FolderPatchRequest;
import com.analoggpixel.nowinwiki.parameter.folder.FolderVO;
import com.analoggpixel.nowinwiki.service.BookmarkFolderService;
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
public class BookmarkFolderServiceImpl implements BookmarkFolderService {

    private final BookmarkFolderMapper bookmarkFolderMapper;
    private final BookmarkMapper bookmarkMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<FolderVO> create(FolderCreateRequest request) {
        Long userId = AuthContext.requireUserId();
        String name = request.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("收藏夹名称不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        BookmarkFolder folder = new BookmarkFolder();
        folder.setId(snowflakeIdGenerator.nextId());
        folder.setUserId(userId);
        folder.setName(name);
        folder.setDescription(blankToNull(request.getDescription()));
        folder.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : nextSortOrder(userId));
        folder.setIsDefault(false);
        folder.setUpdatedAt(now);
        folder.setDeletedAt(null);
        bookmarkFolderMapper.insert(folder);
        return Result.ok(toVo(folder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<FolderVO> update(Long folderId, FolderPatchRequest request) {
        Long userId = AuthContext.requireUserId();
        BookmarkFolder folder = requireActiveFolder(userId, folderId);

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (!StringUtils.hasText(name)) {
                throw new BusinessException("收藏夹名称不能为空");
            }
            folder.setName(name);
        }
        if (request.getDescription() != null) {
            folder.setDescription(blankToNull(request.getDescription()));
        }
        if (request.getSortOrder() != null) {
            folder.setSortOrder(request.getSortOrder());
        }
        folder.setUpdatedAt(LocalDateTime.now());
        bookmarkFolderMapper.updateById(folder);
        return Result.ok(toVo(folder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<FolderVO> delete(Long folderId) {
        Long userId = AuthContext.requireUserId();
        BookmarkFolder folder = requireActiveFolder(userId, folderId);
        if (Boolean.TRUE.equals(folder.getIsDefault())) {
            throw new BusinessException("默认收藏夹不可删除");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Bookmark> bookmarks = bookmarkMapper.selectList(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .eq(Bookmark::getFolderId, folderId)
                        .isNull(Bookmark::getDeletedAt)
        );
        for (Bookmark bookmark : bookmarks) {
            bookmark.setDeletedAt(now);
            bookmark.setUpdatedAt(now);
            bookmarkMapper.updateById(bookmark);
        }

        folder.setDeletedAt(now);
        folder.setUpdatedAt(now);
        bookmarkFolderMapper.updateById(folder);
        return Result.ok(toVo(folder));
    }

    private int nextSortOrder(Long userId) {
        Long count = bookmarkFolderMapper.selectCount(
                new LambdaQueryWrapper<BookmarkFolder>()
                        .eq(BookmarkFolder::getUserId, userId)
                        .isNull(BookmarkFolder::getDeletedAt)
        );
        return count == null ? 0 : count.intValue();
    }

    private BookmarkFolder requireActiveFolder(Long userId, Long folderId) {
        BookmarkFolder folder = bookmarkFolderMapper.selectOne(
                new LambdaQueryWrapper<BookmarkFolder>()
                        .eq(BookmarkFolder::getId, folderId)
                        .eq(BookmarkFolder::getUserId, userId)
        );
        if (folder == null || folder.getDeletedAt() != null) {
            throw new BusinessException("收藏夹不存在");
        }
        return folder;
    }

    private FolderVO toVo(BookmarkFolder folder) {
        FolderVO vo = new FolderVO();
        vo.setId(folder.getId());
        vo.setName(folder.getName());
        vo.setDescription(folder.getDescription());
        vo.setSortOrder(folder.getSortOrder());
        vo.setIsDefault(Boolean.TRUE.equals(folder.getIsDefault()));
        vo.setUpdatedAt(SyncTimeUtils.format(folder.getUpdatedAt()));
        vo.setDeleted(folder.getDeletedAt() != null);
        return vo;
    }

    private static String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
