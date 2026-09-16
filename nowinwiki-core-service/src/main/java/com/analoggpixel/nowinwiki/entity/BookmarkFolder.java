package com.analoggpixel.nowinwiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bookmark_folder")
public class BookmarkFolder {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long userId;

    private String name;

    private String description;

    private Integer sortOrder;

    /** 1 = system default folder created at registration; cannot be deleted. */
    private Boolean isDefault;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
