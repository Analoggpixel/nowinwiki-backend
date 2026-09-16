package com.analoggpixel.nowinwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bookmark")
public class Bookmark {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long userId;

    private Long folderId;

    private String title;

    private String language;

    private Long bookmarkedAt;

    private String description;

    private String thumbnailUrl;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
